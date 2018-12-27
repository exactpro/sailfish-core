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

import com.exactpro.sf.configuration.suri.SailfishURI;

public class MatrixWrapper {

    private InputStream matrixInputStream;

    private String matrixName;

    private String path;

    private MatrixType type;

    private SailfishURI providerURI;

    public MatrixWrapper(MatrixType type, SailfishURI providerURI) {
        this.type = type;
        this.providerURI = providerURI;
    }

    public MatrixWrapper() {
        this(MatrixType.TYPE_LOCAL, null);
    }


    public InputStream getMatrixInputStream() {
        return matrixInputStream;
    }

    public void setMatrixInputStream(InputStream is, String fileName) {

        this.matrixInputStream = is;
        this.matrixName = fileName;
        this.path = matrixName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public MatrixType getType() {
        return type;
    }

    public void setType(MatrixType type) {

        this.type = type;
    }

    public SailfishURI getProviderURI() {
        return providerURI;
    }

    public void setProviderURI(SailfishURI providerURI) {
        this.providerURI = providerURI;
    }

    public String getMatrixName() {
        return matrixName;
    }

    public void setMatrixName(String matrixName) {
        this.matrixName = matrixName;
    }
}
