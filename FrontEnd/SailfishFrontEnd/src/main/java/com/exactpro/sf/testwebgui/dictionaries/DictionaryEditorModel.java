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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.faces.application.FacesMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.omnifaces.model.tree.ListTreeModel;
import org.omnifaces.model.tree.TreeModel;
import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.DictionarySettings;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.configuration.IDictionaryRegistrator;
import com.exactpro.sf.configuration.dictionary.DefaultDictionaryValidatorFactory;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.WorkspaceLayerException;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.structures.ModifiableAttributeStructure;
import com.exactpro.sf.testwebgui.structures.ModifiableDictionaryStructure;
import com.exactpro.sf.testwebgui.structures.ModifiableFieldStructure;
import com.exactpro.sf.testwebgui.structures.ModifiableMessageStructure;
import com.exactpro.sf.testwebgui.structures.ModifiableXmlDictionaryStructureLoader;
import com.exactpro.sf.testwebgui.structures.XmlDictionaryStructureWriter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class DictionaryEditorModel {

    private static final Logger logger = LoggerFactory.getLogger(DictionaryEditorModel.class);

    private static final SailfishURI DEFAULT_VALIDATOR_URI = SailfishURI.unsafeParse("default");

    private String dictFileName; // Name of the current dictionary

    private List<DictFileContainer> files;

    // Selected dictionary in folder
    private DictFileContainer selectedFileContainer;
    private DictFileContainer loadedFileContainer;
    private List<DictFileContainer> selectedDictionariesForDownload;

    // Selected message in treeTable (All messages)
    private FieldEditorModel selectedLeftField;
    // Selected field in table (all fields)
    private FieldEditorModel selectedRightField;
    // Selected Field in left table (Manage message dialog)
    private ModifiableFieldStructure selectedFieldInMess;
    // Selected Field or Message in right table (Manage message dialog)
    private ModifiableFieldStructure selectedFieldInAll;

    // Current dictionary
    private ModifiableDictionaryStructure dict;
    // Messages tree
    private TreeModel<FieldEditorModel> tree;
    // Reference fields
    private List<FieldEditorModel> dictFieldModels;
    // If new dictionary has been created
    private boolean newDict;

    private SailfishURI selectedValidator;
    private IDictionaryValidator dictionaryValidator;

    // Indicates, where to add new attribute/value
    private int attributeMode = 0;

    // For attributes adding...
    private ModifiableAttributeStructure newAttribute = new ModifiableAttributeStructure();
    private FieldEditorModel fieldEditorModel;
    // For complex attributes adding
    private ComplexAttributesUpdateModel complexAttrsUpdateModel = new ComplexAttributesUpdateModel();
    // ...and removing
    private FieldEditorModel modelForAttrRemoving;
    private AttributeModel attrModelForRemoving;
    private boolean attrForRemoving;

    // For field adding
    private ModifiableFieldStructure newField = new ModifiableFieldStructure();
    private int position;
    // For 'Message' type in dialog
    private String newType = "";

    // Save As...
    private DictFileContainer saveAsContainer = new DictFileContainer(null, null, null);
    private DictionarySettings saveAsSettings = new DictionarySettings();

    private Set<SailfishURI> saveAsUtilityClassURIs;
    private boolean overwriteFile;

    // File Upload...
    private DictFileContainer uploadContainer = new DictFileContainer(null, null, null);
    private DictionarySettings uploadSettings = new DictionarySettings();

    private Set<SailfishURI> uploadUtilityClassURIs;
    private ModifiableDictionaryStructure uploadDict;

    private Set<SailfishURI> utilitySURI;
    private List<SailfishURI> validatorsURIs;

    // For Add/Move field (one dialog, but different implementation)
    private boolean movingFromRight;
    // For Clone dialogs
    private boolean cloneDialog = false;

    // For field removing dialog
    private int refCount;
    private String refString;

    private List<DictionaryValidationError> mainErrors;

    private FieldEditorModel searchModel;

    private final ISFContext context;

    public DictionaryEditorModel(final ISFContext context) {
        this.context = context;

        utilitySURI = new TreeSet<>(context.getUtilityManager().getUtilityURIs());

        this.validatorsURIs = new ArrayList<>(Arrays.asList(context.getStaticServiceManager().getServiceURIs()));
        Collections.sort(this.validatorsURIs);

        this.selectDirectory();

        this.dictionaryValidator = (new DefaultDictionaryValidatorFactory()).createDictionaryValidator();
    }

    public void selectDirectory() {

        files = new ArrayList<>();

        Map<SailfishURI, String> locations = context.getDictionaryManager().getDictionaryLocations();

        StringBuilder errorBuilder = new StringBuilder();

        for (Entry<SailfishURI, String> entry : locations.entrySet()) {

            String filename = entry.getValue();
            SailfishURI uri = entry.getKey();

            try {
                if (context.getWorkspaceDispatcher().exists(FolderType.ROOT, filename)) {
                    String namespace = context.getDictionaryManager().getDictionary(uri).getNamespace();
                    DictFileContainer container = new DictFileContainer(uri, filename, namespace);
                    files.add(container);
                } else {
                    errorBuilder.append("Dictionary ").append(uri).append(" not found in workspaces")
                            .append(System.lineSeparator());
                }
            } catch (WorkspaceSecurityException e) {

                logger.error(e.getMessage(), uri, e);
                errorBuilder.append("Dictionary ").append(uri).append(" not found in workspaces")
                        .append(System.lineSeparator());

            } catch (Exception ex) {

                logger.error("Can't parse dictionary {}", filename, ex);
                errorBuilder.append("Can't parse dictionary ").append(filename).append(System.lineSeparator());
            }
        }

        Collections.sort(files, new DictFileContainerComparator());

        if (errorBuilder.length() > 0) {
            BeanUtil.addErrorMessage("Error", errorBuilder.toString());
        }
    }

    public void createNewDictionary() {

        this.dict = new ModifiableDictionaryStructure();

        this.tree = new ListTreeModel<>();
        this.dictFieldModels = new ArrayList<>();

        this.selectedFileContainer = null;

        this.newDict = true;

        this.saveAsUtilityClassURIs = new HashSet<>();
    }

    public String getFileContainerLabel() {
        if (this.newDict) {
            return "New dictionary";
        }

        return "Select dictionary";
    }

    private void createMessagesTree() {

        List<ModifiableMessageStructure> messages = this.getMessages();

        Collections.sort(messages, new FieldComparator());

        this.tree = new ListTreeModel<>();

        for (ModifiableMessageStructure message : messages) {
            FieldEditorModel model = new FieldEditorModel(message, false);
            TreeModel<FieldEditorModel> child = addChild(tree, model);
            model.setTreeModel(child);
            model.setChildCount(child.getChildCount());
            model.setLevel(child.getLevel());
            model.setIndex(child.getIndex());
        }

        populateDictionaryFields(this.tree);
    }

    private TreeModel<FieldEditorModel> addChild(TreeModel<FieldEditorModel> parent, FieldEditorModel fieldModel) {

        TreeModel<FieldEditorModel> added = parent.addChild(fieldModel);

        ModifiableFieldStructure field = fieldModel.getField();
        List<ModifiableFieldStructure> fieldStructures = Collections.emptyList();
        
        if (field.isMessage()) {
            fieldStructures = ((ModifiableMessageStructure) field).getImplFields();
        } else if (field.isSubMessage()) {
            fieldStructures = ((ModifiableMessageStructure) field.getReference()).getImplFields();
        }
        
        for (ModifiableFieldStructure childField : fieldStructures) {
            FieldEditorModel model = new FieldEditorModel(childField, false);
            TreeModel<FieldEditorModel> node = addChild(added, model);
            model.setTreeModel(node);
            model.updateTreeModelData();
        }

        return added;
    }

    public List<FieldEditorModel> findMessageOrField(String query) {

        List<FieldEditorModel> acList = new ArrayList<>();

        for (TreeModel<FieldEditorModel> message : this.tree.getChildren()) {
            if (message.getData().getField().getName().toLowerCase().contains(query.toLowerCase())) {
                acList.add(message.getData());
            }
        }

        for (FieldEditorModel field : this.dictFieldModels) {
            if (field.getField().getName().toLowerCase().contains(query.toLowerCase())) {
                acList.add(field);
            }
        }

        EditorModelConverter.setModels(acList);

        return acList;
    }

    public void goToFoundElement() {
        goToReference(this.searchModel, true);
        if (this.searchModel.isMessage()) {
            RequestContext.getCurrentInstance().execute("updateLeftSelected(); scrollToLeftSelected();");
        } else {
            RequestContext.getCurrentInstance().execute("updateRightSelected();  scrollToRightSelected();");
        }
    }

    public void selectValidator() {

        if (DEFAULT_VALIDATOR_URI.matches(this.selectedValidator)) {
            this.dictionaryValidator = new DefaultDictionaryValidatorFactory().createDictionaryValidator();
        } else {
            this.dictionaryValidator = context.getStaticServiceManager().createDictionaryValidator(this.selectedValidator);
        }

        if (this.dictionaryValidator == null) {
            this.dictionaryValidator = new DefaultDictionaryValidatorFactory().createDictionaryValidator();
        }

        if (this.dict != null) {
            validateDictionary(false, true);
        }

        RequestContext.getCurrentInstance().update(Arrays.asList("messForm", "fieldsForm", "closeDictButtonPanel"));
    }

    public SailfishURI getDefaultValidatorURI() {
        return DEFAULT_VALIDATOR_URI;
    }

    public List<SailfishURI> getValidatorURIs() {
        return this.validatorsURIs;
    }

    public TreeNode getLeftRoot() {
        return null;
    }

    public TreeNode getRightRoot() {
        return null;
    }

    public String extractFieldIndex(String path) {
        return path.substring(path.lastIndexOf("_") + 1, path.length());
    }

    public Character getFieldTypeLabel(ModifiableFieldStructure field) {

        if (field.isComplex()) {
            return 'M';
        }

        JavaType jType = null;

        if (field.getReference() != null) {
            jType = getRefFieldType(field.getReference());
        }

        if (jType == null) {
            jType = field.getImplJavaType();
        }

        if (jType == null) {
            return '-';
        }

        String type = BeanUtil.getJavaTypeLabel(jType);

        if (type != null && type.length() > 0) {
            return type.charAt(0);
        } else {
            return '-';
        }
    }

    public static JavaType fromTypeLabel(String label) {
        for (JavaType type : JavaType.values()) {
            if (BeanUtil.getJavaTypeLabel(type).equals(label)) {
                return type;
            }
        }
        return null;
    }

    public String getAttributesLabel(ModifiableFieldStructure field) {

        Map<String, String> attributes = new HashMap<>();

        ModifiableFieldStructure current = field;

        while (true) {

            for (ModifiableAttributeStructure attribute : current.getImplAttributes()) {
                if (!attributes.containsKey(attribute.getName())) {
                    attributes.put(attribute.getName(), attribute.getValue());
                }
            }

            if (current.getReference() != null) {
                current = current.getImplReference();
            } else {
                break;
            }
        }

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
        }

        return sb.toString();
    }

    public List<ImmutablePair<String, String>> getAttributeLabels(ModifiableFieldStructure field) {

        Map<String, String> attributes = new HashMap<>();

        ModifiableFieldStructure current = field;

        while (true) {

            for (ModifiableAttributeStructure attribute : current.getImplAttributes()) {
                if (!attributes.containsKey(attribute.getName())) {
                    attributes.put(attribute.getName(), attribute.getValue());
                }
            }

            if (current.getReference() != null) {
                current = current.getImplReference();
            } else {
                break;
            }
        }

        List<ImmutablePair<String, String>> result = new ArrayList<>(attributes.size());

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            result.add(new ImmutablePair<>(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    private ModifiableFieldStructure getFieldById(String id) {
        return dict != null ? getFMById(id, dict.getImplFieldStructures()) : null;
    }

    private ModifiableFieldStructure getMessageById(String id) {
        return dict != null && id != null ? getFMById(id, getMessages()) : null;
    }

    private ModifiableFieldStructure getFMById(String id, List<? extends ModifiableFieldStructure> list) {
        for (ModifiableFieldStructure field : list) {
            if (id.equals(field.getId())) {
                return field;
            }
        }
        return null;
    }

    private FieldEditorModel getFieldModelById(String id) {
        for (FieldEditorModel model : this.dictFieldModels) {
            if (model.getField().getId().equals(id)) {
                return model;
            }
        }
        return null;
    }

    private boolean existsInTree(ModifiableFieldStructure toFind, ModifiableFieldStructure root) {

        if (root.getReference() != null && root.getImplReference().isMessage()) {

            ModifiableMessageStructure referenced = (ModifiableMessageStructure) root.getReference();

            if (referenced.getId().equals(toFind.getId())) {
                return true;
            }

            for (ModifiableFieldStructure field : referenced.getImplFields()) {
                if (existsInTree(toFind, field)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isMessageHasField(ModifiableMessageStructure message, String fieldName) {

        if (message.getFields() == null) {
            return false;
        }

        for (ModifiableFieldStructure field : message.getImplFields()) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public void selectDictFile() {

        if (this.selectedFileContainer == null) {
            BeanUtil.addInfoMessage("Info", "Please select dictionary");
            return;
        }

        if (this.dict != null) {

            RequestContext.getCurrentInstance().execute("PF('dictLoadConfirmDialog').show();");

        } else {
            selectDictFileProcess();

            RequestContext.getCurrentInstance().update("mainView");
            RequestContext.getCurrentInstance().update("ajaxStatusPanel");
        }
    }

    public void cancelSelectDictFile() {

        if (this.loadedFileContainer != null) {
            this.selectedFileContainer = this.loadedFileContainer;
        }
    }

    public DictFileContainer getFileContainerByURI(SailfishURI uri) {

        if (files == null)
            return null;

        for (DictFileContainer container : files) {
            if (container.getURI().equals(uri)) {
                return container;
            }
        }

        return null;
    }

    public void selectDictFileProcess() {

        if (this.selectedFileContainer == null) {
            BeanUtil.addInfoMessage("Info", "Please select dictionary");
            return;
        }

        ModifiableXmlDictionaryStructureLoader loader = new ModifiableXmlDictionaryStructureLoader();
        try {

            try (InputStream in = new FileInputStream(context.getWorkspaceDispatcher().getFile(
                    FolderType.ROOT, selectedFileContainer.getFileName()))) {

                this.dict = loader.load(in);
            }

            this.dictFileName = selectedFileContainer.getFileName();
            this.dictFieldModels = fieldsToFieldEditorModels(this.dict.getImplFieldStructures(), true);
            populateDictFieldModels(this.dictFieldModels);

            this.saveAsSettings = context.getDictionaryManager().getSettings(selectedFileContainer.getURI());
            this.saveAsUtilityClassURIs = new HashSet<>();
            for (SailfishURI suri : this.saveAsSettings.getUtilityClassURIs()) {
                this.saveAsUtilityClassURIs.add(suri);
            }

            createMessagesTree();

            this.selectedLeftField = null;
            this.selectedRightField = null;
            this.selectedFieldInMess = null;
            this.selectedFieldInAll = null;

            this.newDict = false;

            this.loadedFileContainer = this.selectedFileContainer;

            validateDictionary(true, true);
        } catch (Exception ex) {
            logger.error("Can't parse dictionary {}", selectedFileContainer.getFileName(), ex);
            BeanUtil.addErrorMessage("Error", "Can't parse dictionary " + selectedFileContainer.getFileName());
        }
    }

    public int validateDictionary(boolean load, boolean showInfo) {

        List<DictionaryValidationError> errors = this.dictionaryValidator.validate(this.dict, true, null);

        // Setting errors to models

        if (showInfo) {
            if (errors.isEmpty()) {
                BeanUtil.addInfoMessage("Info", "Dictionary has been successfully " + (load ? "loaded" : "validated"));
            } else {
                if (load) {
                    BeanUtil.addWarningMessage("Warning", "Dictionary is loaded with " + errors.size() + " errors");
                } else {
                    BeanUtil.addWarningMessage("Warning", errors.size() + " errors was found");
                }
            }
        }

        this.mainErrors = new ArrayList<>();

        for (DictionaryValidationError error : errors) {
            if (error.getMessage() == null && error.getField() == null) {
                this.mainErrors.add(error);
            }
        }

        for (FieldEditorModel model : this.dictFieldModels) {

            if (!load) {
                model.setErrors(null);
            }

            for (DictionaryValidationError error : errors) {

                if (error.getMessage() != null)
                    continue;

                if (model.getField().getName().equals(error.getField())) {
                    if (model.getErrors() == null) {
                        model.setErrors(new ArrayList<DictionaryValidationError>());
                    }
                    model.getErrors().add(error);
                }
            }
        }

        for (TreeModel<FieldEditorModel> model : this.tree.getChildren()) {

            if (!load) {
                model.getData().setErrors(null);
            }

            List<DictionaryValidationError> fieldErrors = new ArrayList<>();

            for (DictionaryValidationError error : errors) {

                if (model.getData().getField().getName().equals(error.getMessage())) {

                    if (error.getField() == null) {

                        if (model.getData().getErrors() == null) {
                            model.getData().setErrors(new ArrayList<DictionaryValidationError>());
                        }
                        model.getData().getErrors().add(error);

                    } else {

                        fieldErrors.add(error);
                    }
                }

            }

            if (!fieldErrors.isEmpty() || !load) {
                for (TreeModel<FieldEditorModel> child : model.getChildren()) {

                    if (!load) {
                        child.getData().setErrors(null);
                    }

                    if (!fieldErrors.isEmpty()) {
                        for (DictionaryValidationError error : fieldErrors) {

                            if (child.getData().getField().getName().equals(error.getField())) {
                                if (child.getData().getErrors() == null) {
                                    child.getData().setErrors(new ArrayList<DictionaryValidationError>());
                                }
                                child.getData().getErrors().add(error);
                            }
                        }
                    }
                }
            }
        }

        return errors.size();
    }

    public void createFieldEditorModel(ModifiableFieldStructure field, boolean standaloneField) {
        this.fieldEditorModel = new FieldEditorModel(field, standaloneField);
    }

    private void populateDictionaryFields(TreeModel<FieldEditorModel> model) {

        if (model.getData() != null) {
            if (StringUtils.isEmpty(model.getData().getField().getName()) && !StringUtils.isEmpty(model.getData().getField().getId())) {
                model.getData().getField().setName(model.getData().getField().getId());
            }

            String prefix = "F";
            if (model.getLevel() == 1) {
                prefix = "M";
            }

            model.getData().getField().setId(prefix + model.getIndex());

            if (StringUtils.isEmpty(model.getData().getField().getName())) {
                model.getData().getField().setName(model.getData().getField().getId());
            }
        }

        for (TreeModel<FieldEditorModel> child : model.getChildren()) {
            populateDictionaryFields(child);
        }
    }

    private void populateFields(List<ModifiableFieldStructure> list) {
        for (ModifiableFieldStructure field : list) {
            if (StringUtils.isEmpty(field.getName()) && !StringUtils.isEmpty(field.getId())) {
                field.setName(field.getId());
            }
        }
    }

    private void populateDictFieldModels(List<FieldEditorModel> dictFieldModels) {
        for (FieldEditorModel model : dictFieldModels) {

            if (StringUtils.isEmpty(model.getField().getName()) && !StringUtils.isEmpty(model.getField().getId())) {
                model.getField().setName(model.getField().getId());
            }

            model.getField().setId(model.getIndex());

            if (StringUtils.isEmpty(model.getField().getName())) {
                model.getField().setName(model.getField().getId());
            }
        }
    }

    public List<FieldEditorModel> getFieldModels() {
        return this.dictFieldModels;
    }

    private List<FieldEditorModel> fieldsToFieldEditorModels(List<ModifiableFieldStructure> fields, boolean standalone) {

        if (fields == null)
            return null;

        populateFields(fields);

        Collections.sort(fields, new FieldComparator());

        List<FieldEditorModel> models = new ArrayList<>();
        int i = 0;
        for (ModifiableFieldStructure field : fields) {
            FieldEditorModel model = new FieldEditorModel(field, standalone);
            model.setIndex("R" + String.valueOf(i++));
            models.add(model);
        }

        return models;
    }

    public List<ModifiableMessageStructure> getMessages() {
        return dict.getImplMessageStructures();
    }

    public String[] getStrJavaTypes() {

        JavaType[] all = JavaType.values();

        String[] result = new String[all.length];

        for (int i = 0; i < all.length; i++) {
            result[i] = BeanUtil.getJavaTypeLabel(all[i]);
        }

        Arrays.sort(result);

        return result;
    }

    public String getJavaTypeLabel(JavaType type) {
        return BeanUtil.getJavaTypeLabel(type);
    }

    private void removeOldErrors(List<TreeModel<FieldEditorModel>> nodes, DictionaryValidationErrorLevel level) {

        for (TreeModel<FieldEditorModel> node : nodes) {
            node.getData().removeError(level);
        }
    }

    private void removeOldErrorsFromFields(List<FieldEditorModel> models, DictionaryValidationErrorLevel level) {

        for (FieldEditorModel model : models) {
            model.removeError(level);
        }
    }

    private void setNewErrors(List<TreeModel<FieldEditorModel>> nodes, List<DictionaryValidationError> errors, boolean message) {
        for (TreeModel<FieldEditorModel> node : nodes) {

            for (DictionaryValidationError error : errors) {

                if (node.getData().getField().getName().equals(message ? error.getMessage() : error.getField())) {

                    if (node.getData().getErrors() == null) {
                        node.getData().setErrors(new ArrayList<DictionaryValidationError>());
                    }

                    node.getData().getErrors().add(error);
                }
            }
        }
    }

    private void setNewErrorsToFields(List<FieldEditorModel> fields, List<DictionaryValidationError> errors) {
        for (FieldEditorModel fieldModel : fields) {

            for (DictionaryValidationError error : errors) {

                if (fieldModel.getField().getName().equals(error.getField())) {

                    if (fieldModel.getErrors() == null) {
                        fieldModel.setErrors(new ArrayList<DictionaryValidationError>());
                    }

                    fieldModel.getErrors().add(error);
                }
            }
        }
    }

    public void removeLeftField() {

        if (this.dict == null) {
            return;
        }

        if (this.selectedLeftField.getField().isMessage()) {

            dict.removeMessageStructure(this.selectedLeftField.getField().getName());

            // Remove old errors and set a new ones

            removeOldErrors(this.tree.getChildren(), DictionaryValidationErrorLevel.DICTIONARY);

            List<DictionaryValidationError> errors = this.dictionaryValidator.validate(this.dict, false, false);

            setNewErrors(this.tree.getChildren(), errors, true);

        } else {

            ModifiableFieldStructure msg = this.selectedLeftField.getTreeModel().getParent().getData().getField();

            if (msg.isMessage()) {

                ((ModifiableMessageStructure) msg).removeField(this.selectedLeftField.getField());

                removeOldErrors(this.selectedLeftField.getTreeModel().getParent().getChildren(), DictionaryValidationErrorLevel.MESSAGE);

                List<DictionaryValidationError> errors = this.dictionaryValidator.validate(this.dict, (IMessageStructure) msg, false);

                setNewErrors(this.selectedLeftField.getTreeModel().getParent().getChildren(), errors, false);

            } else {
                ((ModifiableMessageStructure) msg.getImplReference()).removeField(this.selectedLeftField.getField());

                // Removing from reference
                TreeModel<FieldEditorModel> parent = getTreeModelMessageById(msg.getImplReference().getId());

                for (TreeModel<FieldEditorModel> child : parent.getChildren()) {
                    if (child.getData().getField().getId().equals(this.selectedLeftField.getField().getId())) {
                        child.remove();
                        break;
                    }
                }

                removeOldErrors(parent.getChildren(), DictionaryValidationErrorLevel.MESSAGE);

                List<DictionaryValidationError> errors = this.dictionaryValidator.validate(this.dict,
                        (IMessageStructure) parent.getData().getField(), false);

                setNewErrors(parent.getChildren(), errors, false);

                RequestContext.getCurrentInstance().update("tree:" + parent.getData().getIndex() + ":inside");

            }
        }

        TreeModel<FieldEditorModel> parent = this.selectedLeftField.getTreeModel().getParent();

        // If field or SubMessage
        if (parent.getData() != null) {

            List<TreeModel<FieldEditorModel>> lor = new ArrayList<>();
            createListOfReferences(lor, parent.getData().getField().getId(), this.tree);

            for (TreeModel<FieldEditorModel> ref : lor) {

                // Removing from all SubMessages...
                for (TreeModel<FieldEditorModel> child : ref.getChildren()) {
                    if (child.getData().getField().getId().equals(this.selectedLeftField.getField().getId())) {
                        child.remove();
                        break;
                    }
                }

                // ...and updating them
                RequestContext.getCurrentInstance().update("tree:" + ref.getData().getIndex() + ":inside");

                reinitializeIndexes(ref.getChildren());
                ref.getData().updateTreeModelData();
            }

        }

        // Removing all SubMessages if a first-level message was removed
        if (parent.getData() == null) {

            List<TreeModel<FieldEditorModel>> lor = new ArrayList<>();
            createListOfReferences(lor, this.selectedLeftField.getTreeModel().getData().getField().getId(), this.tree);

            for (TreeModel<FieldEditorModel> ref : lor) {
                ref.remove();
            }
        }

        this.selectedLeftField.getTreeModel().remove();

        if (parent.getData() != null) {
            
            // Updating just parent message
            parent.getData().updateTreeModelData();
            RequestContext.getCurrentInstance().update("tree:" + parent.getData().getIndex() + ":inside");

        } else {

            // Updating all tree if a first-level message was removed
            RequestContext.getCurrentInstance().update("messForm");
            RequestContext.getCurrentInstance().update("closeDictButtonPanel");
        }

        reinitializeIndexes(parent.getChildren());        

        this.selectedLeftField = null;
    }

    private void reinitializeIndexes(List<TreeModel<FieldEditorModel>> list) {
        for (TreeModel<FieldEditorModel> model : list) {
            model.getData().setIndex(model.getIndex());
            model.getData().getField().setId("F" + model.getIndex());
        }
    }

    private void reinitializeFieldIndexes(List<FieldEditorModel> list) {
        int i = 0;
        for (FieldEditorModel model : list) {
            model.setIndex("R" + i++);
            model.getField().setId(model.getIndex());
        }
    }

    public void preRemoveRightField() {

        List<TreeModel<FieldEditorModel>> references = new ArrayList<>();
        getFieldReferences(this.selectedRightField, this.tree, references, 2, true);

        StringBuilder sb = new StringBuilder();

        int size = references.size() > 3 ? 3 : references.size();

        for (int i = 0; i < size; i++) {

            sb.append("<p><strong>")
                    .append(references.get(i).getParent().getData().getField().getName())
                    .append("/")
                    .append(references.get(i).getData().getField().getName())
                    .append("</strong></p>");
        }

        if (size < references.size()) {
            sb.append("<p>")
                    .append("and ")
                    .append(references.size() - size)
                    .append(" more...</p>");
        }

        this.refCount = references.size();
        this.refString = sb.toString();

        RequestContext.getCurrentInstance().update("removeRightConfirmDialogForm");

        RequestContext.getCurrentInstance().execute("PF('removeRightConfirmDialog').show()");
    }

    public void removeRightField() {

        if (this.dict == null) {
            return;
        }

        List<TreeModel<FieldEditorModel>> references = new ArrayList<>();
        getFieldReferences(this.selectedRightField, this.tree, references, 0, true);

        for (TreeModel<FieldEditorModel> ref : references) {

            ref.getData().getField().setReference(null);
            ref.getData().getField().setJavaType(this.selectedRightField.getField().getImplJavaType());

            ref.getData().setDefaultValueInherited(false);
            ref.getData().setDefaultValueOverrides(false);
            ref.getData().setDescriptionInherited(false);
            ref.getData().setDescriptionOverrides(false);

            ref.getData().reinit();
        }

        this.dict.removeFieldStructure(this.selectedRightField.getField().getName());
        this.dictFieldModels.remove(this.selectedRightField);

        for (FieldEditorModel model : this.dictFieldModels) {
            if (model.getField().getReference() != null) {
                if (model.getField().getImplReference().getId().equals(this.selectedRightField.getField().getId())) {
                    model.getField().setReference(null);
                    model.setDefaultValueInherited(false);
                    model.setDescriptionInherited(false);
                    model.setDefaultValueOverrides(false);
                    model.setDescriptionOverrides(false);
                }
                model.reinit();

                RequestContext.getCurrentInstance().update("rightFieldEntry:" + model.getIndex().substring(1) + ":inside");
            }
        }

        removeOldErrorsFromFields(this.dictFieldModels, DictionaryValidationErrorLevel.DICTIONARY);

        List<DictionaryValidationError> errors = this.dictionaryValidator.validate(this.dict, false, true);

        setNewErrorsToFields(this.dictFieldModels, errors);

        updateFieldReferences(references, this.selectedRightField, false);

        this.selectedRightField = null;
    }

    private boolean isFieldAlreadyExists(String name, List<? extends ModifiableFieldStructure> fields) {
        for (ModifiableFieldStructure field : fields) {
            if (name.equals(field.getName())) {
                return true;
            }
        }
        return false;
    }

    public void preAddNewMessage() {
        this.newField = new ModifiableMessageStructure();
        this.cloneDialog = false;

        RequestContext.getCurrentInstance().update("newMessForm");
    }

    public void addNewMessage() {

        if (this.dict == null) {
            return;
        }

        if (StringUtils.isEmpty(this.newField.getName())) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Message name is required");
            return;
        }

        if (isFieldAlreadyExists(this.newField.getName(), getMessages())) {
            BeanUtil.addErrorMessage("Error", "Message with name \"" + this.newField.getName() + "\" already exists");
            return;
        }

        this.dict.addMessageStructure((ModifiableMessageStructure) this.newField);

        if (this.cloneDialog) {

            for (ModifiableFieldStructure field : ((ModifiableMessageStructure) this.selectedLeftField.getField()).getImplFields()) {
                ((ModifiableMessageStructure) this.newField).addField(cloneField(field));
            }
        }

        FieldEditorModel model = new FieldEditorModel(this.newField, false);
        TreeModel<FieldEditorModel> child = addChild(tree, model);
        model.setTreeModel(child);
        model.setChildCount(child.getChildCount());
        model.setLevel(child.getLevel());
        model.setIndex(child.getIndex());

        populateDictionaryFields(child);

        List<TreeModel<FieldEditorModel>> childs = new ArrayList<>();

        while (this.tree.getChildCount() > 0) {
            TreeModel<FieldEditorModel> m = this.tree.getChildren().get(0);
            childs.add(m);
            m.remove();
        }

        Collections.sort(childs, new TreeModelComparator());

        for (TreeModel<FieldEditorModel> node : childs) {
            this.tree.addChildNode(node).getData().updateTreeModelData();
        }

        this.newField = new ModifiableFieldStructure();

        if (this.cloneDialog) {
            validateDictionary(false, false);
            RequestContext.getCurrentInstance().update("fieldsForm");

            this.cloneDialog = false;

            BeanUtil.addInfoMessage("Info", "Message has been successfully cloned");
        }

        onSelectLeftField(model);

        RequestContext.getCurrentInstance().update(Arrays.asList("messForm", "newMessForm:addMessPanel", "closeDictButtonPanel"));

        RequestContext.getCurrentInstance().execute("PF('addMessDialog').hide(); scrollToLeftSelected();");
    }

    public void addNewField() {

        if (this.dict == null) {
            return;
        }

        if (StringUtils.isEmpty(this.newField.getName())) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Field name is required");
            return;
        }

        if (isFieldAlreadyExists(this.newField.getName(), this.dict.getImplFieldStructures())) {
            BeanUtil.addErrorMessage("Error", "Field with name \"" + this.newField.getName() + "\" already exists");
            return;
        }

        if (this.newField.getReference() == null && this.getNewField().getImplJavaType() == null) {
            BeanUtil.addErrorMessage("Error", "Select java type or reference for this field");
            return;
        }

        if (this.newField.getReference() instanceof String) {
            if (StringUtils.isEmpty((String) this.newField.getReference())) {

                this.newField.setReference(null);

            } else {
                this.newField.setReference(getFieldById((String) this.newField.getReference()));
                if (StringUtils.isEmpty(this.newField.getImplDefaultValue())) {
                    this.newField.setDefaultValue(this.newField.getImplReference().getImplDefaultValue());
                }
            }
        }

        ModifiableFieldStructure field = cloneField(this.newField);
        FieldEditorModel fieldModel = new FieldEditorModel(field, true);

        if (field.getImplReference() != null) {
            fieldModel.setDefaultValueInherited(true);
            if (!StringUtils.equals(field.getImplReference().getImplDefaultValue(), field.getImplDefaultValue())) {
                fieldModel.setDefaultValueOverrides(true);
            }
        }

        this.dict.addFieldStructure(field);
        Collections.sort(this.dict.getImplFieldStructures(), new FieldComparator());

        this.dictFieldModels.add(fieldModel);
        Collections.sort(this.dictFieldModels, new FieldModelComparator());

        reinitializeFieldIndexes(this.dictFieldModels);

        onSelectRightField(fieldModel);

        this.newField = new ModifiableFieldStructure();

        this.cloneDialog = false;

        RequestContext.getCurrentInstance().update("fieldsForm");
        RequestContext.getCurrentInstance().update("newFieldForm:addFieldPanel");
        RequestContext.getCurrentInstance().update("fieldMoveButtonPanel");

        RequestContext.getCurrentInstance().execute("PF('addFieldDialog').hide()");
    }

    private ModifiableFieldStructure cloneField(ModifiableFieldStructure field) {

        ModifiableFieldStructure result;

        if (field.isMessage()) {

            result = new ModifiableMessageStructure();

        } else if (field.isSubMessage()) {

            result = new ModifiableMessageStructure();
            result.setReference(field.getReference());
            result.setRequired(field.isRequired());
            result.setCollection(field.isCollection());

        } else {

            result = new ModifiableFieldStructure();
            result.setDefaultValue(field.getImplDefaultValue());
            result.setRequired(field.isRequired());
            result.setCollection(field.isCollection());
            result.setJavaType(field.getImplJavaType());
            result.setReference(field.getReference());

            if (!field.getImplValues().isEmpty()) {
                result.addValues(field.getImplValues());
            }
        }

        result.setDescription(field.getDescription());
        result.setId(field.getId());
        result.setName(field.getName());

        if (!field.getImplAttributes().isEmpty()) {
            result.addAttributes(field.getImplAttributes());
        }

        return result;
    }

    public void preAddingAttribute(FieldEditorModel model) {
        this.fieldEditorModel = model;
    }

    private ModifiableMessageStructure getParentMsg(FieldEditorModel model) {
        ModifiableMessageStructure parentMsg = null;
        if (!model.isStandaloneField()) {
            if (model.getField().isMessage()) {
                parentMsg = (ModifiableMessageStructure) model.getField();
            } else {
                parentMsg = (ModifiableMessageStructure) model.getTreeModel().getParent().getData().getField();
            }
            if (parentMsg.getReference() != null) {
                parentMsg = (ModifiableMessageStructure) parentMsg.getReference();
            }
        }
        return parentMsg;
    }

    public void deleteCurrentAttribute() {

        this.modelForAttrRemoving.delete(this.attrModelForRemoving, this.attrForRemoving);

        List<DictionaryValidationError> errors = this.dictionaryValidator.validate(
                getParentMsg(this.modelForAttrRemoving), this.modelForAttrRemoving.getField());

        this.modelForAttrRemoving.setErrors(errors.isEmpty() ? null : errors);

        if (!this.modelForAttrRemoving.isStandaloneField()) {
            updateReferences(this.modelForAttrRemoving, true);
        } else {
            updateRightFieldReferences(this.modelForAttrRemoving, true);
            updateFieldReferences(null, this.modelForAttrRemoving, true);
        }

        String updateIndex = "tree:" + this.modelForAttrRemoving.getIndex();

        if (this.modelForAttrRemoving.isStandaloneField()) {
            updateIndex = "rightFieldEntry:" + this.modelForAttrRemoving.getIndex().substring(1);
        }

        RequestContext.getCurrentInstance().update(updateIndex + ":fieldInfo");

        if (this.attrForRemoving) {
            RequestContext.getCurrentInstance().update(updateIndex + ":atrs");
        } else {
            RequestContext.getCurrentInstance().update(updateIndex + ":baseAtrs");
            RequestContext.getCurrentInstance().update(updateIndex + ":propDefaultPanel");
        }
    }

    public void deleteAttribute(FieldEditorModel model, AttributeModel toDelete, boolean attribute, boolean confirm) {
        this.modelForAttrRemoving = model;
        this.attrModelForRemoving = toDelete;
        this.attrForRemoving = attribute;
        if (confirm) {
            RequestContext.getCurrentInstance().update("removeAttributeConfirmDialogForm");
            RequestContext.getCurrentInstance().execute("PF('removeAttributeConfirmDialog').show()");
        } else {
            deleteCurrentAttribute();  // do not show confirm dialog in complex removal
        }
    }

    public AttributeModel getAttrModelForRemoving() {
        return this.attrModelForRemoving;
    }

    public boolean isAttrForRemoving() {
        return this.attrForRemoving;
    }

    public void addNewAttribute() {

        if (this.dict == null || this.fieldEditorModel == null) {
            return;
        }

        this.newAttribute.setName(attrNameReplace(this.newAttribute.getName()));

        try {
            this.newAttribute.setCastValue(StructureUtils.castValueToJavaType(this.newAttribute.getValue(), this.newAttribute.getType()));
        } catch (Throwable e) {
            this.newAttribute.setCastValue(null);
        }

        if (attributeMode == 0) {

            // Add attribute to message/field
            this.fieldEditorModel.getField().addAttribute(this.newAttribute);

        } else if (attributeMode == 1) {

            // Add value to field
            this.fieldEditorModel.getField().addValue(this.newAttribute);
        }

        this.newAttribute = new ModifiableAttributeStructure();

        if (this.fieldEditorModel != null) {
            this.fieldEditorModel.reinit();
        }

        List<DictionaryValidationError> errors = this.dictionaryValidator.validate(
                getParentMsg(this.fieldEditorModel), this.fieldEditorModel.getField());

        this.fieldEditorModel.setErrors(errors.isEmpty() ? null : errors);
        if (!this.fieldEditorModel.isStandaloneField()) {
            RequestContext.getCurrentInstance().update("tree:" + this.fieldEditorModel.getIndex() + ":atrs");
            RequestContext.getCurrentInstance().update("tree:" + this.fieldEditorModel.getIndex() + ":fieldInfo");
            updateReferences(this.fieldEditorModel, true);
        } else {
            RequestContext.getCurrentInstance().update("rightFieldEntry:" + this.fieldEditorModel.getIndex().substring(1) + ":inside");
            updateRightFieldReferences(this.fieldEditorModel, true);
            updateFieldReferences(null, this.fieldEditorModel, true);
        }
    }

    public void addAttrToComplexUpdate() {
        complexAttrsUpdateModel.getNewAttributes().add(newAttribute);
        newAttribute = new ModifiableAttributeStructure();
    }

    private boolean isAttributeChangeMade(AttributeModel attr) {
        return !(Objects.equals(attr.getActual().getName(), attr.getOriginal().getName()) &&
                Objects.equals(attr.getActual().getValue(), attr.getOriginal().getValue()) &&
                Objects.equals(attr.getActual().getType(), attr.getOriginal().getType()));
    }

    public List<String> completeFieldName(String name) {
        name = name.toLowerCase();
        List<String> result = new ArrayList<>();
        for (FieldEditorModel field : this.dictFieldModels) {
            String fieldName = field.getField().getName();
            if (fieldName.toLowerCase().contains(name)) {
                result.add(fieldName);
            }
        }

        return result;
    }

    public List<String> completeMsgName(String name) {
        name = name.toLowerCase();
        List<String> result = new ArrayList<>();

        for (TreeModel<FieldEditorModel> message : this.tree.getChildren()) {
            String msgName = message.getData().getField().getName();
            if (msgName.toLowerCase().contains(name)) {
                result.add(msgName);
            }
        }
        return result;
    }

    public List<String> completeMsgFieldName(String name) {
        name = name.toLowerCase();
        List<String> result = new ArrayList<>();

        for (TreeModel<FieldEditorModel> message : this.tree.getChildren()) {
            for (TreeModel<FieldEditorModel> msgFieldModel : message.getChildren()) {
                String msgFieldName = msgFieldModel.getData().getField().getName();
                if (msgFieldName.toLowerCase().contains(name)) {
                    result.add(msgFieldName);
                }
            }
        }

        return result;
    }

    private void fillAttributeStructure(ModifiableAttributeStructure original, ModifiableAttributeStructure actual, boolean castName) {

        if (castName) {
            original.setName(attrNameReplace(actual.getName()));
        }

        original.setValue(actual.getValue());

        try {
            original.setCastValue(StructureUtils.castValueToJavaType(actual.getValue(), actual.getType()));
        } catch (Throwable e) {
            original.setCastValue(null);
        }

        original.setType(actual.getType());
        actual = AttributeModel.cloneAttribute(original);

    }

    private String attrNameReplace(String attrName) {
        return attrName.replace(' ', '_').replace('-', '_');
    }

    public void onAttributeChange(AttributeModel attr, FieldEditorModel model) {

        if (!isAttributeChangeMade(attr)) {
            return;
        }

        if (attr.isInheritedAttribute()) {

            if (attr.isOverrides()) {

                fillAttributeStructure(attr.getOriginal(), attr.getActual(), false);

            } else {

                ModifiableAttributeStructure newAttribute = new ModifiableAttributeStructure();

                fillAttributeStructure(newAttribute, attr.getActual(), true);

                if (attr.isAttribute()) {
                    attr.getOwner().addAttribute(newAttribute);
                } else {
                    attr.getOwner().addValue(newAttribute);
                }

                attr.setOriginal(newAttribute);
                attr.setActual(AttributeModel.cloneAttribute(newAttribute));
                attr.setOverrides(true);
            }

        } else {

            fillAttributeStructure(attr.getOriginal(), attr.getActual(), true);
        }

        if (null != model) {

            model.reinit();

            List<DictionaryValidationError> errors = this.dictionaryValidator.validate(getParentMsg(model), model.getField());

            model.setErrors(errors.isEmpty() ? null : errors);

            if (!model.isStandaloneField()) {

                RequestContext.getCurrentInstance().update("tree:" + model.getIndex() + ":fieldInfo");
                RequestContext.getCurrentInstance().update("tree:" + model.getIndex() + ":atrs");

            } else {

                String index = model.getIndex().substring(1);

                RequestContext.getCurrentInstance().update("rightFieldEntry:" + index + ":fieldInfo");

                if (attr.isAttribute()) {
                    RequestContext.getCurrentInstance().update("rightFieldEntry:" + index + ":atrs");
                } else {
                    RequestContext.getCurrentInstance().update("rightFieldEntry:" + index + ":baseAtrs");
                    RequestContext.getCurrentInstance().update("rightFieldEntry:" + index + ":propDefaultPanel");
                }

                updateRightFieldReferences(model, true);
                updateFieldReferences(null, model, true);
            }
        }
    }

    public List<ModifiableAttributeStructure> getAttributes() {

        if (this.selectedLeftField == null) {
            return new ArrayList<>();
        }

        return this.fieldEditorModel.getField().getImplAttributes();
    }

    public List<ModifiableFieldStructure> getSelectedMessageFields() {

        if (this.selectedLeftField == null || !(this.selectedLeftField.getField().isMessage())) {
            return new ArrayList<>();
        }

        return ((ModifiableMessageStructure) this.selectedLeftField.getField()).getImplFields();
    }

    public void doAddFromPage() {
        this.movingFromRight = false;
        this.newField = new ModifiableFieldStructure();
        RequestContext.getCurrentInstance().update("addFieldToMessForm");
    }

    public void doLeftClone() {

        if (this.selectedLeftField == null)
            return;

        if (this.selectedLeftField.getField().isMessage()) {

            this.newField = cloneField(this.selectedLeftField.getField());

            this.cloneDialog = true;

            RequestContext.getCurrentInstance().update("newMessForm");

            RequestContext.getCurrentInstance().execute("PF('addMessDialog').show()");
        }
    }

    public void doRightClone() {

        if (this.selectedRightField == null) {
            return;
        }

        this.newField = cloneField(this.selectedRightField.getField());

        this.cloneDialog = true;

        RequestContext.getCurrentInstance().update("newFieldForm");

    }

    public void doMoveFieldToMsg() {

        if (this.selectedRightField == null || this.selectedLeftField == null || !this.selectedLeftField.isMessage()) {
            return;
        }

        this.newField = new ModifiableFieldStructure();
        this.newField.setReference(this.selectedRightField.getField());
        this.newField.setName(this.selectedRightField.getField().getName());
        this.newField.setJavaType(getRefFieldType(this.selectedRightField.getField()));
        this.newType = "";

        this.movingFromRight = true;

        RequestContext.getCurrentInstance().update("addFieldToMessForm");
    }

    public void doMoveFieldToMsgProcess() {

        if (!this.movingFromRight) {
            doAdd();
            return;
        }

        FieldEditorModel addedModel = doAdd();

        if (addedModel == null) {
            return;
        }

        RequestContext.getCurrentInstance().update("tree:" + this.selectedLeftField.getIndex() + ":inside");

        if (this.selectedLeftField.getLevel() == 1) {

            reinitializeIndexes(this.selectedLeftField.getTreeModel().getChildren());

            List<TreeModel<FieldEditorModel>> lor = new ArrayList<>();

            createListOfReferences(lor, this.selectedLeftField.getTreeModel().getData().getField().getId(), this.tree);

            for (TreeModel<FieldEditorModel> ref : lor) {
                RequestContext.getCurrentInstance().update("tree:" + ref.getData().getIndex() + ":inside");
            }
        } else {

            TreeModel<FieldEditorModel> parent = getTreeModelMessageById(this.selectedLeftField.getField().getImplReference().getId());

            RequestContext.getCurrentInstance().update("tree:" + parent.getData().getIndex() + ":inside");
        }

        RequestContext.getCurrentInstance().execute("PF('addFieldToMessDialog').hide()");
    }

    private boolean refCheckNullType(ModifiableFieldStructure field) {
        if (field.getImplJavaType() != null) {
            return false;
        }
        if (field.getReference() == null) {
            return true;
        } else {
            if (field.getReference() instanceof String) {

                ModifiableFieldStructure reference = getFieldById((String) this.newField.getReference());
                if (reference != null) {
                    return refCheckNullType(reference);
                }
                return true;

            } else {
                return refCheckNullType(field.getImplReference());
            }
        }
    }

    // Add Field to Message
    public FieldEditorModel doAdd() {

        if (this.selectedLeftField == null) {
            return null;
        }

        if (StringUtils.isEmpty(this.newField.getName())) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Field name is required");
            return null;
        }

        if ((this.selectedLeftField.getField().isMessage() &&
                isMessageHasField((ModifiableMessageStructure) this.selectedLeftField.getField(), this.newField.getName())) ||
                (this.selectedLeftField.getField().getReference() != null &&
                        isMessageHasField((ModifiableMessageStructure) this.selectedLeftField.getField().getReference(), this.newField.getName()))) {

            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Field with specified name already exists in message");
            return null;
        }

        if (!"Message".equals(this.newType) && refCheckNullType(this.newField)) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Java type is required");
            return null;
        }

        if ("Message".equals(this.newType)) {
            ModifiableMessageStructure newMsg = new ModifiableMessageStructure();
            newMsg.setName(this.newField.getName());
            newMsg.setReference(this.newField.getReference());
            newMsg.setRequired(this.newField.isRequired());
            newMsg.setCollection(this.newField.isCollection());
            this.newField = newMsg;
        }

        boolean newMessageAdded = false;

        if (this.newField.getReference() instanceof String) {
            if (StringUtils.isEmpty((String) this.newField.getReference())) {

                this.newField.setReference(null);

            } else if (!"Message".equals(this.newType)) {

                this.newField.setReference(getFieldById((String) this.newField.getReference()));
                if (StringUtils.isEmpty(this.newField.getImplDefaultValue())) {
                    this.newField.setDefaultValue(this.newField.getImplReference().getImplDefaultValue());
                }

            } else {

                ModifiableFieldStructure foundMessage = getMessageById(getMessageNames().get(this.newField.getReference()));

                if (foundMessage != null) {

                    this.newField.setReference(foundMessage);

                } else {

                    ModifiableMessageStructure newMessage = new ModifiableMessageStructure();
                    newMessage.setName((String) this.newField.getReference());

                    FieldEditorModel newMsgModel = new FieldEditorModel(newMessage, false);

                    TreeModel<FieldEditorModel> newMsgNode = this.tree.addChild(newMsgModel);
                    newMsgModel.setTreeModel(newMsgNode);
                    newMsgModel.setLevel(newMsgNode.getLevel());
                    newMsgModel.setIndex(newMsgNode.getIndex());
                    newMessage.setId("M" + newMsgNode.getIndex());

                    this.newField.setReference(newMessage);

                    newMessageAdded = true;
                }
            }
        }

        if (this.newField.getReference() != null && StringUtils.equals(this.selectedLeftField.getField().getId(),
                this.newField.getImplReference().getId())) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Recoursive message detected.");
            return null;
        }

        if (this.newField.getReference() != null && existsInTree(this.selectedLeftField.getField(), this.newField.getImplReference())) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Recoursive message detected.");
            return null;
        }

        ModifiableFieldStructure newField = cloneField(this.newField);
        int insertPosition = Math.max(0, Math.min(position, selectedLeftField.getChildCount()));

        if (this.selectedLeftField.getField().isMessage()) {
            ((ModifiableMessageStructure) this.selectedLeftField.getField()).addField(insertPosition, newField);
        } else {
            ((ModifiableMessageStructure) this.selectedLeftField.getField().getReference()).addField(insertPosition,
                                                                                                     newField);
        }

        FieldEditorModel model = new FieldEditorModel(newField, false);

        if (!"Message".equals(this.newType)) {
            if (newField.getImplReference() != null) {
                model.setDefaultValueInherited(true);
                if (!StringUtils.equals(newField.getImplReference().getImplDefaultValue(), newField.getImplDefaultValue())) {
                    model.setDefaultValueOverrides(true);
                }
            }
        }

        if (insertPosition == selectedLeftField.getChildCount()) {
            model.setTreeModel(this.selectedLeftField.getTreeModel().addChild(model));
            model.updateTreeModelData();

        } else { // there is no possibility to add node by index, get old children and rebuild tree
            TreeModel<FieldEditorModel> parentModel = this.selectedLeftField.getTreeModel();
            List<TreeModel<FieldEditorModel>> children = new ArrayList<>(parentModel.getChildren());

            // remove items in a loop, because TreeModel.getChildren() return unmodifiable list
            while (parentModel.getChildCount() > 0) {
                parentModel.getChildren().get(0).remove();
            }

            TreeModel<FieldEditorModel> newFieldModel = new ListTreeModel<>();
            newFieldModel.setData(model);
            model.setTreeModel(newFieldModel);
            children.add(insertPosition, newFieldModel);

            for (TreeModel<FieldEditorModel> child : children) {
                parentModel.addChildNode(child).getData().updateTreeModelData();
            }
        }

        if ("Message".equals(this.newType) && newField.getReference() != null) {

            copyNodeToSubMessage(newField, model);

            if (model.getTreeModel().getChildCount() > 0) {

                List<TreeModel<FieldEditorModel>> lor = new ArrayList<>();
                createListOfReferences(lor, model.getTreeModel().getData().getField().getId(), this.tree);

                for (TreeModel<FieldEditorModel> ref : lor) {
                    if (ref != model.getTreeModel()) {
                        copyNodeToSubMessage(newField, addFieldToTreeModel(newField, ref, model));
                    }
                }
            }

            if (model.getLevel() > 2) {
                TreeModel<FieldEditorModel> parent = getTreeModelMessageById(
                        model.getTreeModel().getParent().getData().getField().getImplReference().getId());

                copyNodeToSubMessage(newField, addFieldToTreeModel(newField, parent, model));

                parent.getData().setChildCount(parent.getChildCount());
            }

        } else {

            List<TreeModel<FieldEditorModel>> lor = new ArrayList<>();
            createListOfReferences(lor, model.getTreeModel().getParent().getData().getField().getId(), this.tree);

            for (TreeModel<FieldEditorModel> ref : lor) {
                if (ref != model.getTreeModel().getParent()) {
                    addFieldToTreeModel(newField, ref, model);
                }
            }

            if (model.getLevel() > 2) {
                TreeModel<FieldEditorModel> parent = getTreeModelMessageById(
                        model.getTreeModel().getParent().getData().getField().getImplReference().getId());
                addFieldToTreeModel(newField, parent, model);
            }
        }

        model.getTreeModel().getParent().getData().setChildCount(model.getTreeModel().getParent().getChildCount());

        this.newField = new ModifiableFieldStructure();
        this.newType = "";

        if (!newMessageAdded) {
            updateReferences(model, false);
            RequestContext.getCurrentInstance().update("tree:" + model.getTreeModel().getParent().getIndex() + ":inside");
        } else {
            RequestContext.getCurrentInstance().update("messForm");
        }

        RequestContext.getCurrentInstance().execute("PF('addFieldToMessDialog').hide()");

        position++; //Increment next position after addition

        return model;
    }

    public String getRefName(ModifiableFieldStructure field) {
        if (field == null)
            return null;
        return field.getName();
    }

    public JavaType getRefFieldType(Object field) {

        if (field == null)
            return null;

        List<JavaType> types = new ArrayList<>();
        createRefFieldTypeList(types, field);

        for (int i = types.size() - 1; i >= 0; i--) {
            if (types.get(i) != null)
                return types.get(i);
        }

        return null;
    }

    public String getRefFieldTypeLabel(Object field) {
        return BeanUtil.getJavaTypeLabel(getRefFieldType(field));
    }

    private void createRefFieldTypeList(List<JavaType> list, Object field) {

        if (field == null)
            return;

        if (field instanceof String) {
            ModifiableFieldStructure found = getFieldById((String) field);
            if (found == null)
                return;
            list.add(found.getImplJavaType());
        } else {
            list.add(((ModifiableFieldStructure) field).getImplJavaType());

            if (((ModifiableFieldStructure) field).getReference() != null) {
                createRefFieldTypeList(list, ((ModifiableFieldStructure) field).getReference());
            }
        }
    }

    public boolean isRefFieldTypeRendered(Object field) {

        if (((ModifiableFieldStructure) field).getReference() == null) {
            return true;

        } else if (getRefFieldType(((ModifiableFieldStructure) field).getReference()) == null) {
            return true;
        }

        return false;
    }

    private FieldEditorModel addFieldToTreeModel(ModifiableFieldStructure field, TreeModel<FieldEditorModel> to, FieldEditorModel from) {
        FieldEditorModel m = new FieldEditorModel(field, false);
        m.setTreeModel(to.addChild(m));
        m.setLevel(m.getTreeModel().getLevel());
        m.setIndex(m.getTreeModel().getIndex());
        m.setAttributeModelsWrapper(from.getAttributeModelsWrapper());
        m.setValueModelsWrapper(from.getValueModelsWrapper());
        to.getData().setChildCount(to.getChildCount());
        return m;
    }

    private void copyNodeToSubMessage(ModifiableFieldStructure field, FieldEditorModel parentModel) {

        TreeModel<FieldEditorModel> foundModel = getTreeModelMessageById(field.getImplReference().getId());

        if (foundModel != null) {
            for (TreeModel<FieldEditorModel> childNode : foundModel.getChildren()) {

                FieldEditorModel fm = childNode.getData();

                // Create unique model with a referenced field object

                FieldEditorModel newModel = addCopyOfNode(parentModel.getTreeModel(), fm);

                if (newModel.isMessage()) {
                    copyNodeToSubMessage(newModel.getField(), newModel);
                }
            }

            parentModel.setChildCount(foundModel.getChildCount());
        }
    }

    private FieldEditorModel addCopyOfNode(TreeModel<FieldEditorModel> to, FieldEditorModel from) {

        FieldEditorModel newModel = new FieldEditorModel(from.getField(), from.isStandaloneField());

        newModel.setAttributeModelsWrapper(from.getAttributeModelsWrapper());
        newModel.setValueModelsWrapper(from.getValueModelsWrapper());

        TreeModel<FieldEditorModel> newNode = to.addChild(newModel);
        newModel.setTreeModel(newNode);
        to.getData().setChildCount(to.getChildCount());

        newModel.setLevel(newNode.getLevel());
        newModel.setIndex(newNode.getIndex());

        return newModel;
    }

    private TreeModel<FieldEditorModel> getTreeModelMessageById(String id) {

        for (TreeModel<FieldEditorModel> child : this.tree.getChildren()) {
            if (child.getData().getField().getId().equals(id)) {
                return child;
            }
        }

        return null;
    }

    public void doUp() {
        switchFields(false);
    }

    public void doDown() {
        switchFields(true);
    }

    private void switchFields(boolean down) {
        if (selectedLeftField == null || selectedLeftField.getField().isMessage()) {
            return;
        }

        TreeModel<FieldEditorModel> parent = this.selectedLeftField.getTreeModel().getParent();

        List<TreeModel<FieldEditorModel>> childs = parent.getChildren();

        int curIndex = -1;

        for (int i = 0; i < childs.size(); i++) {
            TreeModel<FieldEditorModel> model = childs.get(i);
            if (model.getData().equals(this.selectedLeftField)) {
                curIndex = i;
                break;
            }
        }

        if ((down && curIndex != -1 && curIndex != childs.size() - 1) || (!down && curIndex > 0)) {

            FieldEditorModel msgModel = parent.getData();

            List<ModifiableFieldStructure> fields;

            if (msgModel.getField().isMessage()) {

                fields = ((ModifiableMessageStructure) msgModel.getField()).getImplFields();

            } else {

                TreeModel<FieldEditorModel> refNode = getTreeModelMessageById(msgModel.getField().getImplReference().getId());

                switchTreeModels(refNode, curIndex, down);

                if (!refNode.getData().isCollapsed()) {
                    RequestContext.getCurrentInstance().update("tree:" + refNode.getData().getIndex() + ":inside");
                }

                fields = ((ModifiableMessageStructure) msgModel.getField().getReference()).getImplFields();
            }

            // Switch nodes in sub messages
            List<TreeModel<FieldEditorModel>> lor = new ArrayList<>();

            createListOfReferences(lor, msgModel.getTreeModel().getData().getField().getId(), this.tree);

            for (TreeModel<FieldEditorModel> ref : lor) {
                if (ref != parent) {
                    switchTreeModels(ref, curIndex, down);

                    if (!ref.getData().isCollapsed()) {
                        RequestContext.getCurrentInstance().update("tree:" + ref.getData().getIndex() + ":inside");
                    }
                }
            }

            int index = fields.indexOf(this.selectedLeftField.getField());

            ModifiableFieldStructure toMove = fields.remove(index);
            fields.add(index + (down ? 1 : -1), toMove);

            switchTreeModels(parent, curIndex, down);
        }
    }

    private void switchTreeModels(TreeModel<FieldEditorModel> parent, int curIndex, boolean down) {

        List<TreeModel<FieldEditorModel>> childs = parent.getChildren();
        List<TreeModel<FieldEditorModel>> newChilds = new ArrayList<>();

        TreeModel<FieldEditorModel> curNode = childs.get(curIndex);

        int i = 0;
        while (i < childs.size()) {
            TreeModel<FieldEditorModel> model = childs.get(i);
            if (!(model.getData().getField().getId().equals(this.selectedLeftField.getField().getId()))) {
                newChilds.add(model);
            }
            model.remove();
        }

        newChilds.add(curIndex + (down ? 1 : -1), curNode);

        for (TreeModel<FieldEditorModel> newModel : newChilds) {
            parent.addChildNode(newModel).getData().updateTreeModelData();
        }
    }

    public void preNewFieldAdding() {
        this.newField = new ModifiableFieldStructure();
    }

    public void setNewReference(Object newReference) {
        this.newField.setReference(getFieldById((String) newReference));
    }

    public Map<String, String> getFieldNames() {
        return this.dict != null ? getFMMap(dict.getImplFieldStructures()) : null;
    }

    public Map<String, String> getMessageNames() {
        return this.dict != null ? getFMMap(dict.getImplMessageStructures()) : null;
    }

    private Map<String, String> getFMMap(List<? extends ModifiableFieldStructure> fields) {
        Map<String, String> result = new TreeMap<>();

        for (ModifiableFieldStructure field : fields) {
            result.put(field.getName(), field.getId());
        }

        return result;
    }

    public StreamedContent getDownloadFiles() {

        try {
            return getNewFile();
        } catch (IOException | RuntimeException e) {
            logger.error("Can not pack current dictionary.", e);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error",
                                 "can not pack current dictionary." + e.getMessage());
            return null;
        }

    }

    private StreamedContent getNewFile() throws IOException {

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XmlDictionaryStructureWriter.write(this.dict, output);

            byte[] bytes = output.toByteArray();

            InputStream input = new ByteArrayInputStream(bytes);

            return new DefaultStreamedContent(input, "application/force-download", selectedFileContainer.getFileName());
        }
    }

    public List<SailfishURI> getCachedDictURIs() {
        return context.getDictionaryManager().getCachedDictURIs();
    }

    public void dropCache() {

        if (getCachedDictURIs().isEmpty()) {
            BeanUtil.addInfoMessage("Info", "The cache is empty already");
            return;
        }

        context.getDictionaryManager().invalidateDictionaries();
        BeanUtil.addInfoMessage("Info", "All cached dictionaries has been successfully droped");
    }

    public void dropCacheByURI(SailfishURI dictURI){
        context.getDictionaryManager().invalidateDictionaries(dictURI);
        BeanUtil.addInfoMessage("Info", "Dictionary " + dictURI + " has been successfully droped");
    }
    public void saveDictionary() {

        if (dict == null) {
            BeanUtil.addErrorMessage("ERROR", "Can not write dictionary to file.");
            return;
        }

        Map<String, String> defaultValuesCleaned = preSaveCleanNotOverridingDefaultValues();

        try {

            XmlDictionaryStructureWriter.write(this.dict, context.getWorkspaceDispatcher().createFile(
                    FolderType.ROOT, true, selectedFileContainer.getFileName()));

            BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info", "Dictionary has been saved");
            context.getDictionaryManager().invalidateDictionaries(selectedFileContainer.getURI());

            RequestContext.getCurrentInstance().update("mainDictForm:dropCacheOverlayPanel");

        } catch (Exception ex) {
            logger.error("Can not write dictionary to file.", ex);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Can not write dictionary to file. " + ex.getMessage());
        } finally {
            afterSaveReturnCleanedDefaultValues(defaultValuesCleaned);
        }
    }

    public void preSaveClick(boolean saveAs) {

        int count = validateDictionary(false, false);

        if (count > 0) {
            BeanUtil.addWarningMessage("Couldn't save dictionary", count + " errors was found");
        }
        RequestContext.getCurrentInstance().update("messForm");
        RequestContext.getCurrentInstance().update("fieldsForm");

        if (count == 0) {

            if (saveAs) {

                RequestContext.getCurrentInstance().update("saveAsForm");
                RequestContext.getCurrentInstance().execute("PF('saveAsDialog').show()");

            } else {

                RequestContext.getCurrentInstance().execute("PF('dictSaveConfirmDialog').show()");

            }
        }
    }

    private Map<String, String> preSaveCleanNotOverridingDefaultValues() {

        Map<String, String> result = new HashMap<>();

        for (FieldEditorModel field : this.dictFieldModels) {
            if (checkDefaultValueEqualsWhileCleaning(field, result)) {
                result.put(field.getField().getId(), field.getField().getImplDefaultValue());
                field.getField().setDefaultValue(null);
            }
        }

        cleanDefaultValuesInTree(this.tree, result);

        return result;
    }

    private void cleanDefaultValuesInTree(TreeModel<FieldEditorModel> node, Map<String, String> result) {

        if (node.getData() != null && !node.getData().isMessage()) {
            FieldEditorModel field = node.getData();
            if (checkDefaultValueEqualsWhileCleaning(field, result)) {
                result.put(field.getField().getId(), field.getField().getImplDefaultValue());
                field.getField().setDefaultValue(null);
            }
        }

        for (TreeModel<FieldEditorModel> child : node.getChildren()) {
            cleanDefaultValuesInTree(child, result);
        }
    }

    private boolean checkDefaultValueEqualsWhileCleaning(FieldEditorModel field, Map<String, String> cleaned) {
        return field.getField().getDefaultValue() != null && field.getField().getImplReference() != null
                && StringUtils.equals(field.getField().getImplDefaultValue(), cleaned.containsKey(field.getField().getImplReference().getId())
                        ? cleaned.get(field.getField().getImplReference().getId()) : field.getField().getImplReference().getImplDefaultValue());
    }

    private void afterSaveReturnCleanedDefaultValues(Map<String, String> cleaned) {

        for (FieldEditorModel field : this.dictFieldModels) {
            if (cleaned.containsKey(field.getField().getId())) {
                field.getField().setDefaultValue(cleaned.get(field.getField().getId()));
            }
        }

        returnDefaultValuesInTree(this.tree, cleaned);
    }

    private void returnDefaultValuesInTree(TreeModel<FieldEditorModel> node, Map<String, String> cleaned) {

        if (node.getData() != null && !node.getData().isMessage()) {
            FieldEditorModel field = node.getData();
            if (cleaned.containsKey(field.getField().getId())) {
                field.getField().setDefaultValue(cleaned.get(field.getField().getId()));
            }
        }

        for (TreeModel<FieldEditorModel> child : node.getChildren()) {
            returnDefaultValuesInTree(child, cleaned);
        }
    }

    public void saveDictionaryAs() {

        if (this.dict == null) {
            BeanUtil.addErrorMessage("Error", "Can not write dictionary to file.");
            return;
        }

        if (this.saveAsContainer.getURI() == null) {
            BeanUtil.addErrorMessage("URI is required", "");
            return;
        }

        if (StringUtils.isEmpty(this.saveAsContainer.getNamespace())) {
            BeanUtil.addErrorMessage("Namespace is required", "");
            return;
        }

        if (!this.overwriteFile && context.getDictionaryManager().getDictionaryURIs().contains(this.saveAsContainer.getURI())) {
            BeanUtil.addErrorMessage("Error", "Dictionary with name " + saveAsContainer.getURI() + " already exists");
            RequestContext.getCurrentInstance().addCallbackParam("validationFailed", true);
            return;
        }

        Map<String, String> defaultValuesCleaned = preSaveCleanNotOverridingDefaultValues();

        try {
            IDictionaryRegistrator registrator = getDictionaryFile(this.saveAsContainer.getURI().getResourceName(), this.overwriteFile);

            if (registrator == null) {
                return;
            }

            this.dict.setNamespace(this.saveAsContainer.getNamespace());
            this.saveAsSettings = new DictionarySettings(this.saveAsSettings);
            this.saveAsSettings.setURI(this.saveAsContainer.getURI());
            for (SailfishURI suri : saveAsUtilityClassURIs) {
                this.saveAsSettings.addUtilityClassURI(suri);
            }

            XmlDictionaryStructureWriter.write(this.dict, context.getWorkspaceDispatcher().getFile(FolderType.ROOT, registrator.getPath()));

            SailfishURI dictionarySURI = registrator.setFactoryClass(this.saveAsSettings.getFactoryClass())
                .addUtilityClassURI(this.saveAsSettings.getUtilityClassURIs())
                .registrate();

            DictFileContainer container = new DictFileContainer(dictionarySURI, registrator.getPath(), dict.getNamespace());
            this.selectedFileContainer = container;

            if (!files.contains(container)) {
                files.add(container);
            }
            Collections.sort(files, new DictFileContainerComparator());

            BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info",
                    "Dictionary " + this.saveAsContainer.getURI() + " saved as ");

            this.saveAsContainer.setURI(null);

            this.newDict = false;

            RequestContext.getCurrentInstance().execute("PF('saveAsDialog').hide();");

        } catch (Exception ex) {
            logger.error("Can not write dictionary to file.", ex);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error",
                    "Can not write dictionary " + this.saveAsContainer.getURI() + " to file. " + ex.getMessage());
        } finally {
            afterSaveReturnCleanedDefaultValues(defaultValuesCleaned);
        }
    }

    public boolean handleFileUpload(UploadedFile uploadFile) {

        ModifiableXmlDictionaryStructureLoader loader = new ModifiableXmlDictionaryStructureLoader();

        try {

            try (InputStream in = uploadFile.getInputstream()) {
                this.uploadDict = loader.load(in);
            }

            populateDictFieldModels(fieldsToFieldEditorModels(this.uploadDict.getImplFieldStructures(), true));

            return true;
        } catch (Exception ex) {
            logger.error("Can't parse uploaded dictionary", ex);
            BeanUtil.addErrorMessage("Error", "Can't parse uploaded dictionary");
            this.uploadDict = null;
            return false;
        }
    }

    public void preUploadClick() {

        this.uploadContainer = new DictFileContainer(null, null, null);
        this.uploadUtilityClassURIs = new HashSet<>();

        RequestContext.getCurrentInstance().update("filenameForm:filenamePanel");
        RequestContext.getCurrentInstance().execute("PF('uploadDialog').show()");
    }

    public void uploadDialogClose() {
        this.uploadContainer.setURI(null);
        this.uploadContainer.setNamespace(null);
        this.uploadUtilityClassURIs = new HashSet<>();
    }

    public void handleFileUploadProcess(UploadedFile uploadFile) {

        if (uploadFile == null) {
            BeanUtil.addErrorMessage("File didn't upload", "");
            return;
        }

        if (this.uploadContainer.getURI() == null) {
            BeanUtil.addErrorMessage("URI is required", "");
            return;
        }

        if (!this.overwriteFile && context.getDictionaryManager().getDictionaryURIs().contains(uploadContainer.getURI())) {
            BeanUtil.addErrorMessage("Error", "Dictionary with name " + uploadContainer.getURI() + " already exists");
            RequestContext.getCurrentInstance().addCallbackParam("validationFailed", true);
            return;
        }

        this.dict = this.uploadDict;
        this.uploadDict = null;

        try {
            IDictionaryRegistrator registrator = getDictionaryFile(this.uploadContainer.getURI().getResourceName(), overwriteFile);

            if (registrator == null) {
                return;
            }

            this.uploadSettings.setURI(this.uploadContainer.getURI());
            for (SailfishURI suri : this.uploadUtilityClassURIs) {
                this.uploadSettings.addUtilityClassURI(suri);
            }

            XmlDictionaryStructureWriter.write(this.dict, context.getWorkspaceDispatcher().getFile(FolderType.ROOT, registrator.getPath()));
            SailfishURI dictionarySURI = registrator.setFactoryClass(this.uploadSettings.getFactoryClass())
                    .addUtilityClassURI(this.uploadSettings.getUtilityClassURIs())
                    .registrate();

            DictFileContainer container = new DictFileContainer(dictionarySURI, registrator.getPath(),
                    this.dict.getNamespace());

            boolean found = false;

            for (DictFileContainer cont : files) {
                if (cont.getURI().equals(container.getURI()) && cont.getFileName().equals(container.getFileName())) {
                    found = true;
                    this.selectedFileContainer = cont;
                    break;
                }
            }

            if (!found) {
                files.add(container);
                this.selectedFileContainer = container;
                this.dictFileName = this.selectedFileContainer.getFileName();
                Collections.sort(files, new DictFileContainerComparator());
            }

            BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info", "Dictionary \"" + this.uploadContainer.getURI() + "\" has been uploaded");

            this.uploadSettings = new DictionarySettings();

            this.uploadContainer.setURI(null);
            this.uploadContainer.setNamespace(null);

            RequestContext.getCurrentInstance().execute("PF('uploadDialog').hide();");

            this.dictFieldModels = fieldsToFieldEditorModels(this.dict.getImplFieldStructures(), true);
            populateDictFieldModels(this.dictFieldModels);
            this.saveAsSettings = context.getDictionaryManager().getSettings(this.selectedFileContainer.getURI());
            this.saveAsUtilityClassURIs = new HashSet<>();
            for (SailfishURI suri : this.saveAsSettings.getUtilityClassURIs()) {
                this.saveAsUtilityClassURIs.add(suri);
            }
            createMessagesTree();

            this.selectedLeftField = null;
            this.selectedRightField = null;
            this.selectedFieldInMess = null;
            this.selectedFieldInAll = null;

            validateDictionary(true, true);

        } catch (Exception ex) {
            logger.error("Can not write uploaded dictionary to file.", ex);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error",
                    "Can't upload dictionary " + this.saveAsContainer.getURI() + ": " + ex.getMessage());
        }
    }

    private IDictionaryRegistrator getDictionaryFile(String title, boolean overwriteFile) {
        try {

            return context.getDictionaryManager().registerDictionary(title, overwriteFile);

        } catch (WorkspaceLayerException e) {

            if (overwriteFile) {

                logger.error("Cannot delete file for title [{}]", title, e);
                BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot delete file for title " + title);

            } else {

                BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "File already exists");
                RequestContext.getCurrentInstance().addCallbackParam("validationFailed", true);
            }

        } catch (IOException | RuntimeException e) {

            logger.error("Cannot create file for title [{}]", title, e);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot create file for title " + title);

        }

        return null;
    }

    public void closeDictionary() {

        if (this.dict != null) {

            this.dict = null;
            this.newDict = false;
            this.selectedFileContainer = null;
            this.loadedFileContainer = null;
            this.dictFieldModels = null;
            this.newField = new ModifiableFieldStructure();
            this.selectedLeftField = null;
            this.selectedRightField = null;

            BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info", "Dictionary has been successfully closed");
        }
    }

    public void applyChanges(FieldEditorModel model) {

        boolean nameChanged = false;

        if (!model.getField().getName().equals(model.getEditedField().getName())) {
            nameChanged = true;
        }

        if ((!model.isMessage() && model.getField().getReference() != null) || (model.isMessage() && model.getField().getReference() != null)) {

            String setDescription = null;
            String setDefaultValue = null;

            if (model.isDefaultValueInherited()) {
                model.setDefaultValueOverrides(
                        !StringUtils.equals(model.getField().getImplReference().getImplDefaultValue(), model.getEditedField().getImplDefaultValue()));
                setDefaultValue = model.getEditedField().getImplDefaultValue();
            }

            if (model.isDescriptionInherited() && !model.isDescriptionOverrides()) {
                if (!StringUtils.equals(model.getField().getImplReference().getDescription(), model.getEditedField().getDescription())) {
                    model.setDescriptionOverrides(true);
                }
                setDescription = model.getEditedField().getDescription();
            }

            if (model.isDescriptionInherited() && !model.isDescriptionOverrides()) {
                model.getEditedField().setDescription(null);
            }

            model.applyChanges();

            if (StringUtils.isEmpty(setDescription)) {
                setDescription = null;
            }

            if (model.isDescriptionInherited() && !model.isDescriptionOverrides()) {
                model.getEditedField().setDescription(setDescription);
            }

            if (model.isDefaultValueInherited() && !model.isDefaultValueOverrides()) {
                model.getEditedField().setDefaultValue(setDefaultValue);
            }

        } else {
            model.applyChanges();
        }

        List<DictionaryValidationError> errors;

        if (model.getField().isMessage()) {

            // Validate properties

            errors = this.dictionaryValidator.validate(this.dict, (ModifiableMessageStructure) model.getField(), false);
            model.setErrors(errors.isEmpty() ? null : errors);

            // Validate structure of main message

            if (nameChanged) {

                removeOldErrors(this.tree.getChildren(), DictionaryValidationErrorLevel.DICTIONARY);

                errors = this.dictionaryValidator.validate(this.dict, false, false);

                setNewErrors(this.tree.getChildren(), errors, true);
            }

        } else {

            errors = this.dictionaryValidator.validate(getParentMsg(model), model.getField());
            model.setErrors(errors.isEmpty() ? null : errors);

            if (nameChanged) {

                if (model.isStandaloneField()) {

                    // Validate reference fields

                    removeOldErrorsFromFields(this.dictFieldModels, DictionaryValidationErrorLevel.DICTIONARY);

                    errors = this.dictionaryValidator.validate(this.dict, false, true);

                    setNewErrorsToFields(this.dictFieldModels, errors);

                } else {

                    if (model.getField().isSubMessage()) {

                        // Validate reference message

                        List<TreeModel<FieldEditorModel>> children = getTreeModelMessageById(
                                model.getField().getImplReference().getId()).getChildren();

                        removeOldErrors(children, DictionaryValidationErrorLevel.MESSAGE);

                        errors = this.dictionaryValidator.validate(this.dict,
                                (ModifiableMessageStructure) model.getField().getReference(), false);

                        setNewErrors(children, errors, true);

                    } else {

                        // Validate parent message

                        removeOldErrors(model.getTreeModel().getParent().getChildren(), DictionaryValidationErrorLevel.MESSAGE);

                        errors = this.dictionaryValidator.validate(this.dict,
                                (ModifiableMessageStructure) model.getTreeModel().getParent().getData().getField(), false);

                        setNewErrors(model.getTreeModel().getParent().getChildren(), errors, true);
                    }
                }
            }
        }

        model.setPropertiesChanged(false);

        if (!model.isStandaloneField()) {
            if (nameChanged && model.getField().isMessage()) {
                RequestContext.getCurrentInstance().update("messForm");
            } else {

                if (model.getField().isSubMessage()) {

                    updateReferences(getTreeModelMessageById(model.getField().getImplReference().getId()).getData(), false);

                } else {

                    if (nameChanged) {
                        RequestContext.getCurrentInstance().update("tree:" + model.getTreeModel().getParent().getData().getIndex() + ":inside");
                    }

                    updateReferences(model, true);
                }
            }
        } else {
            if (nameChanged) {
                RequestContext.getCurrentInstance().update("fieldsForm");
            } else {
                updateRightFieldReferences(model, false);
            }
            updateFieldReferences(null, model, false);
        }
    }

    private Multimap<Integer, FieldEditorModel> findRightFieldReferencesToUpdate(FieldEditorModel model) {
        Multimap<Integer, FieldEditorModel> result = HashMultimap.create();

        for (FieldEditorModel curModel : this.dictFieldModels) {
            if (curModel == model)
                continue;

            ModifiableFieldStructure field = curModel.getField().getImplReference();
            int level = 1;

            while (field != null) {
                if (field.getId().equals(model.getField().getId())) {
                    result.put(level, curModel);
                    break;
                } else {
                    level++;
                    field = field.getImplReference();
                }
            }
        }

        return result;
    }

    private void updateRightFieldReferences(FieldEditorModel model, boolean attrs) {

        Multimap<Integer, FieldEditorModel> toUpdate = findRightFieldReferencesToUpdate(model);

        if (toUpdate.isEmpty()) {
            return;
        }

        List<Integer> toUpdateByLevel = new ArrayList<>(toUpdate.keySet());
        Collections.sort(toUpdateByLevel);

        for (Integer level : toUpdateByLevel) {
            for (FieldEditorModel curModel : toUpdate.get(level)) {

                String index = curModel.getIndex().substring(1);

                if (attrs) {
                    curModel.reinit();
                    RequestContext.getCurrentInstance().update("rightFieldEntry:" + index + ":fieldInfo");
                    if (!curModel.isPropertiesCollapsed()) {
                        RequestContext.getCurrentInstance().update("rightFieldEntry:" + index + ":propDefaultPanel");
                        RequestContext.getCurrentInstance().update("rightFieldEntry:" + index + ":addOrDeleteDefaultValuePanel");
                        RequestContext.getCurrentInstance().update("rightFieldEntry:" + index + ":atrs");
                        RequestContext.getCurrentInstance().update("rightFieldEntry:" + index + ":baseAtrs");
                    }
                } else {
                    if (!curModel.isPropertiesChanged()) {
                        curModel.setEditedField(cloneField(curModel.getField()));
                        curModel.applyChanges();
                        RequestContext.getCurrentInstance().update("rightFieldEntry:" + index + ":fieldType");
                        if (!curModel.isPropertiesCollapsed()) {
                            RequestContext.getCurrentInstance().update("rightFieldEntry:" + index + ":treePropertiesWrapper");
                        }
                    }
                }
            }
        }
    }

    private void getFieldReferences(FieldEditorModel model, TreeModel<FieldEditorModel> node, List<TreeModel<FieldEditorModel>> references,
            int maxLevel, boolean onlyFirstInChain) {

        if (node.getData() != null && !node.getData().isMessage()) {
            if (node.getData().getField().getReference() != null) {
                if (onlyFirstInChain) {
                    if (node.getData().getField().getImplReference().getId().equals(model.getField().getId())) {
                        references.add(node);
                    }
                } else {
                    for (ModifiableFieldStructure reference : getReferenceChain(node)) {
                        if (reference.getId().equals(model.getField().getId())) {
                            references.add(node);
                            break;
                        }
                    }
                }
            }
        }

        if (maxLevel == 0 || node.getLevel() < maxLevel) {
            for (TreeModel<FieldEditorModel> child : node.getChildren()) {
                getFieldReferences(model, child, references, maxLevel, onlyFirstInChain);
            }
        }
    }

    private List<ModifiableFieldStructure> getReferenceChain(TreeModel<FieldEditorModel> node) {
        List<ModifiableFieldStructure> result = new ArrayList<>();
        ModifiableFieldStructure implReference = node.getData().getField().getImplReference();
        while (implReference != null) {
            result.add(implReference);
            implReference = implReference.getImplReference();
        }

        return result;
    }

    private void updateFieldReferences(List<TreeModel<FieldEditorModel>> references, FieldEditorModel model, boolean attrs) {

        if (references == null) {
            references = new ArrayList<>();
            getFieldReferences(model, this.tree, references, 0, false);
        }

        for (TreeModel<FieldEditorModel> ref : references) {

            RequestContext.getCurrentInstance().update("tree:" + ref.getData().getIndex() + ":fieldInfo");

            if (attrs) {
                ref.getData().reinit();
                if (!ref.getParent().getData().isCollapsed()) {
                    if (!ref.getData().isPropertiesCollapsed()) {
                        RequestContext.getCurrentInstance().update("tree:" + ref.getData().getIndex() + ":atrs");
                        RequestContext.getCurrentInstance().update("tree:" + ref.getData().getIndex() + ":propDefaultPanel");
                        RequestContext.getCurrentInstance().update("tree:" + ref.getData().getIndex() + ":addOrDeleteDefaultValuePanel");
                    }
                }
            } else {
                if (!ref.getData().isPropertiesChanged()) {
                    ref.getData().setEditedField(cloneField(ref.getData().getField()));
                    ref.getData().applyChanges();
                    if (!ref.getData().isPropertiesCollapsed()) {
                        RequestContext.getCurrentInstance().update("tree:" + ref.getData().getIndex() + ":treePropertiesWrapper");
                    }
                }
            }
        }
    }

    private void updateReferences(FieldEditorModel model, boolean edit) {
        ModifiableFieldStructure parentMsg = model.getTreeModel().getParent().getData() != null
                ? model.getTreeModel().getParent().getData().getField() : null;
        List<TreeModel<FieldEditorModel>> lor = new ArrayList<>();

        // If SubMessage
        if (parentMsg != null && parentMsg.getReference() != null) {

            TreeModel<FieldEditorModel> parent = getTreeModelMessageById(parentMsg.getImplReference().getId());

            if (!parent.getData().isCollapsed()) {

                // Update the referenced field only
                for (TreeModel<FieldEditorModel> child : parent.getChildren()) {
                    if (child.getData().getField().getId().equals(model.getField().getId())) {

                        // Update properties if expanded
                        if (!child.getData().isPropertiesCollapsed() && !child.getData().isPropertiesChanged()) {
                            child.getData().setEditedField(cloneField(child.getData().getField()));
                        }

                        child.getData().setDefaultValueOverrides(model.isDefaultValueOverrides());
                        child.getData().setDescriptionOverrides(model.isDescriptionOverrides());

                        child.getData().setErrors(model.getErrors());

                        if (edit) {
                            RequestContext.getCurrentInstance().update("tree:" + child.getData().getIndex() + ":inside");
                        }
                        break;
                    }
                }
            }

            if (!edit) {
                RequestContext.getCurrentInstance().update("tree:" + parent.getData().getIndex() + ":inside");
            }
        }

        if (parentMsg != null) {
            createListOfReferences(lor, model.getTreeModel().getParent().getData().getField().getId(), this.tree);
        } else {
            if (model.getLevel() == 1) {
                createListOfReferences(lor, model.getTreeModel().getData().getField().getId(), this.tree);
            }
        }

        for (TreeModel<FieldEditorModel> ref : lor) {

            // Skip the edited SubMessage
            if (ref.getData() != model.getTreeModel().getParent().getData()) {

                if (!ref.getData().isCollapsed()) {
                    for (TreeModel<FieldEditorModel> child : ref.getChildren()) {
                        if (child.getData().getField().getId().equals(model.getField().getId())) {

                            // Update properties if expanded
                            if (!child.getData().isPropertiesCollapsed() && !child.getData().isPropertiesChanged()) {
                                child.getData().setEditedField(cloneField(child.getData().getField()));
                            }

                            child.getData().setDefaultValueOverrides(model.isDefaultValueOverrides());
                            child.getData().setDescriptionOverrides(model.isDescriptionOverrides());

                            child.getData().setErrors(model.getErrors());

                            if (edit) {
                                RequestContext.getCurrentInstance().update("tree:" + child.getData().getIndex() + ":inside");
                            }
                            break;
                        }
                    }
                }

                if (!edit) {
                    RequestContext.getCurrentInstance().update("tree:" + ref.getData().getIndex() + ":inside");
                } else {
                    ref.getData().reinit();
                    if (!ref.getData().isPropertiesChanged()) {
                        ref.getData().setEditedField(cloneField(ref.getData().getField()));
                        if (!ref.getData().isPropertiesCollapsed()) {
                            RequestContext.getCurrentInstance().update("tree:" + ref.getData().getIndex() + ":treePropertiesWrapper");
                        }
                        RequestContext.getCurrentInstance().update("tree:" + ref.getData().getIndex() + ":fieldType");
                    }
                }
            }
        }
    }

    public void createListOfReferences(List<TreeModel<FieldEditorModel>> list, String referenceId, TreeModel<FieldEditorModel> node) {

        if (node.getData() != null && node.getLevel() > 1 && node.getData().isMessage()) {
            if (node.getData().getField().getImplReference().getId().equals(referenceId)) {
                list.add(node);
            }
        }

        for (TreeModel<FieldEditorModel> child : node.getChildren()) {
            if (child.getData().isMessage()) {
                createListOfReferences(list, referenceId, child);
            }
        }
    }

    public void cancelChanges(FieldEditorModel model) {
        model.setEditedField(cloneField(model.getField()));
        model.setPropertiesChanged(false);
    }

    public void goToReference(FieldEditorModel model) {
        goToReference(model, false);
    }

    private void goToReference(FieldEditorModel model, boolean notRef) {

        String fieldName = notRef ? model.getField().getName() : model.getField().getImplReference().getName();

        if (model.getField().isSubMessage() || model.isMessage()) {

            for (TreeModel<FieldEditorModel> node : this.tree.getChildren()) {
                if (node.getData().getField().getName().equals(fieldName)) {
                    onSelectLeftField(node.getData());

                    RequestContext.getCurrentInstance().update("tree:" + node.getData().getIndex() + ":inside");
                    RequestContext.getCurrentInstance().update("messForm:messTabPanelMenu");
                    RequestContext.getCurrentInstance().update("upDownPanel");

                    break;
                }
            }

        } else {

            for (FieldEditorModel fm : this.dictFieldModels) {
                if (fm.getField().getName().equals(fieldName)) {
                    onSelectRightField(fm);

                    RequestContext.getCurrentInstance().update("rightFieldEntry:" + fm.getIndex().substring(1) + ":inside");
                    RequestContext.getCurrentInstance().update("fieldsForm:fieldsPanelMenu");
                    RequestContext.getCurrentInstance().update("fieldMoveButtonPanel");

                    break;
                }
            }
        }
    }

    public void onSelectRightField(FieldEditorModel model) {

        if (this.selectedRightField != null) {
            this.selectedRightField.setSelected(false);
        }

        model.setSelected(true);

        this.selectedRightField = model;
    }

    public void onSelectLeftField(FieldEditorModel model) {

        if (this.selectedLeftField != null) {
            this.selectedLeftField.setSelected(false);
        }

        model.setSelected(true);

        this.selectedLeftField = model;

        position = selectedLeftField.getChildCount();

    }

    public void onExpanderClick(FieldEditorModel model, boolean first) {
        if (first) {
            model.setCollapsed(!model.isCollapsed());
        } else {
            model.setCollapsed(model.isCollapsedForSlide());
        }
    }

    public void onExpanderClickForSlide(FieldEditorModel model, boolean first) {
        if (first) {
            model.setCollapsedForSlide(!model.isCollapsedForSlide());
        } else {
            model.setCollapsedForSlide(model.isCollapsed());
        }
    }

    public void onPropertiesExpanderClick(FieldEditorModel model, boolean first) {

        if (first) {
            model.setPropertiesCollapsed(!model.isPropertiesCollapsed());
            model.setEditedField(cloneField(model.getField()));
        } else {
            model.setPropertiesCollapsed(model.isPropertiesCollapsedForSlide());
            model.setEditedField(null);
        }
    }

    public void onPropertiesExpanderClickForSlide(FieldEditorModel model, boolean first) {
        if (first) {
            model.setPropertiesCollapsedForSlide(!model.isPropertiesCollapsedForSlide());
        } else {
            model.setPropertiesCollapsedForSlide(model.isPropertiesCollapsed());
        }
    }

    public void onDefValChanged(FieldEditorModel model) {
        if (model.isDefaultValueChangesMade()) {
            RequestContext.getCurrentInstance().execute("showPropButtons('#b-" + model.getIndex() + "')");
        }
    }

    public void onDefValAddDeletePressed(FieldEditorModel model) {
        if (model.isStandaloneField()) {
            RequestContext.getCurrentInstance().update("rightFieldEntry:" + model.getIndex().substring(1) + ":propDefaultPanel");
            RequestContext.getCurrentInstance().update("rightFieldEntry:" + model.getIndex().substring(1) + ":addOrDeleteDefaultValuePanel");
        } else {
            RequestContext.getCurrentInstance().update("tree:" + model.getIndex() + ":propDefaultPanel");
            RequestContext.getCurrentInstance().update("tree:" + model.getIndex() + ":addOrDeleteDefaultValuePanel");
        }
        RequestContext.getCurrentInstance().execute("showPropButtons('#b-" + model.getIndex() + "')");
    }

    public boolean renderDeleteDefValLink(FieldEditorModel model) {
        return model.getField().getImplReference() == null ? true
                : !StringUtils.equals(model.getField().getImplReference().getImplDefaultValue(), model.getEditedField().getImplDefaultValue());
    }

    public boolean renderResetTooltip(FieldEditorModel model) {
        return model.isDefaultValueOverrides();
    }

    public void onPropertiesChanged(FieldEditorModel model) {
        if (model.isPropertiesChangesMade()) {
            RequestContext.getCurrentInstance().execute("showPropButtons('#b-" + model.getIndex() + "')");
            model.setPropertiesChanged(true);
        }
    }

    public boolean isUpDownDisabled() {

        if (this.selectedLeftField == null)
            return true;
        if (this.selectedLeftField.getTreeModel().getLevel() <= 1)
            return true;
        if (this.selectedLeftField.getTreeModel().getParent().getData().isCollapsed())
            return true;

        return false;
    }

    public JavaType[] getJavaTypes() {
        return JavaType.values();
    }

    public void collapseAll() {
        collapseAll(this.tree);
    }

    private void collapseAll(TreeModel<FieldEditorModel> node) {
        if (node.getData() != null) {
            collapseField(node.getData());
        }

        for (TreeModel<FieldEditorModel> child : node.getChildren()) {
            collapseAll(child);
        }
    }

    public void collapseAllFields() {
        for (FieldEditorModel fieldModel : this.dictFieldModels) {
            collapseField(fieldModel);
        }
    }

    private void collapseField(FieldEditorModel field) {
        field.setCollapsed(true);
        field.setCollapsedForSlide(true);
        if (field.isPropertiesChanged()) {
            cancelChanges(field);
        }
        field.setPropertiesCollapsed(true);
        field.setPropertiesCollapsedForSlide(true);
    }

    public boolean isCollapseAllButtonDisabled() {
        return isCollapsed(this.tree);
    }

    private boolean isCollapsed(TreeModel<FieldEditorModel> node) {
        if (node == null)
            return true;

        if (node.getData() != null && (!node.getData().isCollapsed() || !node.getData().isPropertiesCollapsed()))
            return false;

        for (TreeModel<FieldEditorModel> child : node.getChildren()) {
            if (!isCollapsed(child)) {
                return false;
            }
        }

        return true;
    }

    public boolean isCollapseAllFieldsButtonDisabled() {
        if (this.dictFieldModels == null)
            return true;

        for (FieldEditorModel fieldModel : this.dictFieldModels) {
            if (!fieldModel.isPropertiesCollapsed())
                return false;
        }
        return true;
    }

    public String getRefRightDefVal() {
        if (this.newField.getReference() == null)
            return null;
        ModifiableFieldStructure ref = getNewFieldRef();
        if (ref == null)
            return null;
        return getDefVal(getFieldModelById(ref.getId()), this.newField);
    }

    public void setRefRightDefVal(String defVal) {
        ModifiableFieldStructure ref = getNewFieldRef();
        if (ref == null) {
            setDefVal(null, this.newField, null);
            return;
        }
        setDefVal(getFieldModelById(ref.getId()), this.newField, defVal);
    }

    public String[] getNewFieldRefDefValues() {
        ModifiableFieldStructure ref = getNewFieldRef();
        if (ref == null)
            return null;
        return getFieldModelById(ref.getId()).getDefValues();
    }

    public boolean isPropCloneDefValRendered() {
        String[] refValues = getNewFieldRefDefValues();

        if (!this.cloneDialog && (this.newField.getReference() == null || refValues == null || refValues.length == 0)) {
            return true;
        }

        String[] defValues = this.selectedRightField == null ? null : this.selectedRightField.getDefValues();

        if (this.cloneDialog && (defValues == null || defValues.length == 0)) {
            return true;
        }

        return false;
    }

    public boolean isPropAddDefValRendered() {
        String[] refValues = getNewFieldRefDefValues();

        if (!this.cloneDialog && this.newField.getReference() != null && refValues != null && refValues.length > 0) {
            return true;
        }

        return false;
    }

    private ModifiableFieldStructure getNewFieldRef() {
        return this.newField.getReference() instanceof String ? getFieldById((String) this.newField.getReference())
                : this.newField.getImplReference();
    }

    public String getCloneRightDefVal() {
        return getDefVal(this.selectedRightField, this.newField);
    }

    public void setCloneRightDefVal(String defVal) {
        setDefVal(this.selectedRightField, this.newField, defVal);
    }

    private String getDefVal(FieldEditorModel from, ModifiableFieldStructure to) {

        for (AttributeModel model : from.getValueModels()) {
            if (model.getActual().getValue().equals(to.getImplDefaultValue())) {
                return model.getActual().getName();
            }
        }

        return to.getImplDefaultValue();
    }

    private void setDefVal(FieldEditorModel from, ModifiableFieldStructure to, String defVal) {

        if (from != null) {
            for (AttributeModel model : from.getValueModels()) {
                if (model.getActual().getName().equals(defVal)) {
                    to.setDefaultValue(model.getActual().getValue());
                    return;
                }
            }
        }

        to.setDefaultValue(defVal);
    }

    public ComplexAttributesUpdateModel getComplexAttrsUpdateModel() {
        return complexAttrsUpdateModel;
    }

    public void setComplexAttrsUpdateModel(ComplexAttributesUpdateModel complexAttrsUpdateModel) {
        this.complexAttrsUpdateModel = complexAttrsUpdateModel;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public static class FieldComparator implements Comparator<ModifiableFieldStructure> {

        @Override
        public int compare(ModifiableFieldStructure o1, ModifiableFieldStructure o2) {
            if (o1.getName() == null)
                return -1;
            if (o2.getName() == null)
                return 1;
            return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
        }
    }

    private class DictFileContainerComparator implements Comparator<DictFileContainer> {

        @Override
        public int compare(DictFileContainer o1, DictFileContainer o2) {
            if (o1.getURI() == null)
                return -1;
            if (o2.getURI() == null)
                return 1;
            return o1.getURI().compareTo(o2.getURI());
        }
    }

    private class FieldModelComparator implements Comparator<FieldEditorModel> {

        @Override
        public int compare(FieldEditorModel o1, FieldEditorModel o2) {
            if (o1.getField().getName() == null)
                return -1;
            if (o2.getField().getName() == null)
                return 1;
            return o1.getField().getName().toLowerCase().compareTo(o2.getField().getName().toLowerCase());
        }
    }

    private class TreeModelComparator implements Comparator<TreeModel<FieldEditorModel>> {

        @Override
        public int compare(TreeModel<FieldEditorModel> o1, TreeModel<FieldEditorModel> o2) {
            if (o1.getData().getField().getName() == null)
                return -1;
            if (o2.getData().getField().getName() == null)
                return 1;
            return o1.getData().getField().getName().toLowerCase().compareTo(o2.getData().getField().getName().toLowerCase());
        }
    }

    public boolean isIsCollectionNewField() {
        return this.newField.isCollection();
    }

    public void setIsCollectionNewField(boolean isCollection) {
        this.newField.setCollection(isCollection);
    }

    public boolean isRequiredNewField() {
        return this.newField.isRequired();
    }

    public void setRequiredNewField(boolean required) {
        this.newField.setRequired(required);
    }

    public String getNewAttributeType() {
        if (this.newAttribute == null || this.newAttribute.getType() == null)
            return null;
        return BeanUtil.getJavaTypeLabel(this.newAttribute.getType());
    }

    public void setNewAttributeType(String str) {
        this.newAttribute.setType(fromTypeLabel(str));
    }

    public ModifiableAttributeStructure getNewAttribute() {
        return newAttribute;
    }

    public void setNewAttribute(ModifiableAttributeStructure newAttribute) {
        this.newAttribute = newAttribute;
    }

    public int getAttributeMode() {
        return attributeMode;
    }

    public void setAttributeMode(int attributeMode) {
        this.attributeMode = attributeMode;
    }

    public ModifiableFieldStructure getSelectedFieldInMess() {
        return selectedFieldInMess;
    }

    public void setSelectedFieldInMess(ModifiableFieldStructure selectedFieldInMess) {
        this.selectedFieldInMess = selectedFieldInMess;
    }

    public ModifiableFieldStructure getSelectedFieldInAll() {
        return selectedFieldInAll;
    }

    public void setSelectedFieldInAll(ModifiableFieldStructure selectedFieldInAll) {
        this.selectedFieldInAll = selectedFieldInAll;
    }

    public DictFileContainer getSelectedFileContainer() {
        return selectedFileContainer;
    }

    public void setSelectedFileContainer(DictFileContainer selectedFileContainer) {
        this.selectedFileContainer = selectedFileContainer;
    }

    public String getDictFileName() {
        return dictFileName;
    }

    public boolean isOverwriteFile() {
        return overwriteFile;
    }

    public void setOverwriteFile(boolean overwriteFile) {
        this.overwriteFile = overwriteFile;
    }

    public List<DictFileContainer> getFiles() {
        return files;
    }

    public ModifiableDictionaryStructure getDict() {
        return dict;
    }

    public void setDict(ModifiableDictionaryStructure dict) {
        this.dict = dict;
    }

    public FieldEditorModel getFieldEditorModel() {
        return fieldEditorModel;
    }

    public TreeModel<FieldEditorModel> getTree() {
        return tree;
    }

    public FieldEditorModel getSelectedLeftField() {
        return selectedLeftField;
    }

    public void setSelectedLeftField(FieldEditorModel selectedMessage) {
        this.selectedLeftField = selectedMessage;
        this.position = selectedLeftField.getChildCount();
    }

    public FieldEditorModel getSelectedRightField() {
        return selectedRightField;
    }

    public void setSelectedRightField(FieldEditorModel selectedRightField) {
        this.selectedRightField = selectedRightField;
    }

    public DictFileContainer getSaveAsContainer() {
        return saveAsContainer;
    }

    public void setSaveAsContainer(DictFileContainer saveAsContainer) {
        this.saveAsContainer = saveAsContainer;
    }

    public DictionarySettings getSaveAsSettings() {
        return saveAsSettings;
    }

    public void setSaveAsSettings(DictionarySettings saveAsSettings) {
        this.saveAsSettings = saveAsSettings;
    }

    public DictFileContainer getUploadContainer() {
        return uploadContainer;
    }

    public void setUploadContainer(DictFileContainer uploadContainer) {
        this.uploadContainer = uploadContainer;
    }

    public DictionarySettings getUploadSettings() {
        return uploadSettings;
    }

    public void setUploadSettings(DictionarySettings uploadSettings) {
        this.uploadSettings = uploadSettings;
    }

    public String getNewFieldType() {
        if (this.newField == null || this.newField.getImplJavaType() == null) {
            return null;
        }
        return BeanUtil.getJavaTypeLabel(this.newField.getImplJavaType());
    }

    public void setNewFieldType(String str) {
        this.newField.setJavaType(fromTypeLabel(str));
    }

    public ModifiableFieldStructure getNewField() {
        return newField;
    }

    public void setNewField(ModifiableFieldStructure newField) {
        this.newField = newField;
    }

    public String getNewType() {
        return newType;
    }

    public void setNewType(String newType) {

        this.newType = newType;

        if (StringUtils.isEmpty(newType)) {

            this.newField.setJavaType(null);

        } else if ("Message".equals(newType)) {

            this.newField.setJavaType(null);
            this.newField.setReference(null);

        } else {

            this.newField.setJavaType(fromTypeLabel(newType));
        }
    }

    public boolean isMovingFromRight() {
        return movingFromRight;
    }

    public int getRefCount() {
        return refCount;
    }

    public String getRefString() {
        return refString;
    }

    public boolean isCloneDialog() {
        return cloneDialog;
    }

    public boolean isNewDict() {
        return newDict;
    }

    public List<DictFileContainer> getSelectedDictionariesForDownload() {
        return selectedDictionariesForDownload;
    }

    public void setSelectedDictionariesForDownload(List<DictFileContainer> selectedDictionariesForDownload) {
        this.selectedDictionariesForDownload = selectedDictionariesForDownload;
    }

    public Set<SailfishURI> getSaveAsUtilityClassURIs() {
        return saveAsUtilityClassURIs;
    }

    public void setSaveAsUtilityClassURIs(Set<SailfishURI> saveAsUtilityClassURIs) {
        this.saveAsUtilityClassURIs = saveAsUtilityClassURIs;
    }

    public Set<SailfishURI> getUploadUtilityClassURIs() {
        return uploadUtilityClassURIs;
    }

    public void setUploadUtilityClassURIs(Set<SailfishURI> uploadUtilityClassURIs) {
        this.uploadUtilityClassURIs = uploadUtilityClassURIs;
    }

    public SailfishURI getSelectedValidator() {
        return selectedValidator;
    }

    public void setSelectedValidator(SailfishURI selectedValidator) {
        this.selectedValidator = selectedValidator;
    }

    public List<DictionaryValidationError> getMainErrors() {
        return mainErrors;
    }

    public FieldEditorModel getSearchModel() {
        return searchModel;
    }

    public void setSearchModel(FieldEditorModel searchModel) {
        this.searchModel = searchModel;
    }

    public Set<SailfishURI> getUtilitySURI() {
        return utilitySURI;
    }

    public void setUtilitySURI(Set<SailfishURI> utilitySURI) {
        this.utilitySURI = utilitySURI;
    }

    public void setFieldEditorModel(FieldEditorModel fieldEditorModel) {
        this.fieldEditorModel = fieldEditorModel;
    }
}
