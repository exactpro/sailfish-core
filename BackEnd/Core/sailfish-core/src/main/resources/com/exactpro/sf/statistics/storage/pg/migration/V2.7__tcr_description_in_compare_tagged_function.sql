DROP FUNCTION tagged_sets_comparison(bigint[], bigint, bigint[], bigint);

-- function body is the same is in V2.2, but TCR description has been added to result set
CREATE OR REPLACE FUNCTION tagged_sets_comparison(IN first_ids bigint[], IN first_count bigint, IN second_ids bigint[], IN second_count bigint)
    RETURNS TABLE(testcaseid character varying(255),
    ftcid bigint, fmatrix character varying(255), fstatus integer, fdescription character varying(255), ffailreason character varying(255), ffailedactions text, fcomment text, fuserStatus text, ftcrid bigint, fmrid bigint,
        fstarttime timestamp, ffinishtime timestamp, ftags text, fhash integer,
    stcid bigint, smatrix character varying(255), sstatus integer, sdescription character varying(255), sfailreason character varying(255), sfailedactions text, scomment text, suserStatus text, stcrid bigint, smrid bigint,
        sstarttime timestamp, sfinishtime timestamp, stags text, shash integer) AS

$BODY$
BEGIN

RETURN QUERY

WITH first_tcrs AS (

    SELECT TC.id AS tcid, TC.testcaseid, M.name, TCR.status, TCR.description, TCR.failreason, TCR.comment, TS.name AS userStatus, TCR.id AS tcrid, MR.id AS mrid,
        TCR.starttime, TCR.finishtime, TCR.hash,
        (SELECT string_agg(T.name, ', ' ORDER BY T.name)  FROM sttags T JOIN stmrtags MRT ON MRT.tag_id = T.id WHERE MRT.mr_id = MR.id ) AS tags,
        string_agg(AR.rank::text || 'ARFRSPLITTER' || AR.failreason, 'FACTSPLITTER' ORDER BY AR.rank) AS failedactions
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
            AND T.id = ANY (first_ids)         -------- tags1 parameter
            GROUP BY MR2.id
            HAVING count(*) >= first_count -------- tags1 count parameter
        )
        AND TC.testcaseid <> '_unknown_tc_'
        GROUP BY 1 ) AS X

        JOIN sttestcaseruns TCR ON TCR.tc_id = X.id AND TCR.startTime = X.startTime
        JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
        JOIN sttestcases TC ON TCR.tc_id = TC.id
        LEFT JOIN sttcrstatus TS ON TCR.status_id = TS.id
        JOIN stmatrices M ON MR.matrix_id = M.id
        LEFT JOIN stactionruns AR ON AR.tc_run_id = TCR.id AND AR.failreason IS NOT NULL
		GROUP BY tcid, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14
),

second_tcrs AS (

    SELECT TC.id AS tcid, TC.testcaseid, M.name, TCR.status, TCR.description, TCR.failreason, TCR.comment, TS.name AS userStatus, TCR.id AS tcrid, MR.id AS mrid,
        TCR.starttime, TCR.finishtime, TCR.hash,
        (SELECT string_agg(T.name, ', ' ORDER BY T.name)  FROM sttags T JOIN stmrtags MRT ON MRT.tag_id = T.id WHERE MRT.mr_id = MR.id ) AS tags,
        string_agg(AR.rank::text || 'ARFRSPLITTER' || AR.failreason, 'FACTSPLITTER' ORDER BY AR.rank) AS failedactions
    FROM
        (SELECT TC.id, max(TCR.startTime) AS startTime -- Latest execution of each test case tagged with set 2
        FROM sttestcaseruns TCR
            JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
            JOIN sttestcases TC ON TCR.tc_id = TC.id
        WHERE EXISTS (

            SELECT MR2.id
            FROM stmatrixruns MR2
                JOIN stmrtags MRT ON MR2.id = MRT.mr_id
                JOIN sttags T ON MRT.tag_id = T.id
            WHERE MR2.id = MR.id
            AND T.id = ANY (second_ids)         -------- tags2 parameter
            GROUP BY MR2.id
            HAVING count(*) >= second_count -------- tags2 count parameter
        )
        AND TC.testcaseid <> '_unknown_tc_'
        GROUP BY 1 ) AS X

        JOIN sttestcaseruns TCR ON TCR.tc_id = X.id AND TCR.startTime = X.startTime
        JOIN stmatrixruns MR ON TCR.matrix_run_id = MR.id
        JOIN sttestcases TC ON TCR.tc_id = TC.id
        LEFT JOIN sttcrstatus TS ON TCR.status_id = TS.id
        JOIN stmatrices M ON MR.matrix_id = M.id
        LEFT JOIN stactionruns AR ON AR.tc_run_id = TCR.id AND AR.failreason IS NOT NULL
		GROUP BY tcid, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14

)

SELECT coalesce(F.testcaseid, S.testcaseid) AS testcaseid,
    F.tcid AS ftcid, F.name AS fmatrix, F.status AS fstatus, F.description AS fdescription, F.failreason AS ffailreason, F.failedactions AS ffailedactions, F.comment AS fcomment, F.userStatus AS fuserStatus, F.tcrid AS ftcrid, F.mrid AS fmrid,
        F.starttime AS fstarttime, F.finishtime AS ffinishtime, F.tags AS ftags, F.hash AS fhash,
    S.tcid AS stcid, S.name AS smatrix, S.status AS sstatus, S.description AS sdescription, S.failreason AS sfailreason, S.failedactions AS sfailedactions, S.comment AS scomment, S.userStatus AS suserStatus, S.tcrid AS stcrid, S.mrid AS smrid,
        S.starttime AS sstarttime, S.finishtime AS sfinishtime, S.tags AS stags, S.hash AS shash

FROM first_tcrs F FULL JOIN second_tcrs S ON F.tcid = S.tcid
ORDER BY 1;

END;
$BODY$
  LANGUAGE plpgsql;
