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
package com.exactpro.sf.configuration.workspace;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.Pair;
import com.google.common.collect.Maps;

public class DefaultWorkspaceDispatcher implements IWorkspaceDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultWorkspaceDispatcher.class);

    /**
     * List with mapping for folder type to folder, from general path to last workspace
     * Allow attach set of workspaces
     */
    private final List<WorkspaceLayer> workspaces = new ArrayList<>();

    private final ReadWriteLock workspacesLock = new ReentrantReadWriteLock(false);

    private final boolean createDirectories;

    public DefaultWorkspaceDispatcher(List<Pair<File, IWorkspaceLayout>> workspaceConfigs) {
        this(workspaceConfigs, true);
    }

    // use DefaultWorkspaceDispatcherBuilder
    public DefaultWorkspaceDispatcher(List<Pair<File, IWorkspaceLayout>> workspaceConfigs, boolean createDirectories) {

        this.createDirectories = createDirectories;

        if(workspaceConfigs == null || workspaceConfigs.isEmpty()) {
        	File workspaceFolder = null;
            if (System.getProperties().containsKey("user.dir")) {
            	workspaceFolder = new File(System.getProperty("user.dir"));
            } else {
            	workspaceFolder =  new File(".");
            }

            workspaceConfigs = new ArrayList<>(1);
            workspaceConfigs.add(new Pair<>(workspaceFolder, DefaultWorkspaceLayout.getInstance()));
        }

        for (Pair<File, IWorkspaceLayout> config : workspaceConfigs) {
            try {
                File workspaceFolder = config.getFirst();
                workspaceFolder = config.getSecond().isEmbedded() ? workspaceFolder : workspaceFolder.getCanonicalFile();

                if (!workspaceFolder.exists()) {
                	workspaceFolder.mkdirs();
                }

                if (!workspaceFolder.isDirectory()) {
                	logger.error("Workspace {} is not folder. Skip", workspaceFolder);
                	continue;
                }

                if (!isDuplicatePath(workspaceFolder)) {
                    this.workspaces.add(new WorkspaceLayer(workspaceFolder, config.getSecond()));
                    logger.info("Workspace {} has been added to the layer {}", workspaceFolder, this.workspaces.size());
                } else {
                	logger.warn("Workspace {} has been skipped, because it overrides existing layers", workspaceFolder);
                }
            } catch (IOException e) {
                logger.warn("Addition workspace {} failed", config, e);
            }
        }

        if (workspaces.isEmpty())
        	throw new IllegalArgumentException("No exisiting workspace was specified");
    }

    @Override
    public boolean selectWorkspace(String path, IWorkspaceLayout layout) {
    	throw new UnsupportedOperationException();
    	/*
        try {
            File targetDir = new File(path).getCanonicalFile();
            if (targetDir.exists() && targetDir.isDirectory()) {
                try {
                    this.workspacesLock.writeLock().lock();
                    WorkspaceLayer workspaceLayer = new WorkspaceLayer(targetDir);
                    this.workspaces.remove(workspaceLayer);
                    this.workspaces.add(workspaceLayer);
                } finally {
                    this.workspacesLock.writeLock().unlock();
                }
                return true;
            }
        } catch (IOException e) {
            logger.error("selectWorkspace", e);
        }

        return false;
        */
    }

    @Override
    public File getFolder(FolderType folderType) throws FileNotFoundException {
        checkFolderType(folderType);

        try {
            this.workspacesLock.readLock().lock();
            File targetDir = this.workspaces.get(this.workspaces.size() - 1).get(folderType);
            if (targetDir.exists() || targetDir.mkdirs()) {
                return targetDir;
            }

            throw new FileNotFoundException("Folder {" + folderType + "}/" + targetDir + " can't be created");
        } finally {
            this.workspacesLock.readLock().unlock();
        }
    }

    @Override
    public File getFile(FolderType folderType, String... fileName) throws FileNotFoundException, WorkspaceSecurityException {
        checkFolderTypeAndFileName(folderType, fileName);
        return getExists(folderType, fileName).getValue();
    }

    @Override
    public File getOrCreateFile(FolderType folderType, String... fileName) throws WorkspaceStructureException, WorkspaceSecurityException, FileNotFoundException {
        checkFolderTypeAndFileName(folderType, fileName);

        try {
            this.workspacesLock.readLock().lock();
            File targetFile = new File(this.workspaces.get(this.workspaces.size() - 1).get(folderType), toPathString(fileName));

            if (targetFile.exists()) {
                return targetFile;
            }

            WorkspaceLayer workspaceLayer = this.workspaces.get(this.workspaces.size() - 1);
            if (workspaceLayer.layout.isEmbedded()) {
                throw new WorkspaceLayerException("File {" + folderType + "}/" + toPathString(fileName) + " can't be created, the last layer is embedded");
            }

            try {
                File targetDir = targetFile.getParentFile();
                targetDir.mkdirs();
                if (targetFile.createNewFile()) {
                    return targetFile;
                }
            } catch (IOException e) {
                throw new WorkspaceStructureException("File {" + folderType + "}/" + toPathString(fileName) + " can't be created", e);
            }

            throw new WorkspaceStructureException("File {" + folderType + "}/" + toPathString(fileName) + " can't be created");
        } finally {
            this.workspacesLock.readLock().unlock();
        }
    }

    @Override
    public File createFolder(FolderType folderType, String... folderName) throws WorkspaceStructureException {
        checkFolderTypeAndFileName(folderType, folderName);

        try {
            this.workspacesLock.readLock().lock();
            WorkspaceLayer workspaceLayer = this.workspaces.get(this.workspaces.size() - 1);
            String path = toPathString(folderName);
            if (workspaceLayer.layout.isEmbedded()) {
                throw new WorkspaceLayerException("Folder {" + folderType + "}/" + path + " can't be created, the last layer is embedded");
            }

            File targetFolder = workspaceLayer.get(folderType);
            targetFolder = new File(targetFolder, path);

            if (targetFolder.exists() || targetFolder.mkdirs()) {
                return targetFolder;
            }

            throw new WorkspaceStructureException("Folder {" + folderType + "}/" + targetFolder + " can't be created");
        } finally {
            this.workspacesLock.readLock().unlock();
        }
    }

    /**
     * Create file in the last workspace
     * @throws WorkspaceStructureException
     */
    @Override
    public File createFile(FolderType folderType, boolean overwrite, String... fileName) throws WorkspaceStructureException {
        checkFolderTypeAndFileName(folderType, fileName);

        try {
            this.workspacesLock.readLock().lock();
            WorkspaceLayer workspaceLayer = this.workspaces.get(this.workspaces.size() - 1);
            String path = toPathString(fileName);
            if (workspaceLayer.layout.isEmbedded()) {
                throw new WorkspaceLayerException("Folder {" + folderType + "}/" + path + " can't be created, the last layer is embedded");
            }

            File targetFile = new File(workspaceLayer.get(folderType), path);
            File targetDir = targetFile.getParentFile();

            if (targetFile.exists()) {
            	if (overwrite) {
            		try {
						FileUtils.forceDelete(targetFile);
					} catch (IOException e) {
						throw new WorkspaceLayerException("File {" + folderType + "}/" + path + " can't be deleted from the last layer", e);
					}
            	} else {
            		throw new WorkspaceLayerException("File {" + folderType + "}/" + path + " already exist on the last layer");
            	}
            }

            try {
            	targetDir.mkdirs();
                if (targetFile.createNewFile()) {
                    return targetFile;
                }
            } catch (IOException e) {
                throw new WorkspaceStructureException("File {" + folderType + "}/" + toPathString(fileName) + " can't be created", e);
            }

            throw new WorkspaceStructureException("File {" + folderType + "}/" + toPathString(fileName) + " can't be created");
        } finally {
            this.workspacesLock.readLock().unlock();
        }
    }

    @Override
    public boolean exists(FolderType folderType, String... fileName) throws WorkspaceSecurityException {
        if (folderType != null && fileName != null) {
            try {
                this.workspacesLock.readLock().lock();
                getExists(folderType, fileName);
                return true;
            } catch (FileNotFoundException | WorkspaceSecurityException e) { // FIXME: is it ok to drop WorkspaceSecurityException ?
            	// it is normal control flow path: return false
            } finally {
                this.workspacesLock.readLock().unlock();
            }
        }
        return false;
    }


	@Override
	public void removeFile(FolderType folderType, String... fileName) throws IOException, FileNotFoundException, WorkspaceSecurityException {
        checkFolderTypeAndFileName(folderType, fileName);
        Entry<WorkspaceLayer, File> entry = getExists(folderType, fileName);
        File file = entry.getValue();

        if (!file.isFile()) {
        	throw new WorkspaceSecurityException("Not a file");
        }

        if (entry.getKey().layout.isEmbedded()) {
            throw new WorkspaceLayerException("File {" + folderType + "}/" + toPathString(fileName) + " can't be removed, the last layer is embedded");
        }

		FileUtils.forceDelete(file);
	}


	@Override
	public void removeFolder(FolderType folderType, String... fileName) throws IOException, FileNotFoundException, WorkspaceSecurityException {
        checkFolderTypeAndFileName(folderType, fileName);
        Entry<WorkspaceLayer, File> entry = getExists(folderType, fileName);
        File folder = entry.getValue();

        if (!folder.isDirectory()) {
        	throw new WorkspaceSecurityException("Not a directory");
        }

        if (entry.getKey().layout.isEmbedded()) {
            throw new WorkspaceLayerException("Folder {" + folderType + "}/" + toPathString(fileName) + " can't be removed, the last layer is embedded");
        }

        removeFolderView(folderType, fileName);
	}

	private void removeFolderView(FolderType folderType, String... fileName) throws IOException, WorkspaceSecurityException {
		Set<String> toRemove = listFiles(null, folderType, fileName);
        for (String path : toRemove) {
        	String relevantPath = toPathString(toPathString(fileName), path);
        	File targetfile = getExists(folderType, relevantPath).getValue();
        	if (targetfile.isDirectory()) {
        		removeFolderView(folderType, relevantPath);
        	} else {
        		FileUtils.forceDelete(targetfile);
        	}
        }
        // remove directory
        File dir = getExists(folderType, fileName).getValue();
        FileUtils.forceDelete(dir);
	}

    @Override
    public Set<String> listFiles(FileFilter filter, FolderType folderType, String... folderName) throws FileNotFoundException, WorkspaceSecurityException {
        return listFiles(filter, folderType, false, folderName);
    }

	@Override
	public Set<String> listFiles(FileFilter filter, FolderType folderType, boolean recursive, String... folderName) throws FileNotFoundException, WorkspaceSecurityException {
        checkFolderType(folderType);

        if (folderName == null) {
            folderName = new String[0];
        }

		File folder = getExists(folderType, folderName).getValue();

		if (!folder.isDirectory()) {
			throw new WorkspaceSecurityException("Not a directory");
		}

		try {
			this.workspacesLock.readLock().lock();

			Set<String> result = new HashSet<>();

	        for (int i = 0; i < this.workspaces.size(); i++) {
	        	File folderInWorkspace = new File(this.workspaces.get(i).get(folderType), toPathString(folderName));
	        	if (!folderInWorkspace.exists()) {
	        	    continue;
	        	}
	        	
	        	Path pathInWorkspace = folderInWorkspace.toPath();
                try {
                    Files.walk(pathInWorkspace, recursive ? Integer.MAX_VALUE : 1, FileVisitOption.FOLLOW_LINKS)
                            .map(Path::toFile)
                            .filter(file -> filter == null || filter.accept(file))
                            .map(file -> pathInWorkspace.relativize(file.toPath()).toString())
                            .forEach(result::add);
                } catch(IOException e) {
                    logger.error("Failed to list files in: {}", pathInWorkspace, e);
                }
	        }

            result.remove(StringUtils.EMPTY);

	        return result;

		} finally {
			this.workspacesLock.readLock().unlock();
		}

	}

    /**
     * Find file in workspaces (search order: form last workspace to deploy folder)
     * @param folderType
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    private Map.Entry<WorkspaceLayer, File> getExists(FolderType folderType, String... fileName) throws FileNotFoundException, WorkspaceSecurityException {
        File targetFile = null;
        try {
        	this.workspacesLock.readLock().lock();
	        for (int i = this.workspaces.size() - 1; i >= 0; i--) {
	            WorkspaceLayer layer = this.workspaces.get(i);
	        	targetFile = new File(layer.get(folderType), toPathString(fileName));

	        	Path targetPath = targetFile.toPath().normalize();
	        	Path rootPath = this.workspaces.get(i).get(folderType).toPath().normalize();
	        	if (! targetPath.startsWith(rootPath)) {
	        		throw new WorkspaceSecurityException("Access outside workspace is denied. Access file: {" + folderType + "}/" + toPathString(fileName));
	        	}


	            if (targetFile.exists()) {
	                return Maps.immutableEntry(layer, targetFile);
	            }
	        }
        } finally {
        	this.workspacesLock.readLock().unlock();
        }

        throw new FileNotFoundException("Workspaces doesn't contain file {" + folderType + "}/" + toPathString(fileName));
    }

    private boolean isDuplicatePath(File rootFolder) {
    	Path newPath = rootFolder.toPath();
    	for (int i = 0; i < this.workspaces.size(); i++) {
    		Path existing = this.workspaces.get(i).getRootFolder().toPath();
            if (newPath.startsWith(existing) || existing.startsWith(newPath)) {
                return true;
            }
        }

        return false;
    }

    private class WorkspaceLayer {
        private final File rootFolder;
        private final Map<FolderType, File> paths;
        private final IWorkspaceLayout layout;

        public WorkspaceLayer(File rootFolder, IWorkspaceLayout layout) throws IOException {
            if (rootFolder == null) {
                throw new NullPointerException("RootFolder can't be null");
            }
            if (layout == null) {
                throw new NullPointerException("Layout can't be null");
            }

            this.rootFolder = rootFolder.getCanonicalFile();
            this.layout = layout;
            this.paths = createFolderMap(this.rootFolder, createDirectories);
        }

        // Return canonical path to ROOT folder
        public File getRootFolder() {
            return rootFolder;
        }

        public File get(FolderType folderType) {
            return this.paths.get(folderType);
        }

        private Map<FolderType, File> createFolderMap(File rootDir, boolean createDir) {
            Map<FolderType, File> result = new EnumMap<>(FolderType.class);
            File targetDir = null;

            for (FolderType folderType : FolderType.values()) {
                targetDir = new File(layout.getPath(rootDir, folderType));
                result.put(folderType, targetDir);

                if (createDir && !layout.isEmbedded() && !targetDir.exists()) {
                    if (targetDir.mkdirs()) {
                        logger.info("Directory {} has created in folder {}", targetDir.getName(), rootDir.getName());
                    }
                }
            }

            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof WorkspaceLayer) {
                return this.rootFolder.equals(((WorkspaceLayer)obj).rootFolder);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.rootFolder.hashCode();
        }
    }

	private String toPathString(String... paths) {
		if (paths == null || paths.length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		for(int i = 0; i < paths.length; i++) {
		    sb.append(FilenameUtils.separatorsToSystem(paths[i]));

		    if(i < paths.length - 1) {
		        sb.append(File.separator);
		    }
		}

		return sb.toString();
	}

    @Override
    public File getWritableFile(FolderType folderType, String... fileName) throws WorkspaceSecurityException, IOException {
        checkFolderTypeAndFileName(folderType, fileName);

        Entry<WorkspaceLayer, File> entry = getExists(folderType, fileName);
        File sourceFile = entry.getValue();

        if(!sourceFile.isFile()) {
            throw new WorkspaceSecurityException("Not a file: " + toPathString(fileName));
        }

        File targetFile = getOrCreateFile(folderType, fileName);

        if(Files.isSameFile(sourceFile.toPath(), targetFile.toPath())) {
            if (entry.getKey().layout.isEmbedded()) {
                throw new WorkspaceLayerException("File {" + folderType + "}/" + toPathString(fileName) + " can't be writable, the last layer is embedded");
            }
            return targetFile;
        }

        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

        return targetFile;
    }

    private void checkFolderTypeAndFileName(FolderType folderType, String... fileName) {
        checkFolderType(folderType);
        checkFileName(fileName);
    }

    private void checkFolderType(FolderType folderType) {
        if(folderType == null) {
            throw new IllegalArgumentException("FolderType can't be null");
        }
    }

    private void checkFileName(String... fileName) {
        if(fileName == null) {
            throw new IllegalArgumentException("FileName can't be null");
        }
    }
}