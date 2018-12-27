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
import java.util.Set;

/**
 * Implements UnionMount[1]-like mechanism for SF
 * We will use it to override dictionaries and configurations
 *
 * [1] https://en.wikipedia.org/wiki/Union_mount
 *
 * Some ideas can be interesting:
 * https://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/tree/Documentation/filesystems/overlayfs.txt
 *
 * @author nikita.smirnov
 *
 */
public interface IWorkspaceDispatcher {

    /**
     * Get folder from last workspace layer
     * @param folderType
     * @return returns existing canonical folder from workspace
     * @throws FileNotFoundException If last workspace layer does not contain folder
     */
    File getFolder(FolderType folderType) throws FileNotFoundException;

    /**
     * Get file from workspace layers
     * @param folderType
     * @param fileName
     * @return
     * @throws FileNotFoundException If no workspace layer contains file
     * @throws WorkspaceSecurityException - if we try to access file outside the workspace (for example '../password.txt')
     */
    File getFile(FolderType folderType, String... fileName) throws FileNotFoundException, WorkspaceSecurityException;

    /**
     *
     * @param folderType
     * @param folderName
     * @return
     * @throws WorkspaceStructureException
     * @throws WorkspaceSecurityException
     */
    File createFolder(FolderType folderType, String... folderName) throws WorkspaceStructureException, WorkspaceSecurityException;

    /**
	 *
	 * @param folderType
	 * @param overwrite - overwrite file on last layer
	 * @param fileName
	 * @return
	 * @throws WorkspaceStructureException
	 * @throws WorkspaceLayerException - if the named file already exists on last layer and parameter overwrite false
	 *             or file can't be deleted from the last layer and overwrite true
	 */
    File createFile(FolderType folderType, boolean overwrite, String... fileName) throws WorkspaceStructureException, WorkspaceSecurityException;


    /**
     * Return file from writable workspace layer (last layer) or create it
     * @param folderType
     * @param fileName
     * @return
     * @throws WorkspaceStructureException
     * @throws WorkspaceSecurityException
     * @throws FileNotFoundException
     */
    File getOrCreateFile(FolderType folderType, String... fileName) throws WorkspaceStructureException, WorkspaceSecurityException, FileNotFoundException;

    boolean exists(FolderType folderType, String... fileName) throws WorkspaceSecurityException;

    void removeFile(FolderType folderType, String... fileName) throws FileNotFoundException, IOException, WorkspaceSecurityException;

    void removeFolder(FolderType folderType, String... fileName) throws FileNotFoundException, IOException, WorkspaceSecurityException;

    /**
     *
     * Note: filter will be applied to all workspace layers.
     *
     * @param filter
     * @param folderType
     * @param fileName
     * @return Set of relative to folderType paths
     * @throws FileNotFoundException
     * @throws WorkspaceSecurityException
     */
    Set<String> listFiles(FileFilter filter, FolderType folderType, String... fileName) throws FileNotFoundException, WorkspaceSecurityException;

    /**
    *
    * Note: filter will be applied to all workspace layers.
    *
    * @param filter
    * @param folderType
    * @param fileName
    * @return Set of relative to folderType paths
    * @throws FileNotFoundException
    * @throws WorkspaceSecurityException
    */
    default Set<String> listFiles(FileFilter filter, FolderType folderType, boolean recursive, String... fileName) throws FileNotFoundException, WorkspaceSecurityException {
        if(recursive) {
            throw new UnsupportedOperationException("Recursive file listing is unsupported");
        }

        return listFiles(filter, folderType, fileName);
    }

    /**
     * Use path as workspace layer. New layer become highest priority layer
     * @param workspacePath
     * @param workspaceLayout
     * @return
     * @throws FileNotFoundException - directory @workspacePath doesn't exists
     * @throws UnsupportedOperationException
     */
	boolean selectWorkspace(String workspacePath, IWorkspaceLayout layout) throws FileNotFoundException, UnsupportedOperationException;

    /**
     * Moves file to the last layer. If it's there already then operation has no effect
     * @return writeable file from last layer
     */
    File getWritableFile(FolderType folderType, String... fileName) throws WorkspaceSecurityException, IOException;
}
