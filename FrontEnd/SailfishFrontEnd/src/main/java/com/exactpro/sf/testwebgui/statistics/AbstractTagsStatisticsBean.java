/*******************************************************************************
 * Copyright (c) 2009-2018, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/

package com.exactpro.sf.testwebgui.statistics;

import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.testwebgui.general.SessionStored;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

//TODO refactor all statistics beans
public abstract class AbstractTagsStatisticsBean extends AbstractStatisticsBean {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTagsStatisticsBean.class);

    @SessionStored
    protected List<Tag> tags = new ArrayList<>();

    protected List<Tag> allTags;

    protected Tag tagToAdd;

    @SessionStored
    protected boolean anyTag = false;

    @Override
    protected void initByStatistics(StatisticsService statisticsService) {
        super.initByStatistics(statisticsService);
        this.allTags = statisticsService.getStorage().getAllTags();
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public void onTagSelect() {

        logger.debug("Tag select invoked");

        this.tags.add(tagToAdd);

        this.tagToAdd = null;

        this.allTags.removeAll(tags);

    }

    public void removeTag(Tag tag) {

        this.tags.remove(tag);

        this.allTags.add(tag);

    }

    protected List<Tag> completeTag(String query, List<Tag> allTags) {

        List<Tag> result = new ArrayList<>();

        String loweredQuery = query.toLowerCase();

        for (Tag tag : allTags) {

            if (tag.getName().toLowerCase().contains(loweredQuery)) {

                result.add(tag);

            }

        }

        return result;

    }

    public List<Tag> completeTag(String query) {
        return completeTag(query, this.allTags);
    }

    public Tag getTagToAdd() {
        return tagToAdd;
    }

    public void setTagToAdd(Tag tagToAdd) {
        this.tagToAdd = tagToAdd;
    }

    public boolean isAnyTag() {
        return anyTag;
    }

    public void setAnyTag(boolean anyTag) {
        this.anyTag = anyTag;
    }

    public List<Tag> getAllTags() {
        return allTags;
    }

    protected AggregateReportParameters getDefaultParams() {
        AggregateReportParameters params = super.getDefaultParams();

        params.setTags(tags);
        params.setAllTags(!anyTag);

        return params;
    }
}
