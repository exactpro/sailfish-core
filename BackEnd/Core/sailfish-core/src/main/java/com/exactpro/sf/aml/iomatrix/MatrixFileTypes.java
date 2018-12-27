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
package com.exactpro.sf.aml.iomatrix;

public enum MatrixFileTypes {
    CSV("csv"), XLS("xls"), XLSX("xlsx"), UNKNOWN("?"), JSON("json");
    
    
    private String fileType;
    
    private MatrixFileTypes(String extension) {
        this.fileType = extension;
    }
    
    public String getExtension() {
        return fileType;
    }
    
	public static MatrixFileTypes detectFileType(String name) {
		for (MatrixFileTypes type : MatrixFileTypes.values()) {
			if (name.toLowerCase().endsWith(type.getExtension().toLowerCase())) {
				return type;
			}
		}

		return UNKNOWN;
	}
}
