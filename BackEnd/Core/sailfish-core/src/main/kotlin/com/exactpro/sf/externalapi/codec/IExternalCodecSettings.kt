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
package com.exactpro.sf.externalapi.codec

import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.configuration.suri.SailfishURI
import com.exactpro.sf.configuration.workspace.FolderType
import com.google.common.collect.Table
import java.io.File

typealias PluginAlias = String
typealias ResourcePath = String

interface IExternalCodecSettings {
    /**
     * Data files mapped to their URI
     */
    val dataFiles: MutableMap<SailfishURI, File>

    /**
     * Data resources mapped to plugin alias and their path
     */
    val dataResources: Table<PluginAlias, ResourcePath, File>

    /**
     * Dictionary files mapped to their URI
     */
    val dictionaryFiles: MutableMap<SailfishURI, File>

    /**
     * Workspaces folders mapped to their folder type
     */
    val workspaceFolders: MutableMap<FolderType, File>

    /**
     * Dictionary which can be used by a codec
     */
    var dictionary: IDictionaryStructure

    /**
     * Map of available codec settings properties types
     */
    val propertyTypes: Map<String, Class<*>>

    /**
     * Returns an instance of internal codec settings
     */
    fun <T : Any> getSettings(): T

    /**
     * Return a value of a codec settings property
     * @param propertyName name of a settings property
     * @return value of a settings property
     */
    operator fun <T> get(propertyName: String): T

    /**
     * Sets a value of a codec settings property
     * @param propertyName name of a settings property
     * @param propertyValue value of a settings property
     */
    operator fun set(propertyName: String, propertyValue: Any?)
}

