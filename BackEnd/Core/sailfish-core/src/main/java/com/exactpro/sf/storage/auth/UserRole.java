/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
 ******************************************************************************/
package com.exactpro.sf.storage.auth;

import java.io.Serializable;

public class UserRole implements Serializable {
    private static final long serialVersionUID = 2004372792782527875L;

    private String name;
	private boolean hasRole;

	public UserRole(String name, boolean hasRole) {
		this.name = name;
		this.hasRole = hasRole;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isHasRole() {
		return hasRole;
	}

	public void setHasRole(boolean hasRole) {
		this.hasRole = hasRole;
	}

}