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
import com.exactpro.sf.externalapi.DictionaryType
import com.exactpro.sf.externalapi.DictionaryType.MAIN
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
    @Deprecated(message = "Set dictionaries by type instead")
    val dictionaryFiles: MutableMap<SailfishURI, File>

    /**
     * Workspaces folders mapped to their folder type
     */
    val workspaceFolders: MutableMap<FolderType, File>

    /**
     * Main codec dictionary
     */
    @Deprecated(message = "Access main dictionary via indexing operators instead")
    var dictionary: IDictionaryStructure
        get() = checkNotNull(this[MAIN]) { "Main dictionary is not set" }
        set(value) {
            this[MAIN] = value
        }

    /**
     * Map of available codec settings properties types
     */
    val propertyTypes: Map<String, Class<*>>

    /**
     * Set of dictionary types required by codec
     */
    val dictionaryTypes: Set<DictionaryType>

    /**
     * Returns an instance of internal codec settings
     */
    fun <T : Any> getSettings(): T

    /**
     * Returns a value of a codec settings property
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

    /**
     * Returns a value of a dictionary property
     * @param dictionaryType type of a dictionary property
     * @return value of a dictionary property
     */
    operator fun get(dictionaryType: DictionaryType): IDictionaryStructure?

    /**
     * Sets a value of a dictionary property
     * @param dictionaryType type of a dictionary property
     * @param dictionary value of a dictionary property
     */
    operator fun set(dictionaryType: DictionaryType, dictionary: IDictionaryStructure)
}

