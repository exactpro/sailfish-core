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
package com.exactpro.sf.testwebgui.restapi.json.action;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.exactpro.sf.aml.Direction;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonAction {

	private final String name;

	private final Collection<String> required;

	private final Collection<String> optional;

	private final Direction direction;
	
	private final Set<String> utilClasses;
	
	private final boolean isAML2, isAML3;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String help;

	public JsonAction(String name, Collection<String> required, Collection<String> optional,
			Direction direction, Set<String> utilClasses, boolean isAML2, boolean isAML3, String help) {
		super();
		this.name = name;
		this.required = required;
		this.optional = optional;
		this.direction = direction;
		this.utilClasses = utilClasses;
		this.isAML2 = isAML2;
		this.isAML3 = isAML3;
        this.help = help;
	}
	
	public JsonAction(String name) {
		this(name, Collections.<String>emptyList(), Collections.<String>emptyList(), null, null, false, true, null);
	}
	
	public JsonAction(String name, Collection<String> required) {
		this(name, required, Collections.<String>emptyList(), null, null, false, true, null);
	}
	
	public JsonAction(String name, Collection<String> required, Collection<String> optional) {
		this(name, required, optional, null, null, false, true, null);
	}

	public String getName() {
		return name;
	}

	public Collection<String> getRequired() {
		return required;
	}

	public Collection<String> getOptional() {
		return optional;
	}

	public Direction getDirection() {
		return direction;
	}

	public Set<String> getUtilClasses() {
		return utilClasses;
	}

	@JsonProperty("isAML2")
	public boolean isAML2() {
		return isAML2;
	}

	@JsonProperty("isAML3")
	public boolean isAML3() {
		return isAML3;
	}

    public String getHelp() {
        return help;
    }
}
