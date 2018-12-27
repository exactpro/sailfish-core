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
package com.exactpro.sf.common.util;

import java.time.format.DateTimeFormatter;

import com.exactpro.sf.util.DateTimeUtility;

public class LocalDateTimeConst {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeUtility.createFormatter("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnn");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeUtility.createFormatter("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeUtility.createFormatter("HH:mm:ss.nnnnnnnnn");
}
