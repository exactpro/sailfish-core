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
package com.exactpro.sf.testwebgui.dictionaries;

import static com.exactpro.sf.testwebgui.BeanUtil.getJavaTypeLabel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.omnifaces.model.tree.TreeModel;

import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorType;
import com.exactpro.sf.testwebgui.structures.ModifiableAttributeStructure;
import com.exactpro.sf.testwebgui.structures.ModifiableFieldStructure;

@SuppressWarnings("serial")
public class FieldEditorModel implements Comparable<FieldEditorModel>, Serializable {

	private ModifiableFieldStructure field;

	private ModifiableFieldStructure editedField;

	private AttributeModelsWrapper attributeModels = new AttributeModelsWrapper();
	private AttributeModelsWrapper valueModels	   = new AttributeModelsWrapper();

	private boolean standaloneField;

    private boolean selected;

	private boolean collapsed 					= true;
	private boolean collapsedForSlide			= true;
	private boolean propertiesCollapsed 		= true;
	private boolean propertiesCollapsedForSlide = true;

    private boolean propertiesChanged;

	private TreeModel<FieldEditorModel> treeModel;
    private int childCount;
	private int level = 1;
	private String index;

    private boolean defaultValueInherited;
    private boolean descriptionInherited;
    private boolean defaultValueOverrides;
    private boolean descriptionOverrides;
    private boolean noDefValue; // This indicates have the field default
                                        // value or not
                                        private boolean resettingDefaultValue; // We need this to prevent
                                                   // calling setter while
                                                   // resetting default value
	
	private List<DictionaryValidationError> errors;
	
	public FieldEditorModel(ModifiableFieldStructure field, boolean standaloneField) {

		this.field = field;
		
        createModels();

        if (!this.field.isMessage() && !this.field.isComplex()) {

            this.defaultValueInherited = this.field.getReference() != null;

            this.defaultValueOverrides = defaultValueInherited
                ? !StringUtils.equals(this.field.getImplDefaultValue(), this.field.getImplReference().getImplDefaultValue()) : false;

        }

		normalizeStrings(this.field);
		
		this.standaloneField = standaloneField;
	}
	
    // This finds the default value to set. If the field have values enum,
    // method returns the first value from enum, otherwise method returns empty
    // string
    private String findFirstValue() {
        return !valueModels.getModels().isEmpty() ? valueModels.getModels().get(0).getActual().getValue() : StringUtils.EMPTY;
    }

	private void normalizeStrings(ModifiableFieldStructure field) {
        if (StringUtils.isEmpty(field.getDescription())) {
            field.setDescription(null);
        }

        // We don't need to normalize if we know that field have no default
        // value
        if(noDefValue) {
            return;
        }

        if (!field.isComplex() && StringUtils.isEmpty(field.getImplDefaultValue())) {
            if (field.getImplDefaultValue() != null) {
                // This indicates that field have default value == empty string
                field.setDefaultValue(StringUtils.EMPTY);
                this.noDefValue = false;
            } else if (field.getImplDefaultValue() != null && getDefValues() != null && getDefValues().length > 0) {
                // This indicates that field have default value == empty string,
                // but field have values enum, so we put the first value from
                // enum to default value
                field.setDefaultValue(findFirstValue());
                this.noDefValue = false;
            } else {
                // This indicates that field have no default value
                field.setDefaultValue(null);
                if (!isDefaultValueInherited()) {
                    this.noDefValue = true;
                }
            }
        }

	}
	
	public class AttributeModelsWrapper implements Serializable {
		
		private List<AttributeModel> models;

		public List<AttributeModel> getModels() {
			return models;
		}

		public void setModels(List<AttributeModel> models) {
			this.models = models;
		}
	}
	
	private List<AttributeModel> createModel(boolean attributes) {

		List<AttributeModel> result = new ArrayList<>();

        Map<String, AttributeModel> knownAttributes = new LinkedHashMap<>();

		Set<String> knownOverrided = new HashSet<>();

        List<ModifiableAttributeStructure> collection = attributes ? field.getImplAttributes() : field.getImplValues();

		for (ModifiableAttributeStructure attribute : collection) {

			AttributeModel newModel = new AttributeModel(attribute, field, attributes);
			knownAttributes.put(attribute.getName(), newModel);
		}

		ModifiableFieldStructure parent = field;

		while ((parent = parent.getImplReference()) != null) {
			
			if (parent.isComplex() && !attributes) {
				continue;
			}

            collection = attributes ? parent.getImplAttributes() : parent.getImplValues();

			for (ModifiableAttributeStructure attribute : collection) {

				if (knownAttributes.containsKey(attribute.getName()) && !knownOverrided.contains(attribute.getName())) {

					AttributeModel knownModel = knownAttributes.get(attribute.getName());

					ModifiableAttributeStructure knownAttribute = knownModel.getOriginal();

                    if (!(Objects.equals(knownAttribute.getValue(), attribute.getValue())
                            && knownAttribute.getType() == attribute.getType())) {
						
						knownModel.setOverrides(true);
					}
					
					knownModel.setInheritedAttribute(true);

					knownOverrided.add(attribute.getName());

				} else {

					AttributeModel newModel = new AttributeModel(attribute, field, attributes);
					newModel.setInheritedAttribute(true);
					newModel.setOverrides(false);
					knownAttributes.put(attribute.getName(), newModel);
				}
			}
		}

		result.addAll(knownAttributes.values());

		Collections.sort(result, new AttributeModelComparator());

		return result;
	}
	
	public void reinit() {
		createModels();
	}

	public void delete(AttributeModel toDelete, boolean attribute) {

		if (attribute) {
            field.removeAttribute(toDelete.getOriginal().getName());
		} else {
            field.removeValue(toDelete.getOriginal().getName());
		}

		createModels();
	}
	
	public void applyChanges() {

        if(!field.isComplex()) {
            field.setDefaultValue(editedField.getImplDefaultValue());
            field.setJavaType(editedField.getImplJavaType());
        }

        if(!field.isMessage()) {
            field.setCollection(editedField.isCollection());
            field.setRequired(editedField.isRequired());
        }

        field.setDescription(editedField.getDescription());
        field.setName(editedField.getName());

        normalizeStrings(field);

        if(field.getDescription() == null) {
            setDescriptionOverrides(false);
		}
	}
	
    public boolean isDefaultValueChangesMade() {
        normalizeStrings(editedField);

        if(!field.isComplex() && !Objects.equals(field.getDefaultValue(), editedField.getDefaultValue())) {
            return true;
        }

        return false;
    }

	public boolean isPropertiesChangesMade() {

        if(!field.getName().equals(editedField.getName())) {
            return true;
        }
        if(!field.isComplex() && field.getImplJavaType() != editedField.getImplJavaType()) {
            return true;
        }

        normalizeStrings(editedField);

        if(!field.isComplex() && !Objects.equals(field.getDefaultValue(), editedField.getDefaultValue())) {
            return true;
        }

        if(!Objects.equals(field.getDescription(), editedField.getDescription())) {
            return true;
        }

        if(field.isRequired() != editedField.isRequired()) {
            return true;
        }
        if(field.isCollection() != editedField.isCollection()) {
            return true;
        }

        if(!field.isComplex() && field.getImplValuesSize() != editedField.getImplValuesSize()) {
            return true;
        }
        if(field.getImplAttributesSize() != editedField.getImplAttributesSize()) {
            return true;
        }

        if(!field.isComplex()) {
            for(ModifiableAttributeStructure val : field.getImplValues()) {
                for(ModifiableAttributeStructure val2 : editedField.getImplValues()) {
                    if (!val.getName().equals(val2.getName())) {
                        return true;
                    }
                    if (!Objects.equals(val.getValue(), val2.getValue())) {
                        return true;
                    }
				}
			}
		}

        for(ModifiableAttributeStructure attr : field.getImplAttributes()) {
            for(ModifiableAttributeStructure attr2 : editedField.getImplAttributes()) {
                if (!attr.getName().equals(attr2.getName())) {
                    return true;
                }
                if(attr.getType() != attr2.getType()) {
                    return true;
                }
                if (!Objects.equals(attr.getValue(), attr2.getValue())) {
                    return true;
                }
			}
		}
		
		return false;
	}
	
	private void createModels() {
        attributeModels.setModels(createModel(true));
        if(!field.isComplex()) {
            valueModels.setModels(createModel(false));
		}
	}

	private static class AttributeModelComparator implements Comparator<AttributeModel> {
		@Override
		public int compare(AttributeModel o1, AttributeModel o2) {
			return o1.getOriginal().getName().compareTo(o2.getOriginal().getName());
		}
	}
	
	public boolean isIsCollection() {
        return !field.isMessage() && field.isCollection();
    }
	
	public void setIsCollection(boolean isCollection) {
        field.setCollection(isCollection);
	}
	
	public void setIsCollection(Boolean isCollection) {
        field.setCollection(isCollection);
	}
	
	public boolean isIsCollectionEdited() {
        return !editedField.isMessage() && editedField.isCollection();
    }
	
	public void setIsCollectionEdited(boolean isCollection) {
        editedField.setCollection(isCollection);
	}
	
	public void setIsCollectionEdited(Boolean isCollection) {
        editedField.setCollection(isCollection);
	}

	public boolean isRequired() {
        return !field.isMessage() && field.isRequired();
    }
	
	public void setRequired(Boolean required) {
        field.setRequired(required);
	}

	public void setRequired(boolean required) {
        field.setRequired(required);
	}
	
	public boolean isRequiredEdited() {
        return !editedField.isMessage() && editedField.isRequired();
    }
	
	public void setRequiredEdited(boolean required) {
        editedField.setRequired(required);
	}
	
	public void setRequiredEdited(Boolean required) {
        editedField.setRequired(required);
	}

	public boolean isValuesEditable() {

        if(field.getReference() != null) {

            if(field.getImplReference().isComplex()) {
				return false;
			}

			return true;
		}

        return !field.isComplex();
	}

	public boolean isMessage() {

        if(field.getReference() != null) {

            if(field.getImplReference().isComplex()) {
				return true;
			}

			return false;
		}

        return field.isComplex();
	}
	
	public String getDescription() {
        return descriptionInherited && !descriptionOverrides ? field.getImplReference().getDescription() : field.getDescription();
    }
	
	public boolean isErrorsInside() {

        if(errors != null && !errors.isEmpty()) {
			return true;
		}

        if(treeModel != null && treeModel.getChildCount() > 0) {

            for(TreeModel<FieldEditorModel> child : treeModel.getChildren()) {
				if (child.getData().getErrors() != null && !child.getData().getErrors().isEmpty()) {
					return true;
				}
			}
		
		}
		
		return false;		
	}
	
	public String[] getDefValues() {

        if(valueModels.getModels().isEmpty()) {
			return null;
		}
		
		List<String> result = new ArrayList<>();

        for(AttributeModel model : valueModels.getModels()) {
			result.add(model.getActual().getName() + " (" + model.getActual().getValue() + ")");
		}
		
		return result.toArray(new String[0]);
	}
	
	public String getDefValEdited() {

        for(AttributeModel model : valueModels.getModels()) {
            if(model.getActual().getValue().equals(editedField.getDefaultValue())) {
				return model.getActual().getName() + " (" + model.getActual().getValue() + ")";
			}
		}

        return editedField.getImplDefaultValue();
	}
	
	public void setDefValEdited(String defVal) {

        if(resettingDefaultValue) {
            this.resettingDefaultValue = false;
            return;
        }
		
		String dv = defVal;
		
		if (defVal.contains(" (")) {
			dv = defVal.substring(0, defVal.indexOf(" ("));

            for(AttributeModel model : valueModels.getModels()) {
                if (model.getActual().getName().equals(dv)) {
                    editedField.setDefaultValue(model.getActual().getValue());
                    this.noDefValue = false;
                    return;
                }
            }
		}

        editedField.setDefaultValue(defVal);
        this.noDefValue = false;
	}
	
	public void removeError(DictionaryValidationErrorLevel errorLevel) {

        if(errors == null || errors.isEmpty()) {
            return;
        }

        Iterator<DictionaryValidationError> errorsIterator = errors.iterator();
		while (errorsIterator.hasNext()) {
			DictionaryValidationError error = errorsIterator.next();

            if(error.getLevel() == errorLevel) {
				errorsIterator.remove();
			}
		}
	}
	
	public String getError(String errorType) {

        if(errors == null || errors.isEmpty()) {
            return null;
        }
		
		DictionaryValidationErrorType type = DictionaryValidationErrorType.valueOf(errorType);
		
		StringBuilder builder = new StringBuilder();
		
		int errCount = 0;

        for(DictionaryValidationError error : errors) {

            if(error.getType() == type) {
				if (!builder.toString().isEmpty()) {
					builder.append("<br />");
				}
				builder.append(error.getError());
				
				errCount++;
			}
		}
		
		String result = builder.toString();
		
		if (errCount == 1) {
            return "Error: " + result;
		} else if (errCount > 1) {
            return "Errors:<br />" + result;
		}
		
		return result;
	}

	public ModifiableFieldStructure getField() {
		return field;
	}

	public void setField(ModifiableFieldStructure field) {
		this.field = field;
	}
	
	public List<AttributeModel> getAttributeModels() {
		return attributeModels.getModels();
	}

	public void setAttributeModels(List<AttributeModel> attributeModels) {
		this.attributeModels.setModels(attributeModels);
	}

	public List<AttributeModel> getValueModels() {
		return valueModels.getModels();
	}

	public void setValueModels(List<AttributeModel> valueModels) {
		this.valueModels.setModels(valueModels);
	}

	public AttributeModelsWrapper getAttributeModelsWrapper() {
		return attributeModels;
	}

	public void setAttributeModelsWrapper(AttributeModelsWrapper attributeModels) {
		this.attributeModels = attributeModels;
	}

	public AttributeModelsWrapper getValueModelsWrapper() {
		return valueModels;
	}

	public void setValueModelsWrapper(AttributeModelsWrapper valueModels) {
		this.valueModels = valueModels;
	}

	public boolean isStandaloneField() {
		return standaloneField;
	}

	public void setStandaloneField(boolean standaloneField) {
		this.standaloneField = standaloneField;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isCollapsed() {
		return collapsed;
	}

	public void setCollapsed(boolean collapsed) {
		this.collapsed = collapsed;
	}

	public boolean isCollapsedForSlide() {
		return collapsedForSlide;
	}

	public void setCollapsedForSlide(boolean collapsedForSlide) {
		this.collapsedForSlide = collapsedForSlide;
	}
	
	public TreeModel<FieldEditorModel> getTreeModel() {
		return treeModel;
	}

	public TreeModel<FieldEditorModel> getTreeModelForPage() {
        return isMessage() && collapsed ? null : treeModel;
    }

	public void setTreeModel(TreeModel<FieldEditorModel> treeModel) {
		this.treeModel = treeModel;
	}

    public void updateTreeModelData() {
        if (treeModel == null) {
            return;
        }

        level = treeModel.getLevel();
        index = treeModel.getIndex();
        childCount = treeModel.getChildCount();

        field.setId(treeModel.getIndex());
    }

	public int getChildCount() {
		return childCount;
	}

	public void setChildCount(int childCount) {
		this.childCount = childCount;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}
	
	public String getEditedFieldType() {
        return editedField == null || editedField.getImplJavaType() == null ? null : getJavaTypeLabel(editedField.getImplJavaType());
    }
	
	public void setEditedFieldType(String str) {
        editedField.setJavaType(DictionaryEditorModel.fromTypeLabel(str));
	}
	
	public ModifiableFieldStructure getEditedField() {
		return editedField;
	}

	public void setEditedField(ModifiableFieldStructure editedField) {
		this.editedField = editedField;
		
		if (editedField != null && !field.isComplex() && field.getReference() != null) {

            if(!descriptionOverrides && StringUtils.isEmpty(field.getDescription()) &&
                    !StringUtils.isEmpty(field.getImplReference().getDescription())) {
				
				this.descriptionInherited = true;
				this.editedField.setDescription(field.getImplReference().getDescription());
			}

            if(!defaultValueOverrides) {
				this.editedField.setDefaultValue(field.getImplReference().getImplDefaultValue());
                this.noDefValue = this.editedField.getImplDefaultValue() == null;
			}
		}
	}

	public boolean isPropertiesChanged() {
		return propertiesChanged;
	}

	public void setPropertiesChanged(boolean propertiesChanged) {
		this.propertiesChanged = propertiesChanged;
	}

	public boolean isPropertiesCollapsed() {
		return propertiesCollapsed;
	}

	public void setPropertiesCollapsed(boolean propertiesCollapsed) {
		this.propertiesCollapsed = propertiesCollapsed;
	}

	public boolean isPropertiesCollapsedForSlide() {
		return propertiesCollapsedForSlide;
	}

	public void setPropertiesCollapsedForSlide(boolean propertiesCollapsedForSlide) {
		this.propertiesCollapsedForSlide = propertiesCollapsedForSlide;
	}

	public boolean isDefaultValueOverrides() {
		return defaultValueOverrides;
	}

	public void setDefaultValueOverrides(boolean defaultValueOverrides) {
		this.defaultValueOverrides = defaultValueOverrides;
	}

	public boolean isDescriptionOverrides() {
		return descriptionOverrides;
	}

	public void setDescriptionOverrides(boolean descriptionOverrides) {
		this.descriptionOverrides = descriptionOverrides;
	}

	public boolean isDefaultValueInherited() {
		return defaultValueInherited;
	}

	public void setDefaultValueInherited(boolean defaultValueInherited) {
		this.defaultValueInherited = defaultValueInherited;
	}

	public boolean isDescriptionInherited() {
		return descriptionInherited;
	}

	public void setDescriptionInherited(boolean descriptionInherited) {
		this.descriptionInherited = descriptionInherited;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public List<DictionaryValidationError> getErrors() {
		return errors;
	}

	public void setErrors(List<DictionaryValidationError> errors) {
		this.errors = errors;
	}

	@Override
	public int compareTo(FieldEditorModel o) {
        return getIndex().compareTo(o.getIndex());
	}

    public boolean isNoDefValue() {
        return noDefValue;
    }

    public void setNoDefValue(boolean noDefValue) {
        if(editedField != null) {
            if (noDefValue) { // No default value flag turned on
                if (isDefaultValueInherited()) { // If default value inherited,
                                                 // we can not delete it, we
                                                 // must reset it to default
                                                 // value of the parent field
                                                 // and turn off the 'overrides'
                                                 // flag
                    editedField.setDefaultValue(field.getImplReference().getImplDefaultValue());
                    this.defaultValueOverrides = false;
                    this.resettingDefaultValue = true;
                    this.noDefValue = editedField.getDefaultValue() == null;
                } else { // If default value not inherited, we delete it
                    editedField.setDefaultValue(null);
                    this.noDefValue = true;
                }
            } else {
                // No default value flag turned on, if the edited field not hold
                // some value,
                // we use findFirstValue() method to set default value
                if(editedField.getImplDefaultValue() == null) {
                    editedField.setDefaultValue(findFirstValue());
                }
                this.noDefValue = false;
            }
        }
    }
}
