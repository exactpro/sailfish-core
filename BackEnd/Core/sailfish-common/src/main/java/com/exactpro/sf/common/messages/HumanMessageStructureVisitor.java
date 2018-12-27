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
package com.exactpro.sf.common.messages;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.messages.structures.IFieldStructure;

public class HumanMessageStructureVisitor implements IMessageStructureVisitor {

    private final IMessageFactory messageFactory;
    private final IHumanMessage humanMessage;

    public HumanMessageStructureVisitor(IMessageFactory messageFactory, String name) {
        this.messageFactory = messageFactory;
        this.humanMessage = messageFactory.createHumanMessage(name);
    }

    public IHumanMessage getHumanMessage() {
        return humanMessage;
    }

    @Override
    public void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitBooleanCollection(String fieldName, List<Boolean> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitByteCollection(String fieldName, List<Byte> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Character value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitCharCollection(String fieldName, List<Character> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitDateCollection(String fieldName, List<LocalDate> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitTimeCollection(String fieldName, List<LocalTime> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitDateTimeCollection(String fieldName, List<LocalDateTime> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitDoubleCollection(String fieldName, List<Double> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitFloatCollection(String fieldName, List<Float> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitIntCollection(String fieldName, List<Integer> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitLongCollection(String fieldName, List<Long> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, BigDecimal value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitBigDecimalCollection(String fieldName, List<BigDecimal> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitShortCollection(String fieldName, List<Short> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visitStringCollection(String fieldName, List<String> value, IFieldStructure fldStruct, boolean isDefault) {
        if (value != null) {
            humanMessage.addField(fldStruct, fieldName, value);
        }
    }

    @Override
    public void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault) {
        if (message != null) {
            MessageTraverser traverser = new MessageTraverser();
            IHumanMessage human = visit(fieldName, message, complexField, traverser);
            humanMessage.addField(complexField, fieldName, human);
        }
    }

    @Override
    public void visitMessageCollection(String fieldName, List<IMessage> messages, IFieldStructure complexField, boolean isDefault) {
        if (messages != null) {
            MessageTraverser traverser = new MessageTraverser();
            List<IHumanMessage> list = new ArrayList<>();

            for (IMessage iMessage : messages) {
                list.add(visit(fieldName, iMessage, complexField, traverser));
            }

            humanMessage.addField(complexField, fieldName, list);
        }
    }

    /**
     * @param fieldName
     * @param message
     * @param complexField
     * @param traverser
     */
    protected IHumanMessage visit(String fieldName, IMessage message, IFieldStructure complexField, MessageTraverser traverser) {
        HumanMessageStructureVisitor visitor = new HumanMessageStructureVisitor(this.messageFactory, complexField.getName());
        traverser.traverse(visitor, complexField.getFields(), message, EmptyMessageStructureReaderHandler.instance());

        return visitor.getHumanMessage();
    }

}
