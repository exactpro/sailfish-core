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
package com.exactpro.sf.bigbutton.importing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.csvreader.CsvReader;
import com.exactpro.sf.bigbutton.library.AbstractLibraryItem;
import com.exactpro.sf.bigbutton.library.BigButtonAction;
import com.exactpro.sf.bigbutton.library.CsvHeader;
import com.exactpro.sf.bigbutton.library.Daemon;
import com.exactpro.sf.bigbutton.library.DaemonList;
import com.exactpro.sf.bigbutton.library.Executor;
import com.exactpro.sf.bigbutton.library.ExecutorList;
import com.exactpro.sf.bigbutton.library.Globals;
import com.exactpro.sf.bigbutton.library.InvalidRowException;
import com.exactpro.sf.bigbutton.library.Library;
import com.exactpro.sf.bigbutton.library.Script;
import com.exactpro.sf.bigbutton.library.ScriptList;
import com.exactpro.sf.bigbutton.library.Service;
import com.exactpro.sf.bigbutton.library.ServiceList;
import com.exactpro.sf.bigbutton.library.ServiceNameNotFoundException;
import com.exactpro.sf.bigbutton.library.SfApiOptions;
import com.exactpro.sf.bigbutton.library.StartMode;
import com.exactpro.sf.bigbutton.library.Tag;
import com.exactpro.sf.bigbutton.library.TagList;
import com.exactpro.sf.bigbutton.util.BigButtonUtil;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

public class CsvLibraryBuilder {

	private static final Logger logger = LoggerFactory.getLogger(CsvLibraryBuilder.class);

	private static final String ITEM_COLUMN_NAME = "item";

    private final IWorkspaceDispatcher workspaceDispatcher;

	private InputStream libraryFile;

	private Library library = new Library();

	private AbstractLibraryItem currentList;

	private LibraryImportResult importResult = new LibraryImportResult();

	private static final Set<String> booleanTrue = new HashSet<>();

	private static final Set<String> booleanFalse = new HashSet<>();

	private Map<Executor, String> executorDaemonMap = new HashMap<>();

	static {

		booleanTrue.add("y");
		booleanTrue.add("t");
		booleanTrue.add("true");
		booleanTrue.add("yes");

		booleanFalse.add("n");
		booleanFalse.add("f");
		booleanFalse.add("false");
		booleanFalse.add("no");

	}

	private ObjectMapper mapper = new ObjectMapper();

	private TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String,String>>() {};

    public CsvLibraryBuilder(InputStream libraryFile, IWorkspaceDispatcher workspaceDispatcher,
                             IStaticServiceManager serviceManager, IDictionaryManager dictionaryManager) {
        this.workspaceDispatcher = workspaceDispatcher;
		this.libraryFile = libraryFile;
	}

	private Map<String, String> rowToMap(String[] headers, CsvReader reader) throws IOException {

		Map<String, String> result = new HashMap<>();

		for(String header : headers) {

			result.put(header.toLowerCase(), reader.get(header));

		}

		return result;

	}

	private void parseCsv() throws IOException {

		CsvReader reader = null;

		try {

			reader = new CsvReader(this.libraryFile, Charset.forName("UTF-8"));

            DocumentBuilder domBuilder;

            try {
                domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new EPSCommonException(e);
            }

			reader.readHeaders();

			String[] headers = reader.getHeaders();

			while(reader.readRecord()) {

				long currentRecordNumber = reader.getCurrentRecord() + 2;

				try {

					Map<String, String> row = rowToMap(headers, reader);

					logger.debug("{}: {}", reader.getCurrentRecord(), row);

                    handleCsvRow(row, currentRecordNumber, domBuilder);

				} catch(Exception e) {

                    this.importResult.getCommonErrors().add(new ImportError(currentRecordNumber, e.getMessage()));

				}

			}

            postParse();

        } finally {

			if(reader != null) {
				reader.close();
			}

		}

	}

    private void postParse() {

        // Transfer options from List to Service
        for (ServiceList serviceList : library.getServiceLists().values()) {
            for (Service service : serviceList.getServices()) {
                StartMode resultStartMode = ObjectUtils.defaultIfNull(service.getStartMode(), StartMode.NONE);
                if (service.getStartMode() == null && serviceList.getStartMode() != null) {
                    resultStartMode = serviceList.getStartMode();
                }
                service.setStartMode(resultStartMode);
            }
        }

        // Check script list
        Set<String> executorNames = library.getExecutors().getExecutors().stream()
                .map(Executor::getName)
                .collect(Collectors.toSet());

        library.getScriptLists().stream()
                .filter(scriptList -> scriptList.getExecutor() != null)
                .filter(scriptList -> !executorNames.contains(scriptList.getExecutor()))
                .forEach(scriptList -> {
                    this.importResult.getCommonErrors().add(new ImportError(scriptList.getLineNumber(),
                            "Unknown executor '" + scriptList.getExecutor() + "' in script list '" + scriptList.getName() + "\'"));
                });
    }

    private String parseLibraryFolder(Map<String, String> row) throws InvalidRowException {

		String libraryFolder = row.get(CsvHeader.Path.getFieldKey());

		if(libraryFolder == null) {
			throw new InvalidRowException(CsvHeader.Path.getFieldKey() + " is missing");
		}

		return libraryFolder;

	}

    private Globals parseGlobals(Map<String, String> row, long lineNumber) throws InvalidRowException {

		Globals result = new Globals();

		String services = row.get(CsvHeader.Services.getFieldKey());

		if(services != null) {

			result.setServiceLists(parseServiceListReferences(services));

		}

		result.setApiOptions( parseApiOptions(row) );

        result.setLineNumber(lineNumber);

		return result;

	}

    private Executor parseExecutor(Map<String, String> row, long lineNumber) throws InvalidRowException {

		Executor result = new Executor();

		String name = row.get(CsvHeader.Name.getFieldKey());
		String path = row.get(CsvHeader.Path.getFieldKey());
		String daemon = row.get(CsvHeader.Daemon.getFieldKey());

		int timeout = 0;
		String timeoutStr = row.get(CsvHeader.Timeout.getFieldKey());
		if (!StringUtils.isEmpty(timeoutStr)) {
			timeout = Integer.parseInt(timeoutStr);
		}
		String services = row.get(CsvHeader.Services.getFieldKey());

		if(name == null || path == null) {
			throw new InvalidRowException(CsvHeader.Name.getFieldKey()
					+ " and "
					+ CsvHeader.Path.getFieldKey()
					+ " are required for Executor");
		}

		result.setName(name);
		result.setPath(path);
		result.setTimeout(timeout);
		result.setServices(parseServiceListReferences(services));

		result.setApiOptions( parseApiOptions(row) );

		if (!StringUtils.isEmpty(daemon)) {
			this.executorDaemonMap.put(result, daemon);
		}

        result.setLineNumber(lineNumber);

		return result;
	}

	private Daemon parseDaemon(Map<String, String> row) throws InvalidRowException {

		Daemon result = new Daemon();

		String name = row.get(CsvHeader.Name.getFieldKey());
		String path = row.get(CsvHeader.Path.getFieldKey());

		int timeout = 0;
		String timeoutStr = row.get(CsvHeader.Timeout.getFieldKey());
		if (!StringUtils.isEmpty(timeoutStr)) {
			timeout = Integer.parseInt(timeoutStr);
		}

		if(name == null || path == null) {
			throw new InvalidRowException(CsvHeader.Name.getFieldKey()
					+ " and "
					+ CsvHeader.Path.getFieldKey()
					+ " are required for Daemon");
		}

		result.setName(name);
		result.setPath(path);
		result.setTimeout(timeout);

		for (Executor executor : this.executorDaemonMap.keySet()) {
			String daemonName = this.executorDaemonMap.get(executor);
			if (daemonName.equals(name)) {
				executor.setDaemon(result);
			}
		}

		return result;
	}

    private ServiceList parseServiceList(Map<String, String> row, long lineNumber) throws InvalidRowException {

		ServiceList result = new ServiceList();

		String name = row.get(CsvHeader.Name.getFieldKey());

		if(name == null) {
			throw new InvalidRowException(CsvHeader.Name.getFieldKey()
					+ " is required for Service List");
		}

		result.setName(name);
        result.setLineNumber(lineNumber);
		result.setStartMode(parseServiceOptions(row));

		return result;

	}

	private Service parseService(Map<String, String> row, long currentRecordNumber,
            DocumentBuilder domBuilder) {

        String path = row.get(CsvHeader.Path.getFieldKey());

        Service result = new Service(path);

        result.setRecordNumber(currentRecordNumber);

		if(path == null) {
            result.setRejectCause(new ImportError(currentRecordNumber, CsvHeader.Path.getFieldKey() + " is required for Service"));
		}

		parseServiceOptions(row, result);

        result.setRejectCause(checkFileExisting(path, currentRecordNumber));

        if (!result.isRejected()) {
            try {
                result.setName(extractServiceName(path, domBuilder));
            } catch (Throwable e) {
                result.setName(FilenameUtils.getBaseName(path));
                result.setRejectCause(new ImportError(currentRecordNumber, e.getMessage()));
            }
        }

		return result;
	}

    private String extractServiceName(String path, DocumentBuilder domBuilder) throws IOException, ServiceNameNotFoundException {
        boolean absent = false;
        try (InputStream fileStream = BigButtonUtil.getStream(library.getRootFolder(), path, workspaceDispatcher)) {
            Document document = domBuilder.parse(fileStream);
            NodeList nodes = document.getElementsByTagName("name");
            if (nodes.getLength() == 1) {
                return nodes.item(0).getTextContent();
            } else if (nodes.getLength() == 0) {
                absent = true;
            }
        } catch (IOException | SAXException e) {
            logger.error("Can not parse [{}] service", path, e);
            throw new IOException("File " + path + " reading error");
        }

        throw new ServiceNameNotFoundException("Service by [" + path + "] path " + (absent ? "doesn't have a name tag" : "have multiple name tags"));
    }

	private void parseServiceOptions(Map<String, String> row, Service service) {

        if(row.containsKey(CsvHeader.StartMode.getFieldKey())) {
           service.setStartMode(parseServiceOptions(row));
        }
	}

	private StartMode parseServiceOptions(Map<String, String> row) {

	    String tmp = row.get(CsvHeader.StartMode.getFieldKey());

	    if (StringUtils.isEmpty(tmp)) {
	        return null;
	    }

	    return StartMode.valueOf(tmp);
	}

	private Set<String> parseServiceListReferences(String services) {

		Set<String> parsedServices = new HashSet<>();

		if(StringUtils.isNotEmpty(services)) {

			String[] splitted = services.split(",");

			for(String serviceRef : splitted) {

				if(!"".equals(serviceRef)) {

					String trimmedRef = serviceRef.trim();

					parsedServices.add(trimmedRef);

				}

			}

			return parsedServices;

		}

		return parsedServices;

	}

    private ScriptList parseScriptList(Map<String, String> row, long currentRecordNumber) throws InvalidRowException {

		String name = row.get(CsvHeader.Name.getFieldKey());

		if(name == null) {
			throw new InvalidRowException(CsvHeader.Name.getFieldKey()
					+ " is required for Script List");
		}

        Set<String> serviceNames = parseServiceListReferences(row.get(CsvHeader.Services.getFieldKey()));

		String priorityString = row.get(CsvHeader.Priority.getFieldKey());

		long priority = 0;
        boolean priorityParsed = true;

		if(StringUtils.isNotEmpty(priorityString)) {

			try {

				priority = Long.parseLong(priorityString);

			} catch(NumberFormatException e) {
                priorityParsed = false;
			}

		}

		String executor = StringUtils.defaultIfBlank(row.get(CsvHeader.Executor.getFieldKey()), null);

        ScriptList result = new ScriptList(name, executor, serviceNames, parseApiOptions(row), priority, currentRecordNumber);

        if (!priorityParsed) {
            result.addRejectCause(new ImportError(currentRecordNumber, "'Priority' value is not parsable to number (long)"));
        }

		return result;

	}

	private Set<Tag> parseTags(String value) {


		if(StringUtils.isEmpty(value)) {
			return Collections.emptySet();
		}

		String[] splitted = value.split(",");

        return Arrays.stream(splitted)
                .map(tag -> StringUtils.trimToEmpty(tag))
                .filter(tag -> StringUtils.isNotEmpty(tag))
                .distinct()
                .map(tag -> new Tag(tag))
                .collect(Collectors.toSet());

	}

	private Map<String, String> parseStaticVariables(String staticVariables) throws InvalidRowException {

	    try {

	        return mapper.readValue(staticVariables, typeRef);

	    } catch(Exception e) {
	    	throw new InvalidRowException("Static variables not parsable");
	    }

	}

	private SfApiOptions parseApiOptions(Map<String, String> row) throws InvalidRowException {

		SfApiOptions result = new SfApiOptions();

		if(row.containsKey(CsvHeader.Range.getFieldKey())) {
			result.setRange((row.get(CsvHeader.Range.getFieldKey())));
		}

		if(row.containsKey(CsvHeader.Language.getFieldKey())) {
			if(StringUtils.isNotEmpty(row.get(CsvHeader.Language.getFieldKey()))) {
				result.setLanguage((row.get(CsvHeader.Language.getFieldKey())));
			}
		}

		if(row.containsKey(CsvHeader.ContinueIfFailed.getFieldKey())) {
			result.setContinueIfFailed(parseBoolean((row.get(CsvHeader.ContinueIfFailed.getFieldKey()))));
		}

		if(row.containsKey(CsvHeader.AutoStart.getFieldKey())) {
			result.setAutoStart(parseBoolean((row.get(CsvHeader.AutoStart.getFieldKey()))));
		}

		if(row.containsKey(CsvHeader.IgnoreAskForContinue.getFieldKey())) {
			result.setIgnoreAskForContinue(parseBoolean((row.get(CsvHeader.IgnoreAskForContinue.getFieldKey()))));
		}

        if(row.containsKey(CsvHeader.RunNetDumper.getFieldKey())){
            result.setRunNetDumper(parseBoolean((row.get(CsvHeader.RunNetDumper.getFieldKey()))));
        }

        if (row.containsKey(CsvHeader.SkipOptional.getFieldKey())) {
            result.setSkipOptional(parseBoolean((row.get(CsvHeader.SkipOptional.getFieldKey()))));
        }

		if(row.containsKey(CsvHeader.Tags.getFieldKey())) {
			result.setTags(parseTags((row.get(CsvHeader.Tags.getFieldKey()))));
		}

		if(row.containsKey(CsvHeader.ExecuteOnFailed.getFieldKey())){
		    result.addOnFailed(parseActions(row.get(CsvHeader.ExecuteOnFailed.getFieldKey())));
        }

        if(row.containsKey(CsvHeader.ExecuteOnPassed.getFieldKey())){
            result.addOnPassed(parseActions(row.get(CsvHeader.ExecuteOnPassed.getFieldKey())));
        }

        if (row.containsKey(CsvHeader.ExecuteOnConditionallyPassed.getFieldKey())) {
            result.addOnCondPassed(parseActions(row.get(CsvHeader.ExecuteOnConditionallyPassed.getFieldKey())));
        }

		if(row.containsKey(CsvHeader.StaticVariables.getFieldKey())) {

			String staticVars = row.get(CsvHeader.StaticVariables.getFieldKey());

			if(StringUtils.isNotEmpty(staticVars)) {
				result.setStaticVariables(parseStaticVariables(staticVars));
			}

		}

		return result;

	}

	private Boolean parseBoolean(String value) throws InvalidRowException {

		if(StringUtils.isEmpty(value)) {
			return null;
		}

		String loweredValue = value.toLowerCase();

		if(booleanFalse.contains(loweredValue)) {
			return false;
		}

		if(booleanTrue.contains(loweredValue)) {
			return true;
		}

		throw new InvalidRowException("Invalid boolean value: " + value);

	}

	private Script parseScript(Map<String, String> row, long currentRecordNumber) throws InvalidRowException {

		Script result = new Script(currentRecordNumber);

		String path = row.get(CsvHeader.Path.getFieldKey());

		if(path == null) {
			throw new InvalidRowException(CsvHeader.Path.getFieldKey()
					+ " is required for Script");
		}

		result.setPath(path);

		result.setShortName( Files.getNameWithoutExtension(path) );

		result.setOriginalApiOptions(parseApiOptions(row));

        result.setRejectCause(checkFileExisting(path, currentRecordNumber));

		return result;

	}

	private Tag parseTag(Map<String, String> row) throws InvalidRowException {

		Tag result = new Tag();

		String name = row.get(CsvHeader.Name.getFieldKey());

		if(name == null) {
			throw new InvalidRowException(CsvHeader.Name.getFieldKey()
					+ " is required for Tag");
		}

		result.setName(name);

		String groupName = row.get(CsvHeader.Group.getFieldKey());

		if(!StringUtils.isEmpty(groupName)) {

			result.setGroup(groupName);

		}

		return result;

	}

	private TagList parseTagList(Map<String, String> row) {

		return new TagList();

	}

	private List<BigButtonAction> parseActions(String actionNames) throws InvalidRowException {
        if(StringUtils.isEmpty(actionNames)){
            return Collections.emptyList();
        }

        String[] splitted = actionNames.split(",");

        try {

            return Arrays.stream(splitted)
                    .map(action -> StringUtils.trimToEmpty(action))
                    .filter(action -> StringUtils.isNotEmpty(action))
                    .distinct()
                    .map(action -> BigButtonAction.parse(action))
                    .collect(Collectors.toList());

        }catch (IllegalArgumentException e){
            throw new InvalidRowException("Invalid bb action: " + actionNames);
        }
    }

	private void handleCsvRow(Map<String, String> row, long currentRecordNumber,
            DocumentBuilder domBuilder)
            throws InvalidRowException, IOException {
        Boolean ignore = StringUtils.isNotBlank(row.get(CsvHeader.Ignore.getFieldKey()));

        if(ignore){
            return;
        }

		String item = row.get(ITEM_COLUMN_NAME);

		if(StringUtils.isEmpty(item)) {
			return;
		}

		switch(item.toLowerCase()) {

			case "library folder":

				this.library.setRootFolder(parseLibraryFolder(row));

				break;

			case "reports folder":

				this.library.setReportsFolder(parseLibraryFolder(row));

				break;

			case "globals":

            this.library.setGlobals(Optional.ofNullable(parseGlobals(row, currentRecordNumber)));

				break;

			case "executor list":

				ExecutorList list = new ExecutorList();

				this.library.setExecutors(list);

				this.currentList = list;

				break;

			case "executor":

            Executor executor = parseExecutor(row, currentRecordNumber);

				this.currentList.addNested(executor);

				this.importResult.incNumExecutors();

				break;

			case "daemon list":

				DaemonList daemonList = new DaemonList();

				this.library.setDaemons(daemonList);

				this.currentList = daemonList;

				break;

			case "daemon":

				Daemon daemon = parseDaemon(row);

				this.currentList.addNested(daemon);

				break;

			case "service list":

            ServiceList sList = parseServiceList(row, currentRecordNumber);

				if(this.library.getServiceLists().containsKey(sList.getName())) {
					throw new InvalidRowException("Service List with name " + sList.getName() + " is already defined");
				}

				this.library.getServiceLists().put(sList.getName(), sList);

				this.currentList = sList;

				break;

			case "service":

            Service service = parseService(row, currentRecordNumber, domBuilder);

            this.currentList.addNested(service);
            this.importResult.incNumServices();

				break;

			case "script list":

            ScriptList scriptList = parseScriptList(row, currentRecordNumber);

				this.library.getScriptLists().add(scriptList);

				this.currentList = scriptList;

				break;

			case "script":

				Script script = parseScript(row, currentRecordNumber);

				this.currentList.addNested(script);

				this.importResult.incNumScripts();

                if (script.isRejected()) {
                    ScriptList current = (ScriptList) this.currentList;
                    current.addRejectCause(script.getRejectCause());
                }

				break;

			case "tag list":

				TagList tList = parseTagList(row);

				if(this.library.getTagList() != null) {
					throw new InvalidRowException("Tag List is already defined");
				}

				this.library.setTagList(tList);

				this.currentList = tList;

				break;

			case "tag":

				Tag tag = parseTag(row);

				this.currentList.addNested(tag);

				break;

			default:
				throw new InvalidRowException("Unknown 'item' encountered: " + item);

		}

	}

    private void doPostImportChecks() throws FileNotFoundException, WorkspaceSecurityException {

        checkServiceLists();

        if (this.library.getGlobals().isPresent()) {

            checkGlobalsServices();
        }

		if(this.library.getExecutors() == null ||
				this.library.getExecutors().getExecutors().size() == 0) {

            this.importResult.getCommonErrors().add(new ImportError(0, "At least one executor is required"));

        } else {
            checkExecutors();
		}

		if(this.library.getScriptLists().size() == 0) {

            this.importResult.getCommonErrors().add(
					new ImportError(0, "No script lists defined. Nothing to execute"));

        } else {
            checkScriptListServices();
            checkScriptListTags();
		}

        copyGlobalsErrorsToExecutors();

        checkExecutorsReady();

	}

    private void checkServiceLists() {
        for (ServiceList sList : this.library.getServiceLists().values()) {
            ImportError error = new ImportError(sList.getLineNumber(), String.format("Service List \"%s\" error", sList.getName()));
            for (Service service : sList.getServices()) {
                if (service.isRejected()) {
                    if (!sList.isRejected()) {
                        sList.setRejectCause(error);
                    }
                    sList.addRejectCause(service.getRejectCause());
                }
            }
        }
    }

    private void checkGlobalsServices() {
        Globals globals = this.library.getGlobals().get();

        for (String serviceList : globals.getServiceLists()) {
            if (!library.getServiceLists().containsKey(serviceList)) {
                ImportError error = new ImportError(0, "Service List '" + serviceList + "' is not declared");
                globals.addRejectCause(error);
                importResult.getGlobalsErrors().add(error);
                continue;
            }
            ServiceList sList = this.library.getServiceLists().get(serviceList);
            if (sList.isRejected()) {
                globals.addRejectCause(sList.getRejectCause());
                importResult.getGlobalsErrors().add(sList.getRejectCause());
            }
        }

    }

    private void checkExecutors() {
        Set<String> paths = new HashSet<>();
        for (Executor exec : library.getExecutors().getExecutors()) {
            if (paths.contains(exec.getPath())) {
                exec.addRejectCause(new ImportError(exec.getLineNumber(), String.format("Path is not unique: \"%s\"", exec.getPath())));
            } else {
                paths.add(exec.getPath());
            }
            for (String serviceList : exec.getServices()) {
                if (!library.getServiceLists().containsKey(serviceList)) {
                    exec.addRejectCause(new ImportError(0, "Service List '" + serviceList + "' is not declared"));
                } else {
                    ServiceList sList = this.library.getServiceLists().get(serviceList);
                    if (sList.isRejected()) {
                        exec.addRejectCause(sList.getRejectCause());
                    }
                }
            }
            if (exec.isRejected()) {
                this.importResult.getExecutorErrors().add(exec.getRejectCause());
            }
        }
    }

    private void checkScriptListServices() {

        for (ScriptList list : library.getScriptLists()) {
            Set<String> duplicateServices = new HashSet<>();
            Set<String> helper = new HashSet<>();
            for (String serviceList : list.getServiceLists()) {
                if (!library.getServiceLists().containsKey(serviceList)) {
                    list.addRejectCause(new ImportError(0, "Service List '" + serviceList + "' is not declared"));
                } else {
                    ServiceList sList = this.library.getServiceLists().get(serviceList);
                    if (sList.isRejected()) {
                        list.addRejectCause(sList.getRejectCause());
                    }
                    for (Service service : sList.getServices()) {
                        if (!service.isRejected() && !helper.add(service.getName())) {
                            duplicateServices.add(service.getName());
                        }
                    }
                }
            }

            for (String duplicateService : duplicateServices) {

                Set<String> declaredIn = new HashSet<>();

                for (String serviceList : list.getServiceLists()) {
                    ServiceList sList = this.library.getServiceLists().get(serviceList);
                    for (Service service : sList.getServices()) {
                        if (!service.isRejected() && service.getName().equals(duplicateService)) {
                            declaredIn.add(serviceList);
                        }
                    }
                }

                list.addRejectCause(new ImportError(list.getLineNumber(),
                        String.format("Not unique service %s declared in service lists %s", duplicateService, declaredIn)));
            }

            if (list.isRejected()) {
                this.importResult.getScriptListErrors().add(list.getRejectCause());
            }
        }

    }

    private void checkScriptListTags() {
        if(library.getTagList() == null){
            return;
        }

        for (ScriptList scriptList : library.getScriptLists()) {
            scriptList.addRejectCause(checkTags(scriptList.getApiOptions().getTags(), scriptList.getLineNumber()));

            for(Script script : scriptList.getScripts()){
                script.addRejectCause(checkTags(script.getOriginalApiOptions().getTags(), script.getLineNumber()));
                if(script.isRejected()){
                    scriptList.addRejectCause(script.getRejectCause());
                }
            }
        }
    }

    private Set<ImportError> checkTags(Set<Tag> tags, long line){

        List<Tag> registeredTags = library.getTagList().getTags();
        Set<ImportError> errors = new HashSet<>();

        boolean registered;
        for (Tag tag : tags) {
            registered = false;
            for (Tag registeredTag : registeredTags) {
                if (registeredTag.equals(tag)) {
                    registered = true;
                    break;
                }
            }

            if(!registered){
                errors.add(new ImportError(line,
                        String.format("Tag not registered in Tag List %s", tag.getName())));
            }
        }

        return errors;
    }

    private void copyGlobalsErrorsToExecutors() {
        ExecutorList executorList = library.getExecutors();

        if(executorList == null || executorList.getExecutors().isEmpty()) {
            logger.debug("No executors in library config");
            return;
        }

        for(Executor exec : executorList.getExecutors()) {
            if (this.library.getGlobals().isPresent() && !this.importResult.getGlobalsErrors().isEmpty()) {
                ImportError globalsError = new ImportError(this.library.getGlobals().get().getLineNumber(), "Globals error");
                globalsError.addCause(importResult.getGlobalsErrors());
                exec.addRejectCause(globalsError);
            }
            if (exec.isRejected()) {
                this.importResult.getExecutorErrors().add(exec.getRejectCause());
            }
        }
    }

    private void checkExecutorsReady() {
        int executorsReady = this.library.getExecutors().getExecutors().size();

        if (executorsReady == 0) {
            return;
        }

        for (Executor executor : this.library.getExecutors().getExecutors()) {
            if (executor.isRejected()) {
                executorsReady--;
            }
        }

        if (executorsReady == 0) {
            this.importResult.getCommonErrors().add(new ImportError(0, "All executors gone to error state"));
        }
    }

    private ImportError checkFileExisting(String path, long lineNumber) {
        String libraryPath = this.library.getRootFolder();

        if (libraryPath != null) {

            String relativePath = Paths.get(libraryPath, path).toString();

            int zipIndex = relativePath.indexOf(".zip");

            if (zipIndex != -1) {

                String zipPath = relativePath.substring(0, zipIndex + 4);
                try {
                    File zipFile = this.workspaceDispatcher.getFile(FolderType.TEST_LIBRARY, zipPath);

                    String restPath = relativePath.substring(zipIndex + 5);

                    try {
                        if (!checkFileExistingInZip(zipFile, restPath)) {
                            return new ImportError(lineNumber, "File " + restPath + " not found in zip: " + zipPath);
                        }
                    } catch (IOException e) {
                        return new ImportError(lineNumber, "File " + zipPath + " reading error");
                    }
                } catch (FileNotFoundException e) {
                    return new ImportError(lineNumber, "File " + zipPath + " not found");
                }
            } else {
                if (!this.workspaceDispatcher.exists(FolderType.TEST_LIBRARY, relativePath)) {
                    return new ImportError(lineNumber, "File not exists: " + relativePath);
                }
            }
        }

        return null;
    }

	private boolean checkFileExistingInZip(File zipFile, String entryToCheck) throws IOException {

		ZipEntry ze = null;

		entryToCheck = BigButtonUtil.recognizeSeparatorInZipAndChangePath(zipFile, entryToCheck);

		try (ZipFile zif = new ZipFile(zipFile)) {

			final Enumeration<? extends ZipEntry> entries = zif.entries();

			while (entries.hasMoreElements()) {

				ze = entries.nextElement();

				String entryName = ze.getName();

			    if (entryName.equals(entryToCheck)) {
			        return true;
			    }
			}
		}

		return false;
	}

	private void mergeApiOptionsForScripts() {

		SfApiOptions defaultsAndGlobals = this.library.getDefaultApiOptions();

		if(this.library.getGlobals().isPresent()) {

			SfApiOptions globals = this.library.getGlobals().get().getApiOptions();
			defaultsAndGlobals = defaultsAndGlobals.mergeOptions(globals);

		}

		for(ScriptList list : this.library.getScriptLists()) {

			SfApiOptions mergedListOptions = defaultsAndGlobals.mergeOptions(list.getApiOptions());

			for(Script script : list.getScripts()) {

				SfApiOptions mergedScriptOptions = mergedListOptions.mergeOptions(script.getOriginalApiOptions());

				script.setApiOptions(mergedScriptOptions);

			}

		}


	}

	public LibraryImportResult buildFromCsv(String fileName) throws IOException {

		parseCsv();

		doPostImportChecks();

		mergeApiOptionsForScripts();

		Collections.sort(library.getScriptLists(), Collections.reverseOrder());

		library.setDescriptorFileName(fileName);

		this.importResult.setLibrary(library);

		return this.importResult;

	}

}
