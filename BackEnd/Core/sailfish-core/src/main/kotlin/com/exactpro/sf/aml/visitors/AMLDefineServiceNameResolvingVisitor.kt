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
package com.exactpro.sf.aml.visitors

import com.exactpro.sf.aml.AMLBlockBrace
import com.exactpro.sf.aml.AMLLangConst.BEGIN_STATIC
import com.exactpro.sf.aml.AMLLangConst.END_STATIC
import com.exactpro.sf.aml.AMLLangUtil.getStaticVariableName
import com.exactpro.sf.aml.AMLLangUtil.isExpression
import com.exactpro.sf.aml.AMLLangUtil.isStaticVariableReference
import com.exactpro.sf.aml.generator.Alert
import com.exactpro.sf.aml.generator.AlertCollector
import com.exactpro.sf.aml.generator.JavaValidator.validateVariableName
import com.exactpro.sf.aml.generator.matrix.Column.Action
import com.exactpro.sf.aml.generator.matrix.Column.Reference
import com.exactpro.sf.aml.generator.matrix.Column.ServiceName
import com.exactpro.sf.aml.generator.matrix.JavaStatement
import com.exactpro.sf.aml.generator.matrix.JavaStatement.DEFINE_SERVICE_NAME
import com.exactpro.sf.aml.reader.struct.AMLBlock
import com.exactpro.sf.aml.reader.struct.AMLElement
import java.util.SortedMap
import java.util.TreeMap
import com.exactpro.sf.common.services.ServiceName as ServiceNameBean

/**
 * Substitutes references to services defined via [JavaStatement.DEFINE_SERVICE_NAME].
 * Invalid definitions are ignored
 */
class AMLDefineServiceNameResolvingVisitor(
    private val environmentName: String,
    private val serviceNames: Array<ServiceNameBean>
) : IAMLElementVisitor {
    val definedServiceNames: MutableMap<String, SortedMap<Long, String>> = hashMapOf()
    val alertCollector: AlertCollector = AlertCollector()

    override fun visit(element: AMLElement) {
        if (!element.isExecutable) {
            return
        }

        val actionUri = element.getValue(Action)

        if (AMLBlockBrace.value(actionUri) != null) {
            return
        }

        val serviceName = element.getValue(ServiceName)
        val line = element.line.toLong()

        if (JavaStatement.value(actionUri) == DEFINE_SERVICE_NAME) {
            val uid = element.uid
            val reference = element.getValue(Reference)
            val referenceColumn = Reference.getName()
            val serviceNameColumn = ServiceName.getName()
            val errors = alertCollector.count

            when {
                serviceName.isNullOrBlank() -> alertCollector.add(Alert(line, uid, reference, serviceNameColumn, "Missing column"))
                !isExpression(serviceName) && ServiceNameBean(environmentName, serviceName) !in serviceNames -> {
                    alertCollector.add(Alert(line, uid, reference, serviceNameColumn, "Unknown service: $serviceName"))
                }
            }

            when {
                reference.isNullOrBlank() -> alertCollector.add(Alert(line, uid, null, referenceColumn, "Missing column"))
                isStaticVariableReference(reference) -> validateVariableName(getStaticVariableName(reference))?.apply {
                    alertCollector.add(Alert(line, uid, reference, referenceColumn, "Invalid reference format: $this"))
                }
                else -> alertCollector.add(Alert(line, uid, reference, referenceColumn, "Invalid reference format. Expected $BEGIN_STATIC$reference$END_STATIC"))
            }

            if (errors == alertCollector.count) {
                definedServiceNames.computeIfAbsent(reference) { TreeMap() }[line] = serviceName
            }
        } else if (definedServiceNames.containsKey(serviceName)) {
            val availableDefinitions = definedServiceNames[serviceName]!!.headMap(line)

            if (availableDefinitions.isNotEmpty()) {
                element.setValue(ServiceName, availableDefinitions[availableDefinitions.lastKey()])
            }
        }
    }

    override fun visit(block: AMLBlock) {
        if (block.isExecutable) {
            block.forEach { it.accept(this) }
        }
    }
}