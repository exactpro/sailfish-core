/*******************************************************************************
 *   Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.aml.iomatrix;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

public class MergeMatrix {
    public static final String NAMES_DELIMITER = "_";

    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.out.println("Few arguments");
                System.exit(1);
            }
            File outputDir = new File(args[0]);
            if (!outputDir.exists()) {
                if (!outputDir.mkdirs()) {
                    System.out.println(String.format("Error in 1 argument, directory {%s} does not exist and cannot be created", args[0]));
                    System.exit(2);
                }
            }
            if (!outputDir.isDirectory()) {
                System.out.println(String.format("Error in 1 argument,  {%s} not a directory ", args[0]));
                System.exit(3);
            }

            List<File> fileList = new ArrayList<>();

            for (int i = 1; i < args.length; i++) {
                File file = new File(args[i]);
                if (!file.exists() || file.isDirectory()) {
                    System.out.println(String.format("Error in %d argument,  file {%s} does not exist or it's a directory", i, args[i]));
                    System.exit(4);
                }
                fileList.add(file);

            }

            System.out.println("Output dir:");
            System.out.println(outputDir.getAbsolutePath());

            System.out.println("Input matrices:");
            for (File file : fileList) {
                System.out.println(file.getAbsolutePath());
            }

            File outputFile = new File(outFileName(fileList, outputDir));

            System.out.println("Output matrix:");
            System.out.println(outputFile.getAbsolutePath());

            mergeMatrix(outputFile, fileList);
            if (!outputFile.exists()) {
                System.out.println("Could not create file");
                System.exit(5);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error merge matrices: " + e);
            System.exit(6);
        }

        System.out.println("Success!");
    }

    public static void mergeMatrix(File outputFile, List<File> files) throws Exception {

        try (AdvancedMatrixWriter matrixWriter = new AdvancedMatrixWriter(outputFile, readHeaderAllMatrix(files))) {

            for (File file : files) {
                try (AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(file)) {
                    while (matrixReader.hasNext()) {
                        matrixWriter.writeCells(matrixReader.readCells());
                    }
                }

            }

        }

    }

    private static List<SimpleCell> readHeaderAllMatrix(List<File> files) throws Exception {
        Set<SimpleCell> setHeader = new LinkedHashSet<>();
        for (File file : files) {
            try (AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(file)) {
                setHeader.addAll(matrixReader.getHeader());
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(setHeader));
    }

    private static String outFileName(List<File> files, File outputDir) {
        return outputDir.getAbsolutePath()
                + File.separator
                + files.stream()
                .map(File::getName)
                .map(FilenameUtils::removeExtension)
                .collect(joining(NAMES_DELIMITER))
                + '.'
                + FilenameUtils.getExtension(files.get(0).getName());
    }

}
