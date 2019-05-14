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
package com.exactpro.sf.testwebgui.messages;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.primefaces.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageSorter implements Comparator<MessageAdapter>{

	private static final Logger logger = LoggerFactory.getLogger(MessageSorter.class);

    private final Set<String> fields;
    private final SortOrder order;

    public MessageSorter(Set<String> fields, SortOrder order) {
		this.fields = fields;
		this.order = order;
	}

	@Override
	public int compare(MessageAdapter m1, MessageAdapter m2) {

		try {

			int result = 0;

			for (String field : fields) {

				Field f = MessageAdapter.class.getDeclaredField(field);
				f.setAccessible(true);
				Object value1 = f.get(m1);
				Object value2 = f.get(m2);
					result = compareValues(value1, value2);

                if(result != 0) {
                    break;
                }

			}

            return order == SortOrder.ASCENDING ? result : result * -1;

		} catch (Exception e) {

			logger.error("{} {} {} {} {}", e.getMessage(), fields, order, m1, m2, e);
			return 0;

		}

	}

    @SuppressWarnings("unchecked")
    private int compareValues(Object value1, Object value2) {
        return ObjectUtils.compare((Comparable<Object>)value2, (Comparable<Object>)value1);
    }

}
