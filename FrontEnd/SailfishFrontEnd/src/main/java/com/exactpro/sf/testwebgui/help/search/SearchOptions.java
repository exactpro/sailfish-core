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
package com.exactpro.sf.testwebgui.help.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.primefaces.model.TreeNode;

import com.exactpro.sf.help.helpmarshaller.HelpEntityName;

public class SearchOptions implements Serializable {

    private static final long serialVersionUID = 4515042058645494209L;

    public static final int MAX_SEARCH_SIZE = 200;
    
	private transient TreeNode rootNode;
	private String searchText;
	
	private boolean searchFields = true;
	private boolean searchUtils = true;
	private boolean searchMethods = true;
	
    private boolean searchComponents = true; // Services, Languages, Validators,
                                             // Preprocessors, Providers
	
	private boolean searchJustInNames = false;
	private boolean inSelection = false;
    private boolean ignoreCase = true;
    
    private List<String> searchSwitchOptions = new ArrayList<>();
	
	public SearchOptions() {
        this.searchSwitchOptions.add(HelpEntityName.ACTIONS.getValue());
        this.searchSwitchOptions.add(HelpEntityName.DICTIONARIES.getValue());
	}
	
    public SearchOptions(TreeNode rootNode, String searchText) {
		this.rootNode = rootNode;
		this.searchText = searchText;
	}
	
	public TreeNode getRootNode() {
		return rootNode;
	}

	public void setRootNode(TreeNode rootNode) {
		this.rootNode = rootNode;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

	public boolean isSearchMethods() {
		return searchMethods;
	}

	public void setSearchMethods(boolean searchMethods) {
		this.searchMethods = searchMethods;
	}

	public boolean isSearchFields() {
		return searchFields;
	}

	public void setSearchFields(boolean searchFields) {
		this.searchFields = searchFields;
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}

	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	public int getMaxSearchSize() {
		return MAX_SEARCH_SIZE;
	}

    public boolean isSearchActions() {
        return this.searchSwitchOptions.contains("Actions");
    }

    public boolean isSearchDictionaries() {
        return this.searchSwitchOptions.contains("Dictionaries");
    }

    public boolean isInSelection() {
        return inSelection;
    }

    public void setInSelection(boolean inSelection) {
        this.inSelection = inSelection;
    }

    public boolean isSearchUtils() {
        return searchUtils;
    }

    public void setSearchUtils(boolean searchUtils) {
        this.searchUtils = searchUtils;
    }

    public boolean isSearchComponents() {
        return searchComponents;
    }

    public void setSearchComponents(boolean searchComponents) {
        this.searchComponents = searchComponents;
    }

    public boolean isSearchJustInNames() {
        return searchJustInNames;
    }

    public void setSearchJustInNames(boolean searchJustInNames) {
        this.searchJustInNames = searchJustInNames;
    }

    public List<String> getSearchSwitchOptions() {
        return searchSwitchOptions;
    }

    public void setSearchSwitchOptions(List<String> searchSwitchOptions) {
        this.searchSwitchOptions = searchSwitchOptions;
    }
}