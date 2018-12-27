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
package com.exactpro.sf.testwebgui.help;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.commons.lang3.StringUtils;
import org.mvel2.MVEL;
import org.primefaces.context.RequestContext;
import org.primefaces.event.NodeCollapseEvent;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.help.helpmarshaller.HelpEntityName;
import com.exactpro.sf.help.helpmarshaller.HelpEntityType;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.HelpJsonContainer;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.MethodJsonContainer;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.URIJsonContainer;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.help.search.Search;
import com.exactpro.sf.testwebgui.help.search.SearchOptions;

@ManagedBean(name="helpBean")
@SessionScoped
@SuppressWarnings("serial")
public class HelpBean implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(HelpBean.class);

	private transient TreeNode selectedNode;

	private String mainContent;

	// Util-functions executor fields

	private boolean showExecutor = false;

	private String functionToExec;

	private String functionParameters;

	private String functionResult;

	private String functionExecString;

	// Search fields

	private SearchOptions searchOptions = new SearchOptions();

	private transient List<TreeNode> searchResult;

    private boolean isPageReady;

    private String mainRedminePage;
    private String selectedWikiPage;
    private boolean wikiPageVisible;
    private boolean enabledWiki;

    private transient List<TreeNode> expandedNodes;

    private transient TreeNode rootNode;

    private String customHeader;

    private transient CopyingFormat copyingFormat;

	private Tab activeTab = Tab.Plugins;
	private enum Tab {

        Plugins(0),
        //TODO reanimate guide
        Guide(1);



        private final int index;

        private Tab(int index) {
            this.index = index;
        }
	}
	
	//restore transient fields after deserialization
	private Object readResolve()  {
	    initTree();
	    return this;
	}
	
    @PostConstruct
	public void initTree() {

	    if (this.rootNode == null) {
            this.rootNode = BeanUtil.getHelpContentHolder().getRootNode();
	    }

	    RequestContext.getCurrentInstance().update(Arrays.asList(new String[] { "contentForm", "mainMenuBar" }));

        if (Tab.Plugins.equals(this.activeTab)) {
            RequestContext.getCurrentInstance().update("tab:tPlugins");
        }
	}

	public void preRenderView() {
        if (BeanUtil.getSfContext() == null) {
			BeanUtil.addErrorMessage("SFContext error", "SFContext is not initialized correctly. See log file for details.");
			return;
		}
	}

	public void search() {
		logger.debug("search invoked {}", getUser());

		if (StringUtils.isEmpty(this.searchOptions.getSearchText())) {
			hideSearchResults();
			return;
		}

		TreeNode startNode = this.searchOptions.isInSelection() && this.selectedNode != null ? this.selectedNode : getRoot();

		this.searchOptions.setRootNode(startNode);

		if (this.searchResult != null) {
			this.searchResult.clear();
		}

		switch (activeTab) {
			case Plugins:
				searchResult = Search.doSearch(this.searchOptions);
				setMainContent("");
				break;
			case Guide:
				break;
		}
	}

	public void collapseAll() {

	    if (this.expandedNodes == null) {
	        return;
	    }

	    for (TreeNode node : this.expandedNodes) {
	        node.setExpanded(false);
	    }

        if(this.selectedNode != null) {
            this.selectedNode.setSelected(false);
        }

        this.selectedNode = null;

	    this.expandedNodes.clear();

        setMainContent("");
	}

    public void onTabChange(TabChangeEvent e) {

        logger.debug("onTabChange invoked {} tabChangeEvent[{}]", getUser(), e);
        String title = e.getTab().getTitle();
        this.wikiPageVisible = false;
        this.showExecutor = false;

        if (title.equals(Tab.Plugins.toString())) {

            activeTab = Tab.Plugins;
            if (this.selectedNode != null) {
                nodeSelected(this.selectedNode);
            }

        } else {
            loadWikiIndexText();
            setMainContent("");
            wikiPageVisible = true;
            activeTab = Tab.Guide;
        }
    }

    public void expand(final TreeNode node) {

		logger.debug("expand invoked {}", getUser());

		if (node == null) return;

		collapseAll();

		if (this.selectedNode != null) {
		    this.selectedNode.setSelected(false);
		}

        this.selectedNode = node;

		expandNode(node.getParent());

		nodeSelected(node);

		node.setSelected(true);

	}

    private void expandNode(final TreeNode node) {

		logger.debug("expandNode invoked {}", getUser());

        if (node != null) {

            node.setExpanded(true);
            addToExpandedNodes(node);

            expandNode(node.getParent());
        }
	}

	public void hideSearchResults() {
		this.searchResult = null;
	}

	public void executeFunction() {
		logger.debug("executeFunction invoked {}", getUser());
		this.functionResult = "";

		Map<String, Object> mvelContext = new HashMap<>();
		mvelContext.put("um", SFLocalContext.getDefault().getUtilityManager());

		try {
		    String mvelExpression = StringUtils.isNotBlank(functionParameters) ? String.format("%s, %s)", functionExecString, functionParameters) : functionExecString + ")";
 			this.functionResult = MVEL.eval(mvelExpression, mvelContext).toString();
		} catch (Exception e) {
			logger.error("{}", e);
			Throwable ee = e;
			while (ee.getCause() != null)
			{
				ee = ee.getCause();
			}
			if (ee instanceof EPSCommonException) {
				BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Could not execute utility function", ee.getMessage());
			}
			else
			{
				BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Could not execute utility function", "please, check function parameters");
			}
		}
	}

	public String getParents(TreeNode node) {

        List<String> names = new ArrayList<>();

        if (node.getParent() != null && node.getParent().getData() != null) {
            addParents(names, node.getParent());
        }

        Collections.reverse(names);

        return StringUtils.join(names, " > ");
    }

    private void addParents(List<String> names, TreeNode node) {
        if (node.getParent() != null) {
            names.add(((HelpJsonContainer) node.getData()).getName());
            addParents(names, node.getParent());
        }
    }

	public TreeNode getRoot() {
		return this.rootNode;
	}

	private void fillExecutorParameters(TreeNode node) {
        HelpJsonContainer selected = (HelpJsonContainer) node.getData();
        String utilUri = ((URIJsonContainer) node.getParent().getData()).getUri().toString();

        String functionWithoutParameters = StringUtils.substringBefore(selected.getName(), "(");

        this.functionExecString = String.format("um.call(%s.parse('%s%s')", SailfishURI.class.getCanonicalName(), utilUri, functionWithoutParameters);

        this.functionToExec = utilUri + functionWithoutParameters;

        this.showExecutor = true;
    }

	public void onNodeSelect(NodeSelectEvent event) {

		logger.debug("onNodeSelect invoked {}", getUser());

		nodeSelected(event.getTreeNode());

        hideSearchResults();
	}

	private void nodeSelected(TreeNode node) {

        if (node == null)
            return;
        this.showExecutor = false;

        HelpJsonContainer selectedNodeData = (HelpJsonContainer) this.selectedNode.getData();

        setMainContent(
                BeanUtil.getHelpContentHolder().getDescription(selectedNodeData, BeanUtil.getHelpContentHolder().getPluginName(this.selectedNode)));

        if (selectedNodeData instanceof MethodJsonContainer) {
            if (((MethodJsonContainer) selectedNodeData).getIsUtilMethod()) {
                fillExecutorParameters(node);
            }
        }
    }

    public void copyToClipboard() {
        this.copyingFormat = new CopyingFormat(this.customHeader);

        this.copyingFormat.format(this.selectedNode);

        copyToClipboardJs(this.copyingFormat.copyNewColumns(), this.copyingFormat.copyJustHeader(), this.copyingFormat.copyToClipboard(),
                this.copyingFormat.copyAllStructure());
        RequestContext.getCurrentInstance().update("copyDialog");
        RequestContext.getCurrentInstance().execute("PF('copyDialog').show()");
    }

    private void copyToClipboardJs(String toCopy1, String toCopy2, String toCopy3, String toCopy4) {

        StringBuilder builder = new StringBuilder();

        builder.append("copyToClipboard('")
                .append(toCopy1)
                .append("', '")
                .append(toCopy2)
                .append("', '")
                .append(toCopy3)
                .append("', '")
                .append(toCopy4)
                .append("')");

        RequestContext.getCurrentInstance().execute(builder.toString());
    }

    public void showCopiedMessage() {
        BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Structure has been copied successfully", "");
    }

    public class HeaderColumn {

        private final String name;
        private final int tabCount;
        private final boolean isNew;

        public HeaderColumn(String name, int tabCount, boolean isNew) {
            this.name = name;
            this.tabCount = tabCount;
            this.isNew = isNew;
        }

        public String getName() {
            return name;
        }

        public int getTabCount() {
            return this.tabCount;
        }

        public boolean isNewColumn() {
            return this.isNew;
        }
    }

    public String getCustomHeaderDialogTitle() {

        int newColumnsCount = getNewColumnsCount();

        if (newColumnsCount == 0) {
            return "The structure will contain this column(s)";
        } else {
            return "Your custom header doesn't contain " + newColumnsCount + " column(s)";
        }
    }

    public int getNewColumnsCount() {
        return this.copyingFormat == null ? 0 : this.copyingFormat.getNewColumnsCount();
    }

    public List<HeaderColumn> getNewColumnList() {

        if (this.copyingFormat == null || this.selectedNode == null) {
            return null;
        }

        List<String> newColumnStrings = new ArrayList<>(this.copyingFormat.getNewColumns());

        List<String> notUsedColumns = new ArrayList<>(newColumnStrings);

        List<HeaderColumn> newColumns = new ArrayList<>();

        addToNewColumnList(newColumns, newColumnStrings, notUsedColumns, this.selectedNode, 0);

        // Adding the remained headers like #reference or #message_type
        if (notUsedColumns.size() > 0) {

            List<HeaderColumn> otherColumns = new ArrayList<>();

            for (String column : notUsedColumns) {
                otherColumns.add(new HeaderColumn(column, 0, true));
            }

            newColumns.addAll(0, otherColumns);
        }

        return newColumns;
    }

    private void addToNewColumnList(List<HeaderColumn> newColumns, List<String> newColumnStrings, List<String> notUsedColumns,
            TreeNode parent, int level) {

        for (TreeNode node : parent.getChildren()) {

            HelpJsonContainer container = (HelpJsonContainer) node.getData();

            boolean contain = newColumnStrings.contains(container.getName());

            HeaderColumn newColumn = new HeaderColumn(container.getName(), level, contain);

            if (contain) {
                notUsedColumns.remove(container.getName());
            }

            newColumns.add(newColumn);

            if (node.getChildCount() > 0) {
                addToNewColumnList(newColumns, newColumnStrings, notUsedColumns, node, level + 1);
            }
        }
    }

	public void onNodeExpand(NodeExpandEvent event) {
        logger.debug("onNodeExpand invoked {}", getUser());

        TreeNode node = event.getTreeNode();
        String pluginName = BeanUtil.getHelpContentHolder().getPluginName(node);


        HelpJsonContainer nodeData = (HelpJsonContainer) node.getData();

        if (HelpEntityName.DICTIONARIES.getValue().equals(nodeData.getName())) {
            BeanUtil.getHelpContentHolder().checkDictionaryListWasChanged(node, pluginName);
        } else if (HelpEntityType.DICTIONARY.name().equals(node.getType())) {
            BeanUtil.getHelpContentHolder().checkDictionaryWasChanged(node, pluginName);
        }

        if (node.getChildren().get(0).getData() == null) {
            BeanUtil.getHelpContentHolder().buildFromJson(node, pluginName);
        }

        addToExpandedNodes(node);
        node.setExpanded(true);

		RequestContext.getCurrentInstance().update("collapseAllPanel");
	}

	public void onNodeCollapse(NodeCollapseEvent event) {

		event.getTreeNode().setExpanded(false);

		this.expandedNodes.remove(event.getTreeNode());

		// Close childs if the parent was collapsed
		Iterator<TreeNode> iter = this.expandedNodes.iterator();
		while (iter.hasNext()) {
		    TreeNode node = iter.next();
		    if (checkNodeParent(node, event.getTreeNode())) {
		        node.setExpanded(false);
		        iter.remove();
		    }
		}

		RequestContext.getCurrentInstance().update("collapseAllPanel");
	}

	private void addToExpandedNodes(TreeNode node) {
	    if (this.expandedNodes == null) {
	        this.expandedNodes = new ArrayList<>();
	    }
	    this.expandedNodes.add(node);
	}

	private boolean checkNodeParent(TreeNode child, TreeNode parent) {
	    if (child == parent) return true;
	    if (child.getParent() == null) return false;
	    return checkNodeParent(child.getParent(), parent);
	}

	public String getMainContent() {
		return mainContent;
	}

	public void setMainContent(final String mainContent) {

		logger.debug("setMainContent invoked {} mainContent[{}]", getUser(), mainContent);

		if (mainContent == null || mainContent.trim().isEmpty()) {
			this.mainContent = mainContent;
			return;
		}

		if (StringUtils.isEmpty(this.searchOptions.getSearchText())) {
			this.mainContent = mainContent;
			return;
		}

		String source;
		String match;

		if (this.searchOptions.isIgnoreCase()) {
			source = mainContent.toLowerCase();
			match = this.searchOptions.getSearchText().toLowerCase();
		} else {
			source = mainContent;
			match = this.searchOptions.getSearchText();
		}

		StringBuilder sb = new StringBuilder();
		int beginIndex = 0;
		int endIndex = source.indexOf(match);

		while (endIndex != -1) {

			sb.append(mainContent.substring(beginIndex, endIndex));
			sb.append("<font class=\"highlightSearch\">");

			beginIndex = endIndex+this.searchOptions.getSearchText().length();

			sb.append(mainContent.subSequence(endIndex, beginIndex));
			sb.append("</font>");

			endIndex = source.indexOf(match, beginIndex);
		}

		sb.append(mainContent.substring(beginIndex));
		this.mainContent = sb.toString();
	}

	public TreeNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(TreeNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public String getFunctionToExec() {
		return functionToExec;
	}

	public void setFunctionToExec(String functionToExec) {
		logger.debug("setFunctionToExec invoked {} functionToExec[{}]", getUser(), functionToExec);
		this.functionToExec = functionToExec;
	}

	public String getFunctionParameters() {
		return functionParameters;
	}

	public void setFunctionParameters(String functionParameters) {
		logger.debug("setFunctionParameters invoked {} functionParameters[{}]", getUser(), functionParameters);
		this.functionParameters = functionParameters;
	}

	public String getFunctionResult() {
		return functionResult;
	}

	public void setFunctionResult(String functionResult) {
		logger.debug("setFunctionResult invoked {} functionResult[{}]", getUser(), functionResult);
		this.functionResult = functionResult;
	}

	public boolean isShowExecutor() {
		return showExecutor;
	}

	public void setShowExecutor(boolean showExecutor) {
		logger.debug("setShowExecutor invoked {} showExecutor[{}]", getUser(), showExecutor);
		this.showExecutor = showExecutor;
	}

	public List<TreeNode> getSearchResult() {
		return searchResult;
	}

	public SearchOptions getSearchOptions() {
        return searchOptions;
    }

	protected String getUser(){
		return System.getProperty("user.name");
	}

	public int getMaxSearchSize() {
		return SearchOptions.MAX_SEARCH_SIZE;
	}

    public String getMainRedminePage() {
        return mainRedminePage;
    }

    public void loadWikiIndexText() {
        if (this.enabledWiki) {
        String result = BeanUtil.getHelpContentHolder().getRedmine().buildWikiPageContent();
        mainRedminePage = result;
        } else {
            mainRedminePage = null;
            setSelectedWikiPage("Wiki page does not exist");
        }
    }

    public void loadRedmineWikiPage() {
        String pageName = BeanUtil.getRequestParam("pageName");

        if(pageName == null) {
            return;
        }

        String content = BeanUtil.getHelpContentHolder().getRedmine().buildWikiPageContent(pageName);
        setMainContent("");
        setSelectedWikiPage(content);
    }

    public String getSelectedWikiPage() {
        return selectedWikiPage;
    }

    public void setSelectedWikiPage(String selectedWikiPage) {
        this.selectedWikiPage = selectedWikiPage;
    }

    public boolean isWikiPageVisible() {
        return wikiPageVisible;
    }

    public void setWikiPageVisible(boolean wikiPageVisible) {
        this.wikiPageVisible = wikiPageVisible;
    }

    public boolean isPageReady() {
        return isPageReady;
    }

    public void setPageReady(boolean isPageReady) {
        this.isPageReady = isPageReady;
    }

    public void onPageLoad() throws FileNotFoundException {

        setPageReady(true);

        this.enabledWiki = SFLocalContext.getDefault().getWorkspaceDispatcher().exists(FolderType.ROOT, "help", "RedminePages.xml");

        loadWikiIndexText();

        if (!enabledWiki) {
            this.activeTab = Tab.Plugins;
            setWikiPageVisible(false);
        }

        if (this.activeTab != Tab.Plugins) {
            setWikiPageVisible(true);
        }
    }

    public boolean isEnabledWiki() {
        return enabledWiki;
    }

    public int getLoadingProgress() {

        if (BeanUtil.getHelpContentHolder() == null) {
            return 0;
        }

        return BeanUtil.getHelpContentHolder().getLoadingProgress();
    }

    public String getLoadingStage() {

        if (BeanUtil.getHelpContentHolder() == null) {
            return null;
        }

        return BeanUtil.getHelpContentHolder().getLoadingStage();
    }

    public boolean isSomeNodesExpanded() {
        return this.expandedNodes != null && this.expandedNodes.size() > 0 && this.activeTab.equals(Tab.Plugins);
    }

    public int getActiveTabIndex() {
        return this.activeTab.index;
    }

    public void setActiveTabIndex(int index) {
        // do nothing
    }

    public boolean isShowCopyButton() {

        if (this.selectedNode == null) {
            return false;
        }

        return this.selectedNode.getType().equals(HelpEntityType.MESSAGE.name()) || this.selectedNode.getType()
                .equals(HelpEntityType.FIELD.name());
    }

    public String getCustomHeader() {
        return customHeader;
    }

    public void setCustomHeader(String customHeader) {
        this.customHeader = customHeader;
    }

    public void setCustomHeaderWasSet(boolean value) {
        // do nothing
    }

    public boolean getCustomHeaderWasSet() {
        return StringUtils.isNotEmpty(this.customHeader);
    }
}
