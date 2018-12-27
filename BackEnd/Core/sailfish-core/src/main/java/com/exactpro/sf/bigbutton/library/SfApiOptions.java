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
package com.exactpro.sf.bigbutton.library;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public class SfApiOptions implements Serializable {
	
	private Boolean continueIfFailed;
	
	private String language;
	
	private String range;
	
	private Boolean autoStart;
	
	private Boolean ignoreAskForContinue;

    private Boolean runNetDumper;

    private Boolean skipOptional;

	private Map<String, String> staticVariables = new HashMap<>();
	
	private Set<Tag> tags = new HashSet<>();

	private Set<BigButtonAction> onPassed = new LinkedHashSet<>();

    private Set<BigButtonAction> onCondPassed = new LinkedHashSet<>();

    private Set<BigButtonAction> onFailed = new LinkedHashSet<>();

	public SfApiOptions() {
		
	}
	
	public SfApiOptions mergeOptions(SfApiOptions child) {
		
		SfApiOptions result = new SfApiOptions();
		
		result.continueIfFailed = (child.continueIfFailed != null ? child.continueIfFailed : this.continueIfFailed);
		result.language = (child.language != null ? child.language : this.language );
		result.range = (child.range != null ? child.range : this.range );
		result.autoStart = (child.autoStart != null ? child.autoStart : this.autoStart );
		result.ignoreAskForContinue = (child.ignoreAskForContinue != null ? child.ignoreAskForContinue : this.ignoreAskForContinue);
		result.runNetDumper = (child.runNetDumper != null ? child.runNetDumper : this.runNetDumper);
        result.skipOptional = (child.skipOptional != null ? child.skipOptional : this.skipOptional);

		result.staticVariables.putAll(this.staticVariables);
		result.staticVariables.putAll(child.staticVariables);

		result.setOnPassed(child.onPassed); // BigButtonActions not inherited
		result.setOnFailed(child.onFailed);
        result.setOnCondPassed(child.onCondPassed);

		result.tags.addAll(this.tags);
		result.tags.addAll(child.tags);
		
		return result;
		
	}

	public Boolean getContinueIfFailed() {
		return continueIfFailed;
	}

	public void setContinueIfFailed(Boolean continueIfFailed) {
		this.continueIfFailed = continueIfFailed;
	}

	public String getRange() {
		return range;
	}

	public void setRange(String range) {
		this.range = range;
	}

	public Boolean getAutoStart() {
		return autoStart;
	}

	public void setAutoStart(Boolean autoStart) {
		this.autoStart = autoStart;
	}

	public Boolean getIgnoreAskForContinue() {
		return ignoreAskForContinue;
	}

	public void setIgnoreAskForContinue(Boolean ignoreAskForContinue) {
		this.ignoreAskForContinue = ignoreAskForContinue;
	}

    public Boolean getRunNetDumper() {
        return runNetDumper;
    }

    public void setRunNetDumper(Boolean runNetDumper) {
        this.runNetDumper = runNetDumper;
    }

	public Map<String, String> getStaticVariables() {
		return staticVariables;
	}

	public void setStaticVariables(Map<String, String> staticVariables) {
		this.staticVariables = staticVariables;
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public void setTags(Set<Tag> tags) {
		this.tags = tags;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

    public Set<BigButtonAction> getOnPassed() {
        return onPassed;
    }

    public void setOnPassed(Set<BigButtonAction> onPassed) {
        this.onPassed = onPassed;
    }


    public void addOnPassed(Collection<BigButtonAction> onPassed) {
        this.onPassed.addAll(onPassed);
    }

    public Set<BigButtonAction> getOnFailed() {
        return onFailed;
    }

    public void setOnFailed(Set<BigButtonAction> onFailed) {
        this.onFailed = onFailed;
    }

    public void addOnFailed(Collection<BigButtonAction> onFailed) {
	    this.onFailed.addAll(onFailed);
    }

    public Set<BigButtonAction> getOnCondPassed() {
        return onCondPassed;
    }

    public void setOnCondPassed(Set<BigButtonAction> onCondPassed) {
        this.onCondPassed = onCondPassed;
    }

    public void addOnCondPassed(Collection<BigButtonAction> onCondPassed) {
        this.onCondPassed.addAll(onCondPassed);
    }

    public Boolean getSkipOptional() {
        return skipOptional;
    }

    public void setSkipOptional(Boolean skipOptional) {
        this.skipOptional = skipOptional;
    }
}
