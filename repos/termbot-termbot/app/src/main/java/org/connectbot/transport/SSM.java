/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2007 Kenny Root, Jeffrey Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.connectbot.transport;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.connectbot.R;
import org.connectbot.bean.HostBean;
import org.connectbot.util.HostDatabase;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * Placeholder SSM transport skeleton.
 *
 * The implementation is intentionally non-functional until TKT-0262 adds
 * StartSession and stream channel handling.
 */
public class SSM extends AbsTransport {
	private static final String TAG = "CB.SSM";
	private static final String PROTOCOL = "ssm";
	private static final int DEFAULT_PORT = 443;

	private volatile boolean connected;
	private volatile boolean sessionOpen;

	public static String getProtocolName() {
		return PROTOCOL;
	}

	@Override
	public void connect() {
		connected = false;
		sessionOpen = false;
		Log.i(TAG, "SSM transport skeleton invoked before full implementation");
		if (bridge != null) {
			bridge.outputLine("SSM support is in progress (TKT-0262).");
			bridge.dispatchDisconnect(false);
		}
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		throw new IOException("SSM transport is not connected");
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		if (!connected) {
			return;
		}
		throw new IOException("SSM write path is not implemented");
	}

	@Override
	public void write(int c) throws IOException {
		if (!connected) {
			return;
		}
		throw new IOException("SSM write path is not implemented");
	}

	@Override
	public void flush() throws IOException {
		// No-op until stream channel is implemented.
	}

	@Override
	public void close() {
		connected = false;
		sessionOpen = false;
	}

	@Override
	public void setDimensions(int columns, int rows, int width, int height) {
		// No-op until stream channel is implemented.
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public boolean isSessionOpen() {
		return sessionOpen;
	}

	@Override
	public int getDefaultPort() {
		return DEFAULT_PORT;
	}

	@Override
	public String getDefaultNickname(String username, String hostname, int port) {
		String safeUser = username == null ? "ssm" : username;
		String safeHost = hostname == null ? "target" : hostname;
		if (port == DEFAULT_PORT) {
			return String.format(Locale.US, "%s@%s", safeUser, safeHost);
		}
		return String.format(Locale.US, "%s@%s:%d", safeUser, safeHost, port);
	}

	public static Uri getUri(String input) {
		if (input == null || input.trim().isEmpty()) {
			return null;
		}

		String value = input.trim();
		if (value.startsWith(PROTOCOL + "://")) {
			return Uri.parse(value);
		}

		int slashIndex = value.indexOf('/');
		if (slashIndex > 0) {
			String region = value.substring(0, slashIndex);
			String target = value.substring(slashIndex + 1);
			if (!region.isEmpty() && !target.isEmpty()) {
				return Uri.parse(PROTOCOL + "://" + Uri.encode(region)
						+ "/" + Uri.encode(target)
						+ "/#" + Uri.encode(value));
			}
		}

		return null;
	}

	@Override
	public HostBean createHost(Uri uri) {
		HostBean host = new HostBean();
		host.setProtocol(PROTOCOL);

		String region = uri.getHost();
		host.setHostname(region);

		int port = uri.getPort();
		if (port < 0) {
			port = DEFAULT_PORT;
		}
		host.setPort(port);

		host.setUsername(uri.getUserInfo());

		String nickname = uri.getFragment();
		if (nickname == null || nickname.length() == 0) {
			host.setNickname(getDefaultNickname(host.getUsername(), host.getHostname(), host.getPort()));
		} else {
			host.setNickname(nickname);
		}

		return host;
	}

	@Override
	public void getSelectionArgs(Uri uri, Map<String, String> selection) {
		selection.put(HostDatabase.FIELD_HOST_PROTOCOL, PROTOCOL);
		selection.put(HostDatabase.FIELD_HOST_NICKNAME, uri.getFragment());
		selection.put(HostDatabase.FIELD_HOST_HOSTNAME, uri.getHost());
		selection.put(HostDatabase.FIELD_HOST_PORT,
				Integer.toString(uri.getPort() < 0 ? DEFAULT_PORT : uri.getPort()));
		selection.put(HostDatabase.FIELD_HOST_USERNAME, uri.getUserInfo());
	}

	public static String getFormatHint(Context context) {
		return String.format("%s/%s",
				context.getString(R.string.format_hostname),
				context.getString(R.string.format_port));
	}

	@Override
	public Map<String, String> getOptions() {
		return new HashMap<>();
	}

	@Override
	public boolean usesNetwork() {
		return true;
	}
}
