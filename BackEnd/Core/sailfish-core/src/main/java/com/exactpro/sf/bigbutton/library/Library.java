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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.text.StringSubstitutor;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.util.DateTimeUtility;

@SuppressWarnings("serial")
public class Library implements Serializable /*extends AbstractLibraryItem*/ {

	private String descriptorFileName;

	private Optional<Globals> globals = Optional.empty();

	private ExecutorList executors;

	private DaemonList daemons;

	private Map<String, ServiceList> serviceLists = new HashMap<>();

	private List<ScriptList> scriptLists = new ArrayList<>();

	private TagList tagList;

	private String rootFolder = ""; // library root

	private String reportsFolder;

    private String variableSetsFile;

    private final SfApiOptions defaultApiOptions = new SfApiOptions();

	public Library() {
        defaultApiOptions.setLanguage("Auto");
        defaultApiOptions.setContinueIfFailed(false);
        defaultApiOptions.setIgnoreAskForContinue(false);
        defaultApiOptions.setContinueIfFailed(false);
        defaultApiOptions.setAutoStart(false);
        defaultApiOptions.setRunNetDumper(false);
        defaultApiOptions.setSkipOptional(false);
	}

	public Optional<Globals> getGlobals() {
		return globals;
	}

	public void setGlobals(Optional<Globals> globals) {
		this.globals = globals;
	}

	public Map<String, ServiceList> getServiceLists() {
		return serviceLists;
	}

	public void setServiceLists(Map<String, ServiceList> serviceLists) {
		this.serviceLists = serviceLists;
	}

	public List<ScriptList> getScriptLists() {
		return scriptLists;
	}

	public void setScriptLists(List<ScriptList> scriptLists) {
		this.scriptLists = scriptLists;
	}

    /**
     * relative (to TEST_LIBRARY folder) path
     */
	public String getRootFolder() {
		return rootFolder;
	}

    /**
     * relative (to TEST_LIBRARY folder) path
     *
     * @param rootFolder
     */
	public void setRootFolder(String rootFolder) {
		this.rootFolder = rootFolder;
	}

	public ExecutorList getExecutors() {
		return executors;
	}

	public void setExecutors(ExecutorList executors) {
		this.executors = executors;
	}

	public DaemonList getDaemons() {
		return daemons;
	}

	public void setDaemons(DaemonList daemons) {
		this.daemons = daemons;
	}

	public TagList getTagList() {
		return tagList;
	}

	public void setTagList(TagList tagList) {
		this.tagList = tagList;
	}

	public SfApiOptions getDefaultApiOptions() {
		return defaultApiOptions;
	}

    /**
     * relative (to REPORT folder) path
     */
	public String getReportsFolder() {
		return reportsFolder;
	}

    /**
     * relative (to REPORT folder) path
     *
     * @param reportsFolder
     */
	public void setReportsFolder(String reportsFolder) {
		this.reportsFolder = reportsFolder;
	}

    public String getVariableSetsFile() {
        return variableSetsFile;
    }

    public void setVariableSetsFile(String variableSetsFile) {
        this.variableSetsFile = variableSetsFile;
    }

    public String getDescriptorFileName() {
		return descriptorFileName;
	}

	public void setDescriptorFileName(String descriptorFileName) {
		this.descriptorFileName = descriptorFileName;
	}

    private Map<String, String> initEnvironmentVariable() {
        Map<String, String> map = new HashMap<>();
        LocalDateTime date = DateTimeUtility.nowLocalDateTime();
        map.put("date_time", date.format(DateTimeUtility.createFormatter("yyyy_MM_dd_HH_mm_ss")));
        map.put("date", date.format(DateTimeUtility.createFormatter("yyyy_MM_dd")));
        map.put("time", date.format(DateTimeUtility.createFormatter("HH_mm_ss")));
        map.put("sf_version", SFLocalContext.getDefault().getVersion());
        return Collections.unmodifiableMap(map);
    }

    /**
     * Create and apply envaironment variables to names
     */
    public void normalize() {
        Map<String, String> environmentVariable = initEnvironmentVariable();
        StringSubstitutor substitutor = new StringSubstitutor(environmentVariable);

        if (tagList != null) {
            for (Tag tag : tagList.getTags()) {
                tag.normalize(substitutor);
            }
        }

        for(ScriptList scriptList : scriptLists){ // FIXME tag from SfApiOptions should be refer to tag from TagList

            for(Tag tag : scriptList.getApiOptions().getTags()){
                tag.normalize(substitutor);
            }

            for(Script script : scriptList.getScripts()){
                for(Tag tag : script.getApiOptions().getTags()){
                    tag.normalize(substitutor);
                }
            }

        }

        if (globals.isPresent()) {
            for(Tag tag : globals.get().getApiOptions().getTags()){
                tag.normalize(substitutor);
            }
        }

        this.reportsFolder = substitutor.replace(reportsFolder);
    }
}
