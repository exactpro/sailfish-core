/******************************************************************************
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
package com.exactpro.sf.actions;

import com.exactpro.sf.aml.script.ActionContext;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.util.AbstractTest;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class TestXMLActions extends AbstractTest {
    private ActionContext actionContext;
    private XMLActions xmlActions;

    private String getDocument(String file) {
        try {
            IDataManager dataManager = serviceContext.getDataManager();
            InputStream dataInputStream = dataManager.getDataInputStream(SailfishURI.parse(file));
            return IOUtils.toString(dataInputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void init(){
        ScriptContext context = getScriptContext();
        IDataManager dataManager = serviceContext.getDataManager();
        Mockito.when(context.getDataManager()).thenReturn(dataManager);
        actionContext = new ActionContext(context, true);
        xmlActions = new XMLActions();
    }

    @Test
    public void test() {
        HashMap<String, String> settings = new HashMap<>();
        settings.put(XMLActions.TEMPLATE, "TestSimpleTemplate");
        settings.put("msgType", "AddOrder");
        settings.put("valueField", "ToMapping");
        HashMap<?, ?> hashMap = xmlActions.generateDocument(actionContext, settings);
        Assert.assertEquals(getDocument("TestSimpleTemplateFinal"), hashMap.get(XMLActions.OUT));
    }
}
