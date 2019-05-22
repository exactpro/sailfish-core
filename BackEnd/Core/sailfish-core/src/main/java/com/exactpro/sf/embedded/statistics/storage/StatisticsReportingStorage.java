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

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupDimension;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.embedded.statistics.entities.KnownBug;
import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.entities.TagGroup;
import com.exactpro.sf.embedded.statistics.entities.TestCaseRunStatus;
import com.exactpro.sf.embedded.statistics.storage.reporting.ActionInfoRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.AggregateReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.KnownBugRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.ScriptWeatherRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.TagGroupReportResult;
import com.exactpro.sf.embedded.statistics.storage.reporting.TaggedComparisonResult;
import com.exactpro.sf.embedded.statistics.storage.reporting.TaggedComparisonRow;
import com.exactpro.sf.embedded.statistics.storage.reporting.TaggedComparisonRow.TaggedComparisonSet;
import com.exactpro.sf.embedded.statistics.storage.reporting.TaggedSetsComparisonParameters;
import com.exactpro.sf.embedded.statistics.storage.reporting.TestCasesDisplayMode;
import com.exactpro.sf.embedded.storage.HibernateStorageSettings;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.util.DateTimeUtility;

public class StatisticsReportingStorage implements IAdditionalStatisticsLoader {

	private static final Logger logger = LoggerFactory.getLogger(StatisticsReportingStorage.class);

	// See V2.0__compare_tagged_function migration script
	private static final String FAILED_ACTION_SPLITTER = "FACTSPLITTER";
	private static final String ACTION_RANK_FREASON_SPLITTER = "ARFRSPLITTER";

    private final SessionFactory sessionFactory;

    private final HibernateStorageSettings settings;

	public StatisticsReportingStorage(SessionFactory sessionFactory, HibernateStorageSettings settings) {

		this.sessionFactory = sessionFactory;
		this.settings = settings;

	}

	private List<AggregatedReportRow> parseAggregatedReportResult(List<Object[]> results) {

		List<AggregatedReportRow> result = new ArrayList<>();

		for(Object[] row : results) {

			logger.trace("Row {}", (Object)row);

			AggregatedReportRow parsedRow = new AggregatedReportRow();

			parsedRow.setSfId((Long)row[0]);
			parsedRow.setMatrixName((String)row[1]);
			if(row[2] != null) {
				parsedRow.setDescription((String)row[2]);
			}
			if(row[3] != null) {
				parsedRow.setStatus(StatusType.getStatusType((Integer)row[3]));
			}
			if(row[4] != null) {
				parsedRow.setFailReason((String)row[4]);
			}

			parsedRow.setStartTime((LocalDateTime)row[5]);
			if(row[6] != null) {
				parsedRow.setFinishTime((LocalDateTime)row[6]);
			}
			parsedRow.setUserName((String)row[7]);
			parsedRow.setHost((String)row[8]);

			parsedRow.setTestCaseRunId((Long)row[9]);
			parsedRow.setMatrixStartTime((LocalDateTime)row[10]);
			parsedRow.setMatrixFinishTime((LocalDateTime)row[11]);
			parsedRow.setEnvironmentName((String)row[12]);
			parsedRow.setTestCaseName("Test Case " + row[13]);
            parsedRow.setTestCaseNumber((Long)row[13]);

			parsedRow.setSfName((String)row[14]);
			parsedRow.setPort((Integer)row[15]);
			parsedRow.setMatrixRunId((Long)row[16]);
			parsedRow.setTestCaseId((String)row[17]);

			parsedRow.setReportFolder((String)row[18]);
			parsedRow.setReportFile((String)row[19]);

			TestCaseRunComments comments = parsedRow.getUserComments();

			comments.setComment((String)row[20]);
			comments.setFixedVersion((String)row[21]);

			Long runStatusId = (Long)row[22];

			if(runStatusId != null) {

				TestCaseRunStatus tcrStatus = new TestCaseRunStatus();

				tcrStatus.setId(runStatusId);
				tcrStatus.setName((String)row[23]);

				comments.setStatus(tcrStatus);

			}
            if (row[24] != null) {
                parsedRow.setHash((Integer)row[24]);
            }

			if (row.length == 30) {
                Long sfCurrentID = (Long)row[25];
                if (sfCurrentID != null) {
                    SfInstance sfCurrent = new SfInstance();

                    sfCurrent.setId(sfCurrentID);
                    sfCurrent.setHost((String)row[26]);
                    sfCurrent.setPort((Integer)row[27]);
                    sfCurrent.setName((String)row[28]);

                    parsedRow.setSfCurrentInstance(sfCurrent);
                }
                if (row[29] != null) {
                    parsedRow.setMatrixFailReason((String) row[29]);
                }
            }

			result.add(parsedRow);

		}

		if(!result.isEmpty()) {

			loadFailedActions(result);

			findUsedServices(result);

            loadAndAddAllTags(result);

            loadKnownBugInfo(result);

		}

		return result;

	}

    private void loadKnownBugInfo(List<AggregatedReportRow> result) {
        Map<Long, AggregatedReportRow> testCaseRows = new HashMap<>();
        for (AggregatedReportRow row : result) {
            Long testCaseRunId = row.getTestCaseRunId();
            if (testCaseRunId != null) {
                testCaseRows.put(testCaseRunId, row);
            }
        }

        if (testCaseRows.isEmpty()) {
            return;
        }

        Long[] ids = testCaseRows.keySet().toArray(new Long[0]);

        String querySting = "select "
                + "TCR.id, "
                + "ARKB.reproduced, "
                + "count(distinct ARKB.id.knownBug) "
                + "from ActionRun as AR "
                + "join AR.tcRun as TCR "
                + "join AR.actionRunKnownBugs as ARKB "
                + "where TCR.id in (:ids) "
                + "group by TCR.id, ARKB.reproduced";

        Session session = sessionFactory.openSession();
        try (AutoCloseable ignore = session::close) {
            Query query = session.createQuery(querySting);
            query.setParameterList("ids", ids);
            @SuppressWarnings("unchecked")
            List<Object[]> resultList = query.list();
            for (Object[] resultRow : resultList) {
                Long tcId = (Long) resultRow[0];
                Boolean reproduced = (Boolean) resultRow[1];
                Long bugCount = (Long) resultRow[2];
                if (reproduced != null) {
                    AggregatedReportRow row = testCaseRows.get(tcId);
                    if (reproduced) {
                        row.setReproducedKnownBugsCount(bugCount);
                    } else {
                        row.setNonReproducedKnownBugsCount(bugCount);
                    }
                }
            }
        } catch (Exception e) {
            throw new EPSCommonException(e);
        }
    }

	private List<AggregatedReportRow> parseAggregatedTaggedReportResult(List<Object[]> results) {
        List<AggregatedReportRow> result = new ArrayList<>();
        for(Object[] row : results) {
            logger.trace("Row {}", (Object)row);

            AggregatedReportRow parsedRow = new AggregatedReportRow();

            int i = 0;

            parsedRow.setMatrixRunId((Long)row[i++]);
            parsedRow.setTestCaseId((String)row[i++]);
            parsedRow.setMatrixName((String)row[i++]);

            if(row[i] != null) {
                parsedRow.setDescription((String)row[i]);
            }
            i++;

            parsedRow.setHost((String)row[i++]);
            parsedRow.setMatrixStartTime((LocalDateTime) row[i++]);

            if(row[i] != null) {
                parsedRow.setMatrixFinishTime((LocalDateTime) row[i]);
            }
            i++;

            if(row[i] != null) {
                parsedRow.setStatus(StatusType.getStatusType((Integer)row[i]));
            }
            i++;

            if(row[i] != null) {
                parsedRow.setFailReason((String)row[i]);
            }
            i++;

            parsedRow.setTestCaseRunId((Long)row[i]);

            result.add(parsedRow);
        }
        if(!result.isEmpty()) {
            loadFailedActions(result);
            findUsedServices(result);
            loadAndAddAllTags(result);
        }
        return result;

    }

	private Object[] toIds(List<SfInstance> sfInstances) {

		Object[] result = new Object[sfInstances.size()];

		int index = 0;

		for(SfInstance sf : sfInstances) {

			result[index++] = sf.getId();

		}

		return result;

	}

	private Object[] tagsToIds(List<Tag> tags) {

		Object[] result = new Object[tags.size()];

		int index = 0;

		for(Tag tag : tags) {

			result[index++] = tag.getId();

		}

		return result;

	}

	private void loadAndAddAllTags(List<AggregatedReportRow> rows) {
		Map<Long, List<AggregatedReportRow>> matrixRows = new HashMap<>();
		Map<Long, AggregatedReportRow> testCaseRows = new HashMap<>();
		for(AggregatedReportRow row : rows) {
			if(row.getTestCaseRunId() != null) {
                testCaseRows.put(row.getTestCaseRunId(), row);
			}
            List<AggregatedReportRow> reportRows = matrixRows.computeIfAbsent(row.getMatrixRunId(), l -> new ArrayList<>());
			reportRows.add(row);
        }
        loadAndAddMatrixTags(matrixRows);
        if (!testCaseRows.isEmpty()) {
            loadAndAddTestCaseTags(testCaseRows);
        }
	}

	private void findUsedServices(List<AggregatedReportRow> rows) {

		Map<Long, List<AggregatedReportRow>> matrixRunToRow = new HashMap<>();

		for(AggregatedReportRow row : rows) {

			if(!matrixRunToRow.containsKey(row.getMatrixRunId())) {

				List<AggregatedReportRow> matrixRows = new ArrayList<>();

				matrixRows.add(row);

				matrixRunToRow.put(row.getMatrixRunId(), matrixRows);

			} else {

				matrixRunToRow.get(row.getMatrixRunId()).add(row);

			}

		}

		// Find used services

		String queryString = "select distinct "
				+ "MR.id, "
				+ "S.name "
				+ "from ActionRun as AR "
				+ "join AR.tcRun as TCR "
				+ "join TCR.matrixRun as MR "
				+ "join AR.service as S "
				+ "where MR.id in (:ids) "
				+ "order by MR.id";

		Session session = null;

		try {

            session = sessionFactory.openSession();
			Query query = session.createQuery(queryString);

			query.setParameterList("ids", matrixRunToRow.keySet());

			@SuppressWarnings("unchecked")
			List<Object[]> resultSet = query.list();

			Map<Long, StringBuilder> servicesUsed = readServicesUsedResultSet(resultSet);

            for(Entry<Long, StringBuilder> entry : servicesUsed.entrySet()) {

				List<AggregatedReportRow> mrRows = matrixRunToRow.get(entry.getKey());

				for(AggregatedReportRow row : mrRows) {

					row.setServicesUsed(entry.getValue().toString());

				}

			}

		} finally {

			if(session != null) {
				session.close();
			}

		}

	}

	private void loadFailedActions(List<AggregatedReportRow> rows) {

		// Find test case run ids of failed test cases

		List<Long> result = new ArrayList<>();

		Map<Long, AggregatedReportRow> failedTestCaseRunRows = new HashMap<>();

		for(AggregatedReportRow row : rows) {

			if(row.getStatus() == StatusType.FAILED) { //Failed

				result.add(row.getTestCaseRunId());

				failedTestCaseRunRows.put(row.getTestCaseRunId(), row);

			}

		}

		Long[] failedTcRunIds = result.toArray(new Long[result.size()]);

		if(failedTcRunIds.length == 0) {
			return;
		}

		// Find ranks of failed actions

		String queryString = "select "
				+ "TCR.id, "
				+ "AR.rank "
				+ "from ActionRun as AR "
				+ "join AR.tcRun as TCR "
				+ "where TCR.id in (:ids) and AR.status = " + StatusType.FAILED.getId() + " "
				+ "order by TCR.startTime, AR.rank";

		Session session = null;

		try {

            session = sessionFactory.openSession();
			Query query = session.createQuery(queryString);

			query.setParameterList("ids", failedTcRunIds);

			@SuppressWarnings("unchecked")
			List<Object[]> resultSet = query.list();

			Map<Long, StringBuilder> failedActions = readFailedActionsResultSet(resultSet);

            for(Entry<Long, StringBuilder> entry : failedActions.entrySet()) {

				failedTestCaseRunRows.get(entry.getKey()).setFailedActions(entry.getValue().toString());

			}

		} finally {

			if(session != null) {
				session.close();
			}

		}

	}

	private Map<Long, StringBuilder> readFailedActionsResultSet(List<Object[]> resultSet) {

		Map<Long, StringBuilder> result = new HashMap<>();

		for(Object[] rsRow : resultSet) {

			Long tcrId = (Long)rsRow[0];
			Long actionRank = (Long)rsRow[1];

			if(!result.containsKey(tcrId)) {

				StringBuilder sb = new StringBuilder(Long.toString(actionRank));

				result.put(tcrId, sb);

			} else {

                result.get(tcrId).append(", ").append(actionRank);

			}

		}

		return result;

	}

	private Map<Long, StringBuilder> readServicesUsedResultSet(List<Object[]> resultSet) {

		Map<Long, StringBuilder> result = new HashMap<>();

		for(Object[] rsRow : resultSet) {

			Long mrId = (Long)rsRow[0];
			String serviceName = (String)rsRow[1];

			if(!result.containsKey(mrId)) {

				StringBuilder sb = new StringBuilder(serviceName);

				result.put(mrId, sb);

			} else {

				result.get(mrId).append(", ").append(serviceName);

			}

		}

		return result;

	}

	private Map<Long, List<Tag>> readTagsResultSet(List<Object[]> resultSet, BiConsumer<Tag, Object[]> customSetter,
                                                   boolean matrixTag) {

		Map<Long, List<Tag>> result = new HashMap<>();

		for(Object[] rsRow : resultSet) {

			Long id = (Long)rsRow[0];
            Long tagId = (Long)rsRow[1];
			String tagName = (String)rsRow[2];
			String groupName = (String)rsRow[3];

			if(!result.containsKey(id)) {

				List<Tag> tags = new ArrayList<>();

				result.put(id, tags);

			}

			Tag tag = new Tag();

			tag.setId(tagId);
			tag.setName(tagName);
			customSetter.accept(tag, rsRow);
			tag.setForAllTestCaseRuns(matrixTag);

            if (groupName != null) {
                TagGroup tagGroup = new TagGroup();
                tagGroup.setName(groupName);
                tag.setGroup(tagGroup);
            }

			result.get(id).add(tag);

		}

		return result;

	}

    public SortedMap<Long, List<Long>> getMatrixRunAndTestCaseRunIDs(AggregateReportParameters params) {
        boolean tagsFilter = CollectionUtils.isNotEmpty(params.getTags());
        boolean sfInstancesFilter = CollectionUtils.isNotEmpty(params.getSfInstances());

        String queryString = "select MR.id, TCR.id "
                + "from TestCaseRun as TCR "
                + "right join TCR.matrixRun as MR "
                + "join MR.sfInstance as SF "
                + "where MR.startTime >= :from and MR.finishTime <= :to ";

        if (sfInstancesFilter) {
            queryString += "and SF.id in (:ids) ";
        }

        if (tagsFilter) {
            queryString += "and (exists (select MR2.id from "
                    + "MatrixRun as MR2 "
                    + "join MR2.tags as T "
                    + "where MR2.id = MR.id "
                    + "and T.id in (:mrTags) "
                    + "group by MR2.id ) "
                    + "or "
                    + "exists (select TCR2.id from "
                    + "TestCaseRun as TCR2 "
                    + "join TCR2.tags as TCRT "
                    + "where TCR2.id = TCR.id "
                    + "and TCRT.tag.id in (:tcrTags) "
                    + "group by TCR2.id )) ";
        }
        queryString += "order by MR.startTime, MR.id";

        Session session = sessionFactory.openSession();
        try (AutoCloseable ignore = session::close) {
            Query query = session.createQuery(queryString);
            query.setParameter("from", params.getFrom());
            query.setParameter("to", params.getTo());
            if (sfInstancesFilter) {
                query.setParameterList("ids", toIds(params.getSfInstances()));
            }
            if (tagsFilter) {
                Object[] tagsIds = tagsToIds(params.getTags());
                query.setParameterList("mrTags", tagsIds);
                query.setParameterList("tcrTags", tagsIds);
            }
            @SuppressWarnings("unchecked")
            List<Object[]>  matrixAndTestCaseIDs = query.list();
            TreeMap<Long, List<Long>> result = new TreeMap<>();
            for (Object[] ids : matrixAndTestCaseIDs) {
                Long matrixId = (Long) ids[0];
                Long testCaseId = (Long) ids[1];
                List<Long> testCaseIds = result.computeIfAbsent(matrixId, l -> new ArrayList<>());
                if (testCaseId != null) {
                    testCaseIds.add(testCaseId);
                }
            }
            return result;
        } catch (Exception e) {
            throw new EPSCommonException(e);
        }
    }

	@SuppressWarnings("unchecked")
	public List<AggregatedReportRow> generateAggregatedReport(AggregateReportParameters params) {

        if (params.getMatrixRunIds() == null || params.getMatrixRunIds().isEmpty()) {
            return Collections.emptyList();
        }

		String queryString = "select "
				+ "MR.sfRunId, "
				+ "M.name, "
				+ "TCR.description, "
				+ "TCR.status, "
				+ "TCR.failReason, "
				+ "TCR.startTime, "
				+ "TCR.finishTime, "
				+ "U.name, "
				+ "SF.host, "

				+ "TCR.id, "
				+ "MR.startTime, "
				+ "MR.finishTime, "
				+ "E.name, "
				+ "TCR.rank, "
				+ "SF.name, "
				+ "SF.port, "
				+ "MR.id, "
				+ "T.testCaseId, "

				+ "MR.reportFolder, "
				+ "TCR.reportFile, "

				+ "TCR.comment, "
				+ "TCR.fixRevision, "
				+ "S.id,"
				+ "S.name, "
				+ "TCR.hash "

				+ "from TestCaseRun as TCR "
				+ "join TCR.matrixRun as MR "
				+ "join MR.matrix as M "
				+ "join MR.sfInstance as SF "
				+ "join MR.user as U "
				+ "join MR.environment as E "
				+ "join TCR.testCase as T "
				+ "left join TCR.runStatus AS S "
				+ "where (MR.id in (:mrId)";

        if (CollectionUtils.isNotEmpty(params.getTestCaseRunIds())) {
            queryString += " or TCR.id in (:tcrId)";
        }
        queryString += ") order by MR.startTime, TCR.rank";

		Session session = null;

		try {

            session = sessionFactory.openSession();
			Query query = session.createQuery(queryString);

			query.setParameterList("mrId", params.getMatrixRunIds());
            if (CollectionUtils.isNotEmpty(params.getTestCaseRunIds())) {
                query.setParameterList("tcrId", params.getTestCaseRunIds());
            }

            return parseAggregatedReportResult(query.list());

		} finally {

			if(session != null) {
				session.close();
			}

		}

	}

	@SuppressWarnings("unchecked")
	public List<TestCaseHistoryRow> generateTestCaseHistoryReport(AggregateReportParameters params) {

        String queryString = "select new com.exactpro.sf.embedded.statistics.storage.TestCaseHistoryRow(TC.testCaseId, M.name, TCR.startTime, TCR.finishTime, TCR.status, TCR.failReason) "
				+ "from TestCaseRun as TCR "
				+ "join TCR.testCase as TC "
				+ "join TCR.matrixRun as MR "
				+ "join MR.matrix AS M "
				+ "where TC.id = :tcId "
				+ "order by TCR.startTime, TCR.id";

		Session session = null;

		try {

            session = sessionFactory.openSession();
			Query query = session.createQuery(queryString);

			query.setParameter("tcId", params.getTestCaseId());

			query.setMaxResults(10);

			return query.list();

		} finally {

			if(session != null) {
				session.close();
			}

		}

	}

	@SuppressWarnings("unchecked")
	public List<DayleTestCasesStatRow> generateDayleTestCasesStatReport(AggregateReportParameters params) {
        StringBuilder sb = new StringBuilder(
                "select new com.exactpro.sf.embedded.statistics.storage.DayleTestCasesStatRow("
                        + " "
                        + " "
                        + "cast(TCR.startTime as date) as dd, "
                        + "sum(case when TCR.status = " + StatusType.PASSED.getId() + " then 1 else 0 end) as pp, "
                        + "sum(case when TCR.status = " + StatusType.CONDITIONALLY_PASSED.getId() + " then 1 else 0 end) as cp, "
                        + "sum(case when TCR.status = " + StatusType.FAILED.getId() + " then 1 else 0 end) as ff )"
                        + "from TestCaseRun as TCR "
                        + "join TCR.matrixRun MR "
                        + "join MR.matrix M "
                        + "join MR.sfInstance as SF "
                        + "where TCR.startTime >= :from and TCR.finishTime <= :to and SF.id in (:ids) ");

        if(StringUtils.isNotEmpty(params.getMatrixNamePattern())) {
            sb.append("and M.name like :matrixName ");
        }

        if(params.getTags() != null && !params.getTags().isEmpty()) {
            sb.append(
                    "and :numTags <= " +
                            "(select count(distinct TCRT.tag.id) from "
                            + "TestCaseRun as TCR2 "
                            + "join TCR2.tags as TCRT "
                            + "where TCR2.id = TCR.id "
                            + "and TCRT.tag.id in (:tcrTags)) + " +
                            "(select count(distinct T.id) from "
                            + "MatrixRun as MR2 "
                            + "join MR2.tags as T "
                            + "where MR2.id = MR.id "
                            + "and T.id in (:mrTags)) ");

        }

        sb.append("group by 1 order by 1 ");
		Session session = null;

		try {

            session = sessionFactory.openSession();
			Query query = session.createQuery(sb.toString());

			query.setParameter("from", params.getFrom());
			query.setParameter("to", params.getTo());
			query.setParameterList("ids", toIds(params.getSfInstances()));

            if(StringUtils.isNotEmpty(params.getMatrixNamePattern())) {
                query.setParameter("matrixName", params.getMatrixNamePattern());
            }

            if(params.getTags() != null && !params.getTags().isEmpty()) {
                Object[] tagIds = tagsToIds(params.getTags());
                query.setParameterList("tcrTags", tagIds);
                query.setParameterList("mrTags", tagIds);
                query.setParameter("numTags", params.isAllTags() ? (long)params.getTags().size() : 1l);
            }

			query.setMaxResults(10);

			return query.list();

		} finally {

			if(session != null) {
				session.close();
			}

		}

	}

	@SuppressWarnings("unchecked")
	public List<AggregatedReportRow> generateTestScriptsReport(AggregateReportParameters params) {

		StringBuilder sb = new StringBuilder();

		sb.append("select "
				+ "MR.sfRunId, "
				+ "M.name, "
				+ "TCR.description, "
				+ "TCR.status, "
				+ "TCR.failReason, "
				+ "TCR.startTime, "
				+ "TCR.finishTime, "
				+ "U.name, "
				+ "SF.host, "

				+ "TCR.id, "
				+ "MR.startTime, "
				+ "MR.finishTime, "
				+ "E.name, "
				+ "TCR.rank, "
				+ "SF.name, "
				+ "SF.port, "
				+ "MR.id, "
				+ "T.testCaseId, "

				+ "MR.reportFolder, "
				+ "TCR.reportFile, "

				+ "TCR.comment, "
				+ "TCR.fixRevision, "
				+ "S.id, "
				+ "S.name, "
				+ "TCR.hash, "
                + "CurrentSF.id, "
                + "CurrentSF.host, "
                + "CurrentSF.port, "
                + "CurrentSF.name, "
                + "MR.failReason ");

		sb.append("from TestCaseRun as TCR "
                + "right join TCR.matrixRun as MR "
                + "join MR.matrix as M "
                + "join MR.sfInstance as SF "
                + "join MR.user as U "
                + "join MR.environment as E "
                + "left join MR.sfCurrentInstance as CurrentSF "
                + "left join TCR.testCase as T "
                + "left join TCR.runStatus AS S ");


        if (params.isIncludeExecutedInFromToRange()) {
            sb.append("where MR.startTime <= :to and MR.finishTime >= :from ");
        } else {
            sb.append("where MR.startTime >= :from and MR.finishTime <= :to ");
        }
        sb.append("and SF.id in (:ids) ");

		if(params.isEmptyCommentOnly()) {

			sb.append("and (TCR.comment is null or TCR.comment = '') ");

		}

		if(StringUtils.isNotEmpty(params.getMatrixNamePattern())) {
			sb.append("and M.name like :matrixName ");
		}

		if(params.getTags() != null && !params.getTags().isEmpty()) {
			sb.append(
                    "and :numTags <= " +
                    "(select count(distinct TCRT.tag.id) from "
                    + "TestCaseRun as TCR2 "
                    + "join TCR2.tags as TCRT "
                    + "where TCR2.id = TCR.id "
                    + "and TCRT.tag.id in (:tcrTags)) + " +
                    "(select count(distinct T.id) from "
                    + "MatrixRun as MR2 "
                    + "join MR2.tags as T "
                    + "where MR2.id = MR.id "
                    + "and T.id in (:mrTags)) ");

		}

        if(params.getTcDisplayMode() == TestCasesDisplayMode.FailedOnly) {
			sb.append("and (TCR.status = 0 or MR.failReason is not null or MR.failReason <> '') ");

		}

		sb.append("order by ");
		sb.append(params.getSortBy());

		sb.append(params.isSortAsc() ? " asc " : " desc ");

		sb.append(" , MR.id ");

		sb.append(params.isSortAsc() ? " asc " : " desc ");

        if(params.getTcDisplayMode() == TestCasesDisplayMode.FailedOnly) {
			sb.append(", TCR.rank");
		}

        if(params.getTcDisplayMode() == TestCasesDisplayMode.AllNatural) {
			sb.append(", TCR.rank");
		}

        if(params.getTcDisplayMode() == TestCasesDisplayMode.AllFailedFirst) {
			sb.append(", TCR.status, TCR.rank");
		}

		Session session = null;

		try {

            session = sessionFactory.openSession();
			Query query = session.createQuery(sb.toString());

			query.setParameter("from", params.getFrom());
			query.setParameter("to", params.getTo());

			if(StringUtils.isNotEmpty(params.getMatrixNamePattern())) {
				query.setParameter("matrixName", params.getMatrixNamePattern());
			}

			query.setParameterList("ids", toIds(params.getSfInstances()));

			if(params.getTags() != null && !params.getTags().isEmpty()) {
                Object[] ids = tagsToIds(params.getTags());
                query.setParameterList("tcrTags", ids);
                query.setParameterList("mrTags", ids);
                query.setParameter("numTags", params.isAllTags() ? (long)params.getTags().size() : 1l);

			}

            return parseAggregatedReportResult(query.list());

		} finally {

			if(session != null) {
				session.close();
			}

		}

	}

	@Override
    @SuppressWarnings("unchecked")
	public List<ActionInfoRow> generateFailedActionsInfo(AggregateReportParameters params) {

        String queryString = "select new com.exactpro.sf.embedded.statistics.storage.reporting.ActionInfoRow( "
                + "AR.rank, AR.description, AR.failReason, A.name, M.name, S.name, AR.tag, AR.status, AR.hash) "
				+ "from ActionRun as AR "
				+ "join AR.tcRun as TCR "
				+ "left join AR.action as A "
				+ "left join AR.service as S "
				+ "left join AR.msgType as M "

				+ "where TCR.id = :tcrId and AR.status = 0 "
				+ "order by AR.rank";

		Session session = null;

		try {

            session = sessionFactory.openSession();
			Query query = session.createQuery(queryString);

			query.setParameter("tcrId", params.getTestCaseRunId());

			query.setMaxResults(10);

            //TODO: Remove escaping after release 3.1, because escaping is executed on save data
            return escapeDesription(query.list());

		} finally {

			if(session != null) {
				session.close();
			}

		}

	}

    @Override
    public Map<Long, List<ActionInfoRow>> generateTestCasesFailedActionsInfo(AggregateReportParameters params) {
        if (params.getTestCaseRunIds() == null || params.getTestCaseRunIds().isEmpty()) {
            return Collections.emptyMap();
        }

        String queryString = "select "
                + "TCR.id, "
                + "AR.rank, "
                + "AR.description, "
                + "AR.failReason, "
                + "A.name, "
                + "M.name, "
                + "S.name, "
                + "AR.tag, "
                + "AR.status, "
                + "AR.hash "
                + "from ActionRun as AR "
                + "join AR.tcRun as TCR "
                + "left join AR.action as A "
                + "left join AR.service as S "
                + "left join AR.msgType as M "

                + "where TCR.id in (:tcrIds) and AR.status = 0 "
                + "order by AR.rank";
        Session session = sessionFactory.openSession();
        try (AutoCloseable ignore = session::close) {
            Query query = session.createQuery(queryString);
            query.setParameterList("ids", params.getTestCaseRunIds());
            @SuppressWarnings("unchecked")
            List<Object[]> resultList = query.list();
            Map<Long, List<ActionInfoRow>> testCaseToActionInfo = new HashMap<>();
            for (Object[] resultRow : resultList) {
                Long tcrId = (Long) resultRow[0];
                long rank = (long) resultRow[1];
                //TODO: Remove escaping after release 3.1, because escaping is executed on save data
                String description = StringEscapeUtils.escapeEcmaScript((String) resultRow[2]);
                String failReason = (String) resultRow[3];
                String actionName = (String) resultRow[4];
                String msgType = (String) resultRow[5];
                String service = (String) resultRow[6];
                String tag = (String) resultRow[7];
                Integer status = (Integer) resultRow[8];
                Integer hash = (Integer) resultRow[9];

                testCaseToActionInfo.computeIfAbsent(tcrId, id -> new ArrayList<>())
                        .add(new ActionInfoRow(rank, description, failReason, actionName, msgType, service, tag, status, hash));
            }
            return testCaseToActionInfo;
        } catch (Exception e) {
            throw new EPSCommonException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<ActionInfoRow> generateTaggedActionsInfo(AggregateReportParameters params) {
        String queryString = "select new com.exactpro.sf.embedded.statistics.storage.reporting.ActionInfoRow( "
                + "AR.rank, AR.description, AR.failReason, A.name, M.name, S.name, AR.tag, AR.status, AR.hash) "
                + "from ActionRun as AR "
                + "join AR.tcRun as TCR "
                + "left join AR.action as A "
                + "left join AR.service as S "
                + "left join AR.msgType as M "

                + "where TCR.id = :tcrId and AR.tag is not null "
                + "order by AR.rank";

        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery(queryString);
            query.setParameter("tcrId", params.getTestCaseRunId());
            List<ActionInfoRow> list = query.list();
            list.sort((o1, o2) -> o1.getTag().compareTo(o2.getTag()));

            //TODO: Remove escaping after release 3.1, because escaping is executed on save data
            return escapeDesription(list);
        } finally {
            if(session != null) {
                session.close();
            }
        }
    }

    @Override
    public List<KnownBugRow> generateKnownBugsReport(AggregateReportParameters params) {
        String queryString = "select new com.exactpro.sf.embedded.statistics.storage.reporting.KnownBugRow( "
                + "KB.knownBug, ARKB.reproduced) "
                + "from ActionRun as AR "
                + "join AR.tcRun as TCR "
                + "join AR.actionRunKnownBugs as ARKB "
                + "join ARKB.id.knownBug as KB "
                + "where TCR.id = :tcrId";
        Session session = sessionFactory.openSession();
        try (AutoCloseable ignore = session::close) {
            Query query = session.createQuery(queryString);
            query.setParameter("tcrId", params.getTestCaseRunId());
            @SuppressWarnings("unchecked")
            List<KnownBugRow> knownBugs = query.list();
            return knownBugs;
        } catch (Exception e) {
            throw new EPSCommonException(e);
        }
    }

    @Override
    public Map<Long, List<KnownBugRow>> generateTestCasesKnownBugsReports(AggregateReportParameters params) {

        String queryString = "select distinct "
                + "TCR.id, "
                + "ARKB.id.knownBug, "
                + "ARKB.reproduced "
                + "from ActionRun as AR "
                + "join AR.tcRun as TCR "
                + "join AR.actionRunKnownBugs as ARKB "
                + "where TCR.id in (:ids)";

        Session session = sessionFactory.openSession();
        try (AutoCloseable ignore = session::close) {
            Query query = session.createQuery(queryString);
            query.setParameterList("ids", params.getTestCaseRunIds());
            @SuppressWarnings("unchecked")
            List<Object[]> resultList = query.list();
            Map<Long, List<KnownBugRow>> testCaseToBugs = new HashMap<>();
            for (Object[] resultRow : resultList) {
                Long tcId = (Long) resultRow[0];
                KnownBug knownBug = (KnownBug) resultRow[1];
                Boolean reproduced = (Boolean) resultRow[2];
                testCaseToBugs.computeIfAbsent(tcId, id -> new ArrayList<>())
                        .add(new KnownBugRow(knownBug.getKnownBug(), reproduced));
            }
            return testCaseToBugs;
        } catch (Exception e) {
            throw new EPSCommonException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<AggregatedReportRow> generateActionsAggregatedReportRow(AggregateReportParameters params) {
        String queryString = "select  "
                + "AR.description, "
                + "AR.failReason, "
                + "A.name, "
                + "M.name, "
                + "S.name, "
                + "AR.status, "
                + "AR.hash "
                + "from ActionRun as AR "
                + "join AR.tcRun as TCR "
                + "left join AR.action as A "
                + "left join AR.service as S "
                + "left join AR.msgType as M "

                + "where TCR.id = :tcrId "
                + "order by AR.rank";

        Session session = null;

        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery(queryString);
            query.setParameter("tcrId", params.getTestCaseRunId());

            return parseActionsToAggregateReportRow(query.list());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private List<AggregatedReportRow> parseActionsToAggregateReportRow(List<Object[]> results) {

        List<AggregatedReportRow> result = new ArrayList<>();

        for (Object[] row : results) {
            logger.trace("Row {}", (Object) row);
            AggregatedReportRow parsedRow = new AggregatedReportRow();
            parsedRow.setActionRow(true);

            if (row[0] != null) {
                parsedRow.setDescription((String) row[0]);
            }

            if (row[1] != null) {
                parsedRow.setFailReason((String) row[1]);
            }

            parsedRow.setActionName((String) row[2]);

            parsedRow.setMessageType((String) row[3]);

            parsedRow.setServicesUsed((String) row[4]);

            parsedRow.setStatus(StatusType.getStatusType((int) row[5]));

            if (row[6] != null) { //TODO remove after few time
                parsedRow.setHash((int) row[6]);
            }

            result.add(parsedRow);
        }

        return result;

    }

    private List<ActionInfoRow> escapeDesription(List<ActionInfoRow> list) {
        for(ActionInfoRow item : list) {
            item.setDescription(StringEscapeUtils.escapeEcmaScript(item.getDescription()));
        }

        return list;
    }

	private ScriptWeatherRow readMartixExecutionInfoRows(List<Object[]> rows) {

		long passedCount = 0l;
		long conditionallyPassedCount = 0l;
		long failedCount = 0l;
		String name = null;

		for(Object[] row : rows) {

			name = (String)row[0];
			Integer exists = (Integer)row[1];
			Integer passed = (Integer)row[2];
			Integer conditionallyPassed = (Integer)row[3];
			Integer failed = (Integer)row[4];

			if (exists == 1) {
    			passedCount += passed;
    			conditionallyPassedCount += conditionallyPassed;
    			failedCount += failed;
			}
		}

		if (failedCount != 0l && (passedCount != 0l || conditionallyPassedCount != 0l)) {
		    ScriptWeatherRow result = new ScriptWeatherRow();

		    result.setMatrixName(name);
		    result.setFailed(failedCount);
		    result.setConditionallyPassed(conditionallyPassedCount);
	        result.setPassed(passedCount);

	        return result;
		}

		return null;

	}

	@SuppressWarnings("unchecked")
	public List<ScriptWeatherRow> generateScriptsWeatherReport(AggregateReportParameters params) {

		List<ScriptWeatherRow> result = new ArrayList<>();

		// Find ids of last X executed matrices;

		String lastScriptsQuery = "select M.id "
				+ "from MatrixRun as MR "
				+ "join MR.matrix as M "
				+ "group by M.id "
				+ "order by max(MR.startTime) desc";

		String queryString = "select M.name, "
    		        + "CASE WHEN exists(select TCR.id from TestCaseRun as TCR "
                    + "join TCR.matrixRun as MR2 where MR2.id = MR.id) THEN 1 ELSE 0 END as rowExists, "
                    + "CASE WHEN exists(select TCR.id from TestCaseRun as TCR "
                    + "join TCR.matrixRun as MR2 where TCR.status = 1 and MR2.id = MR.id) THEN 1 ELSE 0 END as passed, "
                    + "CASE WHEN exists(select TCR.id from TestCaseRun as TCR "
                    + "join TCR.matrixRun as MR2 where TCR.status = 2 and MR2.id = MR.id) THEN 1 ELSE 0 END as conditionallyPassed, "
					+ "CASE WHEN exists(select TCR.id from TestCaseRun as TCR "
					+ "join TCR.matrixRun as MR2 where TCR.status = 0 and MR2.id = MR.id) THEN 1 ELSE 0 END as failed "
				+ ""
				+ "from MatrixRun as MR "
				+ "join MR.matrix as M "

				+ "where M.id = :id "
				+ "order by MR.startTime desc ";

		Session session = null;

		try {

            session = sessionFactory.openSession();
			Query query = session.createQuery(lastScriptsQuery);

			//query.setParameterValue("tcrId", params.getTestCaseRunId());

			query.setMaxResults((int)params.getLimit());

			List<Long> ids = query.list();

			for(Long id : ids) {

				query = session.createQuery(queryString);

				query.setParameter("id", id);

				query.setMaxResults((int)params.getSecondLimit());

				ScriptWeatherRow weatherRow = readMartixExecutionInfoRows(query.list());

				if (weatherRow != null) {
				    result.add(weatherRow);
				}
			}

            return result;
        } finally {

			if(session != null) {
				session.close();
			}

		}

    }

	private List<TaggedComparisonRow> readTaggedSetsComparisonResultSet(List<Object[]> rs) {

		List<TaggedComparisonRow> result = new ArrayList<>();

		for(Object[] rsRow : rs) {

            //logger.info(Arrays.toString(row));

            TaggedComparisonRow row = new TaggedComparisonRow();

            row.setTestCaseId((String) rsRow[0]);

            int middle = (rsRow.length + 1) / 2;
            addDataToTaggedComparisonSet(row.getFirstSet(), ArrayUtils.subarray(rsRow, 1, middle));

            addDataToTaggedComparisonSet(row.getSecondSet(), ArrayUtils.subarray(rsRow, middle, rsRow.length));

			result.add(row);

		}

		return result;

	}

    private void addDataToTaggedComparisonSet(TaggedComparisonSet row, Object[] rsRow) {
        row.setTestCaseId(rsRow[0] != null ? ((BigInteger) rsRow[0]).longValue() : null);
        row.setMatrixName((String) rsRow[1]);
        row.setStatus(rsRow[2] != null ? StatusType.getStatusType((Integer) rsRow[2]) : null);
        row.setDescription((String) rsRow[3]);
        row.setFailReason((String) rsRow[4]);
        row.setFailedActions(exctractFailedActionsMap((String) rsRow[5]));
        row.setUserComment((String) rsRow[6]);
        row.setUserStatus((String) rsRow[7]);
        row.setTestCaseRunId(rsRow[8] != null ? ((BigInteger) rsRow[8]).longValue() : null);
        row.setMatrixRunId(rsRow[9] != null ? ((BigInteger) rsRow[9]).longValue() : null);
        row.setStartTime(rsRow[10] != null ? DateTimeUtility.toLocalDateTime((Timestamp) rsRow[10]) : null);
        row.setFinishTime(rsRow[11] != null ? DateTimeUtility.toLocalDateTime((Timestamp) rsRow[11]) : null);
        row.setRawTags((String) rsRow[12]);
        row.setRawHash((Integer) rsRow[13]);

        SfInstance sfInstance = new SfInstance();
        sfInstance.setHost((String) rsRow[14]);
        sfInstance.setName((String) rsRow[15]);
        sfInstance.setPort(rsRow[16] != null ? (Integer) rsRow[16] : 0);
        row.setSfInstance(sfInstance);

        SfInstance sfCurrentInstance = new SfInstance();
        sfCurrentInstance.setHost((String) rsRow[17]);
        sfCurrentInstance.setName((String) rsRow[18]);
        sfCurrentInstance.setPort(rsRow[19] != null ? (Integer) rsRow[19] : 0);
        row.setSfCurrentInstance(sfCurrentInstance);

        row.setReportFolder((String) rsRow[20]);
        row.setReportFile((String) rsRow[21]);
    }

	private Map<Long, String> exctractFailedActionsMap(String dbString) {

	    if (StringUtils.isEmpty(dbString)) {
	        return new HashMap<>();
	    }

	    Map<Long, String> result = new TreeMap<>();

	    try {
	        String[] fActions = dbString.split(FAILED_ACTION_SPLITTER);

	        for (String fAction : fActions) {
	            String[] rankReasonSplitted = fAction.split(ACTION_RANK_FREASON_SPLITTER);
	            result.put(Long.valueOf(rankReasonSplitted[0]), rankReasonSplitted[1]);
	        }
	    } catch (Throwable e) {
	        logger.error("Can not exctract failed action info. Incorrect return from Storage: <{}>", dbString);
        }

	    return result;
	}

	private Long[] tagsSetToArray(List<Tag> tags) {

		Long[] result = new Long[tags.size()];

		int i = 0;

		for(Tag tag : tags) {

			result[i++] = tag.getId();

		}

		return result;

	}

	public TaggedComparisonResult generateTaggedSetsComparisonReport(TaggedSetsComparisonParameters params) {

		TaggedComparisonResult result = new TaggedComparisonResult();

		NativeDbOperations nativeOps = new NativeDbOperations(settings);

		Session session = null;

		try {

            session = sessionFactory.openSession();

			result.setRows(
					readTaggedSetsComparisonResultSet(
							nativeOps.executeTaggedComparisonQuery(session,
									tagsSetToArray(params.getFirstSet()),
									tagsSetToArray(params.getSecondSet()))));

			return result;

		} finally {

			if(session != null) {
				session.close();
			}

		}

	}

	public List<TagGroupReportResult> generateTagGroupReport(TagGroupReportParameters params, List<TagGroupDimension> dimensions) {

        Session session = null;

        NativeDbOperations nativeOps = new NativeDbOperations(settings);

        try {

            session = sessionFactory.openSession();

            AbstractTagGroupQueryBuilder queryBuilder = NativeQueryUtil.isMysql(settings.getDbms())
                    ? new MySqlTagGroupQueryBuilder()
                    : new PostgresTagGroupQueryBuilder();

            nativeOps.createTagGroupTempTable(session, params.getTagIds());

            queryBuilder.setJoinType(nativeOps.recognizeJoinType(session));

            List<TagGroupReportResult> results = new ArrayList<>();

            for(int i = 0; i < dimensions.size(); i++) {
                params.setLoadForLevel(i);
                results.add(nativeOps.generateTagGroupReport(session, params, queryBuilder.clone()));
            }

            return results;
        } finally {

            if(session != null) {
                session.close();
            }

        }

    }

    @SuppressWarnings("unchecked")
    public List<AggregatedReportRow> generateAggregatedTaggedReport(AggregateReportParameters params) {
        String queryString = "select "
                + "MR.id, "
                + "T.testCaseId, "
                + "M.name, "
                + "TCR.description, "
                + "SF.host, "
                + "MR.startTime, "
                + "MR.finishTime, "
                + "TCR.status, "
                + "TCR.failReason, "
                + "TCR.id "

                + "from TestCaseRun as TCR "
                + "join TCR.matrixRun as MR "
                + "join MR.matrix as M "
                + "join MR.sfInstance as SF "
                + "join TCR.testCase as T "
                + "where MR.startTime >= :from and MR.finishTime <= :to ";

        if (params.getTags() != null && !params.getTags().isEmpty()) {
            queryString += "and 1 <= " +
                            "(select count(distinct TCRT.tag.id) from "
                            + "TestCaseRun as TCR2 "
                            + "join TCR2.tags as TCRT "
                            + "where TCR2.id = TCR.id "
                            + "and TCRT.tag.id in (:tcrTags)) + " +
                            "(select count(distinct T.id) from "
                            + "MatrixRun as MR2 "
                            + "join MR2.tags as T "
                            + "where MR2.id = MR.id "
                            + "and T.id in (:mrTags)) ";
        }

        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery(queryString);

            query.setParameter("from", params.getFrom());
            query.setParameter("to", params.getTo());

            if(params.getTags() != null && !params.getTags().isEmpty()) {
                Object[] ids = tagsToIds(params.getTags());
                query.setParameterList("tcrTags", ids);
                query.setParameterList("mrTags", ids);
            }

            return parseAggregatedTaggedReportResult(query.list());
        } finally {
            if(session != null) {
                session.close();
            }
        }
    }

    private void loadAndAddMatrixTags(Map<Long, List<AggregatedReportRow>> matrixRows) {
        String queryString = "select distinct "
                + "MR.id, "
                + "T.id, "
                + "T.name, "
                + "G.name "
                + "from MatrixRun as MR "
                + "join MR.tags as T "
                + "left join T.group as G "
                + "where MR.id in (:ids) "
                + "order by MR.id, T.name";
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery(queryString);
            query.setParameterList("ids", matrixRows.keySet());
            @SuppressWarnings("unchecked")
            List<Object[]> resultSet = query.list();
            Map<Long, List<Tag>> tagsUsed = readTagsResultSet(resultSet, (t, row) -> t.setCustom(false), true);
            for(Entry<Long, List<Tag>> entry : tagsUsed.entrySet()) {
                List<AggregatedReportRow> rows = matrixRows.get(entry.getKey());
                for(AggregatedReportRow r : rows) {
                    r.setTags(new ArrayList<>(entry.getValue()));
                }
            }
        } finally {
            if(session != null) {
                session.close();
            }
        }
    }

    private void loadAndAddTestCaseTags(Map<Long, AggregatedReportRow> rows) {
        String queryString = "select distinct "
                + "TCR.id, "
                + "TCRT.tag.id, "
                + "TCRT.tag.name, "
                + "G.name, "
                + "TCRT.custom "
                + "from TestCaseRun as TCR "
                + "join TCR.tags as TCRT "
                + "left join TCRT.tag.group as G "
                + "where TCR.id in (:ids) "
                + "order by TCR.id, TCRT.tag.name";
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Query query = session.createQuery(queryString);
            query.setParameterList("ids", rows.keySet());
            @SuppressWarnings("unchecked")
            List<Object[]> resultSet = query.list();
            Map<Long, List<Tag>> tagsUsed = readTagsResultSet(resultSet, (t, row) -> t.setCustom((Boolean)row[4]), false);
            for(Entry<Long, List<Tag>> entry : tagsUsed.entrySet()) {
                AggregatedReportRow row = rows.get(entry.getKey());
                List<Tag> tags = row.getTags();
                if (tags == null) {
                    tags = new ArrayList<>();
                    row.setTags(tags);
                }
                tags.addAll(entry.getValue());
            }
        } finally {
            if(session != null) {
                session.close();
            }
        }
    }

}
