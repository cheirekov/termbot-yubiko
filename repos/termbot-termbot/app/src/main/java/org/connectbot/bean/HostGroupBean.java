/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.connectbot.bean;

import android.content.ContentValues;
import org.connectbot.util.HostDatabase;

public class HostGroupBean extends AbstractBean {
	public static final String BEAN_NAME = "hostgroup";

	private long id = -1;
	private String name;

	@Override
	public String getBeanName() {
		return BEAN_NAME;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public ContentValues getValues() {
		ContentValues values = new ContentValues();
		values.put(HostDatabase.FIELD_HOST_GROUP_NAME, name);
		return values;
	}
}
