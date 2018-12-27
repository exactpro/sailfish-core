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
import java.util.ArrayList;
import java.util.List;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidatorFactory;

public class DictionaryValidator {

    
    //private static final Logger logger = LoggerFactory.getLogger(DictionaryValidator.class);

        
    public static void main(String[] args) {

        if (args.length < 1) {
            throw new EPSCommonException("Received wrong args");
        } else if (args.length == 1) {
            //nothing to validate
            return;
        }
        
        String validatorFactory = args[0];
        List<File> dictionaries = new ArrayList<>(); 
        
        for (int i=1; i<args.length; i++) {
            dictionaries.add(new File(args[i]));
        }
        
        IDictionaryValidatorFactory factory;
        try {
            factory = (IDictionaryValidatorFactory) DictionaryValidator.class.getClassLoader().loadClass(validatorFactory).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e1) {
            throw new EPSCommonException("Class " + validatorFactory + "not found! Check plugin dependencies.");
        }
        IDictionaryValidator validator = factory.createDictionaryValidator();
        IDictionaryStructureLoader dictionaryLoader = new XmlDictionaryStructureLoader();
        
        List<?> errors = new ArrayList<>();
        boolean hasErrors = false;
        for (File dictionary:dictionaries) {
            try (InputStream dictionaryStream = new FileInputStream(dictionary)) {
                System.out.println("Validating " + dictionary.getName());
                IDictionaryStructure structure = dictionaryLoader.load(dictionaryStream);
                errors = validator.validate(structure, true, null);
                if (errors.size() != 0) {
                    hasErrors = true;
                    System.err.println("Errors detected in " + dictionary.getName());
                    for (Object o:errors) {
                        System.err.println(o);
                    }
                } else {
                    System.out.println(dictionary.getName() + " OK");
                }
            } catch (Exception e) {
                hasErrors = true;
                e.printStackTrace();
            }
        }
        
        if (hasErrors) {
            throw new EPSCommonException("Errors has been detected in dictionaries batch");
        }
    }
}
