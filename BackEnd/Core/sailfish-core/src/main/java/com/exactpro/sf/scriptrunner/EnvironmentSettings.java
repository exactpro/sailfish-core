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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.mvel2.math.MathProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.ValidateRegex;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.google.common.collect.ImmutableSet;

public class EnvironmentSettings implements ICommonSettings
{
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentSettings.class);

    public enum StorageType {
        DB("db"),
        FILE("file"),
        MEMORY("memory");

        private final String name;

        private StorageType(String name) {
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

        private ReportOutputFormat(String name) {
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

        private String name;

        private RelevantMessagesSortingMode(String name) {
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

	private final static String GENERAL_KEY = "GeneralSettings";
	private final static String ASYNC_RUN_MATRIX_KEY = "AsyncRunMatrix";
	private final static String NOTIFICATION_IF_SOME_SERVICES_NOT_STARTED = "NotificationIfSomeServicesNotStarted";
	private final static String MATRIX_COMPILER_PRIORITY = "MatrixCompilerPriority";
	private final static String EXCLUDED_MESSAGES_FROM_REPORT = "ExcludedMessagesFromReport";
    private final static String COMPARISON_PRECISION = "ComparisonPrecision";

	private final static String SCRIPT_RUN = "ScriptRun";
	private final static String FAIL_UNEXPECTED_KEY = "FailUnexpected";
	private final static String REPORT_OUTPUT_FORMAT = "ReportOutputFormat";
    private final static String RELEVANT_MESSAGES_SORTING_MODE = "RelevantMessagesSortingMode";

	private StorageType storageType = StorageType.DB;
	private String fileStoragePath = "storage";
	private boolean storeAdminMessages;
	private boolean asyncRunMatrix;

	private boolean notificationIfServicesNotStarted;
	private int matrixCompilerPriority;
	@Description(value = "Exclude messages from information block about all messages in report")
	@ValidateRegex(regex = IDictionaryValidator.NAME_REGEX)
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
		    config.getRootNode().addChild(new HierarchicalConfiguration.Node(GENERAL_KEY));
        }
        if (config.configurationsAt(SCRIPT_RUN).isEmpty()) {
            config.getRootNode().addChild(new HierarchicalConfiguration.Node(SCRIPT_RUN));
        }
	}

	@Override
    public EnvironmentSettings clone() {
	    EnvironmentSettings result = new EnvironmentSettings(this.config);
	    result.fileStoragePath = this.fileStoragePath;
	    result.storeAdminMessages = this.storeAdminMessages;
	    result.asyncRunMatrix = this.asyncRunMatrix;
	    result.notificationIfServicesNotStarted = this.notificationIfServicesNotStarted;
	    result.matrixCompilerPriority = this.matrixCompilerPriority;
	    result.excludedMessages = this.excludedMessages;
	    result.failUnexpected = this.failUnexpected;
        result.storageType = this.storageType;
        result.reportOutputFormat = this.reportOutputFormat;
        result.relevantMessagesSortingMode = this.relevantMessagesSortingMode;
        result.comparisonPrecision = this.comparisonPrecision;

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

	    update();
	}

	public String getFileStoragePath() {
		return this.fileStoragePath;
	}

	public void setFileStoragePath(String path) {
		this.fileStoragePath = path;
		update();
	}

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
        update();
    }

	public boolean isStoreAdminMessages() {
		return storeAdminMessages;
	}

	public void setStoreAdminMessages(boolean storeAdminMessages) {
		this.storeAdminMessages = storeAdminMessages;
		update();
	}

	public boolean isAsyncRunMatrix() {
		return asyncRunMatrix;
	}

	public void setAsyncRunMatrix(boolean asyncRunMatrix) {
		this.asyncRunMatrix = asyncRunMatrix;
		update();
	}

	public boolean isNotificationIfServicesNotStarted() {
		return notificationIfServicesNotStarted;
	}

	public void setNotificationIfServicesNotStarted(boolean notificationIfServicesNotStarted) {
		this.notificationIfServicesNotStarted = notificationIfServicesNotStarted;
		update();
	}

    public int getMatrixCompilerPriority() {
        return matrixCompilerPriority;
    }

    public void setMatrixCompilerPriority(int matrixCompilerPriority) {
        this.matrixCompilerPriority = matrixCompilerPriority;
        update();
    }

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

    public void setExcludedMessages(Set<String> excludedMessages) {
        this.excludedMessages = CollectionUtils.isEmpty(excludedMessages) ? Collections.emptySet() : ImmutableSet.copyOf(excludedMessages);
        update();
    }

    public RelevantMessagesSortingMode getRelevantMessagesSortingMode() {
        return relevantMessagesSortingMode;
    }

    public void setRelevantMessagesSortingMode(RelevantMessagesSortingMode relevantMessagesSortingMode) {
        this.relevantMessagesSortingMode = relevantMessagesSortingMode;
    }

    public BigDecimal getComparisonPrecision() {
        return comparisonPrecision;
    }

    public void setComparisonPrecision(BigDecimal comparisonPrecision) {
        this.comparisonPrecision = comparisonPrecision;
    }

	@Override
	public void load(HierarchicalConfiguration config)
	{
		if ( !(config.configurationsAt(GENERAL_KEY).isEmpty()) )
			loadGeneralSettings(config.configurationAt(GENERAL_KEY));

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
		if (!(config.configurationsAt(GENERAL_KEY).isEmpty())) {
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

		this.storageType = StorageType.parse(config.getString("StorageType", StorageType.DB.getName()));

        this.comparisonPrecision = config.getBigDecimal(COMPARISON_PRECISION, MathProcessor.COMPARISON_PRECISION);
	}

    private void updateGeneralSettings(HierarchicalConfiguration config) {
		config.setProperty("FileStoragePath", fileStoragePath);
		config.setProperty("StoreAdminMessages", storeAdminMessages);
		config.setProperty(ASYNC_RUN_MATRIX_KEY, asyncRunMatrix);
        config.setProperty("StorageType", storageType.getName());
        config.setProperty(COMPARISON_PRECISION, comparisonPrecision);
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
