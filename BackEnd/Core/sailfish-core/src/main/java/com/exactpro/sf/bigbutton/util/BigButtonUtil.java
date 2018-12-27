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
package com.exactpro.sf.bigbutton.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;

public class BigButtonUtil {

    public static String recognizeSeparatorInZipAndChangePath(File zipFile, String path) throws IOException {

        ZipEntry ze;
        try (ZipFile zif = new ZipFile(zipFile)) {

            final Enumeration<? extends ZipEntry> entries = zif.entries();

            while (entries.hasMoreElements()) {

                ze = entries.nextElement();
                String entryName = ze.getName();

                if(entryName.contains("/")) {
                    return path.replaceAll("\\\\", "/");
                }

                if(entryName.contains("\\")) {
                    return path.replaceAll("/", "\\");
                }

            }
        }

        return path;
    }

    public static InputStream getStream(String rootFolder, String relativePath,
                                        IWorkspaceDispatcher workspaceDispatcher) throws IOException {

        String relativeTestLibrary = Paths.get(rootFolder, relativePath).toString();
        int zipIndex = relativeTestLibrary.indexOf(".zip");

        if (zipIndex != -1) {

            String zipPath = relativeTestLibrary.substring(0, zipIndex + 4);
            String restPath = relativeTestLibrary.substring(zipIndex + 5);

            File zipFile = workspaceDispatcher.getFile(FolderType.TEST_LIBRARY, zipPath);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            getEntryFromReportZip(zipFile, restPath, stream);

            return new ByteArrayInputStream(stream.toByteArray());
        }

        return new FileInputStream(workspaceDispatcher.getFile(FolderType.TEST_LIBRARY, relativeTestLibrary));
    }

    public static void getEntryFromReportZip(File zipFile, String entryToWrite, OutputStream out) throws IOException {

        entryToWrite = BigButtonUtil.recognizeSeparatorInZipAndChangePath(zipFile, entryToWrite);

        ZipEntry ze = null;
        try (ZipFile zif = new ZipFile(zipFile)) {

            final Enumeration<? extends ZipEntry> entries = zif.entries();

            while (entries.hasMoreElements()) {

                ze = entries.nextElement();

                String entryName = ze.getName();

                if (entryName.equals(entryToWrite)) {
                    try (InputStream in = zif.getInputStream(ze)) {
                        IOUtils.copy(in, out);
                    }
                    break;
                }
            }
        }
    }
}
