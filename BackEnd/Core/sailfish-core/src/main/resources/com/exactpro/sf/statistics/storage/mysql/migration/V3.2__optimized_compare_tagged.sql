DROP PROCEDURE IF EXISTS ${db_name}.tagged_sets_comparison;

delimiter //;
create procedure ${db_name}.tagged_sets_comparison(IN all_ids varchar(1024), IN first_ids varchar(512), IN first_count int, IN second_ids varchar(512), IN second_count int)
BEGIN

SET SESSION group_concat_max_len = 4294967295;

 -- Latest execution of each test case tagged with set 1 and 2
CREATE TEMPORARY TABLE common
SELECT
    MR.id AS mr_id,
    TCR.id AS tcr_id,
    TC.id AS tc_id,
    max(TCR.startTime) AS startTime,
    COUNT(DISTINCT(CASE WHEN FIND_IN_SET(TCRTS.tag_id, first_ids) THEN TCRTS.tag_id ELSE NULL END))
        + COUNT(DISTINCT(CASE WHEN FIND_IN_SET(MRTS.tag_id, first_ids) THEN MRTS.tag_id ELSE NULL END)) AS tcr_tag_id_f,
    COUNT(DISTINCT(CASE WHEN FIND_IN_SET(TCRTS.tag_id, second_ids) THEN TCRTS.tag_id ELSE NULL END))
        + COUNT(DISTINCT(CASE WHEN FIND_IN_SET(MRTS.tag_id, second_ids) THEN MRTS.tag_id ELSE NULL END)) AS tcr_tag_id_s
        FROM stmatrixruns AS MR
        LEFT JOIN stmrtags AS MRTS ON MRTS.mr_id = MR.id
        LEFT JOIN sttestcaseruns AS TCR ON TCR.matrix_run_id = MR.id
        LEFT JOIN sttcrtags AS TCRTS ON TCRTS.tcr_id = TCR.id
        LEFT JOIN sttestcases AS TC ON TC.id = TCR.tc_id
        WHERE FIND_IN_SET(TCRTS.tag_id, all_ids) OR FIND_IN_SET(MRTS.tag_id, all_ids)
        GROUP BY 1,2,3
        HAVING COUNT(DISTINCT(CASE WHEN FIND_IN_SET(TCRTS.tag_id, first_ids) THEN TCRTS.tag_id ELSE NULL END))
                    + COUNT(DISTINCT(CASE WHEN FIND_IN_SET(MRTS.tag_id, first_ids) THEN MRTS.tag_id ELSE NULL END)) >= first_count
            OR COUNT(DISTINCT(CASE WHEN FIND_IN_SET(TCRTS.tag_id, second_ids) THEN TCRTS.tag_id ELSE NULL END))
            + COUNT(DISTINCT(CASE WHEN FIND_IN_SET(MRTS.tag_id, second_ids) THEN MRTS.tag_id ELSE NULL END)) >= second_count;

CREATE TEMPORARY TABLE first_set as select * from common where tcr_tag_id_f >= first_count;
CREATE TEMPORARY TABLE second_set as select * from common where tcr_tag_id_s >= second_count;
CREATE TEMPORARY TABLE first_set_copy as select * from first_set;
CREATE TEMPORARY TABLE second_set_copy as select * from second_set;

SELECT RESULT.testcaseid  AS testcaseid,
    RESULT.ftcid, RESULT.fmatrix, RESULT.fstatus, RESULT.fdescription, RESULT.ffailreason, RESULT.ffailedactions, RESULT.fcomment, RESULT.fuserStatus, RESULT.ftcrid, RESULT.fmrid,
        RESULT.fstarttime, RESULT.ffinishtime, RESULT.ftags, RESULT.fhash, FSF.host AS fhost, FSF.name AS fname, FSF.port as fport, CFSF.host AS fchost, CFSF.name AS fcname, CFSF.port AS fcport, RESULT.freportfolder, RESULT.freportfile,
    RESULT.stcid, RESULT.smatrix, RESULT.sstatus, RESULT.sdescription, RESULT.sfailreason, RESULT.sfailedactions, RESULT.scomment, RESULT.suserStatus, RESULT.stcrid, RESULT.smrid,
        RESULT.sstarttime, RESULT.sfinishtime, RESULT.stags, RESULT.shash, SSF.host AS shost, SSF.name AS sname, SSF.port as sport, CSSF.host AS schost, CSSF.name AS scname, CSSF.port AS scport, RESULT.sreportfolder, RESULT.sreportfile
FROM (

SELECT coalesce(F.testcaseid, S.testcaseid) AS testcaseid,
    F.tcid AS ftcid, F.name AS fmatrix, F.status AS fstatus, F.description AS fdescription, F.failreason AS ffailreason, F.failedactions AS ffailedactions,
    F.comment AS fcomment, F.userStatus AS fuserStatus, F.tcrid AS ftcrid, F.mrid AS fmrid,
        F.starttime AS fstarttime, F.finishtime AS ffinishtime, F.tags AS ftags, F.hash AS fhash,                                                    	    F.sfid as fsfid, F.sfcurrentid as fsfcurrentid, F.reportfolder as freportfolder, F.reportfile as freportfile,
    S.tcid AS stcid, S.name AS smatrix, S.status AS sstatus, S.description AS sdescription, S.failreason AS sfailreason, S.failedactions AS sfailedactions,
    S.comment AS scomment, S.userStatus AS suserStatus, S.tcrid AS stcrid, S.mrid AS smrid,
        S.starttime AS sstarttime, S.finishtime AS sfinishtime, S.tags AS stags, S.hash AS shash,
        S.sfid as ssfid, S.sfcurrentid as ssfcurrentid, S.reportfolder as sreportfolder, S.reportfile as sreportfile

FROM
 (SELECT TC.id AS tcid, TC.testcaseid, M.name, TCR.status, TCR.description, TCR.failreason, TCR.comment, TS.name AS userStatus, TCR.id AS tcrid, MR.id AS mrid,
        TCR.starttime, TCR.finishtime, TCR.hash,
        (SELECT GROUP_CONCAT(T.name SEPARATOR ', ')  FROM sttags T JOIN stmrtags MRT ON MRT.tag_id = T.id WHERE MRT.mr_id = MR.id ) AS tags,
        GROUP_CONCAT(CONCAT(AR.rank, 'ARFRSPLITTER', AR.failreason) SEPARATOR 'FACTSPLITTER') AS failedactions,
        MR.sf_id as sfid, MR.sf_current_id as sfcurrentid, MR.reportFolder as reportfolder, TCR.reportFile as reportfile
        FROM first_set as X
        JOIN sttestcaseruns TCR ON TCR.tc_id = X.tc_id AND TCR.startTime = X.startTime
        JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
        JOIN sttestcases TC ON TCR.tc_id = TC.id
        LEFT JOIN sttcrstatus TS ON TCR.status_id = TS.id
        JOIN stmatrices M ON MR.matrix_id = M.id
        LEFT JOIN stactionruns AR ON AR.tc_run_id = TCR.id AND AR.failreason IS NOT NULL
        GROUP BY tcid, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
        AS F LEFT JOIN
 (SELECT TC.id AS tcid, TC.testcaseid, M.name, TCR.status, TCR.description, TCR.failreason, TCR.comment, TS.name AS userStatus, TCR.id AS tcrid, MR.id AS mrid,
        TCR.starttime, TCR.finishtime, TCR.hash,
        (SELECT GROUP_CONCAT(T.name SEPARATOR ', ') FROM sttags T JOIN stmrtags MRT ON MRT.tag_id = T.id WHERE MRT.mr_id = MR.id ) AS tags,
        GROUP_CONCAT(CONCAT(AR.rank, 'ARFRSPLITTER', AR.failreason) SEPARATOR 'FACTSPLITTER') AS failedactions,
        MR.sf_id as sfid, MR.sf_current_id as sfcurrentid, MR.reportFolder as reportfolder, TCR.reportFile as reportfile
        FROM second_set AS X
        JOIN sttestcaseruns TCR ON TCR.tc_id = X.tc_id AND TCR.startTime = X.startTime
        JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
        JOIN sttestcases TC ON TCR.tc_id = TC.id
        LEFT JOIN sttcrstatus TS ON TCR.status_id = TS.id
        JOIN stmatrices M ON MR.matrix_id = M.id
        LEFT JOIN stactionruns AR ON AR.tc_run_id = TCR.id AND AR.failreason IS NOT NULL
        GROUP BY tcid, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)

 S ON F.tcid = S.tcid

 UNION -- used for full outer join

 SELECT
 coalesce(F.testcaseid, S.testcaseid) AS testcaseid,
    F.tcid AS ftcid, F.name AS fmatrix, F.status AS fstatus, F.description AS fdescription, F.failreason AS ffailreason, F.failedactions AS ffailedactions,
    F.comment AS fcomment, F.userStatus AS fuserStatus, F.tcrid AS ftcrid, F.mrid AS fmrid,
        F.starttime AS fstarttime, F.finishtime AS ffinishtime, F.tags AS ftags, F.hash AS fhash,
        F.sfid as fsfid, F.sfcurrentid as fsfcurrentid, F.reportfolder as freportfolder, F.reportfile as freportfile,
    S.tcid AS stcid, S.name AS smatrix, S.status AS sstatus, S.description AS sdescription, S.failreason AS sfailreason, S.failedactions AS sfailedactions,
    S.comment AS scomment, S.userStatus AS suserStatus, S.tcrid AS stcrid, S.mrid AS smrid,
        S.starttime AS sstarttime, S.finishtime AS sfinishtime, S.tags AS stags, S.hash AS shash,
        S.sfid as ssfid, S.sfcurrentid as ssfcurrentid, S.reportfolder as sreportfolder, S.reportfile as sreportfile
  FROM

 (SELECT TC.id AS tcid, TC.testcaseid, M.name, TCR.status, TCR.description, TCR.failreason, TCR.comment, TS.name AS userStatus, TCR.id AS tcrid, MR.id AS mrid,
        TCR.starttime, TCR.finishtime, TCR.hash,
        (SELECT GROUP_CONCAT(T.name SEPARATOR ', ')  FROM sttags T JOIN stmrtags MRT ON MRT.tag_id = T.id WHERE MRT.mr_id = MR.id ) AS tags,
        GROUP_CONCAT(CONCAT(AR.rank, 'ARFRSPLITTER', AR.failreason) SEPARATOR 'FACTSPLITTER') AS failedactions,
        MR.sf_id as sfid, MR.sf_current_id as sfcurrentid, MR.reportFolder as reportfolder, TCR.reportFile as reportfile
        FROM first_set_copy as X
        JOIN sttestcaseruns TCR ON TCR.tc_id = X.tc_id AND TCR.startTime = X.startTime
        JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
        JOIN sttestcases TC ON TCR.tc_id = TC.id
        LEFT JOIN sttcrstatus TS ON TCR.status_id = TS.id
        JOIN stmatrices M ON MR.matrix_id = M.id
        LEFT JOIN stactionruns AR ON AR.tc_run_id = TCR.id AND AR.failreason IS NOT NULL
        GROUP BY tcid, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)

 AS F RIGHT JOIN
 (SELECT TC.id AS tcid, TC.testcaseid, M.name, TCR.status, TCR.description, TCR.failreason, TCR.comment, TS.name AS userStatus, TCR.id AS tcrid, MR.id AS mrid,
        TCR.starttime, TCR.finishtime, TCR.hash,
        (SELECT GROUP_CONCAT(T.name SEPARATOR ', ') FROM sttags T JOIN stmrtags MRT ON MRT.tag_id = T.id WHERE MRT.mr_id = MR.id ) AS tags,
        GROUP_CONCAT(CONCAT(AR.rank, 'ARFRSPLITTER', AR.failreason) SEPARATOR 'FACTSPLITTER') AS failedactions,
        MR.sf_id as sfid, MR.sf_current_id as sfcurrentid, MR.reportFolder as reportfolder, TCR.reportFile as reportfile
        FROM second_set_copy AS X
        JOIN sttestcaseruns TCR ON TCR.tc_id = X.tc_id AND TCR.startTime = X.startTime
        JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
        JOIN sttestcases TC ON TCR.tc_id = TC.id
        LEFT JOIN sttcrstatus TS ON TCR.status_id = TS.id
        JOIN stmatrices M ON MR.matrix_id = M.id
        LEFT JOIN stactionruns AR ON AR.tc_run_id = TCR.id AND AR.failreason IS NOT NULL
        GROUP BY tcid, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)

 S ON F.tcid = S.tcid)  as RESULT
  LEFT JOIN stsfinstances FSF ON RESULT.fsfid = FSF.id
  LEFT JOIN stsfinstances CFSF ON RESULT.fsfcurrentid = CFSF.id
  LEFT JOIN stsfinstances SSF ON RESULT.ssfid = SSF.id
  LEFT JOIN stsfinstances CSSF ON RESULT.ssfcurrentid = CSSF.id
  ORDER BY 1;
END
//;
DELIMITER ;