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
package com.exactpro.sf.aml;

import com.exactpro.sf.configuration.suri.SailfishURI;

public class AMLLangConst {
    public static final String MAP_NAME = "messages";

    public static final String REGEX_MVEL_STRING = "(\"[^\"]*\")+|('[^']*')+";
    public static final String REGEX_MVEL_DELIMETER = "(\\+|\\-|\\*|/| |\\(|\\)|=|>|<|!|&|\\||~|%|,|\\?|:)+";
    public static final String REGEX_MVEL_NOT_VARIABLE = "(" + REGEX_MVEL_STRING + "|" + REGEX_MVEL_DELIMETER + "|$)+";

    public static final String REGEX_FIELD_START = "Regexp[";
    public static final String REGEX_FIELD_END = "]";

    public static final String TAG_INTERPRET_AS_JAVA = "java:";
    public static final String BEGIN_REFERENCE = "${";
    public static final String END_REFERENCE = "}";
    public static final String BEGIN_STATIC = "%{";
    public static final String END_STATIC = "}";
    public static final String BEGIN_FUNCTION = "#{";
    public static final String END_FUNCTION = "}";

    public static final String INCLUDE_BLOCK_OLD_ACTION = "include block";

    public static final SailfishURI DEFINE_MESSAGE_ACTION_URI = SailfishURI.unsafeParse("System_DefineMessage");
    public static final SailfishURI COMPARE_ACTION_URI = SailfishURI.unsafeParse("Compare");
    public static final SailfishURI ASK_FOR_CONTINUE_ACTION_URI = SailfishURI.unsafeParse("AskForContinue");
    public static final SailfishURI GET_CHECK_POINT_ACTION_URI = SailfishURI.unsafeParse("GetCheckPoint");
    public static final SailfishURI GET_ADMIN_CHECK_POINT_ACTION_URI = SailfishURI.unsafeParse("GetAdminCheckPoint");
    public static final SailfishURI INIT_MAP_URI = SailfishURI.unsafeParse("initMap");

    public static final String YES = "y";
    public static final String NO = "n";
    public static final String ALL = "a";
    public static final String OPTIONAL = "o";

    public static final String SMART_CHECKPOINT_PREFIX = "!";

    public static final SailfishURI AML2 = SailfishURI.unsafeParse("AML_v2");
    public static final SailfishURI AML3 = SailfishURI.unsafeParse("AML_v3");
}
