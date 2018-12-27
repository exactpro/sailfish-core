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
package com.exactpro.sf.aml.writer;

import java.io.File;

import com.exactpro.sf.aml.iomatrix.AdvancedMatrixWriter;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.exactpro.sf.aml.visitors.AMLHeadersVisitor;
import com.exactpro.sf.aml.visitors.AMLWriterVisitor;

public class AMLWriter {
    public static void write(File matrixPath, AMLMatrix matrix) throws Exception {
    	
    	//in JSMatrixEditor can be added new headers. we can't each time redefine them.
    	AMLHeadersVisitor shv = new AMLHeadersVisitor(matrix.getHeader());
    	for(AMLBlock block : matrix.getBlocks()) {
            block.accept(shv);
        }
    	
        try(AdvancedMatrixWriter matrixWriter = new AdvancedMatrixWriter(matrixPath, shv.getMainHeaders())) {
            AMLWriterVisitor visitor = new AMLWriterVisitor(matrixWriter);

            for(AMLBlock block : matrix.getBlocks()) {
                block.accept(visitor);
            }
        }
    }
}
