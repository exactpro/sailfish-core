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
package com.exactpro.sf.scriptrunner;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.ValidateRegex;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration.Node;
import org.apache.commons.lang3.StringUtils;
import org.mvel2.math.MathProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EnvironmentSettings implements ICommonSettings
{
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentSettings.class);

    public enum StorageType {
        DB("db"),
        FILE("file"),
        MEMORY("memory");

        private final String name;

        StorageType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static StorageType parse(String value) {
            if(value != null) {
                for(StorageType element : StorageType.values()) {
                    if(element.name.equalsIgnoreCase(value)) {
                        return element;
                    }
                }
            }

            return null;
        }
    }

    public enum ReportOutputFormat {
        ZIP("zip"),
        FILES("files"),
        ZIP_FILES("zip_files");

        private final String name;

        ReportOutputFormat(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static ReportOutputFormat parse(String value) {
            if(value != null) {
                for(ReportOutputFormat element : ReportOutputFormat.values()) {
                    if(element.name.equalsIgnoreCase(value)) {
                        return element;
                    }
                }
            }

            return null;
        }

        public boolean isEnableZip(){
            return !name.equalsIgnoreCase(FILES.name);
        }

        public boolean isEnableFiles(){
            return !name.equalsIgnoreCase(ZIP.name);
        }
    }

    public enum RelevantMessagesSortingMode {
        ARRIVAL_TIME("arrival_time"),
        FAILED_FIELDS("failed_fields");

        private final String name;

        RelevantMessagesSortingMode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static RelevantMessagesSortingMode parse(String value) {
            if(value != null) {
                for(RelevantMessagesSortingMode mode : values()) {
                    if(mode.name.equalsIgnoreCase(value)) {
                        return mode;
                    }
                }
            }

            return null;
        }
    }

    private static final String GENERAL_KEY = "GeneralSettings";
    private static final String ASYNC_RUN_MATRIX_KEY = "AsyncRunMatrix";
    private static final String NOTIFICATION_IF_SOME_SERVICES_NOT_STARTED = "NotificationIfSomeServicesNotStarted";
    private static final String MATRIX_COMPILER_PRIORITY = "MatrixCompilerPriority";
    private static final String EXCLUDED_MESSAGES_FROM_REPORT = "ExcludedMessagesFromReport";
    private static final String COMPARISON_PRECISION = "ComparisonPrecision";

    private static final String SCRIPT_RUN = "ScriptRun";
    private static final String FAIL_UNEXPECTED_KEY = "FailUnexpected";
    private static final String REPORT_OUTPUT_FORMAT = "ReportOutputFormat";
    private static final String RELEVANT_MESSAGES_SORTING_MODE = "RelevantMessagesSortingMode";
    private static final String MAX_STORAGE_QUEUE_SIZE = "MaxStorageQueueSize";

	private StorageType storageType = StorageType.DB;
	private String fileStoragePath = "storage";
	private boolean storeAdminMessages;
	private boolean asyncRunMatrix;
	private long maxQueueSize;

	private boolean notificationIfServicesNotStarted;
	private int matrixCompilerPriority;
	private Set<String> excludedMessages = ImmutableSet.of("Heartbeat");
    private String failUnexpected = "N";
    private ReportOutputFormat reportOutputFormat = ReportOutputFormat.ZIP_FILES;
    private RelevantMessagesSortingMode relevantMessagesSortingMode;
    private BigDecimal comparisonPrecision = MathProcessor.COMPARISON_PRECISION;

    // this config is one per SF
    private final HierarchicalConfiguration config;

	public EnvironmentSettings(HierarchicalConfiguration hierarchicalConfiguration) {
		this.config = hierarchicalConfiguration;
		if (config.configurationsAt(GENERAL_KEY).isEmpty()) {
            config.getRootNode().addChild(new Node(GENERAL_KEY));
        }
        if (config.configurationsAt(SCRIPT_RUN).isEmpty()) {
            config.getRootNode().addChild(new Node(SCRIPT_RUN));
        }
	}

	@Override
    public EnvironmentSettings clone() {
        EnvironmentSettings result = new EnvironmentSettings(config);
        result.fileStoragePath = fileStoragePath;
        result.storeAdminMessages = storeAdminMessages;
        result.asyncRunMatrix = asyncRunMatrix;
        result.notificationIfServicesNotStarted = notificationIfServicesNotStarted;
        result.matrixCompilerPriority = matrixCompilerPriority;
        result.excludedMessages = excludedMessages;
        result.failUnexpected = failUnexpected;
        result.storageType = storageType;
        result.reportOutputFormat = reportOutputFormat;
        result.relevantMessagesSortingMode = relevantMessagesSortingMode;
        result.comparisonPrecision = comparisonPrecision;
        result.maxQueueSize = maxQueueSize;

	    return result;
	}

	public void set(EnvironmentSettings other) {
	    this.fileStoragePath = other.fileStoragePath;
	    this.storageType = other.storageType;
	    this.storeAdminMessages = other.storeAdminMessages;
	    this.asyncRunMatrix = other.asyncRunMatrix;
	    this.notificationIfServicesNotStarted = other.notificationIfServicesNotStarted;
	    this.matrixCompilerPriority = other.matrixCompilerPriority;
	    this.excludedMessages = other.excludedMessages;
	    this.failUnexpected = other.failUnexpected;
        this.relevantMessagesSortingMode = other.relevantMessagesSortingMode;
        this.comparisonPrecision = other.comparisonPrecision;
        this.maxQueueSize = other.maxQueueSize;

	    update();
	}

	public String getFileStoragePath() {
        return fileStoragePath;
	}

	@Description("The directory where the files which contain all the sent/received messages are stored. It is required for 'file' store type.<br><br>" +
            "NOTE: Changes of this setting will be applied only after Sailfish restart.") //TODO color highlight
	public void setFileStoragePath(String path) {
		this.fileStoragePath = path;
		update();
	}

    public StorageType getStorageType() {
        return storageType;
    }

    @Description("This setting allows to set the type of HDD storage used for the long-term " +
            "storage of the received/sent messages.<br>" +
            "Supported storage types:<br>" +
            "<ul>" +
                "<li>file – for each message, a separate file is created in the directory specified in the 'File Storage Path' field. " +
                    "This type of storage is recommended to be used when opening Sailfish for a short period of time:" +
                    "to check the connection to the test system, to run a smoke test, for demo demonstration.<br>" +
                    "When using this storage type, the user cannot query the messages on the Messages page.</li>" +
                "<li>db – messages are saved in a Database. The DB connection should be set up in the Database tab. This " +
                    "storage type is recommended for long-term work with Sailfish.</li>" +
            "</ul><br>" +
            "NOTE: Changes of this setting will be applied only after Sailfish restart.") //TODO color highlight
    @ValidateRegex(regex = "(?i)^(file|db)$")
    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
        update();
    }

	public boolean isStoreAdminMessages() {
		return storeAdminMessages;
	}

	@Description("The setting that allows the user to store or not to store admin " +
            "messages in HDD storage (e.g. messages for creating, maintaining and closing the connections " +
            "between the services and the target server).<br>" +
            "If admin messages are being stored, they will not be displayed in the report or on the Messages page, " +
            "and they will not be obtained via the retrieve action.<br>" +
            "Not storing admin messages will save the space in HDD storage.<br><br>" +
            "NOTE: Changes of this setting will be applied only after Sailfish restart.") //TODO color highlight
    @ValidateRegex(regex = "^(true|false)$")
	public void setStoreAdminMessages(boolean storeAdminMessages) {
		this.storeAdminMessages = storeAdminMessages;
		update();
	}

	public boolean isAsyncRunMatrix() {
		return asyncRunMatrix;
	}

	@Description("This parameter is used for changing the matrix execution mode.<br>" +
            "<ul>" +
                "<li>false – sequential execution (only one matrix will be executed at once).</li>" +
                "<li>true – execution in parallel (this mode allows to run up to 3 matrices at once, unless the same services are used in these matrices).</li>" +
            "</ul><br>" +
            "NOTE: Changes of this setting will be applied only after Sailfish restart.") //TODO color highlight
    @ValidateRegex(regex = "^(true|false)$")
	public void setAsyncRunMatrix(boolean asyncRunMatrix) {
		this.asyncRunMatrix = asyncRunMatrix;
		update();
	}

	public long getMaxStorageQueueSize() {
	    return maxQueueSize;
    }

    @Description("Maximum MQ volume capacity in RAM for saving messages in " +
            "HDD. If the memory is full, the arrived messages will not be stored. These messages will not be " +
            "displayed on the Messages page, neither in the ‘All Messages’ table in the report and will not be " +
            "found using the retrieve action. MQ overflow can be provoked by the arrival of a large number " +
            "of messages. The ‘Max Storage Queue Size’ functionality allows preventing JVM crash with OutOfMemory error.<br><br>" +
            "NOTE: Changes of this setting will be applied only after Sailfish restart.") //TODO color highlight
    @ValidateRegex(regex = "^\\d+$")
    public void setMaxStorageQueueSize(long size) {
	    maxQueueSize = size;
	    update();
    }

	public boolean isNotificationIfServicesNotStarted() {
		return notificationIfServicesNotStarted;
	}

	@Description("Option is used to notify the user, before matrix execution is started, if the services specified in the test script are not started.<br>" +
            "If value is true, and at least one of the services specified in the matrix is not " +
            "started, the execution of the test script will be on hold, which would be observed on the Test scripts " +
            "page in the Reports, and on the Matrix execution panel the user will see: ’The following services have " +
            "not been started’ (not started services will be listed). The user will be able to move to another step, " +
            "continue or stop the execution.<br>" +
            "This feature can be useful when running the test scripts manually, because it allows to take an action " +
            "before the execution is started.")
    @ValidateRegex(regex = "^(true|false)$")
	public void setNotificationIfServicesNotStarted(boolean notificationIfServicesNotStarted) {
		this.notificationIfServicesNotStarted = notificationIfServicesNotStarted;
		update();
	}

    public int getMatrixCompilerPriority() {
        return matrixCompilerPriority;
    }

    @Description("Option is used for regulating the priority between the Matrices compilation " +
            "and the rest of the actions: start of the services, matrices execution and general Sailfish processes.<br>" +
            "Integer values have to be specified: 1 – minimal priority, 10 – maximum priority. By default, 5 is used.<br><br>" +
            "NOTE: Changes of this setting will be applied only after Sailfish restart.") //TODO color highlight
    @ValidateRegex(regex = "^([1-9]|10)$")
    public void setMatrixCompilerPriority(int matrixCompilerPriority) {
        this.matrixCompilerPriority = matrixCompilerPriority;
        update();
    }

    @Description("The setting that allows the user to set up the default value in the " +
            "#faild_unexpected field, which sets the way of comparing the messages, for example, in the receive action.<br>" +
            "Possible values in the #faild_unexpected field:<br>" +
            "<ul>" +
                "<li>N or n – only filtered fields are checked.</li>" +
                "<li>Y or n – all fields in the message are checked.</li>" +
                "<li>A or a – all fields in the message as well as the message structure are checked.</li>" +
            "</ul>")
    @ValidateRegex(regex = "(?i)^(N|Y|A)$")
	public void setFailUnexpected(String failUnexpected) {
		this.failUnexpected = failUnexpected;
		update();
	}

	public String getFailUnexpected() {
		return failUnexpected;
	}

	public ReportOutputFormat getReportOutputFormat(){
	    return reportOutputFormat;
    }

    public Set<String> getExcludedMessages() {
        return excludedMessages;
    }

    @Description("These messages will not be presented in a report.<br>" +
            "The excluded messages must be listed separated by commas and must have the same names as the ones set in the dictionary.<br>" +
            "This feature might be useful for messages like Heartbeat – this will allow to decrease the size of the report, thus, saving memory, excluding undescriptive messages.<br><br>" +
            "NOTE: Changes of this setting will be applied only after Sailfish restart.") //TODO color highlight
    @ValidateRegex(regex = IDictionaryValidator.NAME_REGEX)
    public void setExcludedMessages(Set<String> excludedMessages) {
        this.excludedMessages = CollectionUtils.isEmpty(excludedMessages) ? Collections.emptySet() : ImmutableSet.copyOf(excludedMessages);
        update();
    }

    public RelevantMessagesSortingMode getRelevantMessagesSortingMode() {
        return relevantMessagesSortingMode;
    }

    @Description("This setting allows to change the way of displaying similar " +
            "messages in the failed actions of the report.<br>" +
            "Supported parameters:<br>" +
            "<ul>" +
            "<li>ARRIVAL_TIME – similar messages will be displayed in the same order as they have been received by " +
                "the service (by default).</li>" +
            "<li>FAILED_FIELDS – similar messages will be displayed in the report sorted by the number of failed fields, " +
                "from low to high. This function is only supported in an HTML report.</li>" +
            "</ul>")
    @ValidateRegex(regex = "(?i)^(ARRIVAL_TIME|FAILED_FIELDS)$")
    public void setRelevantMessagesSortingMode(RelevantMessagesSortingMode relevantMessagesSortingMode) {
        this.relevantMessagesSortingMode = relevantMessagesSortingMode;
    }

    public BigDecimal getComparisonPrecision() {
        return comparisonPrecision;
    }

    @Description("Allows to set up the default sensibility of numeric values with a floating" +
            "point. The values are considered equal when their absolute difference does not exceed the set value.<br>" +
            "Comparison Precision is considered when Receive actions are executed and can be re-set up using the " +
            "Precision application function.<br><br>" +
            "NOTE: Changes of this setting will be applied only after Sailfish restart.") //TODO color highlight
    public void setComparisonPrecision(BigDecimal comparisonPrecision) {
        this.comparisonPrecision = comparisonPrecision;
    }

	@Override
	public void load(HierarchicalConfiguration config)
	{
        if(!config.configurationsAt(GENERAL_KEY).isEmpty()) {
            loadGeneralSettings(config.configurationAt(GENERAL_KEY));
        }

        if (!config.configurationsAt(SCRIPT_RUN).isEmpty()){
            loadScriptRunSettings(config.configurationAt(SCRIPT_RUN));
        }
	}

	private static Set<String> parseSet(HierarchicalConfiguration config, String propertyName, String regex, Set<String> defaultValue) {
	    List<?> value = config.getList(propertyName);
        if (CollectionUtils.isNotEmpty(value)) {
            Pattern pattern = Pattern.compile(regex);
            Set<String> incorrectNames = new HashSet<>();
            Set<String> result = ImmutableSet.copyOf(value.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .filter(name -> {
                        if (pattern.matcher(name).matches()) {
                            return true;
                        } else {
                            incorrectNames.add(name);
                            return false;
                        }
                    })
                    .collect(Collectors.toSet()));
            if (!incorrectNames.isEmpty()) {
                logger.error("Property '{}' has incorrect values {}", propertyName, incorrectNames);

            }
            return result;
        }
        return defaultValue;
    }

    private void update() {
        if(!config.configurationsAt(GENERAL_KEY).isEmpty()) {
            updateGeneralSettings(config.configurationAt(GENERAL_KEY));
        }

        if (!config.configurationsAt(SCRIPT_RUN).isEmpty()){
            updateScriptRunSettings(config.configurationAt(SCRIPT_RUN));
        }
	}

	private void loadGeneralSettings(HierarchicalConfiguration config) {
		this.fileStoragePath = config.getString("FileStoragePath", "storage");

		this.storeAdminMessages = config.getBoolean("StoreAdminMessages", true);

		this.asyncRunMatrix = config.getBoolean(ASYNC_RUN_MATRIX_KEY, false);

		this.maxQueueSize = config.getLong(MAX_STORAGE_QUEUE_SIZE, 1024*1024*32);

		this.storageType = StorageType.parse(config.getString("StorageType", StorageType.DB.getName()));

        this.comparisonPrecision = config.getBigDecimal(COMPARISON_PRECISION, MathProcessor.COMPARISON_PRECISION);
	}

    private void updateGeneralSettings(HierarchicalConfiguration config) {
		config.setProperty("FileStoragePath", fileStoragePath);
		config.setProperty("StoreAdminMessages", storeAdminMessages);
		config.setProperty(ASYNC_RUN_MATRIX_KEY, asyncRunMatrix);
        config.setProperty("StorageType", storageType.getName());
        config.setProperty(COMPARISON_PRECISION, comparisonPrecision);
        config.setProperty(MAX_STORAGE_QUEUE_SIZE, maxQueueSize);
	}

    private void loadScriptRunSettings(HierarchicalConfiguration config) {
        notificationIfServicesNotStarted = config.getBoolean(NOTIFICATION_IF_SOME_SERVICES_NOT_STARTED, false);
        failUnexpected = config.getString(FAIL_UNEXPECTED_KEY, "N");
        matrixCompilerPriority = config.getInt(MATRIX_COMPILER_PRIORITY, Thread.NORM_PRIORITY);
        reportOutputFormat = ReportOutputFormat.parse(config.getString(REPORT_OUTPUT_FORMAT, ReportOutputFormat.ZIP_FILES.getName()));
        excludedMessages = parseSet(config, EXCLUDED_MESSAGES_FROM_REPORT, IDictionaryValidator.NAME_REGEX, excludedMessages);
        relevantMessagesSortingMode = RelevantMessagesSortingMode.parse(config.getString(RELEVANT_MESSAGES_SORTING_MODE, RelevantMessagesSortingMode.ARRIVAL_TIME.getName()));
	}

    private void updateScriptRunSettings(HierarchicalConfiguration config) {
        config.setProperty(NOTIFICATION_IF_SOME_SERVICES_NOT_STARTED, notificationIfServicesNotStarted);
        config.setProperty(FAIL_UNEXPECTED_KEY, failUnexpected);
        config.setProperty(MATRIX_COMPILER_PRIORITY, matrixCompilerPriority);
        config.setProperty(EXCLUDED_MESSAGES_FROM_REPORT, excludedMessages.isEmpty() ? "" : excludedMessages);
        config.setProperty(REPORT_OUTPUT_FORMAT, reportOutputFormat);
        config.setProperty(RELEVANT_MESSAGES_SORTING_MODE, relevantMessagesSortingMode.getName());
	}

}
