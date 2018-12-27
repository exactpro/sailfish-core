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
package com.exactpro.sf.aml;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.aml.visitors.AMLBlockFlattenerVisitor;
import com.exactpro.sf.aml.visitors.AMLBlockHasherVisitor;
import com.exactpro.sf.aml.visitors.AMLLanguageDetectorVisitor;
import com.exactpro.sf.aml.visitors.ActionURIResolverVisitor;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.languagemanager.AutoLanguageFactory;

public class AMLBlockUtility {
    public static List<AMLElement> flatten(Iterable<AMLElement> elements) throws AMLException {
        return flatten(elements, true);
    }

    public static List<AMLElement> flatten(Iterable<AMLElement> elements, boolean onlyExecutable) throws AMLException {
        AMLBlockFlattenerVisitor visitor = new AMLBlockFlattenerVisitor(onlyExecutable);

        for(AMLElement element : elements) {
            element.accept(visitor);
        }

        return visitor.getElements();
    }

    public static SailfishURI detectLanguage(List<AMLBlock> blocks, Set<SailfishURI> languageURIs, SailfishURI selectedLanguageURI, IActionManager actionManager) throws AMLException {
        if(AutoLanguageFactory.URI.matches(selectedLanguageURI)) {
            AMLLanguageDetectorVisitor visitor = new AMLLanguageDetectorVisitor(actionManager, languageURIs);

            for(AMLBlock block : blocks) {
                block.accept(visitor);
            }

            AlertCollector alertCollector = visitor.getAlertCollector();

            if(alertCollector.getCount(AlertType.ERROR) > 0) {
                throw new AMLException("Failed to detect language", alertCollector);
            }

            Set<SailfishURI> compatibleLanguages = visitor.getCompatibleLanguageURIs();

            if(compatibleLanguages.isEmpty()) {
                throw new AMLException("Failed to detect language");
            } else {
                return compatibleLanguages.iterator().next();
            }
        }

        return selectedLanguageURI;
    }

    public static void resolveActionURIs(List<AMLBlock> blocks, IActionManager actionManager, IConnectionManager connectionManager, String environmentName) throws AMLException {
        ActionURIResolverVisitor visitor = new ActionURIResolverVisitor(actionManager, connectionManager, environmentName);

        for(AMLBlock block : blocks) {
            block.accept(visitor);
        }

        AlertCollector alertCollector = visitor.getAlertCollector();

        if(alertCollector.getCount(AlertType.ERROR) > 0) {
            throw new AMLException("Failed to resolve action URIs", alertCollector);
        }
    }

    public static int hash(AMLBlock block, AMLMatrixWrapper wrapper, Map<String, String> staticVariables, Map<AMLBlock, Integer> cache, Set<String> references) throws AMLException {
        AMLBlockHasherVisitor visitor = new AMLBlockHasherVisitor(wrapper, staticVariables, cache, references);
        block.accept(visitor);
        return visitor.getHash();
    }

    public static int hash(AMLElement element, AMLMatrixWrapper wrapper, Map<String, String> staticVariables,
                           Map<AMLBlock, Integer> cache, Set<String> references) throws AMLException {
        AMLBlockHasherVisitor visitor = new AMLBlockHasherVisitor(wrapper, staticVariables, cache, references);
        element.accept(visitor);
        return visitor.getHash();
    }
}
