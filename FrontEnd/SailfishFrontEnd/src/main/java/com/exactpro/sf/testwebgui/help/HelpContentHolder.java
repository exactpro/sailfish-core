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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.configuration.IDictionaryManagerListener;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.help.HelpBuilder;
import com.exactpro.sf.help.helpmarshaller.HelpEntityName;
import com.exactpro.sf.help.helpmarshaller.HelpEntityType;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.HelpJsonContainer;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.URIJsonContainer;
import com.exactpro.sf.util.DirectoryFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

import freemarker.template.TemplateException;

public class HelpContentHolder implements IDictionaryManagerListener{

    private static String ROOT = "root";
    private static String ROWKEY_SEPARATOR = "_";

    private static final Logger logger = LoggerFactory.getLogger(HelpContentHolder.class);

    private volatile TreeNode rootNode;

    private LoadingProgress loadingProgress;

    private volatile Map<TreeNode, Long> dictionaryModified = new ConcurrentHashMap<>();

    private final RedminePageBuilder redmine = new RedminePageBuilder();

    private final ISFContext context;

    private final ObjectMapper mapper;

    private final ReadWriteLock lock;

    public HelpContentHolder(final ISFContext context) {

        this.context = context;

        this.mapper = new ObjectMapper();

        this.lock = new ReentrantReadWriteLock();

        loadTree();
    }

    private void loadTree() {
        lock.writeLock().lock();
        try {

            rootNode = new DefaultTreeNode(ROOT, null);

            Set<String> pluginFolders = context.getWorkspaceDispatcher().listFiles(DirectoryFilter.getInstance(), FolderType.PLUGINS);

            List<File> rootFiles = new ArrayList<>();

            rootFiles.add(context.getWorkspaceDispatcher().getFile(FolderType.ROOT, HelpBuilder.HELP, HelpBuilder.ROOT));

            for (String plugin : pluginFolders) {
                try {
                    rootFiles.add(context.getWorkspaceDispatcher().getFile(FolderType.PLUGINS, plugin, HelpBuilder.HELP, HelpBuilder.ROOT));
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }

            if (rootFiles.isEmpty()) {
               logger.error("Workspace doesn't contains help files");
               rootNode = new DefaultTreeNode(null, null);
               return;
            }

            loadingProgress = new LoadingProgress(rootFiles.size());

            for (File root : rootFiles) {
                loadPlugin(root, rootNode);
                loadingProgress.incrementProgress();
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    private void loadPlugin(File root, TreeNode rootNode) throws IOException {

        String jsonString = HelpBuilder.fileToJsonString(root);

        HelpJsonContainer pluginJsonNode = mapper.readValue(jsonString, HelpJsonContainer.class);

        buildTree(pluginJsonNode, rootNode);

    }

    public void buildTree(HelpJsonContainer parentJson, TreeNode parentNode) {

        TreeNode node = new DefaultTreeNode(parentJson.getType().name(), parentJson, parentNode);

        List<HelpJsonContainer> childJsonNodes = parentJson.getChildNodes();

        if (childJsonNodes != null) {

            for (HelpJsonContainer childNode : childJsonNodes) {
                buildTree(childNode, node);
            }

        } else if (parentJson.getFilePath().contains(HelpBuilder.JSON)) {
            new DefaultTreeNode(HelpEntityType.NAMED.name(), null, node);
        }

    }

    public void buildFromJson(TreeNode parentNode, String pluginName) {
        lock.writeLock().lock();
        try {
            String rowKey = parentNode.getRowKey();

            TreeNode rootParentNode = getNodeByRowKey(rowKey);
            if (rootParentNode.getChildren().get(0).getData() == null) {

                HelpJsonContainer parentNodeData = (HelpJsonContainer) parentNode.getData();
                File json;

                if (pluginName.equals(IVersion.GENERAL)) {
                    json = context.getWorkspaceDispatcher().getFile(FolderType.ROOT, HelpBuilder.HELP, getJsonPath(parentNodeData));
                } else {
                    json = context.getWorkspaceDispatcher().getFile(FolderType.PLUGINS, pluginName, HelpBuilder.HELP, getJsonPath(parentNodeData));
                }

                String jsonString = HelpBuilder.fileToJsonString(json);

                HelpJsonContainer parentContainer = mapper.readValue(jsonString, HelpJsonContainer.class);

                rootParentNode.getChildren().clear();

                for (HelpJsonContainer childNode : parentContainer.getChildNodes()) {

                    buildTree(childNode, rootParentNode);

                }

                parentNodeData.setChildNodes(parentContainer.getChildNodes());
            }

            parentNode.getChildren().clear();

            copyChildren(rootParentNode, parentNode);

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    public void checkDictionaryListWasChanged(TreeNode mainNode, String pluginName) {
        boolean reloadDicts = false;
        File rootFile;
        lock.readLock().lock();
        try {
            rootFile = getHelpFile(pluginName, HelpBuilder.ROOT);
            if(rootFile == null) {
                return;
            }
            Long lastModified = dictionaryModified.get(mainNode);

            if (lastModified == null) {
                dictionaryModified.put(mainNode, rootFile.lastModified());
            } else if (!lastModified.equals(rootFile.lastModified())) {
                dictionaryModified.put(mainNode, rootFile.lastModified());
                reloadDicts = true;
            }

        } finally {
            lock.readLock().unlock();
        }

        if (reloadDicts) {
            lock.writeLock().lock();
            try {

                HelpJsonContainer rootContainer = mapper.readValue(HelpBuilder.fileToJsonString(rootFile), HelpJsonContainer.class);

                HelpJsonContainer dictionariesContainer = null;

                for (HelpJsonContainer child : rootContainer.getChildNodes()) {
                    if (child.getName().equals(HelpEntityName.DICTIONARIES.getValue())) {
                        dictionariesContainer = child;
                    }
                }

                if (dictionariesContainer != null && dictionariesContainer.getChildNodes().size() > mainNode.getChildren().size()) {

                    Set<SailfishURI> uris = context.getDictionaryManager().getDictionaryURIs(pluginName);
                    List<SailfishURI> found = new ArrayList<>();

                    TreeNode rootDictionaries = getNodeByRowKey(mainNode.getRowKey());

                    for (TreeNode dictNode : rootDictionaries.getChildren()) {
                        URIJsonContainer container = (URIJsonContainer) dictNode.getData();
                        if (uris.contains(container.getUri())) {
                            found.add(container.getUri());
                        }
                    }

                    for (HelpJsonContainer dictionary : dictionariesContainer.getChildNodes()) {

                        if (!found.contains(((URIJsonContainer) dictionary).getUri())) {
                            TreeNode dictionaryRootNode = new DefaultTreeNode(HelpEntityType.DICTIONARY.name(), dictionary, rootDictionaries);
                            new DefaultTreeNode(HelpEntityType.NAMED.name(), null, dictionaryRootNode);

                            TreeNode dictionaryNode = new DefaultTreeNode(HelpEntityType.DICTIONARY.name(), dictionary, mainNode);
                            new DefaultTreeNode(HelpEntityType.NAMED.name(), null, dictionaryNode);
                        }

                    }
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Update node with children clearing
     */

    public void checkDictionaryWasChanged(TreeNode dictionaryNode, String pluginName) {
        boolean reloadDict = false;
        lock.readLock().lock();
        try {
            Long prevLastModified = dictionaryModified.get(dictionaryNode);

            HelpJsonContainer dictionaryData = (HelpJsonContainer) dictionaryNode.getData();

            File checkedFile = getHelpFile(pluginName, getJsonPath(dictionaryData));

            Long currentLastModified = checkedFile.lastModified();
            if (prevLastModified == null) {
                dictionaryModified.put(dictionaryNode, currentLastModified);
            } else if (!prevLastModified.equals(currentLastModified)) {
                dictionaryModified.put(dictionaryNode, currentLastModified);
                reloadDict = true;
            }
        }
        finally {
            lock.readLock().unlock();
        }
        if (reloadDict) {
            buildFromJson(dictionaryNode, pluginName);
        }
    }

    private File getHelpFile(String pluginName, String path) {
        File helpFile;
        try {
            if (pluginName.equals(IVersion.GENERAL)) {
                helpFile = context.getWorkspaceDispatcher().getFile(FolderType.ROOT, HelpBuilder.HELP, path);
            } else {
                helpFile = context.getWorkspaceDispatcher()
                        .getFile(FolderType.PLUGINS, pluginName, HelpBuilder.HELP, path);
            }
            return helpFile;
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }
    public String getHtmlPath(HelpJsonContainer nodeData)  {
        String filePath = nodeData.getFilePath();
        if (filePath.endsWith(HelpBuilder.JSON)) {
            return filePath.substring(0, filePath.length() - HelpBuilder.JSON.length()) + HelpBuilder.HTML;
        } else {
            return filePath;
        }
    }

    private String getJsonPath(HelpJsonContainer nodeData) {
        String filePath = nodeData.getFilePath();
        if (filePath.endsWith(HelpBuilder.HTML)) {
            return filePath.substring(0, filePath.length() - HelpBuilder.HTML.length()) + HelpBuilder.JSON;
        } else {
            return filePath;
        }
    }

    public String getDescription(HelpJsonContainer nodeData, String pluginName) {
        try {
            String htmlPath = getHtmlPath(nodeData);

            File data;

            if (pluginName.equals(IVersion.GENERAL)) {
                data = context.getWorkspaceDispatcher().getFile(FolderType.ROOT, HelpBuilder.HELP, htmlPath);
            } else {
                data = context.getWorkspaceDispatcher().getFile(FolderType.PLUGINS, pluginName, HelpBuilder.HELP, htmlPath);
            }

            return htmlToString(data);
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
            return e.getMessage();
        }

    }

    private String htmlToString(File file) {

        try {
            List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
            return StringUtils.join(lines.subList(1, lines.size()), "");// Skip css import
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return "";
        }
    }

    public String getPluginName(TreeNode node) {
        TreeNode pluginNode = node;
        while (!pluginNode.getParent().getData().equals(ROOT)) {
            pluginNode = pluginNode.getParent();
        }
        return ((HelpJsonContainer) (pluginNode.getData())).getName();
    }

    public TreeNode getRootNode() {
        lock.readLock().lock();
        try {
            return copyRoot();
        } finally {
            lock.readLock().unlock();
        }

    }

    public HelpJsonContainer getContainer(TreeNode node) {
        return (HelpJsonContainer) node.getData();

    }

    public HelpJsonContainer getContainer(HelpJsonContainer node, String pluginName) {
        try {

            if (pluginName.equals(IVersion.GENERAL)) {
                return mapper.readValue(
                        HelpBuilder.fileToJsonString(context.getWorkspaceDispatcher().getFile(FolderType.ROOT, HelpBuilder.HELP, node.getFilePath())),
                        HelpJsonContainer.class);
            }
            return mapper.readValue(HelpBuilder
                            .fileToJsonString(context.getWorkspaceDispatcher().getFile(FolderType.PLUGINS, pluginName, HelpBuilder.HELP, node.getFilePath())),
                    HelpJsonContainer.class);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public int getNodeIndex(TreeNode node) {
        if (node.getParent().equals(rootNode)) {
            return Integer.parseInt(node.getRowKey());
        }
        return Integer.parseInt(StringUtils.substringAfterLast(node.getRowKey(), ROWKEY_SEPARATOR));
    }

    private TreeNode getNodeByRowKey(String rowKey) {
        String[] indexes = StringUtils.split(rowKey, ROWKEY_SEPARATOR);
        TreeNode node = rootNode;
        for (String index : indexes) {
            node = node.getChildren().get(Integer.parseInt(index));
        }
        return node;
    }

    private TreeNode copyRoot() {

        TreeNode newRoot = new DefaultTreeNode(ROOT, null);
        copyChildren(this.rootNode, newRoot);
        return newRoot;
    }

    private void copyChildren(TreeNode fromParent, TreeNode toParent) {

        for (TreeNode child : fromParent.getChildren()) {

            TreeNode newChild = new DefaultTreeNode(child.getType(), child.getData(), toParent);

            if (child.getChildCount() > 0) {
                copyChildren(child, newChild);
            }
        }
    }

    public void dictionaryCreated(SailfishURI dict) {
        lock.writeLock().lock();
        try {
            HelpBuilder dictBuilder = new HelpBuilder(context.getWorkspaceDispatcher(), context.getDictionaryManager(), context.getUtilityManager());
            dictBuilder.buildNewDictionary(dict);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void dictionaryModified(SailfishURI dict, String pluginName) {
        lock.writeLock().lock();
        try {
            HelpBuilder rebuilder = new HelpBuilder(context.getWorkspaceDispatcher(), context.getDictionaryManager(), context.getUtilityManager());

            rebuilder.rebuildDictionary(dict, pluginName);
        } catch (IOException | TemplateException e) {
            logger.error(e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public void invalidateEvent(Set<SailfishURI> uris) {
        for (SailfishURI uri : uris) {
            dictionaryModified(uri, context.getDataManager().getRelativePathToPlugin(uri.getPluginAlias()).getFileName().toString());
        }
    }

    @Override
    public void createEvent(SailfishURI uri) {
        dictionaryCreated(uri);
    }

    protected class LoadingProgress {

        private AtomicInteger progress = new AtomicInteger(0);

        private final int totalCount;

        public String stage;

        public LoadingProgress(int totalCount) {
            this.totalCount = totalCount;
        }

        public int getPercent() {
            int result = Math.round((progress.floatValue() / totalCount) * 100f);
            if (result >= 100) {
                result = isLoaded() ? 100 : 99;
                stage = "Postprocessing...";
            }
            return result;
        }

        public void incrementProgress() {
            progress.getAndIncrement();
        }

        public String getStage() {
            return stage;
        }

        public boolean isLoaded() {
            return this.progress.get() == this.totalCount;
        }
    }

    public int getLoadingProgress() {
        return this.loadingProgress == null ? 0 : this.loadingProgress.getPercent();
    }

    public String getLoadingStage() {
        return this.loadingProgress == null ? null : this.loadingProgress.getStage();
    }

    public RedminePageBuilder getRedmine() {
        return this.redmine;
    }
}
