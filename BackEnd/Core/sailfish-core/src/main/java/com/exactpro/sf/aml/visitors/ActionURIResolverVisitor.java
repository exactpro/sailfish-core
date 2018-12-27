/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.aml.visitors;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.aml.AMLBlockBrace;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.services.ServiceDescription;

public class ActionURIResolverVisitor implements IAMLElementVisitor {
    private final IActionManager actionManager;
    private final IConnectionManager connectionManager;
    private final String environmentName;
    private final AlertCollector alertCollector;
    private final Map<String, String> serviceNames;

    public ActionURIResolverVisitor(IActionManager actionManager, IConnectionManager connectionManager, String environmentName) {
        this.actionManager = actionManager;
        this.connectionManager = connectionManager;
        this.environmentName = environmentName;
        this.alertCollector = new AlertCollector();
        this.serviceNames = new HashMap<>();
    }

    @Override
    public void visit(AMLElement element) throws AMLException {
        if(!element.isExecutable()) {
            return;
        }

        String actionName = element.getValue(Column.Action);

        if(actionName == null || AMLBlockBrace.value(actionName) != null) {
            return;
        }

        JavaStatement statement = JavaStatement.value(actionName);

        if(statement != null) {
            //TODO: populate this map outside and pass it here and in codegen
            if(statement == JavaStatement.DEFINE_SERVICE_NAME && element.containsCell(Column.Reference) && element.containsCell(Column.ServiceName)) {
                serviceNames.put(element.getValue(Column.Reference), element.getValue(Column.ServiceName));
            }

            return;
        }

        try {
            SailfishURI actionURI = SailfishURI.parse(actionName);
            String name = element.getValue(Column.ServiceName);
            ServiceName serviceName = new ServiceName(environmentName, ObjectUtils.defaultIfNull(serviceNames.get(name), name));

            if(serviceName.getServiceName() != null && !SailfishURIRule.REQUIRE_PLUGIN.check(actionURI)) {
                ServiceDescription serviceDescription = connectionManager.getServiceDescription(serviceName);

                if(serviceDescription != null) {
                    SailfishURI mergedURI = actionURI.merge(serviceDescription.getType());

                    if(actionManager.containsAction(mergedURI)) {
                        element.setValue(Column.Action, mergedURI.toString());
                    }
                }
            }
        } catch(SailfishURIException e) {
            alertCollector.add(new Alert(element.getLine(), element.getUID(), element.getValue(Column.Reference), Column.Action.getName(), e.getMessage()));
        }
    }

    @Override
    public void visit(AMLBlock block) throws AMLException {
        if(!block.isExecutable()) {
            return;
        }

        for(AMLElement element : block) {
            element.accept(this);
        }
    }

    public AlertCollector getAlertCollector() {
        return alertCollector;
    }
}
