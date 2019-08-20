/*
 * Copyright (c) 2009-2019, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 */

package com.exactpro.sf.common;

import static org.hamcrest.CoreMatchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.DefaultMessageStructureVisitor;
import com.exactpro.sf.common.messages.DirtyConst;
import com.exactpro.sf.common.messages.DirtyMessageTraverser;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.MessageTraverser;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureType;
import com.exactpro.sf.common.messages.structures.impl.FieldStructure;
import com.exactpro.sf.common.messages.structures.impl.MessageStructure;

public class DirtyMessageTraverserTest {

    public static final String FIRST_FIELD = "firstField";
    public static final String SECOND_FIELD = "secondField";

    private final IMessageStructure byDictionary;

    public DirtyMessageTraverserTest() {
        IFieldStructure firstField = new FieldStructure(FIRST_FIELD, "test", JavaType.JAVA_LANG_STRING, false, StructureType.SIMPLE);
        IFieldStructure secondField = new FieldStructure(SECOND_FIELD, "test", JavaType.JAVA_LANG_STRING, false, StructureType.SIMPLE);
        Map<String, IFieldStructure> fields = new LinkedHashMap<>();
        fields.put(firstField.getName(), firstField);
        fields.put(secondField.getName(), secondField);
        byDictionary = new MessageStructure("test", "test", "", fields, Collections.emptyMap(), null);
    }

    /**
     * Test that additional dirty fields presented at IMessage with name same to original will be overriden
     */
    @Test
    public void testOverrideDirty() {
        MessageTraverser traverser = new MessageTraverser();

        IMessage message = DefaultMessageFactory.getFactory().createMessage("test", "test");
        message.addField(FIRST_FIELD, 1L);
        message.addField(SECOND_FIELD, "kek");
        message.addField("unknownField", 1.0F);

        AtomicInteger e = new AtomicInteger();

        traverser.traverse(new DefaultMessageStructureVisitor() {
            @Override
            public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
                Assert.assertThat(fieldName, is(SECOND_FIELD));
                Assert.assertThat(value, is("kek"));
                e.incrementAndGet();
            }

            @Override
            public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
                Assert.assertThat(fieldName, is(FIRST_FIELD));
                Assert.assertThat(value, is(1L));
                e.incrementAndGet();
            }

            @Override
            public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {
                Assert.assertThat(fieldName, is("unknownField"));
                Assert.assertThat(value, is(1.0F));
                e.incrementAndGet();
            }
        }, byDictionary, message, MessageStructureReaderHandlerImpl.instance());

        Assert.assertThat(e.get(), is(3));
    }

    /**
     * Test that fields with special value EXCLUDE_FIELD will be not traversed
     * and using FIELD_ORDER pop specified fields to start of message in right order
     */
    @Test
    public void testDirtyFieldOrder() {
        MessageTraverser traverser = new DirtyMessageTraverser();

        IMessage message = DefaultMessageFactory.getFactory().createMessage("test", "test");
        message.addField(FIRST_FIELD, 1L);
        message.addField(SECOND_FIELD, "kek");
        message.addField("unknownField", 1.0F);
        message.addField("excludeMePls", DirtyConst.EXCLUDED_FIELD);
        message.addField(DirtyConst.FIELD_ORDER, Arrays.asList("unknownField", SECOND_FIELD));

        List<String> orderHistoty = new ArrayList<>();

        traverser.traverse(new DefaultMessageStructureVisitor() {
            @Override
            public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
                orderHistoty.add(fieldName);
                Assert.assertThat(fieldName, is(SECOND_FIELD));
                Assert.assertThat(value, is("kek"));
            }

            @Override
            public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
                orderHistoty.add(fieldName);
                Assert.assertThat(fieldName, is(FIRST_FIELD));
                Assert.assertThat(value, is(1L));
            }

            @Override
            public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {
                orderHistoty.add(fieldName);
                Assert.assertThat(fieldName, is("unknownField"));
                Assert.assertThat(value, is(1.0F));
            }
        }, byDictionary, message, MessageStructureReaderHandlerImpl.instance());

        Assert.assertThat(orderHistoty, is(Arrays.asList("unknownField", SECOND_FIELD, FIRST_FIELD )));
    }

}
