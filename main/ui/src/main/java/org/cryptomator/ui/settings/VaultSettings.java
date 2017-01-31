/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.settings;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.fxmisc.easybind.EasyBind;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class VaultSettings {

	private final String id;
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	private final StringProperty mountName = new SimpleStringProperty();
	private final StringProperty winDriveLetter = new SimpleStringProperty();

	public VaultSettings(String id) {
		this.id = Objects.requireNonNull(id);

		EasyBind.subscribe(path, this::deriveMountNameFromPath);
		// TODO: automatically save settings, when chaning vaultSettings
	}

	private void deriveMountNameFromPath(Path path) {
		if (path != null && StringUtils.isBlank(mountName.get())) {
			mountName.set(normalizeMountName(path.getFileName().toString()));
		}
	}

	public static VaultSettings withRandomId() {
		return new VaultSettings(generateId());
	}

	private static String generateId() {
		return asBase64String(nineBytesFrom(UUID.randomUUID()));
	}

	private static String asBase64String(byte[] bytes) {
		byte[] base64Bytes = Base64.getUrlEncoder().encode(bytes);
		return new String(base64Bytes, StandardCharsets.US_ASCII);
	}

	private static byte[] nineBytesFrom(UUID uuid) {
		ByteBuffer uuidBuffer = ByteBuffer.allocate(9);
		uuidBuffer.putLong(uuid.getMostSignificantBits());
		uuidBuffer.put((byte) (uuid.getLeastSignificantBits() & 0xFF));
		uuidBuffer.flip();
		return uuidBuffer.array();
	}

	public static String normalizeMountName(String mountName) {
		String normalizedMountName = StringUtils.stripAccents(mountName);
		StringBuilder builder = new StringBuilder();
		for (char c : normalizedMountName.toCharArray()) {
			if (Character.isWhitespace(c)) {
				if (builder.length() == 0 || builder.charAt(builder.length() - 1) != '_') {
					builder.append('_');
				}
			} else if (c < 127 && Character.isLetterOrDigit(c)) {
				builder.append(c);
			} else {
				if (builder.length() == 0 || builder.charAt(builder.length() - 1) != '_') {
					builder.append('_');
				}
			}
		}
		return builder.toString();
	}

	/* Getter/Setter */

	public String getId() {
		return id;
	}

	public ObjectProperty<Path> path() {
		return path;
	}

	public StringProperty mountName() {
		return mountName;
	}

	public StringProperty winDriveLetter() {
		return winDriveLetter;
	}

	/* Hashcode/Equals */

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VaultSettings && obj.getClass().equals(this.getClass())) {
			VaultSettings other = (VaultSettings) obj;
			return Objects.equals(this.id, other.id);
		} else {
			return false;
		}
	}

}
