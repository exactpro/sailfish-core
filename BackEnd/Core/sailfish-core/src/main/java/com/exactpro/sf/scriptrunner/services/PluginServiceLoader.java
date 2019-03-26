package com.exactpro.sf.scriptrunner.services;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.ArrayUtils.contains;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exactpro.sf.common.services.ServiceName;
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
            } catch(Throwable t) {
                throw new EPSCommonException("Failed to load description from file: " + descriptionFile.getCanonicalPath(), t);
            }
        }

        if(!errors.isEmpty()) {
            throw new EPSCommonException("Failed to load service descriptions. Errors:" + System.lineSeparator() + String.join(System.lineSeparator(), errors));
        }

        List<String> environments = connectionManager.getEnvironmentList();

        for(ServiceDescription description : descriptions) {
            ServiceName serviceName = description.getServiceName();

            if(contains(connectionManager.getServiceNames(), serviceName)) {
                continue;
            }

            String environment = serviceName.getEnvironment();

            if(!serviceName.isDefault() && !environments.contains(environment)) {
                connectionManager.addEnvironment(environment, null);
            }

            try {
                connectionManager.addService(description, null).get();
            } catch(Throwable t) {
                throw new EPSCommonException("Failed to add service: " + description.getServiceName(), t);
            }
        }
    }
}
