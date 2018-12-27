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
package com.exactpro.sf.testwebgui.scriptruns;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Future;

import com.exactpro.sf.aml.converter.ConversionMonitor;

public class MatrixConverterFeature {
    private final ConversionMonitor conversionMonitor;
    private final Future<Boolean> future;
    private final File outputFile;

    public MatrixConverterFeature(ConversionMonitor conversionMonitor, Future<Boolean> future, File outputFile) {
		this.conversionMonitor = conversionMonitor;
        this.future = future;
        this.outputFile = outputFile;
	}

	public File getOutputFile() {
        return outputFile;
	}

	public int getProgress() {
		return conversionMonitor.getProgress();
	}

	public Future<Boolean> getFuture() {
		return future;
	}

	public Set<String> errors() {
        return conversionMonitor.getErrors();
    }
}
