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
package com.exactpro.sf.matrixhandlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class LocalMatrixProvider implements IMatrixProvider {

	private String name;
    private final String link;

    public LocalMatrixProvider(String link){
        this.link = link;
    }

    @Override
    public InputStream getMatrix() throws Exception {
        File file = new File(link);
        if(!file.exists()){
            throw new IllegalArgumentException("File " + link + " not found on the server machine");
        }
        name = file.getName();
        return new FileInputStream(file);
    }

    @Override
    public String getName() throws Exception {
        if(name == null){
            File file = new File(link);
            if(!file.exists()){
                throw new IllegalArgumentException("File " + link + " not found on the server machine");
            }
            name = file.getName();
        }
        return name;
    }

}
