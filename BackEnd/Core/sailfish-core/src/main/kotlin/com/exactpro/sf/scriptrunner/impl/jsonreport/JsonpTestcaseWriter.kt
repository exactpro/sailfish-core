/******************************************************************************
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
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Action
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.JsonpTestcase
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.LogEntry
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Message
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.TestCase
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

private val TEST_CASE_JSONP_TEMPLATE = "window.loadTestCase(%s);"

private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .setDateFormat(JsonReport.DATE_FORMAT)


class JsonpTestcaseWriter(testcaseNumber: Number, jsonpReportDir: Path, private val reportRootDir: Path, dispatcher: IWorkspaceDispatcher) {
    private val testcaseDirectoryPath = jsonpReportDir.resolve("testcase-$testcaseNumber")

    val testCaseFilePath: Path = dispatcher.createFile(FolderType.REPORT, true, reportRootDir.parent.relativize(testcaseDirectoryPath).resolve("testcase.js").toString()).toPath()

    private val dataStreams = mapOf(
            Message::class.java to JsonpChunkedDataWriter<Message>("Message", testcaseDirectoryPath.resolve("messages"), reportRootDir, dispatcher, 100, mapper),
            Action::class.java to JsonpChunkedDataWriter<Action>("Action", testcaseDirectoryPath.resolve("actions"), reportRootDir, dispatcher, 50, mapper),
            LogEntry::class.java to JsonpChunkedDataWriter<LogEntry>("LogEntry", testcaseDirectoryPath.resolve("logs"), reportRootDir, dispatcher, 100, mapper)
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T : IJsonReportNode> getReportDataStream(clazz: Class<T>): JsonpChunkedDataWriter<T> {
        return (dataStreams[clazz] ?: error("Unknown type: $clazz. Supported types: ${dataStreams.keys}")) as JsonpChunkedDataWriter<T>
    }

    fun updateTestCaseFile(testcase: TestCase) {
        Files.write(testCaseFilePath, TEST_CASE_JSONP_TEMPLATE.format(
                mapper.writeValueAsString(JsonpTestcase(dataStreams, testcase, reportRootDir))
        ).toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
    }

    fun <T : IJsonReportNode> write(node: T) {
        getReportDataStream(node.javaClass).writeNode(node)
    }
}
