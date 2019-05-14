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

import com.exactpro.sf.embedded.statistics.configuration.DbmsType;

public class NativeQueryUtil {

    public static final String TAG_GROUP_TEMP_TABLE = "mr_and_tcr_ids";

    public static final String DROP_TAG_GROUP_TEMP_TABLE =
            "DROP TABLE IF EXISTS " + TAG_GROUP_TEMP_TABLE + ";";

    public static final String CREATE_TAG_GROUP_TEMP_TABLE =
            "CREATE TEMPORARY TABLE  " + TAG_GROUP_TEMP_TABLE + "  AS  " +
                    "  SELECT  " +
                    "    T.id as tag_id, " +
                    "    T.name as tag_name,  " +
                    "    G.id as group_id, " +
                    "    MRT.mr_id as mr_id, " +
                    "    TCRT.tcr_id as tcr_id " +
                    "  FROM ( " +
                    "    SELECT *  " +
                    "    FROM sttags  " +
                    "    WHERE sttags.id IN (:tagIds) " +
                    "  ) AS T     " +
                    "  LEFT JOIN sttaggroups G ON T.group_id = G.id " +
                    "  LEFT JOIN stmrtags AS MRT ON MRT.tag_id = T.id   " +
                    "  LEFT JOIN sttcrtags AS TCRT ON TCRT.tag_id = T.id;";

    public static final String CHECK_TESTCASE_TAGS_QUERY =
                    "SELECT 1 FROM " +  TAG_GROUP_TEMP_TABLE +
                    "    WHERE "  + TAG_GROUP_TEMP_TABLE + ".tcr_id IS NOT NULL LIMIT 1";

    public static final String CHECK_MATRIX_TAGS_QUERY =
            "SELECT 1 FROM " +  TAG_GROUP_TEMP_TABLE +
                    "    WHERE "  + TAG_GROUP_TEMP_TABLE + ".mr_id IS NOT NULL LIMIT 1";


    public static boolean isMysql(String type) {
        return DbmsType.MySql.getValue().equals(type);
    }

    public static boolean isPostgreSql(String type) {
        return DbmsType.PostgreSQL.getValue().equals(type);
    }
}
