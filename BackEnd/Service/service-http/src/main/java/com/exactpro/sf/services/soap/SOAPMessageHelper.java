/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.soap;

import com.exactpro.sf.common.util.EPSCommonException;

import javax.xml.soap.MessageFactory;

public class SOAPMessageHelper {
    
    public static final String IS_ATTRIBUTE = "IsAttribute";
    public static final String XMLNS = "XMLNS";
    public static final String PREFIX = "Prefix";
    public static final String TNS = "TargetNamespace";
    public static final String SOAPACTION = "SOAPAction";
    public static final String IGNORE_ATTRIBUTE = "ignore";

    public static MessageFactory getSoapMessageFactory() {
        // Java 8 had java web services library as part of Java SE.
        // In java 11 web services library was removed (http://openjdk.java.net/jeps/320).
        // Because of that we should use external dependencies for web services.
        // Thread context class loader under which this code is run doesn't have plugin dependencies in classpath.
        // So we need redefine thread context class loader for factory creation
        // And set it back after factory created.
        MessageFactory factory;
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(SOAPMessageHelper.class.getClassLoader());
            factory = MessageFactory.newInstance();
        } catch (Exception e) {
            throw new EPSCommonException("Unable to create SOAP message factory");
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return factory;
    }
}
