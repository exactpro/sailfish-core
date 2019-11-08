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

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum MatrixFileTypes {
    CSV("csv"), XLS("xls"), XLSX("xlsx"), UNKNOWN("?", false), JSON("json"), YAML("yaml");

    public static final Set<MatrixFileTypes> SUPPORTED_MATRIX_FILE_TYPES = Stream.of(MatrixFileTypes.values())
            .filter(MatrixFileTypes::isSupported)
            .collect(Collectors.collectingAndThen(Collectors.toSet(), ImmutableSet::copyOf));
    
    private final String fileType;
    private final boolean supported;

    MatrixFileTypes(String extension, boolean supported) {
        this.fileType = extension;
        this.supported = supported;
    }

    MatrixFileTypes(String extension) {
        this(extension, true);
    }
    
    public String getExtension() {
        return fileType;
    }

    public boolean isSupported() {
        return supported;
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
