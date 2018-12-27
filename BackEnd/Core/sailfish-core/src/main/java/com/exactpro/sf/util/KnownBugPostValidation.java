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
package com.exactpro.sf.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.IPostValidation;
import com.exactpro.sf.scriptrunner.StatusType;

/**
 * @author sergey.vasiliev
 *
 */
public class KnownBugPostValidation extends AbstractPostValidation {

    private static Logger logger = LoggerFactory.getLogger(KnownBugPostValidation.class);

    public KnownBugPostValidation(IPostValidation parentValidator, IMessage filter) {
        super(parentValidator);
    }

    @Override
    public void doValidate(IMessage message, IMessage filter, ComparatorSettings settings, ComparisonResult result) {
        super.doValidate(message, filter, settings, result);
        process(result);
    }

    public static ComparisonResult process(ComparisonResult result) {
        ActualKnownBugs actualKnownBugs = new ActualKnownBugs();
        actualKnownBugs.calculate(result);
        logger.debug("Known bug info: {}", actualKnownBugs);
        if (!actualKnownBugs.isEmpty()) {
            result.setAllKnownBugs(actualKnownBugs.getDescriptions());
            Status status = validate(actualKnownBugs);
            if (status == Status.CONDITIONALLY_PASSED) {
                Set<BugDescription> reproducedBugs = actualKnownBugs.searchReproducedBugs();
                result.setReproducedBugs(reproducedBugs);
                result.setException(new KnownBugException("Known bugs: " + reproducedBugs, reproducedBugs));
            }
        }
        return result;
    }

    private static Status validate(ActualKnownBugs actualKnownBugs) {
        logger.trace("Start to post validate for KnownBug");
        Status result = Status.PASSED;

        // Process reproduced bugs
        for (BugDescription description : actualKnownBugs.searchReproducedBugs()) {
            KnownBugInfo knownBugInfo = actualKnownBugs.get(description);

            Status statusType = Status.PASSED;
            switch (knownBugInfo.getStatus()) {
            case PART_REPRODUCE:
                statusType = Status.FAILED;
                break;
            case FULL_REPRODUCE:
                statusType = Status.CONDITIONALLY_PASSED;
                break;
            case NOT_REPRODUCE:
            default:
                throw new EPSCommonException(
                        "No action for status " + knownBugInfo.getStatus() + " of bug description " + knownBugInfo.getDescription());
            }

            for (Entry<ComparisonResult, ExpressionResult> entry : knownBugInfo.getComparisonResults()) {
                ComparisonResult comparisonResult = entry.getKey();
                result = result.compareAndGetMax(statusType);
                switch (statusType) {
                case PASSED:
                    setStatus(comparisonResult, statusType.statusType, null);
                    break;
                case CONDITIONALLY_PASSED:
                    comparisonResult.setStatus(statusType.statusType);
                    logger.trace(
                            "CF change. Name: " + comparisonResult.getName() + " Status: " + comparisonResult.getStatus() + " Reason: "
                                    + comparisonResult.getExceptionMessage());
                    break;
                case FAILED:
                default:
                    ExpressionResult expressionResult = entry.getValue();
                    String message = attachOriginReason(comparisonResult, expressionResult,
                            "Known bug '" + knownBugInfo.getDescription() + "' has not been reproduced in full");
                    setStatus(comparisonResult, statusType.statusType, message);
                    break;
                }
            }
        }

        // Process intersections bugs
        for (Collection<BugDescription> intersection : actualKnownBugs.getIntersections()) {
            boolean failedIntersection = false;
            for (BugDescription description : intersection) {
                KnownBugInfo knownBugInfo = actualKnownBugs.get(description);
                if (knownBugInfo.getStatus() == KnownBugStatus.PART_REPRODUCE) {
                    failedIntersection = true;
                    break;
                }
            }

            if (failedIntersection) {
                for (BugDescription description : intersection) {
                    KnownBugInfo knownBugInfo = actualKnownBugs.get(description);
                    if (knownBugInfo.getStatus() != KnownBugStatus.NOT_REPRODUCE) {
                        for (Entry<ComparisonResult, ExpressionResult> entry : knownBugInfo.getComparisonResults()) {
                            result = result.compareAndGetMax(Status.FAILED);
                            String message = attachOriginReason(entry.getKey(), entry.getValue(), "Some of defined known bugs have not been reproduced in full");
                            setStatus(entry.getKey(), StatusType.FAILED, message);
                        }
                    }
                }
            }
        }

        //Process empty actual messages
        for(ComparisonResult comparisonResult : actualKnownBugs.getEmptyActualMessages()) {
            if(ComparisonUtil.getResultCount(comparisonResult, StatusType.CONDITIONALLY_PASSED) == comparisonResult.getResults().size()) {
                comparisonResult.setStatus(StatusType.CONDITIONALLY_PASSED);
            }
        }

        return result;
    }

    private static String attachOriginReason(ComparisonResult comparisonResult, ExpressionResult expressionResult, String mainReason) {
        StringBuilder reasonBuilder = new StringBuilder(mainReason);
        if (!expressionResult.hasKnownBugsInfo()) {
            reasonBuilder.append(System.lineSeparator()).append(comparisonResult.getStatus()).append(": ");
            if (expressionResult.getCause() != null) {
                reasonBuilder.append(expressionResult.getCause().getMessage());
            } else {
                reasonBuilder.append(expressionResult.getDescription());
            }
        }
        return reasonBuilder.toString();
    }

    private static void setStatus(ComparisonResult comparisonResult, StatusType statusType, String message) {
        comparisonResult.setStatus(statusType);
        comparisonResult.setException(message != null ? new KnownBugException(message) : null);
    }

    private static enum Status {
        PASSED(StatusType.PASSED, 0),
        CONDITIONALLY_PASSED(StatusType.CONDITIONALLY_PASSED, 1),
        FAILED(StatusType.FAILED, 2);

        private final StatusType statusType;
        private final int priority;

        private Status(StatusType statusType, int priority) {
            this.statusType = statusType;
            this.priority = priority;
        }

        public Status compareAndGetMax(Status status) {
            if (this.priority < status.priority) {
                return status;
            }
            return this;
        }
    }

    private static class ActualKnownBugs {
        private final Map<BugDescription, KnownBugInfo> map = new HashMap<>();
        private final Set<Collection<BugDescription>> intersections = new HashSet<>();
        private final Set<ComparisonResult> emptyActualMessages = new HashSet<>();

        public void calculate(ComparisonResult comparisonResult) {
            if(comparisonResult == null) {
                return;
            }

            if(comparisonResult.hasResults()) {
                for(ComparisonResult subResult : comparisonResult) {
                    calculate(subResult);
                }

                if(comparisonResult.getStatus() == StatusType.FAILED
                        && comparisonResult.getActual() == null
                        && comparisonResult.getExpected() != null) {
                    emptyActualMessages.add(comparisonResult);
                }
            }

            ExpressionResult expressionResult = comparisonResult.getExpressionResult();

            if(expressionResult == null
                    && comparisonResult.getStatus() == StatusType.NA
                    && comparisonResult.getExpected() instanceof IFilter) {
                IFilter filter = comparisonResult.getExpected();

                expressionResult = filter.validate(comparisonResult.getActual());
            }

            if(expressionResult != null
                    && expressionResult.hasBugsInfo()) {
                add(comparisonResult, expressionResult);
                if (expressionResult.hasKnownBugsInfo()) {
                    Set<BugDescription> descriptions = expressionResult.getActualDescriptions();
                    comparisonResult.setException(new FieldKnownBugException("It's part of bug(s): " + descriptions, descriptions));
                    for (BugDescription description : descriptions) {
                        this.map.get(description).increment();
                    }
                } else {
                    Throwable cause = expressionResult.getCause();
                    if (cause != null) {
                        setStatus(comparisonResult, StatusType.FAILED, cause.getMessage());
                    } else {
                        setStatus(comparisonResult, StatusType.PASSED, null);
                    }
                }
            }
        }

        public Set<Collection<BugDescription>> getIntersections() {
            return this.intersections;
        }

        public Set<BugDescription> getDescriptions() {
            return Collections.unmodifiableSet(map.keySet());
        }

        public Set<ComparisonResult> getEmptyActualMessages() {
            return emptyActualMessages;
        }

        public Set<BugDescription> searchReproducedBugs() {
            if (isEmpty()) {
                return Collections.emptySet();
            }
            Set<BugDescription> result = new HashSet<>();

            for (Entry<BugDescription, KnownBugInfo> entry : this.map.entrySet()) {
                if (entry.getValue().getStatus() != KnownBugStatus.NOT_REPRODUCE) {
                    result.add(entry.getKey());
                }
            }

            return result;
        }

        public KnownBugInfo get(BugDescription description) {
            return this.map.get(description);
        }

        public boolean isEmpty() {
            return this.map.isEmpty();
        }

        private void add(ComparisonResult comparisonResult, ExpressionResult expressionResult) {
            Set<BugDescription> descriptions = expressionResult.getPotentialDescriptions();
            if (descriptions.size() > 1) {
                this.intersections.add(descriptions);
            }
            for (BugDescription description : descriptions) {
                KnownBugInfo knownBugInfo = this.map.get(description);
                if (knownBugInfo == null) {
                    knownBugInfo = new KnownBugInfo(description);
                    this.map.put(description, knownBugInfo);
                }
                knownBugInfo.add(comparisonResult, expressionResult);
            }
        }

        @Override
        public String toString() {
            if (!this.map.isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder();
                if (!this.intersections.isEmpty()) {
                    stringBuilder.append("intersections: ").append(this.intersections).append(' ');
                }
                stringBuilder.append("bugs: ").append(this.map.values());
                return stringBuilder.toString();
            }

            return StringUtils.EMPTY;
        }
    }

    private static class KnownBugInfo {
        private final Map<ComparisonResult, ExpressionResult> comparisonResults = new IdentityHashMap<>() ;
        private final BugDescription description;
        private int count = 0;

        public KnownBugInfo(BugDescription description) {
            this.description = description;
        }

        public void add(ComparisonResult comparisonResult, ExpressionResult expressionResult) {
            this.comparisonResults.put(comparisonResult, expressionResult);
        }

        public void increment() {
            this.count++;
        }

        public KnownBugStatus getStatus() {
            if (this.count == 0) {
                return KnownBugStatus.NOT_REPRODUCE;
            } else if (this.count < this.comparisonResults.size()) {
                return KnownBugStatus.PART_REPRODUCE;
            } else if (this.count == this.comparisonResults.size()) {
                return KnownBugStatus.FULL_REPRODUCE;
            }
            throw new EPSCommonException("Internal error, description for bug description " + this.description);
        }

        public BugDescription getDescription() {
            return description;
        }

        public Set<Entry<ComparisonResult, ExpressionResult>> getComparisonResults() {
            return comparisonResults.entrySet();
        }

        @Override
        public String toString() {
            return new StringBuilder("'").append(this.description).append("'-(").append(this.count).append('/').append(this.comparisonResults.size())
                    .append(')').toString();
        }
    }

    private static enum KnownBugStatus {
        NOT_REPRODUCE,
        PART_REPRODUCE,
        FULL_REPRODUCE;
    }
}