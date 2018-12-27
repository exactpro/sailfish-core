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
package com.exactpro.sf.help;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.help.helpmarshaller.AbstrFieldMess;
import com.exactpro.sf.help.helpmarshaller.Field;
import com.exactpro.sf.help.helpmarshaller.Message;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.DictionarySettings;
import com.exactpro.sf.aml.IValidator;
import com.exactpro.sf.aml.converter.MatrixConverterLoader;
import com.exactpro.sf.aml.preprocessor.PreprocessorDefinition;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.center.impl.CoreVersion;
import com.exactpro.sf.center.impl.PluginLoader;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.DataManager;
import com.exactpro.sf.configuration.DefaultAdapterManager;
import com.exactpro.sf.configuration.DictionaryManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoadableManager;
import com.exactpro.sf.configuration.StaticServiceDescription;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceDispatcherBuilder;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.embedded.statistics.handlers.StatisticsReportHandlerLoader;
import com.exactpro.sf.help.helpmarshaller.HelpEntityName;
import com.exactpro.sf.help.helpmarshaller.HelpEntityType;
import com.exactpro.sf.help.helpmarshaller.PluginHelpContainer;
import com.exactpro.sf.help.helpmarshaller.describers.FieldsDescriber;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.FieldJsonContainer;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.HelpJsonContainer;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.MessageJsonContainer;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.MethodJsonContainer;
import com.exactpro.sf.help.helpmarshaller.jsoncontainers.URIJsonContainer;
import com.exactpro.sf.matrixhandlers.IMatrixProviderFactory;
import com.exactpro.sf.matrixhandlers.MatrixProviderHolder;
import com.exactpro.sf.scriptrunner.PreprocessorLoader;
import com.exactpro.sf.scriptrunner.ValidatorLoader;
import com.exactpro.sf.scriptrunner.actionmanager.ActionClass;
import com.exactpro.sf.scriptrunner.actionmanager.ActionManager;
import com.exactpro.sf.scriptrunner.languagemanager.ILanguageFactory;
import com.exactpro.sf.scriptrunner.services.DefaultStaticServiceManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityClass;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityManager;
import com.exactpro.sf.util.HelpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import freemarker.template.TemplateException;


public class HelpBuilder {
    private static final Logger logger = LoggerFactory.getLogger(HelpBuilder.class);

    public static final String HELP = "help";
    private static final String ACTIONS = "actions";
    private static final String UTILS = "utils";
    public static final String DICTIONARIES = "dictionaries";
    private static final String LANGUAGES = "languages";
    private static final String PROVIDERS = "providers";
    private static final String SERVICES = "services";
    private static final String VALIDATORS = "validators";
    private static final String PREPROCESSORS = "preprocessors";
    private static final String METHODS = "methods";
    private static final String FIELDS = "fields";
    private static final String MESSAGES = "messages";
    private static final String RESOURCES = "resources";

    private static final String INDEX_HTML = "index.html";
    private static final String PLUGIN_HTML = "plugin.html";
    private static final String ACTIONS_HTML = "actions.html";
    private static final String DICTIONARIES_HTML = "dictionaries.html";
    private static final String LANGUAGES_HTML = "languages.html";
    private static final String PROVIDERS_HTML = "providers.html";
    private static final String SERVICES_HTML = "services.html";
    private static final String VALIDATORS_HTML = "validators.html";
    private static final String PREPROCESSORS_HTML = "preprocessors.html";
    private static final String ERROR_HTML = "error.html";

    private static final String INDEX_TEMPLATE = "index.ftlh";
    private static final String GROUP_TEMPLATE = "group.ftlh";
    private static final String PLUGIN_TEMPLATE = "plugin.ftlh";
    private static final String ACTION_TEMPLATE = "action.ftlh";
    private static final String ACTION_METHOD_TEMPLATE = "action-method.ftlh";
    private static final String FIELD_TEMPLATE = "field.ftlh";
    private static final String MESSAGE_TEMPLATE = "message.ftlh";
    private static final String UTIL_TEMPLATE = "util.ftlh";
    private static final String UTIL_METHOD_TEMPLATE = "util-method.ftlh";
    private static final String DICTIONARY_TEMPLATE = "dictionary.ftlh";
    private static final String FIELD_STRUCTURE_TEMPLATE = "field_structure.ftlh";
    private static final String MESSAGE_STRUCTURE_TEMPLATE = "message_structure.ftlh";
    private static final String LANGUAGE_TEMPLATE = "language.ftlh";
    private static final String PROVIDER_TEMPLATE = "provider.ftlh";
    private static final String SERVICE_TEMPLATE = "service.ftlh";
    private static final String VALIDATOR_TEMPLATE = "validator.ftlh";
    private static final String PREPROCESSOR_TEMPLATE = "preprocessor.ftlh";
    private static final String ERROR_TEMPLATE = "error.ftlh";

    public static final String ROOT = "root.json";
    private static final String ERROR_JSON = "root.json";

    public static final String JSON = ".json";
    public static final String START_JSON = "data = '";
    public static final String END_JSON = "';";
    public static final String HTML = ".html";

    private static final String TEMPLATE_PACKAGE_PATH = "/com/exactpro/sf/help/template";
    private static final String[] PAGE_RESOURCES = new String[] { "static-help.css", "static-help.js", "ui-bg_flat_75_ffffff_40x100.png",
            "ui-icons_c02669_256x240.png", "SFicon.png" };

    private final HelpTemplateWrapperFactory templateWrapperFactory;

    private Set<String> messageStructureSet;

    private Set<String> utilSet;

    private Map<String, HelpJsonContainer> utilMap;

    private Set<String> fmSet;

    private final IWorkspaceDispatcher wd;

    private final DefaultStaticServiceManager staticServiceManager;

    private final ActionManager actionManager;

    private final DictionaryManager dictionaryManager;

    private final PreprocessorLoader preprocessorLoader;

    private final ValidatorLoader validatorLoader;

    private final HelpLanguageManager languageManager;

    private final MatrixProviderHolder matrixProviderHolder;

    private final ILoadableManager matrixConverterManager;

    private final UtilityManager utilityManager;

    private final DataManager dataManager;

    private final ILoadableManager adapterManager;

    private final ILoadableManager statisticsReportLoader;

    private final Comparator<HelpJsonContainer> actionComparator;

    private final Comparator<IMessageStructure> messageComparator;

    private final IVersion version;

    private FolderType folderType;

    private String currentHelp;

    private final ObjectMapper mapper;

    private  String currentAlias;

    private HelpBuilder(IVersion version, String... workspaceLayers) throws IOException {

        DefaultWorkspaceDispatcherBuilder builder = new DefaultWorkspaceDispatcherBuilder();
        for (String layer : workspaceLayers) {
            builder.addWorkspaceLayer(new File(layer), DefaultWorkspaceLayout.getInstance());
        }

        this.wd = builder.build();

        this.utilityManager = new UtilityManager();

        this.languageManager = new HelpLanguageManager();

        this.actionManager = new ActionManager(utilityManager, languageManager);

        this.dictionaryManager = new DictionaryManager(wd, utilityManager);

        this.staticServiceManager = new DefaultStaticServiceManager();

        this.dataManager = new DataManager(wd);

        this.preprocessorLoader = new PreprocessorLoader(dataManager);

        this.validatorLoader = new ValidatorLoader();

        this.matrixProviderHolder = new MatrixProviderHolder();

        this.matrixConverterManager = new MatrixConverterLoader();

        this.templateWrapperFactory = new HelpTemplateWrapperFactory(TEMPLATE_PACKAGE_PATH);

        this.adapterManager = DefaultAdapterManager.getDefault();

        this.statisticsReportLoader = new StatisticsReportHandlerLoader();

        this.version = version;

        this.mapper = new ObjectMapper();

        this.actionComparator = new Comparator<HelpJsonContainer>() {
            @Override public int compare(HelpJsonContainer o1, HelpJsonContainer o2) {
                return o1.getFilePath().compareTo(o2.getFilePath());
            }
        };

        this.messageComparator = new Comparator<IMessageStructure>() {
            @Override public int compare(IMessageStructure o1, IMessageStructure o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };
    }

    public HelpBuilder(IWorkspaceDispatcher wd, IDictionaryManager dictionaryManager, IUtilityManager utilityManager) throws IOException {

        this.wd = wd;

        this.dictionaryManager = (DictionaryManager) dictionaryManager;

        this.utilityManager = (UtilityManager) utilityManager;

        this.templateWrapperFactory = new HelpTemplateWrapperFactory(TEMPLATE_PACKAGE_PATH);

        this.mapper = new ObjectMapper();

        this.messageComparator = new Comparator<IMessageStructure>() {
            @Override public int compare(IMessageStructure o1, IMessageStructure o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };

        this.actionManager = null;

        this.staticServiceManager = null;

        this.dataManager = null;

        this.preprocessorLoader = null;

        this.validatorLoader = null;

        this.languageManager = null;

        this.matrixProviderHolder = null;

        this.matrixConverterManager = null;

        this.adapterManager = null;

        this.actionComparator = null;

        this.statisticsReportLoader = null;

        this.version = null;
    }

    private void build(String rootName, String rootAlias) throws IOException, SailfishURIException, TemplateException {

        if (this.actionManager == null) {
            throw new UnsupportedOperationException();
        }

        PluginLoader pluginLoader = new PluginLoader(wd, staticServiceManager, actionManager, dictionaryManager, preprocessorLoader, validatorLoader,
                adapterManager, dataManager, languageManager, matrixProviderHolder, matrixConverterManager, statisticsReportLoader, version);

        pluginLoader.load();

        for(IVersion plugin : pluginLoader.getPluginVersions()) {
            IVersion fileVersion = plugin;

            if (plugin.isGeneral()) {
                folderType = FolderType.ROOT;
                currentHelp = HELP;
                fileVersion = new VersionWrapper(rootAlias, plugin);
            } else {
                folderType = FolderType.PLUGINS;
                currentHelp = plugin.getAlias() + File.separator + HELP;
            }
            currentAlias = fileVersion.getAlias();
            utilSet = new HashSet<>();
            utilMap = new HashMap<>();

            File rootJson = wd.createFile(folderType, true, currentHelp, ROOT);

            HelpJsonContainer pluginRootJson = buildPlugin(rootName, fileVersion);

            buildActions(plugin, pluginRootJson);
            buildDictionaries(plugin, pluginRootJson);
            buildServices(plugin, pluginRootJson);
            buildLanguages(plugin, pluginRootJson);
            buildValidators(plugin, pluginRootJson);
            buildPreprocessors(plugin, pluginRootJson);
            buildMatrixProviders(plugin, pluginRootJson);
            // TODO: add building statistic reports help
            buildIndex();

            writeJson(rootJson, pluginRootJson);
            copyResources();
        }

    }

    private void buildIndex() throws IOException, TemplateException {
        File indexHtml = wd.createFile(folderType, true, currentHelp, INDEX_HTML);
        try (Writer helpWriter = new BufferedWriter(new FileWriter(indexHtml))) {
            HelpTemplateWrapper templateWrapper = templateWrapperFactory.createWrapper(INDEX_TEMPLATE);
            templateWrapper.write(helpWriter);
        }
    }

    private HelpJsonContainer buildPlugin(String pluginName, IVersion plugin) throws IOException, TemplateException {

        File pluginHtml = wd.createFile(folderType, true, currentHelp, PLUGIN_HTML);

        try (Writer helpWriter = new BufferedWriter(new FileWriter(pluginHtml))) {
            HelpTemplateWrapper templateWrapper = templateWrapperFactory.createWrapper(PLUGIN_TEMPLATE);

            templateWrapper.setData("name", pluginName);
            templateWrapper.setData("version", plugin.buildVersion());

            templateWrapper.write(helpWriter);
        }

        HelpJsonContainer pluginJsonFileContainer = new HelpJsonContainer(pluginName, PLUGIN_HTML, HelpJsonContainer.PLUGIN_ICON,
                HelpEntityType.NAMED, new ArrayList<HelpJsonContainer>());

        return pluginJsonFileContainer;

    }

    private void buildActions(IVersion plugin, HelpJsonContainer jsonParent) throws IOException, TemplateException, SailfishURIException {

        List<ActionClass> unmarshalledActions = actionManager.getActionClasses();
        List<ActionClass> validActions = new ArrayList<>();

        for (ActionClass loadedAction : unmarshalledActions) {
            if (loadedAction.getPlugin().equals(plugin)) {
                validActions.add((loadedAction));
            }
        }

        if (validActions.isEmpty()) {
            return;
        }

        HelpTemplateWrapper actionsTemplate = templateWrapperFactory.createWrapper(GROUP_TEMPLATE);

        File actionsHtml = wd.createFile(folderType, true, currentHelp, ACTIONS_HTML);

        try (Writer helpWriter = new BufferedWriter(new FileWriter(actionsHtml))) {

            actionsTemplate.setData("title", HelpEntityName.ACTIONS.getValue());
            actionsTemplate.setData("count", validActions.size());

            actionsTemplate.write(helpWriter);

        }

        HelpJsonContainer actionsJsonFileContainer = new HelpJsonContainer(HelpEntityName.ACTIONS, ACTIONS_HTML, HelpJsonContainer.ACTION_ICON,
                HelpEntityType.NAMED, new ArrayList<HelpJsonContainer>());

        for (ActionClass action : validActions) {
            buildAction(action, actionsJsonFileContainer);
        }

        sortActionsByTitle(actionsJsonFileContainer);

        jsonParent.addChild(actionsJsonFileContainer);
    }

    private void buildAction(ActionClass action, HelpJsonContainer jsonParent) throws IOException, TemplateException, SailfishURIException {

        String actionAlias = action.getClassAliases().get(0);

        SailfishURI uri = new SailfishURI(currentAlias, actionAlias);

        HelpTemplateWrapper actionTemplate = templateWrapperFactory.createWrapper(ACTION_TEMPLATE);

        File actionHtml = wd.createFile(folderType, true, currentHelp, ACTIONS, actionAlias + HTML);

        try (Writer helpWriter = new BufferedWriter(new FileWriter(actionHtml))) {

            actionTemplate.setData("description", action.getClassDescription());
            actionTemplate.setData("aliases", StringUtils.join(action.getClassAliases(), ","));
            actionTemplate.setData("uri", uri);
            actionTemplate.setData("methodsCount", action.getClassMethods().size());

            actionTemplate.write(helpWriter);
        }

        HelpJsonContainer actionJsonNode = new HelpJsonContainer(ACTIONS + File.separator + actionAlias + HTML, new ArrayList<HelpJsonContainer>());

        File actionJson = wd.createFile(folderType, true, currentHelp, ACTIONS, actionAlias + JSON);

        HelpJsonContainer actionJsonFileContainer = new HelpJsonContainer(actionAlias, ACTIONS + File.separator + actionAlias + JSON,
                HelpJsonContainer.ACTION_ICON, HelpEntityType.ACTION);

        jsonParent.addChild(actionJsonFileContainer);

        fmSet = new HashSet<>();

        Map<String, HelpJsonContainer> fmMap = new HashMap<>();

        for (UtilityClass util : action.getUtlityClasses()) {
            buildUtil(util, actionJsonNode, null, false);
        }

        for (Method method : action.getClassMethods()) {
            String parameters = getMethodParameters(method);
            String fullMethodName = getFullMethodName(method, parameters);

            HelpTemplateWrapper methodTemplate = templateWrapperFactory.createWrapper(ACTION_METHOD_TEMPLATE);

            File methodHtml = wd.createFile(folderType, true, currentHelp, ACTIONS, actionAlias, METHODS, fullMethodName + HTML);

            Description description = method.getAnnotation(Description.class);
            CommonColumns commonColumns = method.getAnnotation(CommonColumns.class);
            CustomColumns customColumns = method.getAnnotation(CustomColumns.class);

            try (Writer helpWriter = new BufferedWriter(new FileWriter(methodHtml))) {
                methodTemplate.setData("uri", uri.merge(SailfishURI.parse(method.getName())));
                methodTemplate.setData("returnType", method.getReturnType().getSimpleName());
                methodTemplate.setData("name", method.getName());

                methodTemplate.setData("parameters", parameters);

                if (description != null) {
                    methodTemplate.setData("description", description.value());
                }

                if (commonColumns != null) {
                    methodTemplate.setData("commonColumns", commonColumns.value());
                }

                if (customColumns != null) {
                    methodTemplate.setData("customColumns", customColumns.value());
                }

                methodTemplate.write(helpWriter);
            }

            PluginHelpContainer<Method> methodContainer = new PluginHelpContainer<>(method);

            HelpJsonContainer methodJsonNode;

            List<AbstrFieldMess> fields = fillFieldsAndMessages(method, action);

            if (!fields.isEmpty()) {
                methodJsonNode = new HelpJsonContainer(fullMethodName,
                        ACTIONS + File.separator + actionAlias + File.separator + METHODS + File.separator + fullMethodName + HTML,
                        HelpJsonContainer.METHOD_ICON, HelpEntityType.METHOD, new ArrayList<HelpJsonContainer>());

                buildFieldsAndMessages(methodContainer, fields, methodJsonNode, fmMap, actionAlias);
            } else {
                methodJsonNode = new HelpJsonContainer(fullMethodName,
                        ACTIONS + File.separator + actionAlias + File.separator + METHODS + File.separator + fullMethodName + HTML,
                        HelpJsonContainer.METHOD_ICON, HelpEntityType.METHOD);
            }

            actionJsonNode.addChild(methodJsonNode);

        }

        writeJson(actionJson, actionJsonNode);

    }

    private void buildUtil(UtilityClass util, HelpJsonContainer jsonParent, SailfishURI utilURI, boolean rebuild)
            throws IOException, TemplateException, SailfishURIException {

        if (utilSet.add(util.getClassName())) {

            if (utilURI == null) {
                utilURI = new SailfishURI(currentAlias, util.getClassAliases().get(0), null);
            }

            String utilAlias = utilURI.getClassAlias();

            HelpJsonContainer utilJsonFileContainer = new URIJsonContainer(utilAlias, UTILS + File.separator + utilAlias + JSON,
                    HelpJsonContainer.UTIL_ICON, HelpEntityType.UTIL, utilURI);

            jsonParent.addChild(utilJsonFileContainer);

            utilMap.put(util.getClassName(), utilJsonFileContainer);

            if (rebuild) {
                return;
            }

            HelpTemplateWrapper utilTemplate = templateWrapperFactory.createWrapper(UTIL_TEMPLATE);

            File utilHtml = wd.createFile(folderType, true, currentHelp, UTILS, utilAlias + HTML);

            try (Writer helpWriter = new BufferedWriter(new FileWriter(utilHtml))) {

                utilTemplate.setData("description", util.getClassDescription());
                utilTemplate.setData("aliases", StringUtils.join(util.getClassAliases(), ","));
                utilTemplate.setData("uri", utilURI);
                utilTemplate.setData("methodsCount", util.getClassMethods().size());

                utilTemplate.write(helpWriter);
            }

            HelpJsonContainer utilJsonNode = new HelpJsonContainer(UTILS + File.separator + utilAlias + HTML, new ArrayList<HelpJsonContainer>());

            File utilJson = wd.createFile(folderType, true, currentHelp, UTILS, utilAlias + JSON);

            for (Method method : util.getClassMethods()) {

                HelpTemplateWrapper methodTemplate = templateWrapperFactory.createWrapper(UTIL_METHOD_TEMPLATE);

                Description description = method.getAnnotation(Description.class);

                String parameters = getMethodParameters(method);
                String fullMethodName = getFullMethodName(method, parameters);

                File methodHtml = wd.createFile(folderType, true, currentHelp, UTILS, utilAlias, fullMethodName + HTML);

                try (Writer helpWriter = new BufferedWriter(new FileWriter(methodHtml))) {
                    methodTemplate.setData("uri", utilURI.merge(SailfishURI.parse(method.getName())));
                    methodTemplate.setData("returnType", method.getReturnType().getSimpleName());
                    methodTemplate.setData("name", method.getName());
                    methodTemplate.setData("parameters", parameters);

                    methodTemplate.setData("description", description != null ? description.value() : null);

                    methodTemplate.write(helpWriter);

                }

                HelpJsonContainer methodJsonNode = new MethodJsonContainer(fullMethodName,
                        UTILS + File.separator + utilAlias + File.separator + fullMethodName + HTML, HelpJsonContainer.METHOD_ICON,
                        HelpEntityType.METHOD, true);

                utilJsonNode.addChild(methodJsonNode);
            }

            writeJson(utilJson, utilJsonNode);
        } else {
            jsonParent.addChild(utilMap.get(util.getClassName()));
        }
    }

    private void sortActionsByTitle(HelpJsonContainer root) {
        Collections.sort(root.getChildNodes(), actionComparator);
    }

    private String getFullMethodName(Method method, String parameters) {

        StringBuilder fullMethodName = new StringBuilder();

        fullMethodName.append(method.getName());

        fullMethodName.append("(");

        fullMethodName.append(parameters);

        fullMethodName.append(")");
        return fullMethodName.toString();
    }

    private String getMethodParameters(Method method) {

        Class<?>[] paramTypes = method.getParameterTypes();

        if (paramTypes.length == 0) {
            return "";
        }

        StringBuilder paramSB = new StringBuilder();

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> param = paramTypes[i];
            paramSB.append(param.getSimpleName());
            if ((i + 1 == paramTypes.length) && param.isArray() && method.isVarArgs()) {
                paramSB.replace(paramSB.length() - 2, paramSB.length(), "...");
            }

            if (i + 1 < paramTypes.length) {
                paramSB.append(",");
            }
        }

        return paramSB.toString();
    }

    private List<AbstrFieldMess> fillFieldsAndMessages(Method method, ActionClass actionClass) {

        List<AbstrFieldMess> fields = new ArrayList<>();

        for (Method actionMethod : actionClass.getClassMethods()) {
            if (method.getName().equals(actionMethod.getName()) && actionMethod.getParameterTypes().length == 2) {
                FieldsDescriber.describeFields(fields, actionMethod.getParameterTypes()[1]);
                break;
            }
        }
        return fields;
    }

    private void buildFieldsAndMessages(PluginHelpContainer<?> parentObject, List<AbstrFieldMess> fields, HelpJsonContainer jsonParent,
            Map<String, HelpJsonContainer> fmMap, String actionName) throws IOException, TemplateException {

        if ((parentObject.getContent() instanceof Method) || (parentObject.getContent() instanceof Message)) {

            List<AbstrFieldMess> collection = parentObject.getContent() instanceof Method ?
                    fields :
                    ((Message) parentObject.getContent()).getFieldOrMessage();

            if (collection != null) {

                for (AbstrFieldMess abstrFM : collection) {

                    if (fmSet.add(abstrFM.getName())) {

                        if (abstrFM instanceof Field) {

                            Field field = ((Field) abstrFM);

                            String fieldName = field.getName();

                            File fieldHtml = wd.createFile(folderType, true, currentHelp, ACTIONS, actionName, FIELDS, fieldName + HTML);

                            HelpTemplateWrapper fieldTemplate = templateWrapperFactory.createWrapper(FIELD_TEMPLATE);

                            try (Writer helpWriter = new BufferedWriter(new FileWriter(fieldHtml))) {

                                fieldTemplate.setData("name", fieldName);
                                fieldTemplate.setData("tag", field.getTag());
                                fieldTemplate.setData("type", field.getType());
                                fieldTemplate.setData("valValues", field.getValues());
                                fieldTemplate.write(helpWriter);

                                HelpJsonContainer fieldJsonNode = new FieldJsonContainer(fieldName,
                                        ACTIONS + File.separator + actionName + File.separator + FIELDS + File.separator + fieldName + HTML,
                                        HelpJsonContainer.FIELD_ICON, HelpEntityType.FIELD, field.getType());

                                jsonParent.addChild(fieldJsonNode);

                                fmMap.put(fieldName, fieldJsonNode);

                            }

                        } else {

                            Message message = ((Message) abstrFM);

                            String messageName = message.getName();

                            File messageHtml = wd.createFile(folderType, true, currentHelp, ACTIONS, actionName, MESSAGES, messageName + HTML);

                            HelpTemplateWrapper messageTemplate = templateWrapperFactory.createWrapper(MESSAGE_TEMPLATE);

                            try (Writer helpWriter = new BufferedWriter(new FileWriter(messageHtml))) {

                                messageTemplate.setData("name", messageName);
                                messageTemplate.setData("type", message.getType());

                                boolean hasTag = false;
                                for (AbstrFieldMess abstFM : message.getFieldOrMessage()) {
                                    if (abstFM.getTag() != null) {
                                        hasTag = true;
                                        break;
                                    }
                                }

                                messageTemplate.setData("hasTag", hasTag);
                                messageTemplate.setData("fieldMess", message.getFieldOrMessage());
                                messageTemplate.write(helpWriter);

                                PluginHelpContainer<AbstrFieldMess> messageContainer = new PluginHelpContainer<>(abstrFM);

                                HelpJsonContainer messageJsonNode = new MessageJsonContainer(abstrFM.getName(),
                                        ACTIONS + File.separator + actionName + File.separator + MESSAGES + File.separator + messageName + HTML,
                                        HelpJsonContainer.MESSAGE_ICON, HelpEntityType.MESSAGE, new ArrayList<HelpJsonContainer>());

                                jsonParent.addChild(messageJsonNode);

                                buildFieldsAndMessages(messageContainer, null, messageJsonNode, fmMap, actionName);

                                fmMap.put(messageName, messageJsonNode);
                            }
                        }
                    } else {
                        jsonParent.addChild(fmMap.get(abstrFM.getName()));
                    }
                }
            }
        }
    }

    private void buildDictionaries(IVersion plugin, HelpJsonContainer jsonParent) throws IOException, TemplateException, SailfishURIException {

        Set<SailfishURI> dictionaryURIs = dictionaryManager.getDictionaryURIs(plugin.getAlias());

        if (dictionaryURIs == null || dictionaryURIs.isEmpty()) {
            return;
        }

        List<SailfishURI> dictionaryURIsList = new ArrayList<>(dictionaryURIs);
        Collections.sort(dictionaryURIsList);

        File dictionariesHtml = wd.createFile(folderType, true, currentHelp, DICTIONARIES_HTML);
        try (Writer helpWriter = new BufferedWriter(new FileWriter(dictionariesHtml))) {

            HelpTemplateWrapper dictTemplate = templateWrapperFactory.createWrapper(GROUP_TEMPLATE);
            dictTemplate.setData("title", HelpEntityName.DICTIONARIES.getValue());
            dictTemplate.setData("count", dictionaryURIsList.size());
            dictTemplate.write(helpWriter);

        }

        HelpJsonContainer dictionariesJsonFileContainer = new HelpJsonContainer(HelpEntityName.DICTIONARIES, DICTIONARIES_HTML,
                HelpJsonContainer.DICTIONARY_ICON, HelpEntityType.NAMED, new ArrayList<HelpJsonContainer>());

        jsonParent.addChild(dictionariesJsonFileContainer);

        for (SailfishURI dictionaryURI : dictionaryURIsList) {
            buildDictionary(dictionaryURI, dictionariesJsonFileContainer);
        }

    }

    private HelpJsonContainer buildDictionary(SailfishURI dictionaryURI, HelpJsonContainer jsonParent)
            throws IOException, TemplateException, SailfishURIException {

        String dictionaryResourceName = dictionaryURI.getResourceName();

        File dictionaryHtml = wd.createFile(folderType, true, currentHelp, DICTIONARIES, dictionaryResourceName + HTML);

        try (Writer helpWriter = new BufferedWriter(new FileWriter(dictionaryHtml))) {
            SailfishURI validURI = new SailfishURI(currentAlias, null, dictionaryResourceName);
            HelpTemplateWrapper dictTemplate = templateWrapperFactory.createWrapper(DICTIONARY_TEMPLATE);
            dictTemplate.setData("uri", validURI);
            dictTemplate.write(helpWriter);

            HelpJsonContainer dictionaryJsonNode = new HelpJsonContainer(DICTIONARIES + File.separator + dictionaryResourceName + HTML,
                    new ArrayList<HelpJsonContainer>());

            File dictionaryJson = wd.createFile(folderType, true, currentHelp, DICTIONARIES, dictionaryResourceName + JSON);

            HelpJsonContainer dictionaryJsonFileContainer = new URIJsonContainer(dictionaryResourceName,
                    DICTIONARIES + File.separator + dictionaryResourceName + JSON, HelpJsonContainer.DICTIONARY_ICON, HelpEntityType.DICTIONARY,
                    validURI);

            if (jsonParent != null) {
                jsonParent.addChild(dictionaryJsonFileContainer);
            }

            buildMessagesForDictionary(dictionaryURI, dictionaryJsonNode, false);

            writeJson(dictionaryJson, dictionaryJsonNode);

            return dictionaryJsonFileContainer;
        }

    }

    private void buildMessagesForDictionary(SailfishURI dictionaryURI, HelpJsonContainer jsonParent, boolean rebuild)
            throws IOException, TemplateException, SailfishURIException {

        String dictionaryResourceName = dictionaryURI.getResourceName();

        try {

            IDictionaryStructure dictionary = dictionaryManager.getDictionary(dictionaryURI);

            DictionarySettings dictSettings = dictionaryManager.getSettings(dictionaryURI);

            for (SailfishURI utilityClassURI : dictSettings.getUtilityClassURIs()) {

                UtilityClass utilityClass = utilityManager.getUtilityClassByURI(utilityClassURI);

                buildUtil(utilityClass, jsonParent, new SailfishURI(currentAlias, utilityClassURI.getClassAlias(), utilityClassURI.getResourceName()), rebuild);

            }

            List<IMessageStructure> messageStructures = new ArrayList<>(dictionary.getMessageStructures());

            Collections.sort(messageStructures, messageComparator);

            messageStructureSet = new HashSet<>();

            Map<IFieldStructure, String> fieldsPathMap = new HashMap<>();
            Multimap<Integer, IFieldStructure> fieldsMap = HashMultimap.create();
            Map<String, HelpJsonContainer> messageJsonNodes = new HashMap<>();
            for (IMessageStructure messageStructure : messageStructures) {

                String messageName = messageStructure.getName();

                if (messageStructureSet.add(messageName)) {

                    HelpTemplateWrapper messageTemplate = templateWrapperFactory.createWrapper(MESSAGE_STRUCTURE_TEMPLATE);

                    File messageHtml = wd
                            .createFile(folderType, true, currentHelp, DICTIONARIES, dictionaryResourceName, MESSAGES, messageName + HTML);

                    try (Writer helpWriter = new BufferedWriter(new FileWriter(messageHtml))) {

                        messageTemplate.setData("name", messageName);
                        messageTemplate.setData("namespace", messageStructure.getNamespace());
                        messageTemplate.setData("type", messageStructure.getStructureType());
                        messageTemplate.setData("attributes", messageStructure.getAttributes());

                        messageTemplate.write(helpWriter);

                        HelpJsonContainer messageStructureJsonNode = new MessageJsonContainer(messageName,
                                DICTIONARIES + File.separator + dictionaryResourceName + File.separator + MESSAGES + File.separator + messageName
                                        + HTML, HelpJsonContainer.MESSAGE_STRUCTURE_ICON, HelpEntityType.MESSAGE, new ArrayList<HelpJsonContainer>());

                        jsonParent.addChild(messageStructureJsonNode);

                        messageJsonNodes.put(messageName, messageStructureJsonNode);

                        buildMessageFields(messageStructure, messageStructureJsonNode, fieldsPathMap, dictionaryResourceName, fieldsMap);

                    }

                } else {
                    jsonParent.addChild(messageJsonNodes.get(messageName));
                }
            }
        } catch (SailfishURIException e) {
            if (!rebuild) {
                throw e;
            }
            logger.error(e.getMessage(), e);

            File errorHtml = wd.createFile(folderType, true, currentHelp, DICTIONARIES, dictionaryResourceName, ERROR_HTML);

            try (Writer helpWriter = new BufferedWriter(new FileWriter(errorHtml))) {

                String errorMessage = e.getMessage() + (e.getCause() == null ? "" : " (" + e.getCause().getMessage() + ")");
                HelpTemplateWrapper errorTemplate = templateWrapperFactory.createWrapper(ERROR_TEMPLATE);
                errorTemplate.setData("error", errorMessage);
                errorTemplate.write(helpWriter);
                File errorJson = wd.createFile(folderType, true, currentHelp, DICTIONARIES, dictionaryResourceName, ERROR_JSON);

                HelpJsonContainer errorJsonNode = new HelpJsonContainer("error",
                        DICTIONARIES + File.separator + dictionaryResourceName + File.separator + ERROR_HTML, null, HelpEntityType.NAMED);

                HelpJsonContainer errorJsonFileContainer = new HelpJsonContainer(errorJson.getPath(), null);
                jsonParent.addChild(errorJsonFileContainer);

                writeJson(errorJson, errorJsonNode);

            }
        }
    }

    private void buildMessageFields(IFieldStructure fields, HelpJsonContainer jsonParent, Map<IFieldStructure, String> fieldsPathMap, String dictionaryName, Multimap<Integer, IFieldStructure> fieldsMap)
            throws IOException, TemplateException {
        if (fields == null || fields.getFields() == null) {
            return;
        }

        for (IFieldStructure field : fields.getFields()) {
            String fieldName = field.getName();
            boolean hasChildren = field.isCollection() || (field.isComplex());
            HelpEntityType fieldNodeType = hasChildren ? HelpEntityType.MESSAGE : HelpEntityType.FIELD;
            HelpJsonContainer fieldJsonNode;
            Integer customHash = HelpUtil.getHashCode(field);
            IFieldStructure equalField = HelpUtil.getEqualFieldStructure(fieldsMap.get(customHash), field);

            if(equalField == null) {
                equalField = field;

                HelpTemplateWrapper fieldTemplate = templateWrapperFactory.createWrapper(FIELD_STRUCTURE_TEMPLATE);

                File fieldHtml = wd.createFile(folderType, true, currentHelp, DICTIONARIES, dictionaryName, FIELDS, fieldName + field.hashCode() + HTML);

                try (Writer helpWriter = new BufferedWriter(new FileWriter(fieldHtml))) {

                    fieldTemplate.setData("name", fieldName);
                    fieldTemplate.setData("description", field.getDescription());
                    fieldTemplate.setData("isComplex", field.isComplex());
                    fieldTemplate.setData("isCollection", field.isCollection());
                    fieldTemplate.setData("isRequired", field.isRequired());
                    fieldTemplate.setData("referenceName", field.getReferenceName());

                    if (!field.isComplex()) {
                        fieldTemplate.setData("type", getJavaTypeLabel(field.getJavaType()));
                    }

                    fieldTemplate.setData("attributes", field.getAttributes());

                    if (!fieldNodeType.equals(HelpEntityType.MESSAGE)) {
                        fieldTemplate.setData("validValues", field.getValues());
                    }

                    fieldTemplate.write(helpWriter);

                    fieldsPathMap.put(field,
                            DICTIONARIES + File.separator + dictionaryName + File.separator + FIELDS + File.separator + fieldName + field.hashCode() + HTML);
                    fieldsMap.put(customHash, field);
                }
            }
            if (field.isComplex()) {
                fieldJsonNode = new MessageJsonContainer(fieldName, fieldsPathMap.get(equalField), HelpJsonContainer.MESSAGE_ICON, fieldNodeType,
                        new ArrayList<HelpJsonContainer>());
                buildMessageFields(field, fieldJsonNode, fieldsPathMap, dictionaryName, fieldsMap);

            } else {
                fieldJsonNode = new FieldJsonContainer(fieldName, fieldsPathMap.get(equalField), HelpJsonContainer.FIELD_STRUCTURE_ICON, fieldNodeType,
                        getJavaTypeLabel(field.getJavaType()));
            }

            jsonParent.addChild(fieldJsonNode);
        }

    }

    private void buildServices(IVersion plugin, HelpJsonContainer jsonParent) throws IOException, TemplateException, SailfishURIException {

        List<StaticServiceDescription> services = staticServiceManager.getStaticServicesDescriptions();
        List<StaticServiceDescription> validServices = new ArrayList<>();

        for (StaticServiceDescription service : services) {
            if(service.getVersion().equals(plugin)) {
                validServices.add(service);
            }
        }

        if (validServices.isEmpty()) {
            return;
        }

        File servicesHtml = wd.createFile(folderType, true, currentHelp, SERVICES_HTML);

        HelpJsonContainer servicesJsonFileContainer = new HelpJsonContainer("Services", SERVICES_HTML, HelpJsonContainer.SERVICE_ICON,
                HelpEntityType.NAMED, new ArrayList<HelpJsonContainer>());
        jsonParent.addChild(servicesJsonFileContainer);

        try (Writer helpWriter = new BufferedWriter(new FileWriter(servicesHtml))) {

            HelpTemplateWrapper serviceTemplate = templateWrapperFactory.createWrapper(GROUP_TEMPLATE);
            serviceTemplate.setData("title", HelpEntityName.SERVICES.getValue());
            serviceTemplate.setData("count", validServices.size());
            serviceTemplate.write(helpWriter);

        }

        for (StaticServiceDescription service : validServices) {

            String resourceName = service.getURI().getResourceName();

            HelpTemplateWrapper serviceTemplate = templateWrapperFactory.createWrapper(SERVICE_TEMPLATE);

            File serviceHtml = wd.createFile(folderType, true, currentHelp, SERVICES, resourceName + HTML);

            HelpJsonContainer serviceJsonNode = new HelpJsonContainer(resourceName, SERVICES + File.separator + resourceName + HTML,
                    HelpJsonContainer.SERVICE_ICON, HelpEntityType.COMPONENT);

            servicesJsonFileContainer.addChild(serviceJsonNode);

            try (Writer helpWriter = new BufferedWriter(new FileWriter(serviceHtml))) {

                serviceTemplate.setData("description", service.getDescription());
                serviceTemplate.setData("uri", new SailfishURI(currentAlias, service.getURI().getClassAlias(), resourceName));

                if (service.getDictionaryURI() != null) {
                    serviceTemplate.setData("dictionaryAlias", service.getDictionaryURI().getResourceName());
                }

                serviceTemplate.setData("validatorFactoryName", service.getDictionaryValidatorFactoryName());

                serviceTemplate.write(helpWriter);

            }

        }

    }

    private void buildLanguages(IVersion plugin, HelpJsonContainer jsonParent) throws IOException, TemplateException, SailfishURIException {

        Set<ILanguageFactory> languages = languageManager.getOrigLanguageFactoriesByPlugin(plugin);

        if (languages.isEmpty()) {
            return;
        }

        File languagesHtml = wd.createFile(folderType, true, currentHelp, LANGUAGES_HTML);

        HelpJsonContainer languagesJsonFileContainer = new HelpJsonContainer(HelpEntityName.LANGUAGES, LANGUAGES_HTML,
                HelpJsonContainer.LANGUAGE_ICON, HelpEntityType.NAMED, new ArrayList<HelpJsonContainer>());

        jsonParent.addChild(languagesJsonFileContainer);

        try (Writer helWriter = new BufferedWriter(new FileWriter(languagesHtml))) {

            HelpTemplateWrapper languagesTemplate = templateWrapperFactory.createWrapper(GROUP_TEMPLATE);
            languagesTemplate.setData("title", HelpEntityName.LANGUAGES.getValue());
            languagesTemplate.setData("count", languages.size());
            languagesTemplate.write(helWriter);

        }

        for (ILanguageFactory language : languages) {
            String languageName = language.getName();

            HelpTemplateWrapper languageTemplate = templateWrapperFactory.createWrapper(LANGUAGE_TEMPLATE);
            File languageFile = wd.createFile(folderType, true, currentHelp, LANGUAGES, languageName + HTML);

            HelpJsonContainer languageJsonNode = new HelpJsonContainer(languageName, LANGUAGES + File.separator + languageName + HTML,
                    HelpJsonContainer.LANGUAGE_ICON, HelpEntityType.NAMED);

            languagesJsonFileContainer.addChild(languageJsonNode);

            try (Writer helpWriter = new BufferedWriter(new FileWriter(languageFile))) {

                languageTemplate.setData("name", languageName);
                Description description = language.getClass().getAnnotation(Description.class);
                if (description != null) {
                    languageTemplate.setData("description", description.value());
                }
                languageTemplate.write(helpWriter);

            }
        }
    }

    private void buildValidators(IVersion plugin, HelpJsonContainer jsonParent) throws IOException, TemplateException {
        List<IValidator> validators = validatorLoader.getPluginToValidatorsMap().get(plugin);

        if (validators.isEmpty()) {
            return;
        }

        File validatorsFile = wd.createFile(folderType, true, currentHelp, VALIDATORS_HTML);

        HelpJsonContainer validatorsJsonNode = new HelpJsonContainer(HelpEntityName.VALIDATORS, VALIDATORS + File.separator + validators + HTML,
                HelpJsonContainer.VALIDATOR_ICON, HelpEntityType.NAMED, new ArrayList<HelpJsonContainer>());

        jsonParent.addChild(validatorsJsonNode);

        try (Writer helWriter = new BufferedWriter(new FileWriter(validatorsFile))) {

            HelpTemplateWrapper validatorsTemplate = templateWrapperFactory.createWrapper(GROUP_TEMPLATE);
            validatorsTemplate.setData("title", HelpEntityName.VALIDATORS.getValue());
            validatorsTemplate.setData("count", validators.size());
            validatorsTemplate.write(helWriter);

        }

        HelpTemplateWrapper validatorTemplate = templateWrapperFactory.createWrapper(VALIDATOR_TEMPLATE);
        for (IValidator validator : validators) {
            String validatorName = validator.getName();

            File validatorFile = wd.createFile(folderType, true, currentHelp, VALIDATORS, validatorName + HTML);

            HelpJsonContainer validatorJsonNode = new HelpJsonContainer(validatorName, VALIDATORS + File.separator + validatorName + HTML,
                    HelpJsonContainer.VALIDATOR_ICON, HelpEntityType.COMPONENT);

            validatorsJsonNode.addChild(validatorJsonNode);
            try (Writer helpWriter = new BufferedWriter(new FileWriter(validatorFile))) {

                validatorTemplate.setData("name", validatorName);
                validatorTemplate.write(helpWriter);

            }
        }

    }

    private void buildPreprocessors(IVersion plugin, HelpJsonContainer jsonParent) throws IOException, TemplateException {

        List<PreprocessorDefinition> preprocessors = preprocessorLoader.getPluginToPreprocessorsMap().get(plugin);

        if (preprocessors.isEmpty()) {
            return;
        }

        File preprocessorsFile = wd.createFile(folderType, true, currentHelp, PREPROCESSORS_HTML);

        HelpJsonContainer preprocessorsJsonFileContainer = new HelpJsonContainer(HelpEntityName.PREPROCESSORS, PREPROCESSORS_HTML,
                HelpJsonContainer.PREPROCESSOR_ICON, HelpEntityType.NAMED, new ArrayList<HelpJsonContainer>());
        jsonParent.addChild(preprocessorsJsonFileContainer);

        try (Writer helWriter = new BufferedWriter(new FileWriter(preprocessorsFile))) {
            HelpTemplateWrapper preprocessorsTemplate = templateWrapperFactory.createWrapper(GROUP_TEMPLATE);
            preprocessorsTemplate.setData("title", HelpEntityName.PREPROCESSORS.getValue());
            preprocessorsTemplate.setData("count", preprocessors.size());
            preprocessorsTemplate.write(helWriter);

        }

        for (PreprocessorDefinition preprocessor : preprocessors) {
            String preprocessorTitle = preprocessor.getTitle();

            File preprocessorHtml = wd.createFile(folderType, true, currentHelp, PREPROCESSORS, preprocessorTitle + HTML);
            try (Writer helpWriter = new BufferedWriter(new FileWriter(preprocessorHtml))) {

                HelpTemplateWrapper preprocessorTemplate = templateWrapperFactory.createWrapper(PREPROCESSOR_TEMPLATE);
                preprocessorTemplate.setData("title", preprocessorTitle);
                preprocessorTemplate.write(helpWriter);

                HelpJsonContainer preprocessorJsonNode = new HelpJsonContainer(preprocessorTitle,
                        PREPROCESSORS + File.separator + preprocessorTitle + HTML, HelpJsonContainer.PREPROCESSOR_ICON, HelpEntityType.COMPONENT);

                preprocessorsJsonFileContainer.addChild(preprocessorJsonNode);

            }
        }

    }

    private void buildMatrixProviders(IVersion plugin, HelpJsonContainer jsonParent)
            throws IOException, TemplateException, SailfishURIException {

        Set<IMatrixProviderFactory> providers = matrixProviderHolder.getMatrixProviderFactoriesByPlugin(plugin);

        if (providers.isEmpty()) {
            return;
        }

        File providersHtml = wd.createFile(folderType, true, currentHelp, PROVIDERS_HTML);

        HelpJsonContainer providersJsonNode = new HelpJsonContainer(HelpEntityName.PROVIDERS, PROVIDERS_HTML, HelpJsonContainer.PROVIDER_ICON,
                HelpEntityType.NAMED, new ArrayList<HelpJsonContainer>());

        jsonParent.addChild(providersJsonNode);

        try (Writer helpWriter = new BufferedWriter(new FileWriter(providersHtml))) {

            HelpTemplateWrapper providersTemplate = templateWrapperFactory.createWrapper(GROUP_TEMPLATE);
            providersTemplate.setData("title", HelpEntityName.PROVIDERS.getValue());
            providersTemplate.setData("count", providers.size());
            providersTemplate.write(helpWriter);

        }

        HelpTemplateWrapper preprocessorTemplate = templateWrapperFactory.createWrapper(PROVIDER_TEMPLATE);

        for (IMatrixProviderFactory provider : providers) {
            String providerAlias = provider.getAlias();

            File providerFile = wd.createFile(folderType, true, currentHelp, PROVIDERS, providerAlias + HTML);
            try (Writer helpWriter = new BufferedWriter(new FileWriter(providerFile))) {

                preprocessorTemplate.setData("humanReadableName", provider.getHumanReadableName());
                preprocessorTemplate.setData("notes", provider.getNotes());
                preprocessorTemplate.setData("uri", new SailfishURI(currentAlias, null, providerAlias));
                preprocessorTemplate.write(helpWriter);

                HelpJsonContainer providerJsonNode = new HelpJsonContainer(providerAlias, PROVIDERS + File.separator + providerAlias + HTML,
                        HelpJsonContainer.PROVIDER_ICON, HelpEntityType.COMPONENT);

                providersJsonNode.addChild(providerJsonNode);
            }
        }
    }

    private void writeJson(File jsonFile, HelpJsonContainer jsonNode) throws IOException {
        try (Writer jsonWriter = new BufferedWriter(new FileWriter(jsonFile))) {
            jsonWriter.write(START_JSON);
            String jsonString = mapper.writeValueAsString(jsonNode);
            jsonWriter.write(jsonString + END_JSON);
        }
    }

    public static String fileToJsonString(File file) throws IOException {

        return StringUtils.substringBetween(FileUtils.readFileToString(file), START_JSON, END_JSON);

    }

    private String getJavaTypeLabel(JavaType type) {
        return StringUtils.substringAfterLast(type.value(), ".");

    }

    private void copyResources() throws IOException {
        for (String resource : PAGE_RESOURCES) {
            File destinationFile = wd.createFile(folderType, true, currentHelp, RESOURCES, resource);
            FileUtils.copyURLToFile(getClass().getResource(resource), destinationFile);
        }
    }

    public HelpJsonContainer buildNewDictionary(SailfishURI dictionaryURI) throws IOException {
        try {
            folderType = FolderType.ROOT;
            currentHelp = HELP;

            utilSet = new HashSet<>();

            utilMap = new HashMap<>();

            currentAlias = dictionaryURI.getPluginAlias();

            HelpJsonContainer rootContainer = getRootContainer();

            HelpJsonContainer dictionariesRoot = null;

            for (HelpJsonContainer child : rootContainer.getChildNodes()) {
                if (child.getName().equals(HelpEntityName.DICTIONARIES.getValue())) {
                    dictionariesRoot = child;
                    break;
                }
            }

            HelpJsonContainer newDictionary = buildDictionary(dictionaryURI, dictionariesRoot);

            File dictionariesFile = wd.createFile(folderType, true, currentHelp, DICTIONARIES_HTML);

            try (Writer helpWriter = new BufferedWriter(new FileWriter(dictionariesFile))) {

                HelpTemplateWrapper dictTemplate = templateWrapperFactory.createWrapper(GROUP_TEMPLATE);
                dictTemplate.setData("title", HelpEntityName.DICTIONARIES.getValue());
                dictTemplate.setData("count", dictionariesRoot.getChildNodes().size());
                dictTemplate.write(helpWriter);

            }

            File rootFile = wd.createFile(folderType, true, currentHelp, ROOT);

            writeJson(rootFile, rootContainer);

            return newDictionary;

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public void rebuildDictionary(SailfishURI dictionaryURI, String pluginName) throws IOException, TemplateException {
        try {

            utilSet = new HashSet<>();
            utilMap = new HashMap<>();

            String dictionaryResourceName = dictionaryURI.getResourceName();

            if(dictionaryURI.getPluginAlias().equals(IVersion.GENERAL)) {
                currentHelp = HELP;
                folderType = FolderType.ROOT;
            } else {
                currentHelp = pluginName + File.separator + HELP;
                folderType = FolderType.PLUGINS;
            }

            HelpJsonContainer dictionaryNode = new HelpJsonContainer(DICTIONARIES + File.separator + dictionaryResourceName + HTML,
                    new ArrayList<HelpJsonContainer>());

            buildMessagesForDictionary(dictionaryURI, dictionaryNode, true);

            writeJson(wd.createFile(folderType, true, currentHelp, DICTIONARIES, dictionaryResourceName + JSON), dictionaryNode);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private HelpJsonContainer getRootContainer() throws IOException {

        String jsonString = fileToJsonString(getRootFile());

        return mapper.readValue(jsonString, HelpJsonContainer.class);
    }

    private File getRootFile() throws FileNotFoundException {
        return wd.getFile(folderType, currentHelp, ROOT);
    }

    public static void main(String[] args) throws IOException, SailfishURIException, TemplateException {
        String alias = args[0];
        String name = args[1];
        String[] workspaceLayers = Arrays.copyOfRange(args, 2, args.length);
        HelpBuilder builder = new HelpBuilder(new CoreVersion(), workspaceLayers);
        builder.build(name, alias);
    }

    private class VersionWrapper implements IVersion {
        private final String alias;
        private final IVersion version;

        public VersionWrapper(String alias, IVersion version) {
            this.alias = alias;
            this.version = version;
        }

        @Override
        public String buildShortVersion() {
            return version.buildShortVersion();
        }

        @Override
        public String buildVersion() {
            return version.buildVersion();
        }

        @Override
        public int getMajor() {
            return version.getMajor();
        }

        @Override
        public int getMinor() {
            return version.getMinor();
        }

        @Override
        public int getMaintenance() {
            return version.getMaintenance();
        }

        @Override
        public int getBuild() {
            return version.getBuild();
        }

        @Override
        public String getAlias() {
            return alias;
        }

        @Override
        public String getBranch() {
            return version.getBranch();
        }

        @Override
        public String getRevision() {
            return version.getRevision();
        }

        @Override
        public int getMinCoreRevision() {
            return version.getMinCoreRevision();
        }

        @Override
        public boolean isGeneral() {
            return version.isGeneral();
        }

        @Override
        public String getArtifactName() {
            return version.getArtifactName();
        }
    }
}
