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

import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Index
import com.exactpro.sf.scriptrunner.impl.jsonreport.helpers.WorkspaceNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption

private val logger = KotlinLogging.logger {}

private data class DataFile(val file: WorkspaceNode, var counter: Int = 0)

class JsonpChunkedDataWriter<T : IJsonReportNode>(
        loaderFunctionPrefix: String,
        private val dataStreamDirectory: WorkspaceNode,
        private val reportRootDirectory: WorkspaceNode,
        private val perFileItemLimit: Int,
        private val mapper: ObjectMapper) {

    private val template = "load$loaderFunctionPrefix(%d, %s);"
    private val rootFileTemplate = "load${loaderFunctionPrefix}Index(%s);"
    val indexFile: WorkspaceNode get() = dataStreamDirectory.getSubNode("index.js")

    private val dataFiles: MutableList<DataFile> = mutableListOf()
    private var currentDataFile: DataFile? = null

    private var globalCounter = 0

    init {
        updateRootFile()
    }

    fun writeNode(node: T) {
        if (currentDataFile == null || currentDataFile!!.counter >= perFileItemLimit) {
            switchToNextFile()
        }

        try {
            increaseItemCounter()
            write(template.format(currentDataFile!!.counter, mapper.writeValueAsString(node)))
            updateRootFile()
        } catch (e: Exception) {
            logger.error(e) { "unable to write a node" }
        }
    }

    private fun write(data: String) {
        try {
            Files.write(currentDataFile!!.file.toAbsolutePath(), data.toByteArray(), StandardOpenOption.APPEND)
        } catch (e: IOException) {
            logger.error(e) { "unable to write data to a file" }
        }
    }

    @Suppress("unused")
    private fun updateRootFile() {
        try {
            val data: String = rootFileTemplate.format(mapper.writeValueAsString(Index(
                    globalCounter,
                    dataFiles.associate { reportRootDirectory.toAbsolutePath().relativize(it.file.toAbsolutePath()).toString() to it.counter }
            )))

            Files.write (indexFile.toAbsolutePath(), data.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)

        } catch (e: IOException) {
            logger.error(e) { "unable to update live report file" }
        }
    }

    private fun switchToNextFile() {
        val dataFile = DataFile(dataStreamDirectory.getSubNode("data${dataFiles.size}.js"))

        currentDataFile = dataFile
        dataFiles.add(dataFile)
    }

    private fun increaseItemCounter() {
        globalCounter++

        val dataFile = currentDataFile
        if (dataFile != null) {
            dataFile.counter = dataFile.counter + 1
        }
    }
}
