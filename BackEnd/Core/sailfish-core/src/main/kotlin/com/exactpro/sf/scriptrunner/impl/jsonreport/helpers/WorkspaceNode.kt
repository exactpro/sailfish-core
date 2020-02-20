/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.scriptrunner.impl.jsonreport.helpers

import com.exactpro.sf.configuration.workspace.FolderType
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

//FIXME: The existence of this class actually breaks the idea of the workspace dispatcher. This functionality should be integrated into the dispatcher itself.
class WorkspaceNode(private val dispatcher: IWorkspaceDispatcher, private val type: FolderType, private val relativePath: Path = Paths.get("")) {

    fun getSubNode(repativeSubPath: String): WorkspaceNode {
        return getSubNode(Paths.get(repativeSubPath))
    }

    fun getSubNode(relativeSubPath: Path): WorkspaceNode {
        if (relativeSubPath.isAbsolute) {
            throw IllegalArgumentException("$relativeSubPath should be a relative path")
        }

        return WorkspaceNode(dispatcher, type, relativePath.resolve(relativeSubPath))
    }

    fun toFile(overwrite: Boolean = false): File {
        return if (overwrite)
            dispatcher.createFile(type, true, relativePath.toString())
        else
            dispatcher.getOrCreateFile(type, relativePath.toString())
    }

    fun toAbsolutePath(overwrite: Boolean = false): Path {
        return toFile(overwrite).toPath()
    }
}
