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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.matrixhandlers.IMatrixProvider;
import com.exactpro.sf.matrixhandlers.MatrixProviderHolder;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.storage.IMatrixStorage;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;

public class MatrixUtil {

	private static final Logger logger = LoggerFactory.getLogger(MatrixUtil.class);

	private MatrixUtil() {
	}

    public static void reloadMatrixByLink(IMatrixStorage matrixStorage, MatrixProviderHolder holder, IMatrix matrix) throws Exception {
		logger.info("Reload started");

        IMatrixProvider provider = holder.getMatrixProvider(matrix.getLink(), matrix.getProviderURI());
        if (provider == null) {
            logger.warn("Failed to find matrix provider {}", matrix.getProviderURI());
            throw new EPSCommonException("Failed to find matrix provider " + matrix.getProviderURI());
        }
        matrixStorage.reloadMatrix(matrix, provider);

		logger.info("Reload finished");
	}

    public static List<IMatrix> addMatrixByLink(MatrixProviderHolder holder, String link, SailfishURI providerURI)
            throws Exception {
		logger.info("Upload started");
        Map<String, IMatrixProvider> matrixProviders = holder.getMatrixProviders(link, providerURI);
        List<IMatrix> result = new ArrayList<>();
		if (matrixProviders == null) {
            throw new IllegalStateException("Illegal provider SURI: " + providerURI );
		}
		
        for (Entry<String, IMatrixProvider> entry : matrixProviders.entrySet()) {
            try (InputStream inputStream = entry.getValue().getMatrix()) {
                result.add(TestToolsAPI.getInstance()
                                       .uploadMatrix(inputStream, entry.getValue().getName(), null, "Linked", null,
                                                     entry.getKey(), providerURI));
            }
        }
		logger.info("Upload finished");
        return result;
	}
}
