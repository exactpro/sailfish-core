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
package com.exactpro.sf.externalapi.codec.impl

import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.util.ICommonSettings
import com.exactpro.sf.configuration.suri.SailfishURI
import com.exactpro.sf.configuration.workspace.FolderType
import com.exactpro.sf.externalapi.DictionaryProperty
import com.exactpro.sf.externalapi.DictionaryType
import com.exactpro.sf.externalapi.DictionaryType.MAIN
import com.exactpro.sf.externalapi.codec.IExternalCodecSettings
import com.exactpro.sf.externalapi.codec.PluginAlias
import com.exactpro.sf.externalapi.codec.ResourcePath
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.apache.commons.beanutils.PropertyUtilsBean
import org.apache.commons.lang3.reflect.FieldUtils.getFieldsListWithAnnotation
import java.beans.PropertyDescriptor
import java.io.File
import java.util.Collections.unmodifiableMap
import java.util.EnumMap

class ExternalCodecSettings(
    private val settings: ICommonSettings
) : IExternalCodecSettings {
    private val properties: Map<String, PropertyDescriptor> = PropertyUtilsBean().run {
        getPropertyDescriptors(settings).asSequence()
            .filter { it.readMethod != null && it.writeMethod != null }
            .associateBy { it.name }
            .withDefault { throw IllegalArgumentException("Property does not exist: $it") }
    }

    private val dictionaryProperties: Map<DictionaryType, PropertyDescriptor> = getFieldsListWithAnnotation(settings::class.java, DictionaryProperty::class.java).run {
        val map = hashMapOf<DictionaryType, PropertyDescriptor>()

        forEach { field ->
            val descriptor = checkNotNull(properties[field.name]) { "Field is not a property: ${field.name}" }

            check(field.type == SailfishURI::class.java || field.type == IDictionaryStructure::class.java) {
                "Invalid dictionary property type: ${field.type.canonicalName} (field: ${field.name})"
            }

            val type = field.getAnnotation(DictionaryProperty::class.java).type

            check(map.put(type, descriptor) == null) {
                "Duplicate dictionary property type: $type"
            }
        }

        map
    }

    private val dictionaries: MutableMap<DictionaryType, IDictionaryStructure> = EnumMap(DictionaryType::class.java)

    override val dataFiles: MutableMap<SailfishURI, File> = hashMapOf()
    override val dataResources: Table<PluginAlias, ResourcePath, File> = HashBasedTable.create()
    override val dictionaryFiles: MutableMap<SailfishURI, File> = hashMapOf()
    override val workspaceFolders: MutableMap<FolderType, File> = EnumMap(FolderType::class.java)
    override val propertyTypes: Map<String, Class<*>> = unmodifiableMap(properties.mapValues { (_, descriptor) -> descriptor.propertyType })
    override val dictionaryTypes: Set<DictionaryType> = dictionaryProperties.keys + MAIN

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getSettings(): T = settings as T

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(propertyName: String): T = try {
        properties.getValue(propertyName).readMethod(settings) as T
    } catch (e: Exception) {
        throw PropertyReadException("Failed to get property value: $propertyName", e)
    }

    override fun set(propertyName: String, propertyValue: Any?) {
        try {
            properties.getValue(propertyName).writeMethod(settings, propertyValue)
        } catch (e: Exception) {
            throw PropertyWriteException("Failed to set property '$propertyName' value to: $propertyValue", e)
        }
    }

    override fun get(dictionaryType: DictionaryType): IDictionaryStructure? {
        check(dictionaryType in dictionaryTypes) { "No dictionary property with type: $dictionaryType" }
        return dictionaries[dictionaryType]
    }

    override fun set(dictionaryType: DictionaryType, dictionary: IDictionaryStructure) {
        check(dictionaryType in dictionaryTypes) { "No dictionary property with type: $dictionaryType" }
        dictionaries[dictionaryType] = dictionary

        if (dictionaryType != MAIN) {
            dictionaryProperties[dictionaryType]!!.apply {
                when (propertyType) {
                    SailfishURI::class.java -> set(name, dictionaryType.toUri())
                    IDictionaryStructure::class.java -> set(name, dictionary)
                }
            }
        }
    }
}