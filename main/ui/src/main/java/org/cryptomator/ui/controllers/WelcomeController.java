/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.settings.Localization;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.util.ApplicationVersion;
import org.cryptomator.ui.util.AsyncTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

@Singleton
public class WelcomeController extends LocalizedFXMLViewController {

	private static final Logger LOG = LoggerFactory.getLogger(WelcomeController.class);

	private final Application app;
	private final Settings settings;
	private final Comparator<String> semVerComparator;
	private final AsyncTaskService asyncTaskService;

	@Inject
	public WelcomeController(Application app, Localization localization, Settings settings, @Named("SemVer") Comparator<String> semVerComparator, AsyncTaskService asyncTaskService) {
		super(localization);
		this.app = app;
		this.settings = settings;
		this.semVerComparator = semVerComparator;
		this.asyncTaskService = asyncTaskService;
	}

	@FXML
	private Node checkForUpdatesContainer;

	@FXML
	private Label checkForUpdatesStatus;

	@FXML
	private ProgressIndicator checkForUpdatesIndicator;

	@FXML
	private Hyperlink updateLink;

	@Override
	public void initialize() {
		if (areUpdatesManagedExternally()) {
			checkForUpdatesContainer.setVisible(false);
		} else if (settings.checkForUpdates().get()) {
			this.checkForUpdates();
		}
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/welcome.fxml");
	}

	// ****************************************
	// Check for updates
	// ****************************************

	private boolean areUpdatesManagedExternally() {
		return Boolean.parseBoolean(System.getProperty("cryptomator.updatesManagedExternally", "false"));
	}

	private void checkForUpdates() {
		checkForUpdatesStatus.setText(localization.getString("welcome.checkForUpdates.label.currentlyChecking"));
		checkForUpdatesIndicator.setVisible(true);
		asyncTaskService.asyncTaskOf(() -> {
			final HttpClient client = new HttpClient();
			final HttpMethod method = new GetMethod("https://cryptomator.org/downloads/latestVersion.json");
			client.getParams().setParameter(HttpClientParams.USER_AGENT, "Cryptomator VersionChecker/" + ApplicationVersion.orElse("SNAPSHOT"));
			client.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
			client.getParams().setConnectionManagerTimeout(5000);
			client.executeMethod(method);
			final InputStream responseBodyStream = method.getResponseBodyAsStream();
			if (method.getStatusCode() == HttpStatus.SC_OK && responseBodyStream != null) {
				Gson gson = new GsonBuilder().setLenient().create();
				Reader utf8Reader = new InputStreamReader(responseBodyStream, StandardCharsets.UTF_8);
				Map<String, String> map = gson.fromJson(utf8Reader, new TypeToken<Map<String, String>>() {
				}.getType());
				if (map != null) {
					this.compareVersions(map);
				}
			}
		}).andFinally(() -> {
			checkForUpdatesStatus.setText("");
			checkForUpdatesIndicator.setVisible(false);
		}).run();
	}

	private void compareVersions(final Map<String, String> latestVersions) {
		final String latestVersion;
		if (SystemUtils.IS_OS_MAC_OSX) {
			latestVersion = latestVersions.get("mac");
		} else if (SystemUtils.IS_OS_WINDOWS) {
			latestVersion = latestVersions.get("win");
		} else if (SystemUtils.IS_OS_LINUX) {
			latestVersion = latestVersions.get("linux");
		} else {
			// no version check possible on unsupported OS
			return;
		}
		final String currentVersion = ApplicationVersion.orElse(null);
		LOG.debug("Current version: {}, lastest version: {}", currentVersion, latestVersion);
		if (currentVersion != null && semVerComparator.compare(currentVersion, latestVersion) < 0) {
			final String msg = String.format(localization.getString("welcome.newVersionMessage"), latestVersion, currentVersion);
			Platform.runLater(() -> {
				this.updateLink.setText(msg);
				this.updateLink.setVisible(true);
				this.updateLink.setDisable(false);
			});
		}
	}

	@FXML
	public void didClickUpdateLink(ActionEvent event) {
		app.getHostServices().showDocument("https://cryptomator.org/");
	}

}
