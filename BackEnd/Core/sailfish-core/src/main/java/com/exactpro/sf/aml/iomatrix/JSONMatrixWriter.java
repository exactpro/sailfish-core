/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.aml.AMLBlockBrace;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.common.util.EPSCommonException;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSONMatrixWriter implements IMatrixWriter {

    public static final String EXCEPTION_UNSUPPORTED_TYPE_FMT = "Unsupported matrix type ";
    public static final String EXCEPTION_CASE_OUT_OF_BLOCK = "Test case(s) out of block";
    public static final String EXCEPTION_UNCLOSED_BLOCK_FMT = "Unclosed block of ";

    private String[] matrixHeaders = null;

    private final Map<String, JsonEntity> references = new HashMap<>();
    private final Set<String> existedReferenceKeys = new HashSet<>();
    private final List<AMLElement> currentBlockLines = new ArrayList<>();
    private final List<JsonEntity> processedBlocks = new ArrayList<>();

    private AMLBlockBrace currentAMLBlockBrace = null;

    private int runtimeRefCounter = 0;

    private static final boolean USE_NESTED_REFERENCES = false;

    private final ObjectWriter contentWriter;
    private final File outFile;

    public JSONMatrixWriter(File file, MatrixFileTypes type) {
        JsonFactory factory;
        if(type == MatrixFileTypes.JSON) {
            factory = new JsonFactory();
        } else if(type == MatrixFileTypes.YAML) {
            factory = new YAMLFactory().disable(YAMLGenerator.Feature.SPLIT_LINES);
        } else {
            throw new EPSCommonException(EXCEPTION_UNSUPPORTED_TYPE_FMT + type);
        }
        contentWriter = new ObjectMapper(factory).writer(new CustomDefaultPrettyPrinter());
        outFile = file;
    }

    @Override
    public void writeCells(SimpleCell[] cells) throws IOException {
        if(cells == null || cells.length == 0) {
            return;
        }
        if(matrixHeaders == null) {
            matrixHeaders = Arrays.stream(cells)
                    .map(SimpleCell::getValue)
                    .toArray(String[]::new);
        } else {
            process(cells);
        }
    }

    @Override
    public void write(String[] strings) throws IOException {
        if(strings == null || strings.length == 0) {
            return;
        }
        if(matrixHeaders == null) {
            matrixHeaders = Arrays.copyOf(strings, strings.length);
        } else {
            writeCells(Arrays.stream(strings)
                    .map(SimpleCell::new)
                    .toArray(SimpleCell[]::new));
        }
    }

    private void process(SimpleCell[] cells) {
        AMLElementWrapper elementWrapper = processRow(cells);
        if(elementWrapper == null) {
            return;
        }

        int type = elementWrapper.type;
        if(currentBlockLines.isEmpty() && type != AMLElementWrapper.BLOCK_START) {
            throw new EPSCommonException(EXCEPTION_CASE_OUT_OF_BLOCK);
        } else if(!currentBlockLines.isEmpty() && type == AMLElementWrapper.BLOCK_START) {
            throw new EPSCommonException(EXCEPTION_UNCLOSED_BLOCK_FMT + currentAMLBlockBrace.getName());
        }

        currentBlockLines.add(elementWrapper.amlElement);

        if(type == AMLElementWrapper.BLOCK_END) {
            JsonEntity block = processBlock();
            if(block != null) {
                processedBlocks.add(block);
            }
            currentBlockLines.clear();
        }
    }

    private AMLElementWrapper processRow(SimpleCell[] simpleCells) {
        AMLElement amlElement = new AMLElement();
        int type = AMLElementWrapper.BLOCK_CONTENT;

        for(int i = 0; i < simpleCells.length; i++) {
            SimpleCell cell = simpleCells[i];
            if(cell == null || Strings.isNullOrEmpty(cell.getValue())) {
                continue;
            }

            Column column = Column.value(matrixHeaders[i]);
            if (column == Column.Action) {
                AMLBlockBrace blockBrace = AMLBlockBrace.value(cell.getValue());
                if (blockBrace != null) {
                   if(blockBrace.isStart()) {
                       type = AMLElementWrapper.BLOCK_START;
                       currentAMLBlockBrace = blockBrace;
                   } else {
                       type = AMLElementWrapper.BLOCK_END;
                   }
                }
            } else if(column == Column.Reference) {
                existedReferenceKeys.add(cell.getValue());
            }
            amlElement.setCell((column != null)? column.getName(): matrixHeaders[i], cell);
        }

        if(amlElement.getCells().isEmpty()) {
            return null;
        }
        return new AMLElementWrapper(amlElement, type);
    }

    private JsonEntity processBlock() {
        if(currentBlockLines.isEmpty()) {
            return null;
        }

        JsonEntity mainEntity = new JsonEntity();

        for(int i = 0; i < currentBlockLines.size(); i++) {
            JsonEntity subEntity = (i == 0)? mainEntity: new JsonEntity();
            AMLElement element = currentBlockLines.get(i);
            String referenceString = null;
            String descriptionString = null;
            boolean isStartBlockBrace = false;

            Map<String, SimpleCell> cells = element.getCells();
            for(String key : cells.keySet()) {
                String value = cells.get(key).getValue();
                boolean doAppend = true;

                Column column = Column.value(key);
                if(column == Column.Action) {
                    AMLBlockBrace blockBrace = AMLBlockBrace.value(value);
                    if(blockBrace != null && blockBrace.isStart()) {
                        isStartBlockBrace = true;
                    }
                    if(i == 0 || (blockBrace != null && !blockBrace.isStart())) {
                        doAppend = false;
                    }
                } else if(column == Column.Reference) {
                    if(USE_NESTED_REFERENCES) {
                        references.put(value, subEntity);
                    }
                    referenceString = value;
                    doAppend = false;
                } else if(column == Column.Description) {
                    if(descriptionString == null) {
                        descriptionString = value;
                    }
                }

                if(USE_NESTED_REFERENCES && isBraceReferenceString(value)) {
                    String deRefValue = value.trim().substring(1, value.length() - 1);
                    if(references.containsKey(deRefValue)) {
                        subEntity.addEntry(key, references.get(deRefValue));
                        doAppend = false;
                    }
                }

                if(isStartBlockBrace && referenceString != null && !subEntity.containsKey(Column.Reference.getName())) {
                    subEntity.addEntry(Column.Reference.getName(), referenceString);
                }

                if(doAppend) {
                    Long number = tryParseNumber(value);
                    subEntity.addEntry(
                        key, (number != null)? number: value
                    );
                }
            }

            if(i > 0 && subEntity.size() > 0) {
                if(referenceString == null) {
                    referenceString = nextReferenceString(descriptionString);
                }
                mainEntity.addEntry(referenceString, subEntity);
            }
        }

        return new JsonEntity(currentAMLBlockBrace.getName(), mainEntity);
    }

    private String nextReferenceString(String descString) {
        StringBuilder sb = new StringBuilder();
        if(Strings.isNullOrEmpty(descString)) {
            sb.append("runtime_ref");
        } else {
            String[] split = descString.trim()
                    .replaceAll("[#$%&]", "")
                    .split("\\s+");
            if(split.length == 1) {
                sb.append(split[0].toLowerCase());
            } else {
                for(String sp: split) {
                    if(!sp.isEmpty()) {
                        sb.append(Character.toLowerCase(sp.charAt(0)));
                    }
                }
            }
        }
        sb.append('_');

        int stuckLength = sb.length();
        while(true) {
            String result = sb.append(runtimeRefCounter++).toString();
            if(!existedReferenceKeys.contains(result)) {
                return result;
            }
            sb.setLength(stuckLength);
        }
    }

    private static boolean isBraceReferenceString(String refString) {
        return refString.startsWith("[") && refString.endsWith("]");
    }

    private static Long tryParseNumber(String numString) {
        try {
            return Long.parseLong(numString.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void flush() throws IOException {}

    @Override
    public void close() throws Exception {
        if(!currentBlockLines.isEmpty()) {
            throw new EPSCommonException(EXCEPTION_UNCLOSED_BLOCK_FMT + currentAMLBlockBrace.getName());
        }

        String output = contentWriter.writeValueAsString(processedBlocks);
        try(Writer writer = new BufferedWriter(new FileWriter(outFile))) {
            writer.append(output);
        }
    }


    static class JsonEntity {
        private final Map<String, Object> keyValues = new LinkedHashMap<>();

        public JsonEntity() {}

        public JsonEntity(String initKey, Object initValue) {
            addEntry(initKey, initValue);
        }

        public void addEntry(String key, Object value) {
            keyValues.put(key, value);
        }

        public int size() {
            return keyValues.size();
        }

        public boolean containsKey(String key) {
            return keyValues.containsKey(key);
        }

        @JsonAnyGetter
        public Map<String, Object> getValues() {
            if(keyValues.isEmpty()) {
                return null;
            }
            return Collections.unmodifiableMap(keyValues);
        }
    }

    private static class AMLElementWrapper {
        public static final int BLOCK_START = 0;
        public static final int BLOCK_CONTENT = 1;
        public static final int BLOCK_END = 2;

        public final AMLElement amlElement;
        public final int type;

        public AMLElementWrapper(AMLElement element, int type) {
            this.amlElement = element;
            this.type = type;
        }
    }

    private static class CustomDefaultPrettyPrinter extends DefaultPrettyPrinter {
        public static final String TAB = Strings.repeat(" ", 4);

        public CustomDefaultPrettyPrinter() {
            DefaultPrettyPrinter.Indenter indent = new DefaultIndenter(TAB, DefaultIndenter.SYS_LF);
            indentObjectsWith(indent);
            indentArraysWith(indent);
        }

        @NotNull
        @Override
        public DefaultPrettyPrinter createInstance() {
            return new CustomDefaultPrettyPrinter();
        }
    }

}
