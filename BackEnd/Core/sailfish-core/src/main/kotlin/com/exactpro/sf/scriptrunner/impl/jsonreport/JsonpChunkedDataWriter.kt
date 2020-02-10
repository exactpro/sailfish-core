/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.scriptrunner.impl.jsonreport

import com.exactpro.sf.configuration.workspace.FolderType
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Index
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

private val logger = KotlinLogging.logger {}

class JsonpChunkedDataWriter<T : IJsonReportNode>(
        loaderFunctionPrefix: String,
        private val directory: Path,
        private val reportRootDirectory: Path,
        private val dispatcher: IWorkspaceDispatcher,
        private val perFileItemLimit: Int,
        private val mapper: ObjectMapper
) {
    private val template = "load$loaderFunctionPrefix(%d, %s);"
    private val rootFileTemplate = "load${loaderFunctionPrefix}Index(%s);"
    val rootFilePath: Path get() = directory.resolve("index.js")

    private val dataFileNames: MutableMap<String, Int?> = mutableMapOf()
    private var dataFileCounter = 0
    private var currentFileItemCounter = 0
    private var globalItemCounter = 0
    private var currentFilePath: Path? = null

    private var currentFileName: String? = null

    init {
        updateRootFile()
    }

    fun writeNode(node: T) {
        if (globalItemCounter == 0 || currentFileItemCounter >= perFileItemLimit) {
            switchToNextFile()
        }

        try {
            increaseItemCounter()
            write(template.format(globalItemCounter, mapper.writeValueAsString(node)))
            updateRootFile()
        } catch (e: Exception) {
            logger.error(e) { "unable to write a node" }
        }
    }

    private fun write(data: String) {
        try {
            Files.write(currentFilePath!!, data.toByteArray(), StandardOpenOption.APPEND)
        } catch (e: IOException) {
            logger.error(e) { "unable to write data to a file" }
        }
    }

    @Suppress("unused")
    private fun updateRootFile() {
        try {
            val rootFilePath: Path = dispatcher.createFile(FolderType.REPORT, true, reportRootDirectory.parent.relativize(rootFilePath).toString()).toPath()

            val data: String = rootFileTemplate.format(mapper.writeValueAsString(Index(globalItemCounter, dataFileNames.mapKeys {
                reportRootDirectory.relativize(directory.resolve(it.key)).toString()
            })))

            Files.write(rootFilePath, data.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)

        } catch (e: IOException) {
            logger.error(e) { "unable to update live report file" }
        }
    }

    private fun switchToNextFile() {
        dataFileCounter++
        val path: String = reportRootDirectory.parent.relativize(directory.resolve("data$dataFileCounter.js")).toString()

        try {
            currentFilePath = dispatcher.createFile(FolderType.REPORT, true, path).toPath()

            currentFileName = currentFilePath!!.fileName.toString()
            dataFileNames[currentFileName!!] = null
            currentFileItemCounter = 0

        } catch (e: IOException) {
            logger.error(e) { "unable to create jsonp file '$path'" }
        }
    }

    private fun increaseItemCounter() {
        currentFileItemCounter++
        globalItemCounter++
        dataFileNames[currentFileName!!] = globalItemCounter
    }
}
