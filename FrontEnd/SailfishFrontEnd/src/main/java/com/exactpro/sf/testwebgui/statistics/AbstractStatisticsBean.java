/*******************************************************************************
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.exactpro.sf.embedded.statistics.StatisticsService;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.general.SessionStorage;
import com.exactpro.sf.testwebgui.general.SessionStored;

//TODO refactor all statistics beans
public abstract class AbstractStatisticsBean {

    private static final Logger logger = LoggerFactory.getLogger(AbstractStatisticsBean.class);

    @SessionStored
    protected Date from;

    @SessionStored
    protected Date to;

    @SessionStored
    protected String matrixNamePattern;

    @SessionStored
    protected List<SfInstance> selectedSfInstances = new ArrayList<>();

    protected List<SfInstance> allSfInstances;

    protected void init() {

        if (BeanUtil.getSfContext().getStatisticsService().isConnected()) {

            this.from = DateUtils.truncate(new Date(), Calendar.DATE);
            this.to = new Date();
            initByStatistics(BeanUtil.getSfContext().getStatisticsService());
        }

        BeanUtil.findBean("sessionStorage", SessionStorage.class).restoreStateOfAnnotatedBean(this);

        logger.debug("{} [{}] constructed", this.getClass().getSimpleName(), hashCode());
    }

    protected void initByStatistics(StatisticsService statisticsService) {
        this.allSfInstances = statisticsService.getStorage().getAllSfInstances();
        this.selectedSfInstances.addAll(this.allSfInstances);
    }

    public List<SfInstance> getSelectedSfInstances() {
        return selectedSfInstances;
    }

    public void setSelectedSfInstances(List<SfInstance> selectedSfInstances) {
        this.selectedSfInstances = selectedSfInstances;
    }

    public List<SfInstance> getAllSfInstances() {
        return allSfInstances;
    }

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public String getMatrixNamePattern() {
        return matrixNamePattern;
    }

    public void setMatrixNamePattern(String matrixNamePattern) {
        this.matrixNamePattern = matrixNamePattern;
    }

    protected AggregateReportParameters getDefaultParams() {
        AggregateReportParameters params = new AggregateReportParameters();

        params.setFrom(from);
        params.setTo(to);
        params.setSfInstances(new ArrayList<>(selectedSfInstances));
        params.setMatrixNamePattern(matrixNamePattern);

        return params;
    }
}
