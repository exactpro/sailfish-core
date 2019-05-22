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
package com.exactpro.sf.testwebgui.help;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.TreeNode;

import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.iomatrix.CSVDelimiter;
import com.exactpro.sf.aml.iomatrix.CSVMatrixReader;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.help.helpmarshaller.HelpEntityType;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.FieldJsonContainer;
import com.exactpro.sf.testwebgui.BeanUtil;

public class CopyingFormat {

    private static final CSVDelimiter DEFAULT_SEPARATOR = CSVDelimiter.TAB;
    private static final String LINE_SEPARATOR = "\\n";

    private static final String REF_POSTFIX = "_ref";

    private final Set<String> header = new LinkedHashSet<>();
    private final Set<String> newColumns = new LinkedHashSet<>();
    private final Map<String, Map<String, String>> groups = new HashMap<>();
    private final Map<String, String> mainLine = new HashMap<>();

    private final boolean headerWasSet;

    private final String separator;

    public CopyingFormat(String header) {

        if (StringUtils.isEmpty(header)) {
            this.separator = DEFAULT_SEPARATOR.getStringValue();
            this.headerWasSet = false;
            return;
        }

        CSVDelimiter determined = CSVMatrixReader.determineCSVDelimiter(header.getBytes(), header.length());

        if (determined == null) {
            determined = CSVDelimiter.TAB;
        }

        this.separator = determined.getStringValue();

        String[] headerArr = header.split(determined.getEscaped());

        this.header.addAll(Arrays.asList(headerArr));

        this.headerWasSet = true;
    }
    public void format(TreeNode node){
        FieldJsonContainer nodeData = (FieldJsonContainer) node.getData();

        if (node.getType().equals(HelpEntityType.MESSAGE.name())) {

            addToLine(null, Column.MessageType.getName(), nodeData.getName());

            for (TreeNode fieldNode : node.getChildren()) {
               formatStructureChildren(fieldNode, null, 1);
            }

        } else {

          formatStructureChildren(node, null, 0);

        }
    }
    public void addToLine(String group, String header, String content) {

        if(!headerWasSet) {

            this.header.add(header);

        } else if (!this.header.contains(header)) {

            newColumns.add(header);

        }

        if (group == null) {

            mainLine.put(header, content);

        } else {
            groups.get(group).put(header, content);
        }
    }
    
    private String addGroup(String group) {
        String groupName = !groups.containsKey(group) ? group : group + "0";
        groups.put(groupName, new HashMap<String, String>());
        return groupName;
    }

    public String copyToClipboard() {

        StringBuilder builder = new StringBuilder();

        if(groups != null) {
            for(Map<String, String> group : groups.values()) {
                addLine(builder, group);
            }
        }

        addLine(builder, mainLine);

        return builder.toString();
    }

    public String copyNewColumns() {
        return !newColumns.isEmpty() ? StringUtils.join(newColumns, separator) : null;
    }

    public String copyJustHeader() {
        return createAllHeader();
    }

    public String copyAllStructure() {

        StringBuilder builder = new StringBuilder();

        builder.append(createAllHeader()).append(LINE_SEPARATOR);

        header.addAll(newColumns);

        if(groups != null) {
            for(Map<String, String> group : groups.values()) {
                addLine(builder, group);
            }
        }

        addLine(builder, mainLine);

        return builder.toString();
    }

    private String createAllHeader() {

        StringBuilder builder = new StringBuilder();

        builder.append(StringUtils.join(header, separator));

        if(!newColumns.isEmpty()) {
            builder.append(separator).append(StringUtils.join(newColumns, separator));
        }

        return builder.toString();
    }

    private void addLine(StringBuilder builder, Map<String, String> line) {

        List<String> contentLine = new ArrayList<>();

        for(String header : header) {
            contentLine.add(line.containsKey(header) ? line.get(header) : "");
        }

        builder.append(StringUtils.join(contentLine, separator))
                .append(LINE_SEPARATOR);
    }

    public void formatStructureChildren(IFieldStructure structure, String toGroup, int level) {

        if (!structure.isComplex()) {

            addToLine(toGroup, structure.getName(), BeanUtil.getJavaTypeLabel(structure.getJavaType()));

        } else {
            
            String groupName = null;

            if (level > 0) {
                groupName = addGroup(structure.getName() + REF_POSTFIX);
            }
            
            addToLine(groupName, Column.Reference.getName(), groupName);

            if (level > 0) {
                addToLine(null, structure.getName(), "[" + groupName + "]");
            }

            for(IFieldStructure child : structure.getFields().values()) {
                formatStructureChildren(child, groupName, level + 1);
            }
        }
    }

    public void formatStructureChildren(TreeNode node, String toGroup, int level) {

        FieldJsonContainer structure = (FieldJsonContainer) node.getData();

        if (structure.getChildNodes() == null) {
            addToLine(toGroup, structure.getName(), structure.getJavaType());
        } else {

            String groupName = null;

            if (level > 0) {
                groupName = addGroup(structure.getName() + REF_POSTFIX);
            }

            addToLine(groupName, Column.Reference.getName(), groupName);

            if (level > 0) {
                addToLine(null, structure.getName(), "[" + groupName + "]");
            }

            for (TreeNode child : node.getChildren()) {
                formatStructureChildren(child, groupName, level + 1);
            }
        }
    }



    public boolean isNewColumnsWasAdded() {
        return !newColumns.isEmpty();
    }

    public Set<String> getNewColumns() {
        return headerWasSet ? new HashSet<>(newColumns) : new HashSet<>(header);
    }

    public int getNewColumnsCount() {
        return newColumns.size();
    }
}