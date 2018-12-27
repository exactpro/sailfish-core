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
package com.exactpro.sf.actions;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.IPostValidation;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;
import com.exactpro.sf.storage.impl.IDGenerator;
import com.exactpro.sf.util.BugDescription;
import com.exactpro.sf.util.FieldKnownBugException;
import com.exactpro.sf.util.KnownBugPostValidation;

@MatrixUtils
@ResourceAliases({"MiscUtils"})
public class MiscUtils extends AbstractCaller {

	@Description("Returns a random element from an array of strings.<br/>"
            + "<b>arr</b> - array of strings.<br/>"
            + "Example:<br/>"
            + "#{GetRandomString(\"A\", \"B\", \"C\")}")
	@UtilityMethod
	public String GetRandomString(String ... arr)
	{
		double index = Math.random() * arr.length;
		return arr[(int) index];
	}

    @Description("Returns a string with random printable ASCII characters.<br/>"
            + "<b>length</b> - the length of the result string.<br/>"
            + "Example:<br/>"
            + "#{GetRandomString(23)}")
	@UtilityMethod
	public String GetRandomString(int length)
	{
        byte[] asciiBytes = new byte[length];

        // range for printable symbols except 'space'(32)
        int range = 127 - 33;

        for(int i = 0; i < length; i++) {
            asciiBytes[i] = (byte)(33 + (Math.random() * range));
        }

        return new String(asciiBytes, StandardCharsets.US_ASCII);
	}

	@Description("Returns a random element from an array of integers.<br/>"
            + "<b>arr</b> - array of integers.<br/>"
            + "Example:<br/>"
            + "#{GetRandomInt(1, 2, 3)}")
	@UtilityMethod
	public int GetRandomInt(int ... arr)
	{
		double index = Math.random() * arr.length;
		return arr[(int) index];
	}

	@Description("Returns a random element from an array of long."
            + "<b>arr</b> - array of long.<br/>"
            + "Example:<br/>"
            + "#{GetRandomLong(1, 2, 3)}")
	@UtilityMethod
	public long GetRandomLong(long ... arr)
	{
		double index = Math.random() * arr.length;
		return arr[(int) index];
	}

	@Description("Generates a unique string ID like 7e688747-7aa6-4304-b7a0-55d2a18ec91a."
            + "Example:<br/>"
            + "#{GenerateID()}")
	@UtilityMethod
	public String GenerateID()
	{
		return IDGenerator.createId();
	}

    @UtilityMethod
    @Description("Concatenates the string representations of the provided values into a single string.<br/>"
            + "<b>values</b> - string values for concatenating<br/>"
            + "Example:<br/> "
            + "#{concat(\"Text\", ' ', \"example\")} returns \"Text example\"")
    public String concat(Object... values) {
        if(values == null) {
            return null;
        }

        if(values.length == 0) {
            return StringUtils.EMPTY;
        }

        StringBuilder builder = new StringBuilder(values.length * 16);

        for(Object value : values) {
            builder.append(value);
        }

        return builder.toString();
    }

    @UtilityMethod
    @Description("Creates a formatted string using the provided format string and arguments.<br/>"
            + "<b>format</b> - provided format string.<br/>"
            + "<b>args</b> - values for formatting.<br/>"
            + "Example:<br/>"
            + "#{format(\"{} example{}\", \"Text\", '!')} returns \"Text example!\"")
    public String format(String format, Object... args) {
        return MessageFormatter.arrayFormat(format, args).getMessage();
    }

	@UtilityMethod
	@Description("Returns the number of elements in this collection<br/>"
            + "<b>collection</b> - messages for size calculation<br/>"
	        + "Considering the above said, the final syntax is:<br/>"
            + "#{getSize(collection)}")
	public int getSize(Collection<?> collection) {
        return getSize(collection, null);
	}

    @UtilityMethod
    @Description("Returns the number of messages in this collection in accordance with the filter<br/>"
            + "<b>collection</b> - messages for checking.<br/>"
            + "<b>filter</b> - filter message.<br/>"
            + "Considering the above said, the final syntax is:<br/>"
            + "#{getSize(collection, filter)}")
    public int getSize(Collection<?> collection, Object filter) {
        if (filter == null) {
	    return Objects.requireNonNull(collection, "collection cannot be null").size();
	}

        if (filter instanceof IMessage) {
            IMessage messageFilter = (IMessage)filter;
            int count = 0;
            for (Object object : collection) {
                if (object instanceof IMessage) {
                    ComparatorSettings settings = new ComparatorSettings();
                    IMessage message = (IMessage)object;
                    ComparisonResult comparisonResult = MessageComparator.compare(message, messageFilter, settings);
                    StatusType statusType = ComparisonUtil.getStatusType(comparisonResult);

                    if (statusType == StatusType.PASSED) {
                        count++;
                    }
                }
            }
            return count;
        } else {
            return Collections.frequency(collection, filter);
        }
    }

    @UtilityMethod
    @Description("Returns  the equality of the expected count of messages in accordance with the filter and the actual.<br/>"
            + "<b>collection</b> - messages for checking.<br/>"
            + "<b>filter</b> - filter message.<br/>"
            + "<b>expected</b> - expected number of occurrences of the filter message.<br/>"
            + "Considering the above said, the final syntax is:<br/>"
            + "#{getCount(collection, filter, expectedCount)}")
    public boolean checkCount(Collection<?> collection, Object filter, int expected) throws Exception {
        Objects.requireNonNull(collection, "collection cannot be null");
        Objects.requireNonNull(filter, "filter cannot be null");

        if (filter instanceof IMessage) {
            IMessage messageFilter = (IMessage)filter;
            int count = 0;
            StringBuilder knownBugBuilder = new StringBuilder();

            int itemCount = 0;
            for (Object object : collection) {
                if (object instanceof IMessage) {
                    ComparatorSettings settings = new ComparatorSettings();
                    IPostValidation validation = new KnownBugPostValidation(null, messageFilter);
                    settings.setPostValidation(validation);

                    IMessage message = (IMessage)object;
                    ComparisonResult comparisonResult = MessageComparator.compare(message, messageFilter, settings);
                    StatusType statusType = ComparisonUtil.getStatusType(comparisonResult);

                    if (statusType == StatusType.CONDITIONALLY_PASSED || statusType == StatusType.PASSED) {
                        count++;
                        if (statusType == StatusType.CONDITIONALLY_PASSED) {
                            knownBugBuilder.append(itemCount).append(" : ").append(comparisonResult.getExceptionMessage()).append(System.lineSeparator());
                        }
                    }
                }
                itemCount++;
            }

            if (count == expected) {
                if (knownBugBuilder.length() != 0) {
                    throw new FieldKnownBugException("Count equlas with known bugs", new BugDescription(knownBugBuilder.toString()));
                }
                return true;
            }
            return false;
        } else {
            return Collections.frequency(collection, filter) == expected;
        }
    }
}