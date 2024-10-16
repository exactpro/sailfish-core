/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.aml.converter;

import java.io.File;

import org.apache.commons.configuration2.HierarchicalConfiguration;

import com.exactpro.sf.aml.Ignore;
import com.exactpro.sf.common.services.ServiceName;
import org.apache.commons.configuration2.tree.ImmutableNode;

public abstract class AbstractMatrixConverterSettings implements IMatrixConverterSettings {
    @Ignore
    private File inputFile;
    @Ignore
    private File outputFile;
    @Ignore
    private String environment = ServiceName.DEFAULT_ENVIRONMENT;

    @Override
    public void load(HierarchicalConfiguration<ImmutableNode> config) {
        // TODO Auto-generated method stub
    }

    @Override
    public File getInputFile() {
        return inputFile;
    }

    @Override
    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public File getOutputFile() {
        return outputFile;
    }

    @Override
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public String getEnvironment() {
        return environment;
    }

    @Override
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
