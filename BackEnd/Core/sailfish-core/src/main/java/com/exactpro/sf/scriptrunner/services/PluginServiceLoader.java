/*******************************************************************************
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
package com.exactpro.sf.scriptrunner.services;

import static org.apache.commons.io.FilenameUtils.getExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceMarshalManager;

public class PluginServiceLoader {
    private static final String EXTENSION = "xml";

    private final Set<File> descriptionFiles = new HashSet<>();

    public void addDescription(File descriptionFile) throws IOException {
        if(!descriptionFile.exists()) {
            throw new IllegalArgumentException("File does not exist: " + descriptionFile.getCanonicalPath());
        }

        if(!descriptionFile.isFile()) {
            throw new IllegalArgumentException("Description is not a file: " + descriptionFile.getCanonicalPath());
        }

        if(!getExtension(descriptionFile.getName()).equalsIgnoreCase(EXTENSION)) {
            throw new IllegalArgumentException("Description is not a " + EXTENSION + " file: " + descriptionFile.getCanonicalPath());
        }

        descriptionFiles.add(descriptionFile);
    }

    public void load(IConnectionManager connectionManager, ServiceMarshalManager marshalManager) throws IOException {
        if(descriptionFiles.isEmpty()) {
            return;
        }

        List<ServiceDescription> descriptions = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for(File descriptionFile : descriptionFiles) {
            try(InputStream stream = new FileInputStream(descriptionFile)) {
                marshalManager.unmarshalServices(stream, false, descriptions, errors);
            } catch(Exception e) {
                throw new EPSCommonException("Failed to load description from file: " + descriptionFile.getCanonicalPath(), e);
            }
        }

        if(!errors.isEmpty()) {
            throw new EPSCommonException("Failed to load service descriptions. Errors:" + System.lineSeparator() + String.join(System.lineSeparator(), errors));
        }

        for(ServiceDescription description : descriptions) {
            try {
                connectionManager.addDefaultService(description, null);
            } catch(Exception e) {
                throw new EPSCommonException("Failed to add default service: " + description.getName(), e);
            }
        }
    }
}
