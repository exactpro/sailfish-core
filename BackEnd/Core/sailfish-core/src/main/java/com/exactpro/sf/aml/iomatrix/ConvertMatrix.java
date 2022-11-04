/******************************************************************************
 * Copyright 2009-2022 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.aml.iomatrix;

import java.io.File;

public class ConvertMatrix {

    public static final int EXIT_STATUS_BAD_ARGS = 1;
    public static final int EXIT_STATUS_NO_INPUT_FILE = 2;
    public static final int EXIT_STATUS_SAME_FILE = 3;
    public static final int EXIT_STATUS_CONVERT_ERROR = 4;

    public static void main(String[] args) {
        checkArgs(args);

        String inputFileName = args[0];
        String outputFileName = args[1];
        File inputFile = new File(inputFileName);
        File outputFile = new File(outputFileName);

        if(!inputFile.exists()) {
            System.out.printf("Error in 0 argument, file {%s} does not exist\n", inputFileName);
            System.exit(EXIT_STATUS_NO_INPUT_FILE);
        }
        if(inputFile.equals(outputFile)) {
            System.out.println("Error in 0 and 1 arguments: files are same");
            System.exit(EXIT_STATUS_SAME_FILE);
        }

        try {
            convertMatrices(inputFile, outputFile);
        } catch (Exception e) {
            System.out.printf("Error while converting matrices: %s\n", e);
            System.exit(EXIT_STATUS_CONVERT_ERROR);
        }

        System.out.println("Matrices converted successfully");
    }

    public static void convertMatrices(File inputFile, File outputFile) throws Exception {
        try(
            IMatrixReader reader = AdvancedMatrixReader.getReader(inputFile);
            IMatrixWriter writer = AdvancedMatrixWriter.getWriter(outputFile)
        ) {
            while(reader.hasNext()) {
                writer.write(reader.read());
            }
        }
    }

    private static void checkArgs(String[] args) {
        if(args.length < 2) {
            System.out.println("Few arguments. Usage: <Input-File> <Output-File>");
            if(args.length == 1) {
                System.out.println("Output file isn't provided");
            }
            System.exit(EXIT_STATUS_BAD_ARGS);
        }
    }

}
