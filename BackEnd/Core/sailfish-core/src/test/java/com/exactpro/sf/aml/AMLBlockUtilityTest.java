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

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.languagemanager.AutoLanguageFactory;

public class AMLBlockUtilityTest {
    @Test
    public void detectLanguageWithUnknownActionTest() {
        IActionManager actionManager = Mockito.mock(IActionManager.class);
        AMLBlock block = new AMLBlock().addElement(new AMLElement().setValue(Column.Action, "zzz"));

        try {
            AMLBlockUtility.detectLanguage(Collections.singletonList(block), Collections.<SailfishURI>emptySet(), AutoLanguageFactory.URI, actionManager);
            Assert.fail("No exception was thrown");
        } catch(AMLException e) {
            AlertCollector alertCollector = e.getAlertCollector();
            Assert.assertEquals("Failed to detect language", e.getMessage());
            Assert.assertEquals(1, alertCollector.getCount(AlertType.ERROR));
            Assert.assertEquals(0, alertCollector.getCount(AlertType.WARNING));
            Assert.assertEquals(new Alert(0, null, Column.Action.getName(), "Unknown action: zzz"), alertCollector.getAlerts(AlertType.ERROR).iterator().next());
        }
    }
}
