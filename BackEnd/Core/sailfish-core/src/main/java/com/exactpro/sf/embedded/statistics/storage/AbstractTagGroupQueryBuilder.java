/******************************************************************************
 * Copyright (c) 2009-2019, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/
package com.exactpro.sf.embedded.statistics.storage;

import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.scriptrunner.StatusType;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.text.StringSubstitutor;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.exactpro.sf.embedded.statistics.storage.NativeQueryUtil.TAG_GROUP_TEMP_TABLE;

public abstract class AbstractTagGroupQueryBuilder implements Cloneable {

    public static final Map<String, Type> FIELD_NAMES_AND_TYPES = new ImmutableMap.Builder<String, Type>()
        .put("total_exec_time", LongType.INSTANCE)
        .put("total_tcs", LongType.INSTANCE)
        .put("passed_tcs", LongType.INSTANCE)
        .put("conditionally_passed_tcs", LongType.INSTANCE)
        .put("failed_tcs", LongType.INSTANCE)
        .put("passed_percent", BigDecimalType.INSTANCE)
        .put("conditionally_passed_percent", BigDecimalType.INSTANCE)
        .put("failed_percent", BigDecimalType.INSTANCE)
        .put("total_matrixruns", IntegerType.INSTANCE)
        .put("failed_matrices", IntegerType.INSTANCE)
        .build();

    private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    
    private static final String MAIN_QUERY_PATTERN = 
        " SELECT  " +
            "    ${dimensions} " +
            "    sum(CASE WHEN TCR.rank = 1 THEN ${totalTimeField} ELSE 0 END) AS total_exec_time, " +
            "    count(DISTINCT TCR.id) AS total_tcs, " +
            "    sum(CASE WHEN TCR.status = ${passed} THEN 1 ELSE 0 END) AS passed_tcs, " +
            "    sum(CASE WHEN TCR.status = ${conditionallyPassed} THEN 1 ELSE 0 END) AS conditionally_passed_tcs, " +
            "    sum(CASE WHEN TCR.status = ${failed} THEN 1 ELSE 0 END) AS failed_tcs, " +
            "    CASE WHEN (count(TCR.id) <> 0) THEN (sum(CASE WHEN TCR.status = ${passed} THEN 1.0 ELSE 0.0 END) / count(DISTINCT TCR.id) * 100) ELSE 0.0 END AS passed_percent, " +
            "    CASE WHEN (count(TCR.id) <> 0) THEN (sum(CASE WHEN TCR.status = ${conditionallyPassed}  THEN 1.0 ELSE 0.0 END) / count(distinct TCR.id) * 100) ELSE 0.0 END AS conditionally_passed_percent, " +
            "    CASE WHEN (count(TCR.id) <> 0) THEN (sum(CASE WHEN TCR.status = ${failed} THEN 1.0 ELSE 0.0 END) / count(DISTINCT TCR.id) * 100) ELSE 0.0 END AS failed_percent, " +
            "    count(DISTINCT MR.id) AS total_matrixruns, " +
            "    sum(CASE WHEN MR.failReason IS NULL THEN 0 ELSE 1 END) as failed_matrices  " +
            " FROM (" +
            "    SELECT " +
            "        ${nestedResultIds}, " +
            "        ${nestedDimensions}" +
            "    FROM ${tempTable}" + 
            "    ${nestedJoinTables}" +
            "    GROUP BY ${nestedGroupFields}" +
            "    HAVING COUNT(DISTINCT(${tempTable}.group_id))= ${groupCount}" +
            "    ) as filtered    " +
            " ${joinTables} " +
            " ${where} " +
            "GROUP BY ${groupFields} " +
            "ORDER BY ${orderFields}";

    private static final String NESTED_MATRIX_RUN_IDS =  TAG_GROUP_TEMP_TABLE + ".mr_id as mr_id";
    private static final String NESTED_TESTCASE_RUN_IDS = TAG_GROUP_TEMP_TABLE + ".tcr_id as tcr_id";
    private static final String NESTED_ALL_RUN_IDS =
            "    CASE WHEN "  + TAG_GROUP_TEMP_TABLE + ".mr_id IS NULL THEN TCR.matrix_run_id ELSE " +
                    TAG_GROUP_TEMP_TABLE +".mr_id END as mr_id," +
            "    TCR.id as tcr_id";

    private static final String NESTED_JOIN_TABLES = "LEFT JOIN sttestcaseruns AS TCR ON " + TAG_GROUP_TEMP_TABLE +
            ".mr_id = TCR.matrix_run_id or "+ TAG_GROUP_TEMP_TABLE + ".tcr_id = TCR.id";

    private static final String JOIN_TABLES_FOR_MATRIX_RUN_IDS =
            "    JOIN stmatrixruns MR ON filtered.mr_id = MR.id" +
            "    LEFT JOIN sttestcaseruns TCR ON  MR.id = TCR.matrix_run_id";

    private static final String JOIN_TABLES_FOR_TESTCASE_RUN_IDS =
            "    JOIN sttestcaseruns TCR ON TCR.id = filtered.tcr_id" +
            "    JOIN stmatrixruns MR ON MR.id = TCR.matrix_run_id";

    private static final String JOIN_TABLES_FOR_ALL_RUN_IDS =
            "    JOIN stmatrixruns MR ON filtered.mr_id = MR.id" +
            "    LEFT JOIN sttestcaseruns TCR ON filtered.tcr_id = TCR.id";

    private JoinType joinType = JoinType.TEST_CASE_AND_MATRIX_RUN_TAGS;

    private String dimensions;
    private String nestedDimensions;
    private String groupFields;
    private String orderFields;
    private String groupCount;
    private Date from;
    private Date to;
    private List<SfInstance> sfInstances;

    protected abstract String getTotalTimeField();
    protected abstract String formatTimestamp(String value);
    public abstract AbstractTagGroupQueryBuilder clone();

    public AbstractTagGroupQueryBuilder setJoinType(JoinType joinType) {
        this.joinType = joinType;
        return this;
    }

    public AbstractTagGroupQueryBuilder setDimensions(String dimensions) {
        this.dimensions = dimensions;
        return this;
    }

    public AbstractTagGroupQueryBuilder setNestedDimensions(String nestedDimensions) {
        this.nestedDimensions = nestedDimensions;
        return this;
    }

    public AbstractTagGroupQueryBuilder setGroupFields(String groupFields) {
        this.groupFields = groupFields;
        return this;
    }

    public AbstractTagGroupQueryBuilder setOrderFields(String orderFields) {
        this.orderFields = orderFields;
        return this;
    }

    public AbstractTagGroupQueryBuilder setGroupCount(String groupCount) {
        this.groupCount = groupCount;
        return this;
    }

    public AbstractTagGroupQueryBuilder setFrom(Date from) {
        this.from = from;
        return this;
    }

    public AbstractTagGroupQueryBuilder setTo(Date to) {
        this.to = to;
        return this;
    }

    public AbstractTagGroupQueryBuilder setSfInstances(List<SfInstance> sfInstances) {
        this.sfInstances = sfInstances;
        return this;
    }

    public String build() {
        Map<String, String> variables = new HashMap<>();
        variables.put("tempTable", TAG_GROUP_TEMP_TABLE);
        variables.put("dimensions", dimensions);
        variables.put("passed", String.valueOf(StatusType.PASSED.getId()));
        variables.put("conditionallyPassed", String.valueOf(StatusType.CONDITIONALLY_PASSED.getId()));
        variables.put("failed", String.valueOf(StatusType.FAILED.getId()));
        variables.put("nestedDimensions", nestedDimensions);
        variables.put("totalTimeField", getTotalTimeField());
        variables.put("nestedResultIds", joinType.nestedResultFields);
        variables.put("nestedJoinTables", joinType.nestedJoinTables);
        variables.put("nestedGroupFields", joinType.nestedGroupFields);
        variables.put("joinTables", joinType.resultJoinTables);
        variables.put("where", buildWhereClause());
        variables.put("groupCount", groupCount);
        variables.put("groupFields", groupFields);
        variables.put("orderFields", orderFields);
        return new StringSubstitutor(variables).replace(MAIN_QUERY_PATTERN);
    }

    protected void cloneTo(AbstractTagGroupQueryBuilder clone) {
        clone.setDimensions(dimensions);
        clone.setNestedDimensions(nestedDimensions);
        clone.setGroupFields(groupFields);
        clone.setOrderFields(orderFields);
        clone.setGroupCount(groupCount);
        clone.setJoinType(joinType);
        if (sfInstances != null) {
            clone.setSfInstances(Collections.unmodifiableList(sfInstances));
        }
        if (from != null) {
            clone.setFrom(new Date(from.getTime()));
        }
        if (to != null) {
            clone.setTo(new Date(to.getTime()));
        }
    }

    private String buildWhereClause() {
        StringBuilder whereBuilder = new StringBuilder();
        if (this.from != null) {
            addTimestamp(whereBuilder, from, "starttime", ">=");
        }
        if (this.to != null) {
            addTimestamp(whereBuilder, to, "finishtime", "<=");
        }
        if (CollectionUtils.isNotEmpty(sfInstances)) {
            String sfIds = sfInstances.stream()
                            .map(sfInstance -> String.valueOf(sfInstance.getId()))
                            .collect(Collectors.joining(","));
            appendWhereAnd(whereBuilder);
            whereBuilder.append("MR.sf_id IN (")
                    .append(sfIds)
                    .append(") ");
        }
        return whereBuilder.toString();
    }

    private void addTimestamp(StringBuilder whereBuilder, Date timestamp, String fieldName, String operation) {
        appendWhereAnd(whereBuilder);
        String value = TIMESTAMP_FORMAT.get().format(timestamp);
        whereBuilder.append("MR.")
                .append(fieldName)
                .append(" ")
                .append(operation)
                .append(" ")
                .append(formatTimestamp(value));
    }

    private void appendWhereAnd(StringBuilder whereBuilder) {
        if (whereBuilder.length() == 0) {
            whereBuilder.append(" WHERE ");
        } else {
            whereBuilder.append(" AND ");
        }
    }

    public enum JoinType {
        TEST_CASE_RUN_TAGS (NESTED_TESTCASE_RUN_IDS, JOIN_TABLES_FOR_TESTCASE_RUN_IDS, "", "1"),
        MATRIX_RUN_TAGS (NESTED_MATRIX_RUN_IDS, JOIN_TABLES_FOR_MATRIX_RUN_IDS, "", "1"),
        TEST_CASE_AND_MATRIX_RUN_TAGS (NESTED_ALL_RUN_IDS, JOIN_TABLES_FOR_ALL_RUN_IDS, NESTED_JOIN_TABLES, "1,2");

        private final String nestedResultFields;
        private final String resultJoinTables;
        private final String nestedJoinTables;
        private final String nestedGroupFields;

        JoinType(String nestedResultFields, String resultJoinTables, String nestedJoinTables, String nestedGroupFields) {
            this.nestedResultFields = nestedResultFields;
            this.resultJoinTables = resultJoinTables;
            this.nestedJoinTables = nestedJoinTables;
            this.nestedGroupFields = nestedGroupFields;
        }
    }
}
