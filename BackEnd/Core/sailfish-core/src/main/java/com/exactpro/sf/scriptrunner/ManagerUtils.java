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
package com.exactpro.sf.scriptrunner;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityInfo;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityManager;

public class ManagerUtils {
    public static UtilityInfo getUtilityInfo(Set<SailfishURI> utilityURIs, UtilityManager utilityManager, SailfishURI utilityURI, long line, long uid, String column, AlertCollector alertCollector, Class<?>... argTypes)
            throws SailfishURIException {
        Set<UtilityInfo> infos = new HashSet<>();

        for (SailfishURI utilityClassURI : utilityURIs) {
            UtilityInfo utilityInfo = utilityManager.getUtilityInfo(utilityURI.merge(utilityClassURI), line, uid, column, alertCollector, argTypes);

            if (utilityInfo != null) {
                infos.add(utilityInfo);
            }
        }

        if (infos.isEmpty()) {
            return null;
        }

        if (infos.size() > 1) {
            Set<SailfishURI> uris = infos.stream()
                    .map(UtilityInfo::getURI)
                    .collect(Collectors.toSet());

            if (uris.size() > 1) {
                alertCollector.add(new Alert(line, uid, null, column, String.format("Ambiguous utility function URI: %s (matches: %s)", utilityURI, uris), AlertType.WARNING));
            }
        }

        return infos.iterator().next();
    }
}
