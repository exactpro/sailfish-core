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
package com.exactpro.sf.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;

/**
 * @author nikita.smirnov
 *
 */
public class TestConfigurationReader extends EPSTestCase {
    
    @Test
    public void testConfigurationXmlNegative() throws Exception
    {
        String dictName = this.getBaseDir() + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "negative.xml";
        
        IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
        
        boolean error = false;
        
        try (InputStream in = new FileInputStream(dictName)) {
        	
			loader.load(in);
			
        } catch (EPSCommonException e) {
            e.printStackTrace();
            String err = "A field null with an id S4 has neither a type nor a reference";
            if (e.getCause() != null) {
            	Assert.assertEquals(err, e.getCause().getMessage());
            } else {
            	Assert.assertEquals(err, e.getMessage());
            }
            error = true;
        }
        
        Assert.assertEquals("Negatine test", true, error);
    }
}
