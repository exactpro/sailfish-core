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

package com.exactpro.sf.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;

import com.exactpro.sf.scriptrunner.ZipReport;

import static com.exactpro.sf.storage.impl.DefaultTestScriptStorage.ROOT_JSON_REPORT_FILE;
import static com.exactpro.sf.storage.impl.DefaultTestScriptStorage.XML_PROPERTIES_FILE;

public class ReportFilter implements FileFilter {

    private static final ReportFilter instance = new ReportFilter();

    public static ReportFilter getInstance() {
        return instance;
    }

    @Override
    public boolean accept(File file) {
        String name = file.getName();

        if(FilenameUtils.isExtension(name, ZipReport.ZIP_EXTENSION)) {
            String baseName = FilenameUtils.getBaseName(name);


            if(new File(file.getParentFile(), ROOT_JSON_REPORT_FILE).exists() || new File(file.getParentFile(), XML_PROPERTIES_FILE).exists()) {
                return false;
            }

            try(ZipFile zipFile = new ZipFile(file)) {
                ZipEntry json = zipFile.getEntry(baseName + '/' + ROOT_JSON_REPORT_FILE );
                ZipEntry xml = zipFile.getEntry(baseName + '/' + XML_PROPERTIES_FILE );
                return json != null || xml != null;
            } catch(IOException e) {
                return false;
            }
        }

        return file.isDirectory() && (new File(file, ROOT_JSON_REPORT_FILE).exists() || new File(file, XML_PROPERTIES_FILE).exists());
    }

}
