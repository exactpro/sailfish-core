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
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.ArrayUtils;

import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;

public class LocalMatrixProviderFactory implements IMatrixProviderFactory {

    public static final String CSV_POSTFIX = ".csv";
    public static final String XLS_POSTFIX = ".xls";
    public static final String XLSX_POSTFIX = ".xlsx";
    public static final String ALIAS = "LOCAL";

	@Override
	public void init(IWorkspaceDispatcher wd) {
	}

    @Override
    public Set<String> resolveLinks(String link) {
        return getFileByMask(link).stream().map(file -> file.getAbsolutePath())
                .filter(item -> item.endsWith(CSV_POSTFIX) || item.endsWith(XLS_POSTFIX) || item.endsWith(XLSX_POSTFIX)).collect(Collectors.toSet());
    }

	@Override
    public IMatrixProvider getMatrixProvider(String link) {
        return new LocalMatrixProvider(new File(link).getAbsolutePath());
	}

	@Override
	public String getNotes() {
		return "This provider allows you to load matrices from local files";
	}

    public static Set<File> getFileByMask(String linkWithMask){
    	// don't use IWorkspaceDispatcher: we get files outside of workspace???
        Set<File> result = new HashSet<File>();

        File dir;
        File file;
        String mask = getMask(linkWithMask);
        if (mask != null && !mask.trim().isEmpty()){
            file = new File(mask);
            dir = new File(getParentFolderPath(linkWithMask));

            FileFilter fileFilter = new WildcardFileFilter(file.getName());

            File[] files = dir.listFiles(fileFilter);

            if (!ArrayUtils.isEmpty(files)) {
                result.addAll(Arrays.asList(files));
            } else {
                throw new IllegalArgumentException("No files were found for wildcard: " + file.getName());
            }
        }

        return result;
    }

    protected static String getParentFolderPath(String linkWithMask){
        int indexLastSeparator = linkWithMask.lastIndexOf(File.separatorChar);
        if(indexLastSeparator == -1){
            throw new IllegalArgumentException("Incorrect path: " + linkWithMask);
        }
        return linkWithMask.substring(0, indexLastSeparator);
    }

    protected static String getMask(String linkWithMask){
        int indexLastSeparator = linkWithMask.lastIndexOf(File.separatorChar);
        return linkWithMask.substring(indexLastSeparator + 1);
    }

	@Override
	public String getAlias() {
        return ALIAS;
	}

	@Override
	public String getHumanReadableName() {
		return "Local file";
	}

}
