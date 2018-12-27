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

import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.matrixhandlers.MatrixProviderHolder;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.storage.IMatrixStorage;
import com.exactpro.sf.testwebgui.api.IMatrixListener;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;

public class MatrixHolder implements IMatrixListener {

    private static final Logger logger = LoggerFactory.getLogger(MatrixHolder.class);

	private final IWorkspaceDispatcher wd;
	private final IMatrixStorage matrixStorage;
	private final MatrixProviderHolder matrixProviderHolder;

	private List<IMatrix> matrices;

	public MatrixHolder(IWorkspaceDispatcher wd, IMatrixStorage matrixStorage, MatrixProviderHolder matrixProviderHolder) throws FileNotFoundException {
		this.wd = wd;
		this.matrixStorage = matrixStorage;
		this.matrixProviderHolder = matrixProviderHolder;
		this.matrices = new CopyOnWriteArrayList<>(getAllExistMatrices());
		TestToolsAPI.getInstance().addListener(this);
	}

	public List<IMatrix> getMatrices() {
		return matrices;
	}

	@Override
	public void addMatrix(IMatrix matrix) {
		this.matrices.add(0, matrix);
	}

	@Override
	public void removeMatrix(IMatrix matrix) {
		this.matrices.remove(matrix);
	}

	public IMatrix getMatrixById(long id) {

		for(IMatrix matrix : matrices) {
			if(matrix.getId().equals(id)) {
				return matrix;
			}
		}
		return null;
	}

    private List<IMatrix> getAllExistMatrices() {
        List<IMatrix> matrixList = matrixStorage.getMatrixList();
        Iterator<IMatrix> iterator = matrixList.iterator();
        while (iterator.hasNext()) {
            IMatrix matrix = iterator.next();
            boolean removeMatrix = false;
            if (!wd.exists(FolderType.MATRIX, matrix.getFilePath())) {
                if (StringUtils.isNoneEmpty(matrix.getLink())) {
                    try {
                        wd.createFile(FolderType.MATRIX, false, matrix.getFilePath());
                        MatrixUtil.reloadMatrixByLink(matrixStorage, matrixProviderHolder, matrix);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        removeMatrix = true;
                    }
                } else {
                    removeMatrix = true;
                    logger.warn("cant find matrix file by specified path {}, matrix will be removed", matrix.getFilePath());
                }
            }
            if (removeMatrix) {
                logger.warn("Removing matrix from storage {} ", matrix);
                matrixStorage.removeMatrix(matrix);
                iterator.remove();
            }
        }
        return matrixList;
    }
}
