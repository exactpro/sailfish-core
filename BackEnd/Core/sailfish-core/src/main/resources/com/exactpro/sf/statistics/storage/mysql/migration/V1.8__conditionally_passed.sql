ALTER TABLE `stactionruns` CHANGE COLUMN `passed` `status` int NOT NULL;
ALTER TABLE `sttestcaseruns` CHANGE COLUMN `passed` `status` int NOT NULL;

DROP PROCEDURE IF EXISTS ${db_name}.tagged_sets_comparison;

-- procedure body is the same is in V1.6__compare_tagged_function only references to passed fields have been changed to status ones
delimiter //;
create procedure ${db_name}.tagged_sets_comparison(IN first_ids varchar(512), IN first_count int, IN second_ids varchar(512), IN second_count int)
BEGIN

SELECT coalesce(F.testcaseid, S.testcaseid) AS testcaseid,
    F.tcid AS ftcid, F.name AS fmatrix, F.status AS fstatus, F.failreason AS ffailreason, F.comment AS fcomment, F.userStatus AS fuserStatus, F.tcrid AS ftcrid, F.mrid AS fmrid,
        F.starttime AS fstarttime, F.finishtime AS ffinishtime, F.tags AS ftags,
    S.tcid AS stcid, S.name AS smatrix, S.status AS sstatus, S.failreason AS sfailreason, S.comment AS scomment, S.userStatus AS suserStatus, S.tcrid AS stcrid, S.mrid AS smrid,
        S.starttime AS sstarttime, S.finishtime AS sfinishtime, S.tags AS stags

FROM 
 (SELECT TC.id AS tcid, TC.testcaseid, M.name, TCR.status, TCR.failreason, TCR.comment, TS.name AS userStatus, TCR.id AS tcrid, MR.id AS mrid,
        TCR.starttime, TCR.finishtime,
        (SELECT GROUP_CONCAT(T.name SEPARATOR ', ')  FROM sttags T JOIN stmrtags MRT ON MRT.tag_id = T.id WHERE MRT.mr_id = MR.id ) AS tags
    FROM 
        (SELECT TC.id, max(TCR.startTime) AS startTime -- Latest execution of each test case tagged with set 1
        FROM sttestcaseruns TCR 
            JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
            JOIN sttestcases TC ON TCR.tc_id = TC.id
        WHERE EXISTS (

            SELECT MR2.id 
            FROM stmatrixruns MR2 
                JOIN stmrtags MRT ON MR2.id = MRT.mr_id
                JOIN sttags T ON MRT.tag_id = T.id
            WHERE MR2.id = MR.id 
            AND FIND_IN_SET(T.id, first_ids)
            GROUP BY MR2.id 
            HAVING count(*) >= first_count
        )
        AND TC.testcaseid <> '_unknown_tc_'
        GROUP BY 1 ) AS X

        JOIN sttestcaseruns TCR ON TCR.tc_id = X.id AND TCR.startTime = X.startTime
        JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
        JOIN sttestcases TC ON TCR.tc_id = TC.id
        LEFT JOIN sttcrstatus TS ON TCR.status_id = TS.id
        JOIN stmatrices M ON MR.matrix_id = M.id) AS
 F LEFT JOIN 
 (SELECT TC.id AS tcid, TC.testcaseid, M.name, TCR.status, TCR.failreason, TCR.comment, TS.name AS userStatus, TCR.id AS tcrid, MR.id AS mrid,
        TCR.starttime, TCR.finishtime, 
        (SELECT GROUP_CONCAT(T.name SEPARATOR ', ') FROM sttags T JOIN stmrtags MRT ON MRT.tag_id = T.id WHERE MRT.mr_id = MR.id ) AS tags
    FROM 
        (SELECT TC.id, max(TCR.startTime) AS startTime -- Latest execution of each test case tagged with set 1
        FROM sttestcaseruns TCR 
            JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
            JOIN sttestcases TC ON TCR.tc_id = TC.id
        WHERE EXISTS (

            SELECT MR2.id 
            FROM stmatrixruns MR2 
                JOIN stmrtags MRT ON MR2.id = MRT.mr_id
                JOIN sttags T ON MRT.tag_id = T.id
            WHERE MR2.id = MR.id 
            AND FIND_IN_SET(T.id, second_ids)        
            GROUP BY MR2.id 
            HAVING count(*) >= second_count
        )
        AND TC.testcaseid <> '_unknown_tc_'
        GROUP BY 1 ) AS X

        JOIN sttestcaseruns TCR ON TCR.tc_id = X.id AND TCR.startTime = X.startTime
        JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
        JOIN sttestcases TC ON TCR.tc_id = TC.id
        LEFT JOIN sttcrstatus TS ON TCR.status_id = TS.id
        JOIN stmatrices M ON MR.matrix_id = M.id) 

 S ON F.tcid = S.tcid

 UNION

 SELECT 
 coalesce(F.testcaseid, S.testcaseid) AS testcaseid,
    F.tcid AS ftcid, F.name AS fmatrix, F.status AS fstatus, F.failreason AS ffailreason, F.comment AS fcomment, F.userStatus AS fuserStatus, F.tcrid AS ftcrid, F.mrid AS fmrid,
        F.starttime AS fstarttime, F.finishtime AS ffinishtime, F.tags AS ftags,
    S.tcid AS stcid, S.name AS smatrix, S.status AS sstatus, S.failreason AS sfailreason, S.comment AS scomment, S.userStatus AS suserStatus, S.tcrid AS stcrid, S.mrid AS smrid,
        S.starttime AS sstarttime, S.finishtime AS sfinishtime, S.tags AS stags
  FROM 

 (SELECT TC.id AS tcid, TC.testcaseid, M.name, TCR.status, TCR.failreason, TCR.comment, TS.name AS userStatus, TCR.id AS tcrid, MR.id AS mrid,
        TCR.starttime, TCR.finishtime,
        (SELECT GROUP_CONCAT(T.name SEPARATOR ', ')  FROM sttags T JOIN stmrtags MRT ON MRT.tag_id = T.id WHERE MRT.mr_id = MR.id ) AS tags
    FROM 
        (SELECT TC.id, max(TCR.startTime) AS startTime -- Latest execution of each test case tagged with set 1
        FROM sttestcaseruns TCR 
            JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
            JOIN sttestcases TC ON TCR.tc_id = TC.id
        WHERE EXISTS (

            SELECT MR2.id 
            FROM stmatrixruns MR2 
                JOIN stmrtags MRT ON MR2.id = MRT.mr_id
                JOIN sttags T ON MRT.tag_id = T.id
            WHERE MR2.id = MR.id 
            AND FIND_IN_SET(T.id, first_ids)     
            GROUP BY MR2.id 
            HAVING count(*) >= first_count 
        )
        AND TC.testcaseid <> '_unknown_tc_'
        GROUP BY 1 ) AS X

        JOIN sttestcaseruns TCR ON TCR.tc_id = X.id AND TCR.startTime = X.startTime
        JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
        JOIN sttestcases TC ON TCR.tc_id = TC.id
        LEFT JOIN sttcrstatus TS ON TCR.status_id = TS.id
        JOIN stmatrices M ON MR.matrix_id = M.id) AS
 F RIGHT JOIN 
 (SELECT TC.id AS tcid, TC.testcaseid, M.name, TCR.status, TCR.failreason, TCR.comment, TS.name AS userStatus, TCR.id AS tcrid, MR.id AS mrid,
        TCR.starttime, TCR.finishtime, 
        (SELECT GROUP_CONCAT(T.name SEPARATOR ', ') FROM sttags T JOIN stmrtags MRT ON MRT.tag_id = T.id WHERE MRT.mr_id = MR.id ) AS tags
    FROM 
        (SELECT TC.id, max(TCR.startTime) AS startTime -- Latest execution of each test case tagged with set 1
        FROM sttestcaseruns TCR 
            JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
            JOIN sttestcases TC ON TCR.tc_id = TC.id
        WHERE EXISTS (

            SELECT MR2.id 
            FROM stmatrixruns MR2 
                JOIN stmrtags MRT ON MR2.id = MRT.mr_id
                JOIN sttags T ON MRT.tag_id = T.id
            WHERE MR2.id = MR.id 
            AND FIND_IN_SET(T.id, second_ids)      
            GROUP BY MR2.id 
            HAVING count(*) >= second_count 
        )
        AND TC.testcaseid <> '_unknown_tc_'
        GROUP BY 1 ) AS X

        JOIN sttestcaseruns TCR ON TCR.tc_id = X.id AND TCR.startTime = X.startTime
        JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
        JOIN sttestcases TC ON TCR.tc_id = TC.id
        LEFT JOIN sttcrstatus TS ON TCR.status_id = TS.id
        JOIN stmatrices M ON MR.matrix_id = M.id) 

 S ON F.tcid = S.tcid
 

ORDER BY 1;
END
//;
DELIMITER ;