/*******************************************************************************
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

package com.exactpro.sf.testwebgui.dictionaries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.omnifaces.model.tree.TreeModel;

import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.structures.ModifiableAttributeStructure;

public class ComplexAttributesUpdateModel {
    private ModifiableAttributeStructure newAttribute = new ModifiableAttributeStructure();
    private List<ModifiableAttributeStructure> newAttributes = new ArrayList<>();
    private List<String> updatedMsgs = new ArrayList<>();
    private List<String> updatedMsgFields = new ArrayList<>();
    private List<String> updatedFields = new ArrayList<>();

    private String fieldName;
    private String msgName;
    private String msgFieldName;

    private AttributeAction action = AttributeAction.ADD;

    public List<ModifiableAttributeStructure> getNewAttributes() {
        return newAttributes;
    }

    public List<String> getUpdatedMsgs() {
        return updatedMsgs;
    }

    public List<String> getUpdatedMsgFields() {
        return updatedMsgFields;
    }

    public List<String> getUpdatedFields() {
        return updatedFields;
    }

    public void addMsg() {
        updatedMsgs.add(msgName);
        msgName = "";
    }

    public void addMsgField() {
        updatedMsgFields.add(msgFieldName);
        msgFieldName = "";
    }

    public void addField() {
        updatedFields.add(fieldName);
        fieldName = "";
    }

    public void removeAttr(ModifiableAttributeStructure attr) {
        newAttributes.remove(attr);
    }

    public void removeMsg(String name) {
        updatedMsgs.remove(name);
    }

    public void removeMsgField(String name) {
        updatedMsgFields.remove(name);
    }

    public void removeField(String name) {
        updatedFields.remove(name);
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getMsgName() {
        return msgName;
    }

    public void setMsgName(String msgName) {
        this.msgName = msgName;
    }

    public String getMsgFieldName() {
        return msgFieldName;
    }

    public void setMsgFieldName(String msgFieldName) {
        this.msgFieldName = msgFieldName;
    }

    public AttributeAction getAction() {
        return action;
    }

    public void setAction(AttributeAction action) {
        validateOnActionChange(this.action, action);
        this.action = action;
    }

    private void validateOnActionChange(AttributeAction oldAction, AttributeAction newAction) {
        if (oldAction == newAction) {
            return;
        }

        if (oldAction == AttributeAction.REMOVE) {
            boolean showMsg = false;
            Iterator iterator = newAttributes.iterator();
            while (iterator.hasNext()) {
                ModifiableAttributeStructure attr = (ModifiableAttributeStructure) iterator.next();
                if (StringUtils.isEmpty(attr.getValue()) || attr.getType() == null) {
                    iterator.remove();
                    showMsg = true;
                }
            }

            if (showMsg) {
                BeanUtil.addWarningMessage("Validation",
                                           "Invalid attributes for action " + newAction.uiName + " was removing");
            }
        }
    }

    public void addAttr() {
        newAttributes.add(newAttribute);
        newAttribute = new ModifiableAttributeStructure();
    }

    public ModifiableAttributeStructure getNewAttribute() {
        return newAttribute;
    }

    public void setNewAttribute(ModifiableAttributeStructure newAttribute) {
        this.newAttribute = newAttribute;
    }

    public void update(DictionaryEditorModel dictModel) {
        if (dictModel.getDict() == null) {
            BeanUtil.addErrorMessage("Error", "The dictionary was not selected");
            return;
        }

        if (!updatedFields.isEmpty()) {
            for (FieldEditorModel field : dictModel.getFieldModels()) {
                if (updatedFields.contains(field.getField().getName())) {
                    action.accept(dictModel, field, newAttributes);
                }
            }
        }

        boolean updateMsgs = !updatedMsgs.isEmpty();
        boolean updateMsgFields = !updatedMsgFields.isEmpty();

        if (updateMsgs || updateMsgFields) {
            for (TreeModel<FieldEditorModel> message : dictModel.getTree().getChildren()) {
                if (updateMsgs && updatedMsgs.contains(message.getData().getField().getName())) {
                    action.accept(dictModel, message.getData(), newAttributes);
                }

                if (updateMsgFields) {
                    for (TreeModel<FieldEditorModel> msgFieldModel : message.getChildren()) {
                        FieldEditorModel msgField = msgFieldModel.getData();
                        if (updatedMsgFields.contains(msgField.getField().getName())) {
                            action.accept(dictModel, msgField, newAttributes);
                        }
                    }
                }
            }
        }

        clean();
    }

    private void clean() {
        newAttribute = new ModifiableAttributeStructure();
        newAttributes = new ArrayList<>();
        updatedMsgs = new ArrayList<>();
        updatedMsgFields = new ArrayList<>();
        updatedFields = new ArrayList<>();

        fieldName = "";
        msgName = "";
        msgFieldName = "";
    }

    public enum AttributeAction {
        ADD("Add") {
            @Override
            public void accept(DictionaryEditorModel dictModel, FieldEditorModel model,
                               List<ModifiableAttributeStructure> attributes) {
                dictModel.setFieldEditorModel(model);
                for (ModifiableAttributeStructure newAttr : attributes) {
                    dictModel.setNewAttribute(newAttr);
                    dictModel.addNewAttribute();
                }
            }
        }, UPDATE("Update") {
            @Override
            public void accept(DictionaryEditorModel dictModel, FieldEditorModel model,
                               List<ModifiableAttributeStructure> attributes) {
                List<AttributeModel> attributeModels = model.getAttributeModels();
                for (AttributeModel attributeModel : attributeModels) {
                    ModifiableAttributeStructure actual = attributeModel.getActual();
                    for (ModifiableAttributeStructure newValue : attributes) {
                        if (actual.getName().equals(newValue.getName())) {
                            newValue.setCastValue(actual.getCastValue());
                            attributeModel.setActual(newValue);
                            dictModel.onAttributeChange(attributeModel, model);
                            break;
                        }
                    }
                }
            }
        }, REMOVE("Remove") {
            @Override
            public void accept(DictionaryEditorModel dictModel, FieldEditorModel model,
                               List<ModifiableAttributeStructure> attributes) {
                List<AttributeModel> attributeModels = model.getAttributeModels();
                for (AttributeModel attributeModel : attributeModels) {
                    ModifiableAttributeStructure actual = attributeModel.getActual();
                    for (ModifiableAttributeStructure attribute : attributes) {
                        if (actual.getName().equals(attribute.getName())) {
                            dictModel.deleteAttribute(model, attributeModel, true, false);
                            break;
                        }
                    }
                }
            }
        };

        AttributeAction(String uiName) {
            this.uiName = uiName;
        }

        private final String uiName;

        public String getUiName() {
            return uiName;
        }

        public abstract void accept(DictionaryEditorModel dictModel, FieldEditorModel model,
                                    List<ModifiableAttributeStructure> attributes);

    }

}
