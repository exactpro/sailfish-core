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
package com.exactpro.sf.scriptrunner.reportbuilder;

/**
 * @author nikita.smirnov
 *
 */
public enum ReportType {
    BASE("Base", BaseFailReportCreator.class),
    KNOWN_BUC_TC("KnownBug TestCases", EBaseFailReportCreator.class),
    SEND_DATA("Send data", ClOrdIDReportCreator.class),
    FAIL_REASON("Fail reason", RubFailReportCreator.class),
    ETM("ETM", ETMReportCreator.class);

    private final String shortName;
    private final Class<? extends IReportCreator> reportClass;

    private ReportType(String shortName, Class<? extends IReportCreator> reportClass) {
        this.shortName = shortName;
        this.reportClass = reportClass;
    }

    public String getShortName() {
        return shortName;
    }

    public Class<?extends IReportCreator> getReportClass() {
        return reportClass;
    }
}
