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
package com.exactpro.sf.embedded.statistics.storage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.exactpro.sf.embedded.statistics.storage.AbstractTagGroupQueryBuilder.JoinType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupDimension;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportResult;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportRow;
import com.exactpro.sf.embedded.storage.HibernateStorageSettings;

import static com.exactpro.sf.embedded.statistics.storage.NativeQueryUtil.*;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ObjectArrays;

public class NativeDbOperations {

	private static final Logger logger = LoggerFactory.getLogger(NativeDbOperations.class);

	private static final String DIMENSION_NAME_POSTFIX = "__d_";

	private static final String DURATION_FORMAT = "HH:mm:ss";

	private static final String FILTERED_NAME = "filtered";

    private final HibernateStorageSettings settings;

	public NativeDbOperations(HibernateStorageSettings settings) {
		this.settings = settings;
	}

	private String toMysqlSet(Long[] ids) {

		StringBuilder sb = new StringBuilder();

		for(int i=0; i<ids.length; i++) {

			sb.append(ids[i]);

			if(i != ids.length - 1) {

				sb.append(",");

			}

		}

		return sb.toString();

	}

	private String toMysqlSet(Collection<Long> ids) {

		StringBuilder sb = new StringBuilder();

		String delimiter = "";

		for (Long id : ids) {

		    sb.append(delimiter);
			sb.append(id);

			delimiter = ",";

		}

		return sb.toString();

	}

	@SuppressWarnings("unchecked")
	public List<Object[]> executeTaggedComparisonQuery(Session session, Long[] firstSetIds, Long[] secondSetIds) {
        Long[] allTags = ImmutableSet.copyOf(ObjectArrays.concat(firstSetIds, secondSetIds, Long.class))
                .toArray(new Long[0]);

		if(isPostgreSql(this.settings.getDbms())) {

			String queryString = String.format("SELECT * FROM tagged_sets_comparison(ARRAY %s, ARRAY %s, :fcount, ARRAY %s, :scount )",
                    Arrays.toString(allTags), Arrays.toString(firstSetIds), Arrays.toString(secondSetIds));

			Query query = session.createSQLQuery(queryString);

			query.setLong("fcount", firstSetIds.length);
			query.setLong("scount", secondSetIds.length);

			return query.list();

		} else if(isMysql(this.settings.getDbms())) {

			String queryString = String.format("call tagged_sets_comparison('%s', '%s', :fcount, '%s', :scount )",
                    toMysqlSet(allTags), toMysqlSet(firstSetIds), toMysqlSet(secondSetIds));

			SQLQuery query = session.createSQLQuery(queryString);

			query.addScalar("testcaseid", StringType.INSTANCE);

			query.addScalar("ftcid", StringType.INSTANCE);
			query.addScalar("fmatrix", StringType.INSTANCE);
			query.addScalar("fstatus", IntegerType.INSTANCE);
			query.addScalar("fdescription", StringType.INSTANCE);
			query.addScalar("ffailreason", StringType.INSTANCE);
			query.addScalar("ffailedactions", StringType.INSTANCE);
			query.addScalar("fcomment", StringType.INSTANCE);
			query.addScalar("fuserStatus", StringType.INSTANCE);
			query.addScalar("ftcrid", BigIntegerType.INSTANCE);
			query.addScalar("fmrid", BigIntegerType.INSTANCE);
			query.addScalar("fstarttime", TimestampType.INSTANCE);
			query.addScalar("ffinishtime", TimestampType.INSTANCE);
			query.addScalar("ftags", StringType.INSTANCE);
			query.addScalar("fhash", IntegerType.INSTANCE);
			query.addScalar("fhost", StringType.INSTANCE);
			query.addScalar("fname", StringType.INSTANCE);
			query.addScalar("fport", IntegerType.INSTANCE);
			query.addScalar("fchost", StringType.INSTANCE);
			query.addScalar("fcname", StringType.INSTANCE);
			query.addScalar("fcport", IntegerType.INSTANCE);
            query.addScalar("freportfolder", StringType.INSTANCE);
            query.addScalar("freportfile", StringType.INSTANCE);

			query.addScalar("stcid", StringType.INSTANCE);
			query.addScalar("smatrix", StringType.INSTANCE);
			query.addScalar("sstatus", IntegerType.INSTANCE);
			query.addScalar("sdescription", StringType.INSTANCE);
			query.addScalar("sfailreason", StringType.INSTANCE);
			query.addScalar("sfailedactions", StringType.INSTANCE);
			query.addScalar("scomment", StringType.INSTANCE);
			query.addScalar("suserStatus", StringType.INSTANCE);
			query.addScalar("stcrid", BigIntegerType.INSTANCE);
			query.addScalar("smrid", BigIntegerType.INSTANCE);
			query.addScalar("sstarttime", TimestampType.INSTANCE);
			query.addScalar("sfinishtime", TimestampType.INSTANCE);
			query.addScalar("stags", StringType.INSTANCE);
			query.addScalar("shash", IntegerType.INSTANCE);
            query.addScalar("shost", StringType.INSTANCE);
            query.addScalar("sname", StringType.INSTANCE);
            query.addScalar("sport", IntegerType.INSTANCE);
            query.addScalar("schost", StringType.INSTANCE);
            query.addScalar("scname", StringType.INSTANCE);
            query.addScalar("scport", IntegerType.INSTANCE);
            query.addScalar("sreportfolder", StringType.INSTANCE);
            query.addScalar("sreportfile", StringType.INSTANCE);

			query.setLong("fcount", firstSetIds.length);
			query.setLong("scount", secondSetIds.length);

			logger.info(queryString);


			return query.list();

		}

        throw new UnsupportedOperationException("Unsupported DBMS " + settings.getDbms());

	}

	private String buildDimensionPostfix(TagGroupDimension dimension, int index) {
        String delimiter = isPostgreSql(this.settings.getDbms())
                ? "\""
                : "`";
		return delimiter + dimension.getName() + DIMENSION_NAME_POSTFIX + index + delimiter;
	}

    private String buildDimensionTagsColumns(TagGroupReportParameters params) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<= params.getLoadForLevel(); i++) {
            TagGroupDimension dimension = params.getDimensions().get(i);
            sb.append("'").append(dimension.getName()).append("'");
            sb.append(" AS ");
            sb.append(buildDimensionPostfix(dimension, i));
            sb.append(", ");
        }
        return sb.toString();
    }

	private String buildDimensionFilteredColumns(TagGroupReportParameters params) {

		StringBuilder sb = new StringBuilder();

		for(int i = 0; i<= params.getLoadForLevel(); i++) {

			TagGroupDimension dimension = params.getDimensions().get(i);

            sb.append(FILTERED_NAME).append(".d").append(i);

			sb.append(" AS ");
			sb.append(buildDimensionPostfix(dimension, i));
			sb.append(", ");

		}

		return sb.toString();

	}

	private String buildDimensionFilter(TagGroupReportParameters params, boolean isPostgres) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<= params.getLoadForLevel(); i++) {
            TagGroupDimension dimension = params.getDimensions().get(i);
            if (dimension.isTag()) {
                if (isPostgres) {
                    sb.append("cast(")
                        .append("'").append(dimension.getName()).append("'")
                        .append(" AS TEXT)");
                } else {
                    sb.append("'").append(dimension.getName()).append("'");
                }
            } else {
                sb.append(" MAX(CASE WHEN (" + TAG_GROUP_TEMP_TABLE + ".group_id = ")
                    .append(dimension.getId())
                    .append(") THEN " + TAG_GROUP_TEMP_TABLE + ".tag_name ELSE NULL END) ");
            }
            sb.append(" AS d").append(i);
            if (i < params.getLoadForLevel() ) {
                sb.append(", ");
            }
        }
        return sb.toString();

	}

	private List<TagGroupReportRow> parseTagGroupResultSet(List<Object[]> rs, TagGroupReportParameters params) {

		List<TagGroupReportRow> result = new ArrayList<>();

		for(Object[] rsRow : rs) {

			logger.info("{}", (Object)rsRow);

			TagGroupReportRow row = new TagGroupReportRow();
			int index = 0;

			String[] dimensions = new String[params.getLoadForLevel() + 1];

			for(int i = 0; i < params.getLoadForLevel() + 1; i++) {
				dimensions[i] = (String)rsRow[ index++ ];
			}

			row.setDimensionsPath(dimensions);
			row.setTotalExecTime((Long) rsRow[index++]);
			row.setTotalTcCount((Long) rsRow[index++]);
			row.setPassedCount((Long) rsRow[index++]);
			row.setConditionallyPassedCount((Long) rsRow[index++]);
			row.setFailedCount((Long) rsRow[index++]);
			row.setPassedPercent((BigDecimal) rsRow[index++]);
			row.setConditionallyPassedPercent((BigDecimal) rsRow[index++]);
			row.setFailedPercent((BigDecimal) rsRow[index++]);
                        row.setTotalMatrices((Integer) rsRow[index++]);
                        row.setFailedMatrices((Integer) rsRow[index++]);

			row.setFormattedExecTime(DurationFormatUtils.formatDuration(row.getTotalExecTime() * 1000l, DURATION_FORMAT));

			logger.info("{}", row);

			result.add(row);

		}

		return result;

	}

	@SuppressWarnings("unchecked")
	public TagGroupReportResult generateTagGroupReport(Session session, TagGroupReportParameters params,
                                                       AbstractTagGroupQueryBuilder queryBuilder) {

		boolean isPostgres = isPostgreSql(this.settings.getDbms());

		TagGroupReportResult result = new TagGroupReportResult();

		List<Long> groupingColumns = new ArrayList<>();

		for(int i = 0; i<= params.getLoadForLevel(); i++) {

			groupingColumns.add(i + 1l);

		}

        boolean dimensionsExist = isDimensionsExist(params);
        String groupingColumnsString = toMysqlSet(groupingColumns);

        queryBuilder.setDimensions(dimensionsExist
                        ? buildDimensionFilteredColumns(params)
                        : buildDimensionTagsColumns(params))
                .setNestedDimensions(buildDimensionFilter(params, isPostgres))
                .setGroupCount(String.valueOf(params.getNumberOfGroups()))
                .setGroupFields(groupingColumnsString)
                .setOrderFields(groupingColumnsString)
                .setFrom(params.getFrom())
                .setTo(params.getTo())
                .setSfInstances(params.getSelectedSfInstances());

        String stringQuery = queryBuilder.build();

        logger.debug(stringQuery);

        SQLQuery query = session.createSQLQuery(stringQuery);

        for(int i =0; i < groupingColumns.size(); i++) {
            query.addScalar(buildDimensionPostfix(params.getDimensions().get(i), i), StringType.INSTANCE);
        }

        for (Map.Entry<String, Type> fieldNameAndType :
                AbstractTagGroupQueryBuilder.FIELD_NAMES_AND_TYPES.entrySet()) {
            query.addScalar(fieldNameAndType.getKey(), fieldNameAndType.getValue());
        }

        result.setRows( parseTagGroupResultSet(query.list(), params) );

        return result;

	}

    public void createTagGroupTempTable(Session session, Collection<Long> tagIds) {
        String query = DROP_TAG_GROUP_TEMP_TABLE;
        SQLQuery sqlQuery = session.createSQLQuery(query);
        sqlQuery.executeUpdate();

        query = CREATE_TAG_GROUP_TEMP_TABLE.replace(":tagIds",
                tagIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));
        sqlQuery = session.createSQLQuery(query);
        sqlQuery.executeUpdate();
    }

    public JoinType recognizeJoinType(Session session) {
        SQLQuery sqlQuery = session.createSQLQuery(NativeQueryUtil.CHECK_TESTCASE_TAGS_QUERY);
        List list = sqlQuery.list();
        if (list == null || list.isEmpty()) {
            return JoinType.MATRIX_RUN_TAGS;
        }
        sqlQuery = session.createSQLQuery(NativeQueryUtil.CHECK_MATRIX_TAGS_QUERY);
        list = sqlQuery.list();
        if (list == null || list.isEmpty()) {
            return  JoinType.TEST_CASE_RUN_TAGS;
        }
        return JoinType.TEST_CASE_AND_MATRIX_RUN_TAGS;
    }

    private boolean isDimensionsExist(TagGroupReportParameters params) {
        return CollectionUtils.isNotEmpty(params.getTagIds());
    }
}
