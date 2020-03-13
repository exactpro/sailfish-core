/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.fast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.Ignore;
import org.junit.Test;
import org.openfast.Context;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import org.openfast.template.Field;
import org.openfast.template.FieldSet;
import org.openfast.template.Group;
import org.openfast.template.MessageTemplate;
import org.openfast.template.Sequence;
import org.openfast.template.TemplateRegistry;
import org.openfast.template.loader.XMLMessageTemplateLoader;

import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.services.fast.blockstream.StreamBlockLengthReader;

public class TestFastTemplate {

    private final Path templatePath = Paths.get(
            "");
    private final Path dataPath = Paths.get("");
    private final boolean isTraceEnabled = false;

    @Test
    @Ignore("Manual check for file")
    public void testTemplate() throws Exception {
        long number = 1;
        try {
            Context context = new FASTContext();
            context.setTemplateRegistry(loadFastTemplates(templatePath));
            context.setTraceEnabled(isTraceEnabled);
            context.setDecodeTrace(new LoggingTrace());

            try (InputStream in = new BufferedInputStream(Files.newInputStream(dataPath, StandardOpenOption.READ))) {
                FASTMessageInputStream msgStream = new FASTMessageInputStream(in, context);
                msgStream.setBlockReader(new StreamBlockLengthReader());
                for (Message fastMessage = msgStream.readMessage(0);
                     fastMessage != null;
                     fastMessage = msgStream.readMessage(0)) {
                    System.out.println(number + ": " + formatFastMessage(fastMessage));
                    number++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed " + number, e);
        }
    }

    private String formatFastMessage(Message message) {
        StringBuilder builder = new StringBuilder();
        String indent = "";

        MessageTemplate template = message.getTemplate();
        builder.append("Message template: ")
                .append(template.getName())
                .append("->");
        int count = message.getFieldCount();
        formatValues(builder, indent, message, template, count);

        return builder.toString();
    }

    private void formatValues(StringBuilder builder, String indent, GroupValue groupValue, FieldSet fieldSet, int number) {
        for (int fieldIndex = 0; fieldIndex < number; fieldIndex++) {
            if (groupValue.isDefined(fieldIndex)) {
                Field field = fieldSet.getField(fieldIndex);
                if (field instanceof Sequence) {
                    formatSequence(field, groupValue.getSequence(fieldIndex), builder, indent);
                    builder.append("\n")
                            .append(indent);
                } else {
                    builder.append(field.getName())
                            .append('{')
                            .append(field.getId())
                            .append("}=")
                            .append(groupValue.getValue(fieldIndex))
                            .append("|");
                }
            }
        }
    }

    private void formatSequence(Field sequenceField, SequenceValue sequenceValue, StringBuilder builder, String indent) {
        Sequence sequence = sequenceValue.getSequence();
        Group group = sequence.getGroup();
        int fieldCount = group.getFieldCount();
        String sequenceName = sequence.getName();

        GroupValue[] entries = sequenceValue.getValues();

        indent = indent + "\t";

        builder.append("\n")
                .append(indent)
                .append(sequenceName)
                .append('{')
                .append(sequenceField.getId())
                .append("} Sequence[")
                .append(entries.length)
                .append("]:");
        indent = indent + "\t";

        for (int i = 0; i < entries.length; i++) {
            GroupValue entry = entries[i];
            builder.append("\n")
                    .append(indent);
            formatValues(builder, indent, entry, sequence, fieldCount);
        }
    }

    private TemplateRegistry loadFastTemplates(Path templatePath) throws IOException {
        try (InputStream templateStream = new BufferedInputStream(Files.newInputStream(templatePath, StandardOpenOption.READ))) {
            XMLMessageTemplateLoader loader = new XMLMessageTemplateLoader();
            loader.setLoadTemplateIdFromAuxId(true);
            loader.load(templateStream);
            return loader.getTemplateRegistry();
        }
    }
}
