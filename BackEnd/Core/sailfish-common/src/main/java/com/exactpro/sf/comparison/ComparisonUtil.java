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
package com.exactpro.sf.comparison;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.util.DateTimeUtility;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.scriptrunner.StatusType;

/**
 *
 * @author dmitry.guriev
 *
 */
public class ComparisonUtil {

    private static final Map<Class<?>, DateTimeFormatter> CLASS_TO_FORMATTER = new HashMap<>();

	private ComparisonUtil()
	{
		// hide constructor
	}

    static {
        CLASS_TO_FORMATTER.put(LocalDate.class, DateTimeFormatter.ISO_DATE);
        CLASS_TO_FORMATTER.put(LocalTime.class, DateTimeFormatter.ISO_TIME);
        CLASS_TO_FORMATTER.put(LocalDateTime.class, DateTimeFormatter.ISO_DATE_TIME);
    }

    public static String formatTemporal(TemporalAccessor accessor) {
        ZonedDateTime zonedDateTime = DateTimeUtility.toZonedDateTime(accessor);
        return CLASS_TO_FORMATTER.get(accessor.getClass()).format(zonedDateTime);
    }

    public static int getResultCount(ComparisonResult result, StatusType status)
	{
		int count = 0;

        if(status == result.getStatus()) {
			count++;
		}

        for(ComparisonResult subResult : result)
		{
            count += getResultCount(subResult, status);
		}

		return count;
	}

	public static StatusType getStatusType(int failed, int condicitionallyFalied, int condicitionallyPassed, int passed, StatusType defaultStatus) {
        if (failed != 0) {
            return StatusType.FAILED;
        } else if (condicitionallyFalied != 0) {
            return StatusType.FAILED; //Apply StatusType.CONDITIONALLY_FAILED after additional supporting current status
        } else if (condicitionallyPassed != 0) {
            return StatusType.CONDITIONALLY_PASSED;
        } else if (passed != 0) {
            return StatusType.PASSED;
        }

        return defaultStatus;
    }

	public static StatusType getStatusType(int failed, int condicitionallyFalied, int condicitionallyPassed, int passed) {
	    return getStatusType(failed, condicitionallyFalied, condicitionallyPassed, passed, StatusType.NA);
	}

    public static StatusType getStatusType(ComparisonResult comparisonResult) {
	    int failed = getResultCount(comparisonResult, StatusType.FAILED);
	    int condicitionallyFalied = getResultCount(comparisonResult, StatusType.CONDITIONALLY_FAILED);
	    int condicitionallyPassed = getResultCount(comparisonResult, StatusType.CONDITIONALLY_PASSED);
	    int passed = getResultCount(comparisonResult, StatusType.PASSED);

	    return getStatusType(failed, condicitionallyFalied, condicitionallyPassed, passed);
    }

    public static Map<String, Object> toMap(ComparisonResult result, boolean expected) {
        Map<String, Object> map = new LinkedHashMap<>();

        if(!expected) {
            map.put("status", ObjectUtils.defaultIfNull(result.getStatus(), StatusType.NA));
        }

        Object value = expected ? result.getExpected() : result.getActual();

        setTypeAndValue(map, value);

        if(value instanceof IComparisonFilter || !result.hasResults()) {
            return map;
        }

        ComparisonResult firstElement = result.getResult("0");
        boolean array = firstElement != null;

        if(array) {
            String genericType = null;

            if(firstElement.hasResults()) {
                genericType = IMessage.class.getSimpleName();
            } else {
                Object firstValue = ObjectUtils.defaultIfNull(firstElement.getActual(), firstElement.getExpected());
                genericType = ClassUtils.getSimpleName(firstValue, null);
            }

            map.put("type", List.class.getSimpleName() + "<" + genericType + ">");
        } else {
            map.put("type", IMessage.class.getSimpleName());
        }

        if(!expected && result.getExpected() != null && result.getActual() == null) {
            map.put("value", null);
            return map;
        }

        Map<String, Object> subMap = new LinkedHashMap<>();

        for(ComparisonResult subResult : result) {
            if(expected && subResult.getExpected() == null && subResult.getActual() != null) {
                continue;
            }

            subMap.put(subResult.getName(), toMap(subResult, expected));
        }

        map.put("value", array ? subMap.values() : subMap);

        return map;
    }

    private static void setTypeAndValue(Map<String, Object> map, Object value) {
        map.put("type", ClassUtils.getSimpleName(value, null));

        if(value instanceof IComparisonFilter) {
            IComparisonFilter filter = (IComparisonFilter)value;

            if(filter.hasValue()) {
                map.put("type", ClassUtils.getSimpleName(value = filter.getValue(), null));
            } else {
                value = filter.getCondition();
            }
        } else if(value instanceof LocalDate || value instanceof LocalTime || value instanceof LocalDateTime) {
            value = formatTemporal((TemporalAccessor)value);
        }

        map.put("value", value);
    }
}
