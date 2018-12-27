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
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.structures.ModifiableAttributeStructure;
import com.exactpro.sf.testwebgui.structures.ModifiableFieldStructure;

@SuppressWarnings("serial")
public class FieldEditorModel implements Comparable<FieldEditorModel>, Serializable {

	private ModifiableFieldStructure field;

	private ModifiableFieldStructure editedField;

	private AttributeModelsWrapper attributeModels = new AttributeModelsWrapper();
	private AttributeModelsWrapper valueModels	   = new AttributeModelsWrapper();

	private boolean standaloneField;

	private boolean selected = false;

	private boolean collapsed 					= true;
	private boolean collapsedForSlide			= true;
	private boolean propertiesCollapsed 		= true;
	private boolean propertiesCollapsedForSlide = true;

	private boolean propertiesChanged = false;

	private TreeModel<FieldEditorModel> treeModel;
	private int childCount = 0;
	private int level = 1;
	private String index;
	
	private boolean defaultValueInherited = false;
	private boolean descriptionInherited  = false;
	private boolean defaultValueOverrides = false;
	private boolean descriptionOverrides  = false;
    private boolean noDefValue = false; // This indicates have the field default
                                        // value or not
    private boolean resettingDefaultValue = false; // We need this to prevent
                                                   // calling setter while
                                                   // resetting default value
	
	private List<DictionaryValidationError> errors;
	
	public FieldEditorModel(ModifiableFieldStructure field, boolean standaloneField) {

		this.field = field;
		
        createModels();

        if (!this.field.isMessage() && !this.field.isComplex()) {

            this.defaultValueInherited = this.field.getReference() != null;

            this.defaultValueOverrides = this.defaultValueInherited
                ? !StringUtils.equals(this.field.getImplDefaultValue(), this.field.getImplReference().getImplDefaultValue()) : false;

        }

		normalizeStrings(this.field);
		
		this.standaloneField = standaloneField;
	}
	
    // This finds the default value to set. If the field have values enum,
    // method returns the first value from enum, otherwise method returns empty
    // string
    private String findFirstValue() {
        return !this.valueModels.getModels().isEmpty() ? this.valueModels.getModels().get(0).getActual().getValue() : StringUtils.EMPTY;
    }

	private void normalizeStrings(ModifiableFieldStructure field) {
        if (StringUtils.isEmpty(field.getDescription())) {
            field.setDescription(null);
        }

        // We don't need to normalize if we know that field have no default
        // value
        if (this.noDefValue) {
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

		List<ModifiableAttributeStructure> collection;

		Map<String, AttributeModel> knownAttributes = new LinkedHashMap<>();

		Set<String> knownOverrided = new HashSet<>();

		if (attributes) {
			collection = field.getImplAttributes();
		} else {
			collection = field.getImplValues();
		}

		for (ModifiableAttributeStructure attribute : collection) {

			AttributeModel newModel = new AttributeModel(attribute, field, attributes);
			knownAttributes.put(attribute.getName(), newModel);
		}

		ModifiableFieldStructure parent = field;

		while ((parent = parent.getImplReference()) != null) {
			
			if (parent.isComplex() && !attributes) {
				continue;
			}

			if (attributes) {
				collection = parent.getImplAttributes();
			} else {
				collection = parent.getImplValues();
			}

			for (ModifiableAttributeStructure attribute : collection) {

				if (knownAttributes.containsKey(attribute.getName()) && !knownOverrided.contains(attribute.getName())) {

					AttributeModel knownModel = knownAttributes.get(attribute.getName());

					ModifiableAttributeStructure knownAttribute = knownModel.getOriginal();

                    if (!(Objects.equals(knownAttribute.getValue(), attribute.getValue())
                            && Objects.equals(knownAttribute.getType(), attribute.getType()))) {
						
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
			this.field.removeAttribute(toDelete.getOriginal().getName());
		} else {
			this.field.removeValue(toDelete.getOriginal().getName());
		}

		createModels();
	}
	
	public void applyChanges() {
		
		if (!this.field.isComplex()) {
            this.field.setDefaultValue(this.editedField.getImplDefaultValue());
			this.field.setJavaType(this.editedField.getImplJavaType());
		}
		
		if (!this.field.isMessage()) {
			this.field.setCollection(this.editedField.isCollection());
			this.field.setRequired(this.editedField.isRequired());
		}
		
		this.field.setDescription (this.editedField.getDescription());
		this.field.setName(this.editedField.getName());
		
		normalizeStrings(this.field);

		if (this.field.getDescription() == null) {
			this.setDescriptionOverrides(false);
		}
	}
	
    public boolean isDefaultValueChangesMade() {
        normalizeStrings(this.editedField);

        if (!this.field.isComplex() && !Objects.equals(this.field.getDefaultValue(), this.editedField.getDefaultValue())) {
            return true;
        }

        return false;
    }

	public boolean isPropertiesChangesMade() {
		
        if (!this.field.getName().equals(this.editedField.getName())) {
            return true;
        }
        if (!this.field.isComplex() && !Objects.equals(this.field.getImplJavaType(), this.editedField.getImplJavaType())) {
            return true;
        }
		
		normalizeStrings(this.editedField);
		
        if (!this.field.isComplex() && !Objects.equals(this.field.getDefaultValue(), this.editedField.getDefaultValue())) {
            return true;
        }
		
        if (!Objects.equals(this.field.getDescription(), this.editedField.getDescription())) {
            return true;
        }
		
        if (this.field.isRequired() != this.editedField.isRequired()) {
            return true;
        }
        if (this.field.isCollection() != this.editedField.isCollection()) {
            return true;
        }
		
        if (!this.field.isComplex() && this.field.getImplValuesSize() != this.editedField.getImplValuesSize()) {
            return true;
        }
        if (this.field.getImplAttributesSize() != this.editedField.getImplAttributesSize()) {
            return true;
        }
		
		if (!this.field.isComplex()) {
			for (ModifiableAttributeStructure val : this.field.getImplValues()) {
				for (ModifiableAttributeStructure val2 : this.editedField.getImplValues()) {
                    if (!val.getName().equals(val2.getName())) {
                        return true;
                    }
                    if (!Objects.equals(val.getValue(), val2.getValue())) {
                        return true;
                    }
				}
			}
		}
		
		for (ModifiableAttributeStructure attr : this.field.getImplAttributes()) {
			for (ModifiableAttributeStructure attr2 : this.editedField.getImplAttributes()) {
                if (!attr.getName().equals(attr2.getName())) {
                    return true;
                }
                if (!Objects.equals(attr.getType(), attr2.getType())) {
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
		this.attributeModels.setModels(createModel(true));
		if (!this.field.isComplex()) {
			this.valueModels.setModels(createModel(false));
		}
	}

	private static class AttributeModelComparator implements Comparator<AttributeModel> {
		@Override
		public int compare(AttributeModel o1, AttributeModel o2) {
			return o1.getOriginal().getName().compareTo(o2.getOriginal().getName());
		}
	}
	
	public boolean isIsCollection() {
		
		if (this.field.isMessage()) {
			return false;
		}
		
		return this.field.isCollection();
	}
	
	public void setIsCollection(boolean isCollection) {
		this.field.setCollection(isCollection);
	}
	
	public void setIsCollection(Boolean isCollection) {
		this.field.setCollection(isCollection);
	}
	
	public boolean isIsCollectionEdited() {
		
		if (this.editedField.isMessage()) {
			return false;
		}
		
		return this.editedField.isCollection();
	}
	
	public void setIsCollectionEdited(boolean isCollection) {
		this.editedField.setCollection(isCollection);
	}
	
	public void setIsCollectionEdited(Boolean isCollection) {
		this.editedField.setCollection(isCollection);
	}

	public boolean isRequired() {
		
		if (this.field.isMessage()) {
			return false;
		}
		
		return this.field.isRequired();
	}
	
	public void setRequired(Boolean required) {
		this.field.setRequired(required);
	}

	public void setRequired(boolean required) {
		this.field.setRequired(required);
	}
	
	public boolean isRequiredEdited() {
		
		if (this.editedField.isMessage()) {
			return false;
		}
		
		return this.editedField.isRequired();
	}
	
	public void setRequiredEdited(boolean required) {
		this.editedField.setRequired(required);
	}
	
	public void setRequiredEdited(Boolean required) {
		this.editedField.setRequired(required);
	}

	public boolean isValuesEditable() {

		if (this.field.getReference() != null) {

			if (this.field.getImplReference().isComplex()) {
				return false;
			}

			return true;
		}

		return !this.field.isComplex();
	}

	public boolean isMessage() {

		if (this.field.getReference() != null) {

			if (this.field.getImplReference().isComplex()) {
				return true;
			}

			return false;
		}

		return this.field.isComplex();
	}
	
	public String getDescription() {
		
		if (descriptionInherited && !descriptionOverrides) {
			return this.field.getImplReference().getDescription();
		}
		
		return this.field.getDescription();
	}
	
	public boolean isErrorsInside() {
		
		if (this.errors != null && !this.errors.isEmpty()) {
			return true;
		}
		
		if (this.treeModel != null && this.treeModel.getChildCount() > 0) {
		
			for (TreeModel<FieldEditorModel> child : this.treeModel.getChildren()) {
				if (child.getData().getErrors() != null && !child.getData().getErrors().isEmpty()) {
					return true;
				}
			}
		
		}
		
		return false;		
	}
	
	public String[] getDefValues() {
		
		if (this.valueModels.getModels().size() == 0) {
			return null;
		}
		
		List<String> result = new ArrayList<>();
		
		for (AttributeModel model : this.valueModels.getModels()) {
			result.add(model.getActual().getName() + " (" + model.getActual().getValue() + ")");
		}
		
		return result.toArray(new String[0]);
	}
	
	public String getDefValEdited() {
		
		for (AttributeModel model : this.valueModels.getModels()) {
			if (model.getActual().getValue().equals(this.editedField.getDefaultValue())) {
				return model.getActual().getName() + " (" + model.getActual().getValue() + ")";
			}
		}
		
		return this.editedField.getImplDefaultValue();
	}
	
	public void setDefValEdited(String defVal) {

        if (this.resettingDefaultValue) {
            this.resettingDefaultValue = false;
            return;
        }
		
		String dv = defVal;
		
		if (defVal.contains(" (")) {
			dv = defVal.substring(0, defVal.indexOf(" ("));

            for (AttributeModel model : this.valueModels.getModels()) {
                if (model.getActual().getName().equals(dv)) {
                    this.editedField.setDefaultValue(model.getActual().getValue());
                    this.noDefValue = false;
                    return;
                }
            }
		}
		
		this.editedField.setDefaultValue(defVal);
        this.noDefValue = false;
	}
	
	public void removeError(DictionaryValidationErrorLevel errorLevel) {
		
		if (this.errors == null || this.errors.isEmpty()) return;
		
		Iterator<DictionaryValidationError> errorsIterator = this.errors.iterator();
		while (errorsIterator.hasNext()) {
			DictionaryValidationError error = errorsIterator.next();
			
			if (error.getLevel().equals(errorLevel)) {
				errorsIterator.remove();
			}
		}
	}
	
	public String getError(String errorType) {
		
		if (this.errors == null || this.errors.isEmpty()) return null;
		
		DictionaryValidationErrorType type = DictionaryValidationErrorType.valueOf(errorType);
		
		StringBuilder builder = new StringBuilder();
		
		int errCount = 0;
		
		for (DictionaryValidationError error : this.errors) {
			
			if (error.getType().equals(type)) {
				if (!builder.toString().isEmpty()) {
					builder.append("<br />");
				}
				builder.append(error.getError());
				
				errCount++;
			}
		}
		
		String result = builder.toString();
		
		if (errCount == 1) {
			result = "Error: " + result;
		} else if (errCount > 1) {
			result = "Errors:<br />" + result;
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
		if (isMessage() && this.collapsed) return null;
		return treeModel;
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
		if (this.editedField == null || this.editedField.getImplJavaType() == null) return null;
        return BeanUtil.getJavaTypeLabel(this.editedField.getImplJavaType());
	}
	
	public void setEditedFieldType(String str) {
        this.editedField.setJavaType(DictionaryEditorModel.fromTypeLabel(str));
	}
	
	public ModifiableFieldStructure getEditedField() {
		return editedField;
	}

	public void setEditedField(ModifiableFieldStructure editedField) {
		this.editedField = editedField;
		
		if (editedField != null && !field.isComplex() && field.getReference() != null) {
			
			if (!this.descriptionOverrides && StringUtils.isEmpty(field.getDescription()) && 
					!StringUtils.isEmpty(field.getImplReference().getDescription())) {
				
				this.descriptionInherited = true;
				this.editedField.setDescription(field.getImplReference().getDescription());
			}

            if (!this.defaultValueOverrides) {
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
		return this.getIndex().compareTo(o.getIndex());
	}

    public boolean isNoDefValue() {
        return noDefValue;
    }

    public void setNoDefValue(boolean noDefValue) {
        if (this.editedField != null) {
            if (noDefValue) { // No default value flag turned on
                if (isDefaultValueInherited()) { // If default value inherited,
                                                 // we can not delete it, we
                                                 // must reset it to default
                                                 // value of the parent field
                                                 // and turn off the 'overrides'
                                                 // flag
                    this.editedField.setDefaultValue(this.field.getImplReference().getImplDefaultValue());
                    this.defaultValueOverrides = false;
                    this.resettingDefaultValue = true;
                    this.noDefValue = this.editedField.getDefaultValue() == null;
                } else { // If default value not inherited, we delete it
                    this.editedField.setDefaultValue(null);
                    this.noDefValue = true;
                }
            } else {
                // No default value flag turned on, if the edited field not hold
                // some value,
                // we use findFirstValue() method to set default value
                if (this.editedField.getImplDefaultValue() == null) {
                    this.editedField.setDefaultValue(findFirstValue());
                }
                this.noDefValue = false;
            }
        }
    }
}
