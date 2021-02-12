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

import com.exactpro.sf.aml.DictionarySettings
import com.exactpro.sf.aml.generator.AlertCollector
import com.exactpro.sf.common.codecs.AbstractCodec
import com.exactpro.sf.common.messages.IMessageFactory
import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader
import com.exactpro.sf.common.services.ServiceInfo
import com.exactpro.sf.common.services.ServiceName
import com.exactpro.sf.configuration.IDataManager
import com.exactpro.sf.configuration.IDictionaryManager
import com.exactpro.sf.configuration.IDictionaryManagerListener
import com.exactpro.sf.configuration.IDictionaryRegistrator
import com.exactpro.sf.configuration.ILoggingConfigurator
import com.exactpro.sf.configuration.suri.SailfishURI
import com.exactpro.sf.configuration.suri.SailfishURIRule.REQUIRE_RESOURCE
import com.exactpro.sf.configuration.suri.SailfishURIUtils.getMatchingValue
import com.exactpro.sf.configuration.workspace.FolderType
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher
import com.exactpro.sf.configuration.workspace.IWorkspaceLayout
import com.exactpro.sf.externalapi.DictionaryType.MAIN
import com.exactpro.sf.externalapi.codec.IExternalCodec
import com.exactpro.sf.externalapi.codec.IExternalCodecSettings
import com.exactpro.sf.externalapi.codec.PluginAlias
import com.exactpro.sf.externalapi.codec.ResourcePath
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityInfo
import com.exactpro.sf.services.IServiceContext
import com.exactpro.sf.services.ITaskExecutor
import com.exactpro.sf.services.MessageHelper
import com.exactpro.sf.storage.IMessageStorage
import com.google.common.collect.ImmutableTable
import com.google.common.collect.Table
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.Collections
import java.util.EnumMap

abstract class AbstractExternalMinaCodecFactory : AbstractExternalCodecFactory() {
    protected abstract val codecClass: Class<out AbstractCodec>
    protected open val messageHelperClass: Class<out MessageHelper>? = null
    protected open val messageHelperParams: Map<String, String> = emptyMap()

    override fun createCodec(settings: IExternalCodecSettings): IExternalCodec = settings.run {
        val dictionary = checkNotNull(this[MAIN]) { "Main dictionary is not set" }

        val messageFactory = messageFactoryClass.newInstance().apply {
            init(SailfishURI.parse(dictionary.namespace), dictionary)
        }

        ExternalMinaCodec(
            codecClass,
            InternalServiceContext(
                InternalDataManager(dataFiles, dataResources),
                InternalDictionaryManager(this, messageFactory),
                InternalWorkspaceDispatcher(workspaceFolders)
            ),
            getSettings(),
            messageFactory,
            dictionary,
            messageHelperClass,
            messageHelperParams
        )
    }

    private class InternalServiceContext(
        private val dataManager: IDataManager,
        private val dictionaryManager: IDictionaryManager,
        private val workspaceDispatcher: IWorkspaceDispatcher
    ) : IServiceContext {
        override fun getDictionaryManager(): IDictionaryManager = dictionaryManager
        override fun getDataManager(): IDataManager = dataManager
        override fun getLoggingConfigurator(): ILoggingConfigurator = throw UnsupportedOperationException()
        override fun getMessageStorage(): IMessageStorage = throw UnsupportedOperationException()
        override fun lookupService(serviceName: ServiceName): ServiceInfo = throw UnsupportedOperationException()
        override fun getTaskExecutor(): ITaskExecutor = throw UnsupportedOperationException()
        override fun getWorkspaceDispatcher(): IWorkspaceDispatcher = workspaceDispatcher
    }

    private class InternalDataManager(
        dataFiles: Map<SailfishURI, File>,
        dataResources: Table<PluginAlias, ResourcePath, File>
    ) : IDataManager {
        private val dataFiles = dataFiles.run {
            forEach { (uri, file) ->
                require(file.isFile) { "File with URI '$uri' is not a file: ${file.absolutePath}" }
            }
            toMap()
        }

        private val dataResources = dataResources.run {
            cellSet().forEach { cell ->
                val pluginAlias = requireNotNull(cell.rowKey) { "Plugin alias is null: $cell" }
                val resourcePath = requireNotNull(cell.columnKey) { "Resource path is null: $cell" }
                val file = requireNotNull(cell.value) { "File is null: $cell" }
                require(file.isFile) { "File in plugin '$pluginAlias' with path '$resourcePath' is not a file: ${file.absolutePath}" }
            }

            ImmutableTable.copyOf(this)
        }

        override fun getExtension(uri: SailfishURI): String = dataFiles.getByUri(uri).extension
        override fun getRelativePathToPlugin(pluginAlias: String): Path = throw UnsupportedOperationException()
        override fun getDataInputStream(uri: SailfishURI): InputStream = dataFiles.getByUri(uri).inputStream()

        override fun getDataInputStream(pluginAlias: PluginAlias, resourcePath: ResourcePath): InputStream = checkNotNull(dataResources[pluginAlias, resourcePath]) {
            "No data resource in plugin '$pluginAlias' at path: $resourcePath"
        }.inputStream()

        override fun getDataOutputStream(uri: SailfishURI, append: Boolean): OutputStream = FileOutputStream(dataFiles.getByUri(uri), append)
        override fun exists(uri: SailfishURI): Boolean = getMatchingValue(uri, dataFiles, REQUIRE_RESOURCE) != null

    }
    private class InternalDictionaryManager(
        settings: IExternalCodecSettings,
        private val messageFactory: IMessageFactory
    ) : IDictionaryManager {
        private val dictionaryFiles = hashMapOf<SailfishURI, IDictionaryStructure>()

        init {
            settings.dictionaryFiles.mapValuesTo(dictionaryFiles) { (uri, file) ->
                require(file.isFile) { "File with URI '$uri' is not a file: ${file.absolutePath}" }

                file.inputStream().runCatching {
                    use(XmlDictionaryStructureLoader()::load)
                }.getOrElse {
                    throw Exception("Failed to load dictionary from file: ${file.absolutePath}", it)
                }
            }

            settings.dictionaryTypes.forEach { type ->
                dictionaryFiles[type.toUri()] = checkNotNull(settings[type]) {
                    "Dictionary type is not set: $type"
                }
            }
        }

        override fun getDictionaryURIs(pluginAlias: String): Set<SailfishURI> = dictionaryFiles.keys.asSequence().filter { it.pluginAlias == pluginAlias }.toSet()
        override fun getDictionaryURIs(): Set<SailfishURI> = Collections.unmodifiableSet(dictionaryFiles.keys)
        override fun createMessageDictionary(pathName: String): IDictionaryStructure = throw UnsupportedOperationException()
        override fun getSettings(uri: SailfishURI): DictionarySettings = throw UnsupportedOperationException()
        override fun getMessageFactory(uri: SailfishURI): IMessageFactory = messageFactory
        override fun subscribeForEvents(listener: IDictionaryManagerListener) = throw UnsupportedOperationException()
        override fun getDictionary(uri: SailfishURI): IDictionaryStructure = dictionaryFiles.getByUri(uri)
        override fun registerDictionary(title: String, overwrite: Boolean): IDictionaryRegistrator = throw UnsupportedOperationException()
        override fun createMessageFactory(): IMessageFactory = messageFactory
        override fun getDictionaryLocations(): Map<SailfishURI, String> = throw UnsupportedOperationException()
        override fun getDictionaryId(uri: SailfishURI): Long = dictionaryFiles.getByUri(uri).hashCode().toLong()
        override fun getCachedDictURIs(): List<SailfishURI> = dictionaryFiles.keys.toList()
        override fun invalidateDictionaries(vararg uris: SailfishURI) = throw UnsupportedOperationException()
        override fun unSubscribeForEvents(listener: IDictionaryManagerListener) = throw UnsupportedOperationException()
        override fun getUtilityURIs(dictionaryURI: SailfishURI): Set<SailfishURI> = throw UnsupportedOperationException()
        override fun getUtilityInfo(dictionaryURI: SailfishURI, utilityURI: SailfishURI, vararg argTypes: Class<*>): UtilityInfo = throw UnsupportedOperationException()

        override fun getUtilityInfo(
            dictionaryURI: SailfishURI,
            utilityURI: SailfishURI,
            line: Long,
            uid: Long,
            column: String,
            alertCollector: AlertCollector,
            vararg argTypes: Class<*>
        ): UtilityInfo = throw UnsupportedOperationException()
    }

    private class InternalWorkspaceDispatcher(workspaceFolders: Map<FolderType, File>) : IWorkspaceDispatcher {
        private val workspaceFolders = workspaceFolders.run {
            forEach { folderType, file ->
                require(file.isDirectory) { "File with folder type $folderType is not a directory: ${file.absolutePath}" }
            }

            EnumMap(workspaceFolders).withDefault {
                throw FileNotFoundException("Workspace folder does not exist: $it")
            }
        }

        override fun getFile(folderType: FolderType, vararg fileName: String): File = workspaceFolders.getValue(folderType).run {
            val path = fileName.asSequence()
                .map { FilenameUtils.separatorsToSystem(it) }
                .joinToString(File.separator)

            File(this, path).apply {
                if (!exists()) {
                    throw FileNotFoundException("File does not exist: $this")
                }
            }
        }

        override fun selectWorkspace(workspacePath: String, layout: IWorkspaceLayout): Boolean = throw UnsupportedOperationException()
        override fun listFilesAsFiles(filter: FileFilter, folderType: FolderType, recursive: Boolean, vararg fileName: String): Set<File> = throw UnsupportedOperationException()
        override fun removeFolder(folderType: FolderType, vararg fileName: String): Unit = throw UnsupportedOperationException()
        override fun createFile(folderType: FolderType, overwrite: Boolean, vararg fileName: String): File = throw UnsupportedOperationException()
        override fun isLastLayerFile(folderType: FolderType, vararg fileName: String): Boolean = throw UnsupportedOperationException()
        override fun getOrCreateFile(folderType: FolderType, vararg fileName: String): File = throw UnsupportedOperationException()
        override fun getFolder(folderType: FolderType): File = throw UnsupportedOperationException()
        override fun getWritableFile(folderType: FolderType, vararg fileName: String): File = throw UnsupportedOperationException()
        override fun listFiles(filter: FileFilter, folderType: FolderType, vararg fileName: String): Set<String> = throw UnsupportedOperationException()
        override fun createFolder(folderType: FolderType, vararg folderName: String): File = throw UnsupportedOperationException()
        override fun removeFile(folderType: FolderType, vararg fileName: String): Unit = throw UnsupportedOperationException()
        override fun exists(folderType: FolderType, vararg fileName: String): Boolean = throw UnsupportedOperationException()
    }
}

private fun <T : Any> Map<SailfishURI, T>.getByUri(uri: SailfishURI): T {
    return checkNotNull(getMatchingValue(uri, this, REQUIRE_RESOURCE)) {
        "No data with URI: $uri"
    }
}