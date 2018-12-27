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
package com.exactpro.sf.testwebgui.statistics;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.time.DateUtils;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.embedded.statistics.storage.DayleTestCasesStatRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.ScriptWeatherRow;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.general.SessionStorage;
import com.exactpro.sf.testwebgui.general.SessionStored;

@ManagedBean(name="statHomeBean")
@ViewScoped
@SuppressWarnings("serial")
public class StatisticsHomeBean extends AbstractStatisticsBean implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(StatisticsHomeBean.class);

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private LineChartModel areaModel = null;

	private List<ScriptWeatherRow> weatherReport;

	private boolean immediateGenerate = true;

    @SessionStored
    private long weatherScriptsLimit = 10;

    @SessionStored
    private long weatherRunsLimit = 15;

	private static final long WEATHER_STEP = 5l;

	private SessionStorage getSessionStorage() {
		return BeanUtil.findBean("sessionStorage", SessionStorage.class);
	}

	private void createAreaModel(List<DayleTestCasesStatRow> rows) {

		if(rows.isEmpty()) {
			areaModel = null;
			return;
		}

		areaModel = new LineChartModel();

		areaModel.setAnimate(true);
		areaModel.setSeriesColors("D35400,D2AC3F,1ABC85");
		long maxTotal = 0;

        LineChartSeries passed = createLineChartSeries("Passed Test Cases");
        LineChartSeries conditionallyPassed = createLineChartSeries("Conditionally Passed Test Cases");
        LineChartSeries failed = createLineChartSeries("Failed Test Cases");

        for(DayleTestCasesStatRow row : rows) {

        	logger.debug("{}", row);

        	long totalDayTc = row.getPassedCount() + row.getConditionallyPassedCount() + row.getFailedCount();

        	passed.set(row.getDate().toString(), row.getPassedCount());
            conditionallyPassed.set(row.getDate().toString(), row.getConditionallyPassedCount());
        	failed.set(row.getDate().toString(), row.getFailedCount());

        	if(maxTotal < totalDayTc) {
        		maxTotal = totalDayTc;
        	}

        }

        areaModel.addSeries(failed);
        areaModel.addSeries(conditionallyPassed);
        areaModel.addSeries(passed);

        areaModel.setTitle(formatTitle());
        areaModel.setLegendPosition("ne");
        areaModel.setStacked(true);
        areaModel.setShowPointLabels(true);

        Axis xAxis = new CategoryAxis();

        xAxis.setTickAngle(-50);
        areaModel.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = areaModel.getAxis(AxisType.Y);
        yAxis.setLabel("Test Cases Count");

        yAxis.setMin(0);
        yAxis.setMax(maxTotal + 15);

	}

    private LineChartSeries createLineChartSeries(String label) {
        LineChartSeries series = new LineChartSeries();

        series.setFill(true);
        series.setLabel(label);

        return series;
    }

	@PostConstruct
    public void init() {

        super.init();

        this.from = DateUtils.truncate(DateUtils.addDays(new Date(), -7), Calendar.DATE);

        if(!BeanUtil.getSfContext().getStatisticsService().isConnected()) {

			this.immediateGenerate = false;

        }
	}

	private String formatTitle() {

		StringBuilder sb = new StringBuilder();

		DateFormat format = new SimpleDateFormat(DATE_FORMAT);

		sb.append(format.format(from)).append(" - ").append(format.format(to));

		return sb.toString();

	}

	public void generateReport() {

		try {
            getSessionStorage().saveStateOfAnnotatedBean(this);

            createAreaModel(BeanUtil.getSfContext()
                                    .getStatisticsService()
                                    .getReportingStorage()
                                    .generateDayleTestCasesStatReport(getDefaultParams()));

		} catch(Exception e) {

			logger.error(e.getMessage(), e);

			BeanUtil.addErrorMessage(e.getMessage(), "");
		}

	}

	public void generateWeatherReport() {

		if(this.weatherScriptsLimit <= 0 ) {
			BeanUtil.addErrorMessage("Error", "X parameter must be greater than 0");
			return;
		}

		if(this.weatherRunsLimit <= 0 ) {
			BeanUtil.addErrorMessage("Error", "Y parameter must be greater than 0");
			return;
		}

		try {

			AggregateReportParameters params = new AggregateReportParameters();

			params.setLimit(this.weatherScriptsLimit);

			params.setSecondLimit(this.weatherRunsLimit);

			this.weatherReport = BeanUtil.getSfContext().getStatisticsService().getReportingStorage().generateScriptsWeatherReport(params);

		} catch(Exception e) {

			logger.error(e.getMessage(), e);

			BeanUtil.addErrorMessage(e.getMessage(), "");
		}

	}

	public void moreWeather() {

		setWeatherScriptsLimit(this.weatherScriptsLimit + WEATHER_STEP);

		generateWeatherReport();

	}

	public LineChartModel getAreaModel() {
		return areaModel;
	}

	public boolean isImmediateGenerate() {
		return immediateGenerate;
	}

	public void setImmediateGenerate(boolean immediateGenerate) {
		this.immediateGenerate = immediateGenerate;
	}



	public List<ScriptWeatherRow> getWeatherReport() {
		return weatherReport;
	}

	public void setWeatherReport(List<ScriptWeatherRow> weatherReport) {
		this.weatherReport = weatherReport;
	}

	public long getWeatherScriptsLimit() {
		return weatherScriptsLimit;
	}

	public void setWeatherScriptsLimit(long weatherScriptsLimit) {
		this.weatherScriptsLimit = weatherScriptsLimit;

		getSessionStorage().put("statHomeBean.weatherScriptsLimit", weatherScriptsLimit);
	}

	public long getWeatherRunsLimit() {
		return weatherRunsLimit;
	}

	public void setWeatherRunsLimit(long weatherRunsLimit) {
		this.weatherRunsLimit = weatherRunsLimit;

		getSessionStorage().put("statHomeBean.weatherRunsLimit", weatherRunsLimit);
	}

}
