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

package com.exactpro.sf.scriptrunner.impl.jsonreport.beans

import com.exactpro.sf.scriptrunner.impl.jsonreport.IJsonReportNode
import com.exactpro.sf.scriptrunner.impl.jsonreport.JsonpChunkedDataWriter
import com.exactpro.sf.scriptrunner.impl.jsonreport.helpers.WorkspaceNode
import java.nio.file.Path
import java.time.Instant

class JsonpTestcase(
        dataWriters: Map<Class<out IJsonReportNode>, JsonpChunkedDataWriter<out IJsonReportNode>>,
        testCase: TestCase,
        reportRoot: WorkspaceNode,
        val lastUpdate: Instant = Instant.now()) {

    val id: String? = testCase.id

    val tags: Set<String>? = testCase.tags
    val outcomes: List<OutcomeSummary> = testCase.outcomes

    val startTime: Instant? = testCase.startTime
    val finishTime: Instant? = testCase.finishTime

    val name: String? = testCase.name
    val type: String? = testCase.type
    val reference: String? = testCase.reference
    val order: Int = testCase.order
    val matrixOrder: Int = testCase.matrixOrder
    val hash: Int = testCase.hash
    val description: String? = testCase.description
    val status: Status? = testCase.status
    val bugs: Collection<IJsonReportNode> = testCase.bugTree

    val indexFiles: Map<String, String> = dataWriters.asSequence().associate {
        it.key.simpleName.toLowerCase() to reportRoot.toAbsolutePath().relativize(it.value.indexFile.toAbsolutePath()).toString()
    }
}
