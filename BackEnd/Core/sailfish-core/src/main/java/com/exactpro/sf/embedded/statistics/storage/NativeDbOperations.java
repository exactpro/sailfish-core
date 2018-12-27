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

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.embedded.statistics.configuration.DbmsType;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupDimension;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportResult;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportRow;
import com.exactpro.sf.embedded.storage.HibernateStorageSettings;
import com.exactpro.sf.scriptrunner.StatusType;

public class NativeDbOperations {

	private static final Logger logger = LoggerFactory.getLogger(NativeDbOperations.class);

	private static final String DIMENSION_NAME_POSTFIX = "__d_";

	private static final String DURATION_FORMAT = "HH:mm:ss";

	private HibernateStorageSettings settings;

	public NativeDbOperations(HibernateStorageSettings settings) {
		this.settings = settings;
	}

	private boolean isMysql() {

		return this.settings.getDbms().equals(DbmsType.MySql.getValue());

	}

	private boolean isPostgreSql() {

		return this.settings.getDbms().equals(DbmsType.PostgreSQL.getValue());

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

		if(isPostgreSql()) {

			String queryString = String.format("SELECT * FROM tagged_sets_comparison(ARRAY %s, :fcount, ARRAY %s, :scount )",
					Arrays.toString(firstSetIds), Arrays.toString(secondSetIds));

			Query query = session.createSQLQuery(queryString);

			query.setLong("fcount", firstSetIds.length);
			query.setLong("scount", secondSetIds.length);

			return query.list();

		} else if(isMysql()) {

			String queryString = String.format("call tagged_sets_comparison('%s', :fcount, '%s', :scount )",
					toMysqlSet(firstSetIds), toMysqlSet(secondSetIds));

			SQLQuery query = session.createSQLQuery(queryString);

			query.addScalar("testcaseid", StringType.INSTANCE);

			query.addScalar("ftcid", BigIntegerType.INSTANCE);
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

			query.addScalar("stcid", BigIntegerType.INSTANCE);
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

			query.setLong("fcount", firstSetIds.length);
			query.setLong("scount", secondSetIds.length);

			logger.info(queryString);


			return query.list();

		}

		throw new UnsupportedOperationException("Unsupported DBMS " + this.settings.getDbms());

	}

	/*private String buildTagGroupSqlQuery(TagGroupReportParameters params) {

	}*/

	private String buildDimensionPostfix(TagGroupDimension dimension, int index) {
		return dimension.getName() + DIMENSION_NAME_POSTFIX + index;
	}

	private String buildDimensionColumns(TagGroupReportParameters params) {

		StringBuilder sb = new StringBuilder();

		for(int i = 0; i<= params.getLoadForLevel(); i++) {

			TagGroupDimension dimension = params.getDimensions().get(i);

			if(dimension.isTag()) {

				sb.append("'").append(dimension.getName()).append("'");

			} else {
				sb.append(String.format("(SELECT T.name FROM stmatrixruns MR2 "
			            + "JOIN stmrtags MRT ON MR2.id = MRT.mr_id "
			            + "JOIN sttags T ON MRT.tag_id = T.id "
			            + "JOIN sttaggroups G ON T.group_id = G.id "
			            + "WHERE MR2.id = MR.id  "
			            + "AND G.id = %s ORDER BY T.id LIMIT 1 ) ", dimension.getId()));
			}

			sb.append(" AS ");
			sb.append(buildDimensionPostfix(dimension, i));
			sb.append(", ");

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
	public TagGroupReportResult generateTagGroupReport(Session session, TagGroupReportParameters params) {

		boolean isPostgres = isPostgreSql();

		TagGroupReportResult result = new TagGroupReportResult();

		StringBuilder queryBuilder = new StringBuilder();
		List<Long> groupingColumns = new ArrayList<>();

		for(int i = 0; i<= params.getLoadForLevel(); i++) {

			groupingColumns.add(i + 1l);

		}

		// Build query

		queryBuilder.append("SELECT ");

		queryBuilder.append(buildDimensionColumns(params));

		String execTimeCalc;

		if(isPostgres) {

            execTimeCalc = "CASE WHEN TCR.rank = 1 THEN extract ('epoch' from MR.finishTime - MR.startTime) ELSE 0 END";

		} else { // Mysql

            execTimeCalc = "CASE WHEN TCR.rank = 1 THEN TIMESTAMPDIFF(SECOND, MR.startTime, MR.finishTime) ELSE 0 END";

		}

		queryBuilder.append("sum(");

		queryBuilder.append(execTimeCalc);

	        queryBuilder.append(") AS total_exec_time, " + "count(DISTINCT TCR.id) AS total_tcs, "
                    + "sum(CASE WHEN TCR.status = " + StatusType.PASSED.getId() + " THEN 1 ELSE 0 END) AS passed_tcs, "
                    + "sum(CASE WHEN TCR.status = " + StatusType.CONDITIONALLY_PASSED.getId() + " THEN 1 ELSE 0 END) AS conditionally_passed_tcs, " 
                    + "sum(CASE WHEN TCR.status = " + StatusType.FAILED.getId() + " THEN 1 ELSE 0 END) AS failed_tcs, "

                    + "CASE WHEN (count(TCR.id) <> 0) THEN (sum(CASE WHEN TCR.status = " + StatusType.PASSED.getId() + " THEN 1.0 ELSE 0.0 END) / count(DISTINCT TCR.id) * 100) ELSE 0.0 END AS passed_percent, "
                    + "CASE WHEN (count(TCR.id) <> 0) THEN (sum(CASE WHEN TCR.status = " + StatusType.CONDITIONALLY_PASSED.getId() + " THEN 1.0 ELSE 0.0 END) / count(distinct TCR.id) * 100) ELSE 0.0 END AS conditionally_passed_percent, "
                    + "CASE WHEN (count(TCR.id) <> 0) THEN (sum(CASE WHEN TCR.status = " + StatusType.FAILED.getId() + " THEN 1.0 ELSE 0.0 END) / count(DISTINCT TCR.id) * 100) ELSE 0.0 END AS failed_percent, "
                    + "count(DISTINCT MR.id) AS total_matrixruns, "
                    + "sum(CASE WHEN MR.failReason IS NULL THEN 0 ELSE 1 END) as failed_matrices "

                    + "FROM stmatrixruns MR LEFT JOIN sttestcaseruns TCR ON MR.id = TCR.matrix_run_id ");

	        queryBuilder.append("WHERE ");

        if (params.getNumberOfGroups() > 0 && params.getTagIds() != null && !params.getTagIds().isEmpty()) {
            queryBuilder.append("(SELECT count(DISTINCT G2.id) ")
                .append("FROM stmatrixruns MR2 ").append("    JOIN stmrtags MRT ON MR2.id = MRT.mr_id ")
                .append("    JOIN sttags T ON MRT.tag_id = T.id ")
                .append("    JOIN sttaggroups G2 ON T.group_id = G2.id ")
                .append("WHERE MR2.id = MR.id ")
                .append("AND T.id IN (").append(toMysqlSet(params.getTagIds())).append(") ")
                .append(") = ").append(params.getNumberOfGroups()).append(" ");
        	}

		queryBuilder.append(" GROUP BY " + toMysqlSet(groupingColumns));
		queryBuilder.append(" ORDER BY " + toMysqlSet(groupingColumns));

		logger.info("{}", queryBuilder);

		SQLQuery query = session.createSQLQuery(queryBuilder.toString());

		// set types

		for(int i =0; i < groupingColumns.size(); i++) {
			query.addScalar(buildDimensionPostfix(params.getDimensions().get(i), i), StringType.INSTANCE);
		}
		query.addScalar("total_exec_time", LongType.INSTANCE);
		query.addScalar("total_tcs", LongType.INSTANCE);
		query.addScalar("passed_tcs", LongType.INSTANCE);
		query.addScalar("conditionally_passed_tcs", LongType.INSTANCE);
		query.addScalar("failed_tcs", LongType.INSTANCE);
		query.addScalar("passed_percent", BigDecimalType.INSTANCE);
		query.addScalar("conditionally_passed_percent", BigDecimalType.INSTANCE);
		query.addScalar("failed_percent", BigDecimalType.INSTANCE);
                query.addScalar("total_matrixruns", IntegerType.INSTANCE);
                query.addScalar("failed_matrices", IntegerType.INSTANCE);

		// execute

		result.setRows( parseTagGroupResultSet(query.list(), params) );

		return result;

	}

}
