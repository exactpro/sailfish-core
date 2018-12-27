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
package com.exactpro.sf.embedded.statistics.storage.reporting;

import java.util.List;

import com.exactpro.sf.embedded.statistics.entities.Tag;

public class TaggedSetsComparisonParameters {
	
	private List<Tag> firstSet;
	
	private List<Tag> secondSet;

	public List<Tag> getFirstSet() {
		return firstSet;
	}

	public void setFirstSet(List<Tag> firstSet) {
		this.firstSet = firstSet;
	}

	public List<Tag> getSecondSet() {
		return secondSet;
	}

	public void setSecondSet(List<Tag> secondSet) {
		this.secondSet = secondSet;
	}
	
}
