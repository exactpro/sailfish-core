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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.ReportWriterOptions;
import com.exactpro.sf.scriptrunner.ReportWriterOptions.Duration;
import com.exactpro.sf.scriptrunner.TestScriptDescription;

public class DefaultReportWriter implements IReportWriter {

	private static final Logger logger = LoggerFactory.getLogger(DefaultReportWriter.class);

	private final Map<ReportType, Class<? extends IReportCreator>> reportCreators = new HashMap<>();
	private final IWorkspaceDispatcher dispatcher;

	public DefaultReportWriter(IWorkspaceDispatcher dispatcher) {

	    for(ReportType reportType : ReportType.values()) {
	        reportCreators.put(reportType, reportType.getReportClass());
        }
		this.dispatcher = dispatcher;
	}

	@Override
    public void write(File file, List<TestScriptDescription> descriptions, ReportWriterOptions options) throws ReportWriterException {
		try {
			Class<? extends IReportCreator> creatorClass = this.reportCreators.get(options.getSelectedReportType());
			if (creatorClass == null) {
				creatorClass = BaseFailReportCreator.class;
			}

			IReportCreator creator = creatorClass.newInstance();
			creator.init(new XMLReportCreatorImpl(dispatcher)); // change this

			List<TestScriptDescription> filtered = filter(descriptions, options);
			creator.createReport(file, filtered, options);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ReportWriterException(e);
		}
	}

	@Override
	public String getFileName(ReportType reportType) {
		SimpleDateFormat creationTimestampFormat;
		if (ReportType.ETM == reportType) {
			creationTimestampFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss_SSS");
		} else {
			creationTimestampFormat = new SimpleDateFormat("ddMMyyyy_HHmmss_SSS");
		}

		return "AggregatedReport_" + creationTimestampFormat.format(new Date()) + ".csv";
	}

	private List<TestScriptDescription> filterBetween(List<TestScriptDescription> toFilter, Date start, Date end) {

		List<TestScriptDescription> result = new ArrayList<>();

		long startTime = start.getTime();
		long endTime = end.getTime();

		for(TestScriptDescription descr : toFilter) {
			if(startTime < descr.getTimestamp().getTime() && descr.getTimestamp().getTime() < endTime) {
				result.add(descr);
			}
		}
		return result;
	}

	private List<TestScriptDescription> filter(List<TestScriptDescription> toFilter, ReportWriterOptions options) {

		if(options.getSelectedDuration() == Duration.Today) {

			Date now = new Date();

			Date min = DateUtils.truncate(now, Calendar.DATE);
			Date max = DateUtils.addSeconds( min, 24 * 60 * 60 -1 );

			return filterBetween(toFilter, min, max);

		} else if(options.getSelectedDuration() == Duration.Week) {

			Calendar c = Calendar.getInstance();

			c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());

			Date min = DateUtils.truncate(c.getTime(), Calendar.DATE);
			Date max = DateUtils.addSeconds( DateUtils.addWeeks(min, 1), -1);

			return filterBetween(toFilter, min, max);

		} else if(options.getSelectedDuration() == Duration.Month) {

			Date min = DateUtils.truncate(new Date(), Calendar.MONTH);
			Date max = DateUtils.addSeconds( DateUtils.addMonths(min, 1), -1);

			return filterBetween(toFilter, min, max);

		} else if(options.getSelectedDuration() == Duration.Custom) {

			Date defaultMin = new Date(0);
			Date defaultMax = new Date(Long.MAX_VALUE);

			Date min = options.getCustomStart() == null ? defaultMin : options.getCustomStart();
			Date max = options.getCustomEnd() == null ? defaultMax : options.getCustomEnd();

			return filterBetween(toFilter, min, max);

		}

		throw new RuntimeException("Unknown export duration mode: " + options.getSelectedDuration().name());
	}

}