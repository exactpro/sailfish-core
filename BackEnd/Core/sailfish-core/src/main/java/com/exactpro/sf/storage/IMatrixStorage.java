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
package com.exactpro.sf.storage;

import java.io.InputStream;
import java.util.List;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.matrixhandlers.IMatrixProvider;


public interface IMatrixStorage {

	IMatrix addMatrix(InputStream stream, String name, String description, String creator, SailfishURI languageURI, String link, SailfishURI matrixProviderURI);

	void updateMatrix(IMatrix matrix);

	void removeMatrix(IMatrix matrix);

	List<IMatrix> getMatrixList();

	IMatrix getMatrixById(long matrixId);

	void reloadMatrix(IMatrix matrix, IMatrixProvider matrixProvider);

	void subscribeForUpdates(MatrixUpdateListener listener);

	void unSubscribeForUpdates(MatrixUpdateListener listener);

}
