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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.exactpro.sf.aml.AMLBlockBrace;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;

public class AMLLanguageDetectorVisitor implements IAMLElementVisitor {
    private final IActionManager actionManager;
    private final Set<SailfishURI> compatibleLanguageURIs;
    private final AlertCollector alertCollector;

    public AMLLanguageDetectorVisitor(IActionManager actionManager, Set<SailfishURI> languageURIs) {
        this.actionManager = actionManager;
        this.compatibleLanguageURIs = new HashSet<>(languageURIs);
        this.alertCollector = new AlertCollector();
    }

    @Override
    public void visit(AMLElement element) throws AMLException {
        if(!element.isExecutable()) {
            return;
        }

        String actionName = element.getValue(Column.Action);

        if(actionName == null || JavaStatement.value(actionName) != null || AMLBlockBrace.value(actionName) != null) {
            return;
        }

        SailfishURI actionURI = null;

        try {
            actionURI = SailfishURI.parse(actionName);
        } catch(SailfishURIException e) {
            alertCollector.add(new Alert(element.getLine(), element.getUID(), element.getValue(Column.Reference), Column.Action.getName(), e.getMessage()));
            compatibleLanguageURIs.clear();
            return;
        }

        Set<ActionInfo> actionInfos = actionManager.getActionInfos(actionURI);

        if(actionInfos.isEmpty()) {
            alertCollector.add(new Alert(element.getLine(), element.getUID(), element.getValue(Column.Reference), Column.Action.getName(), "Unknown action: " + actionURI));
            compatibleLanguageURIs.clear();
            return;
        }

        if(compatibleLanguageURIs.isEmpty()) {
            return;
        }

        Iterator<SailfishURI> it = compatibleLanguageURIs.iterator();

        nextLanguage:
        while(it.hasNext()) {
            SailfishURI languageURI = it.next();

            for(ActionInfo actionInfo : actionInfos) {
                if(actionInfo.isLanguageCompatible(languageURI, false)) {
                    break nextLanguage;
                }
            }

            it.remove();
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

    public Set<SailfishURI> getCompatibleLanguageURIs() {
        return compatibleLanguageURIs;
    }

    public AlertCollector getAlertCollector() {
        return alertCollector;
    }
}
