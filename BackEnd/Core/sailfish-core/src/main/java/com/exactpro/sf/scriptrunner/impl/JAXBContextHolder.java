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
package com.exactpro.sf.scriptrunner.impl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * Created by alexey.zarovny on 9/18/14.
 */
public class JAXBContextHolder {

    private static JAXBContext instance;

    public static JAXBContext getJAXBContext() throws JAXBException {
        synchronized (JAXBContextHolder.class){
            if (instance == null) {
                instance = JAXBContext.newInstance("com.exactpro.sf.scriptrunner.reporting.xml");
            }
        }
        return instance;
    }
}
