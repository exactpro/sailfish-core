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
package com.exactpro.sf.aml.scriptutil;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.scriptrunner.StatusType;

public class LegReorder {

	private static Logger logger = LoggerFactory.getLogger(LegReorder.class);

	private LegReorder() {
		// hide constructor
	}

	// 1) message matches filter
	// 2) message is IMessage (so it mainly for AML3)
    public static IMessage reorder(final IMessage message, final IMessage filter, ComparatorSettings settings) {
		logger.debug("start reorder legs");
		logger.debug("message: {}", message);
		logger.debug("filter: {}", filter);

		for (String fieldName : message.getFieldNames()) {
			final Object message_field = message.getField(fieldName);
			final Object filter_field = filter.getField(fieldName);

			if (message_field == null || filter_field == null) {
				continue;
			}

			if (message_field instanceof List && filter_field instanceof List) {
				@SuppressWarnings("unchecked")
				final List<IMessage> message_legs = (List<IMessage>) message_field;

				@SuppressWarnings("unchecked")
				final List<IMessage> filter_legs = (List<IMessage>) filter_field;

				final int m_size = message_legs.size();
				final int f_size = filter_legs.size();

				if (m_size == 0 || f_size == 0) {
					continue;
				}

				// Mapping of legs: order[i]-th leg should be i-th in resulting list
				int[] order = findPermute(message_legs, filter_legs, settings);
				if (order == null) {
					StringBuilder sb = new StringBuilder();
					sb.append("\n").append("LEGS: ").append(message_legs);
					sb.append("\n");
					sb.append("FILTER: ").append(filter_field);
					sb.append("\n");
					throw new IllegalArgumentException("Failed to found correct order of legs: " + sb.toString());
				}

				final List<IMessage> new_legs = new ArrayList<>(message_legs.size());

				for (int i = 0; i < order.length; i++) {
					IMessage msg = message_legs.get(order[i]);
					if (i < filter_legs.size()) {
						IMessage fltr = filter_legs.get(i);
						new_legs.add(reorder(msg, fltr, settings)); // recursion
					} else {
						new_legs.add(msg); // no recursion
					}
				}

				message.addField(fieldName, new_legs);

			}
		}

		return message;
	}

    private static int[] findPermute(final List<IMessage> message_legs, final List<IMessage> filter_legs, final ComparatorSettings settings) {
		final int m_size = message_legs.size();
		final int f_size = filter_legs.size();

		// cache for compare(msg[i], filter[j])
		final Boolean[][] equality = new Boolean[m_size][f_size];

		// Mapping of legs: order[i]-th leg should be i-th in resulting list
		int[] permute = new int[m_size];
		for (int i =0; i< permute.length; i++) {
			permute[i] = i;
		}

		boolean isOk = findPermute(permute, 0, equality, message_legs, filter_legs, settings);
		if (!isOk) {
			logger.error("Failed to sort legs: {}", (Object)equality);
			return null;
		}
		return permute;
	}

	// There are n! permutations of legs
    private static boolean findPermute(int[] arr, int k, Boolean[][] equality, List<IMessage> message_legs, List<IMessage> filter_legs, ComparatorSettings settings) {
		logger.trace("Checking permutation: arr={}, k={}", arr, k);

		// each function call we get deeper... check that this subtree of all permutations is OK:
		if (k > 0) {
			// here we will check permutation prefix:
			//
			// we are looking permutation that:
			// * equality[i][j] is true for all pairs of MESSAGE[i] and FILTER[j], where j = arr[i]
			int idx = k - 1;
			int i = arr[idx];
			int j = idx;
			if (j >= equality[0].length) {
				// when filter is shorter than leg... we should sort only first legs
				// This permutation sub-tree is ok: continue to build permutation till the end
			} else {
				if (equality[i][j] == null) {
					IMessage mleg = message_legs.get(i);
					IMessage fleg = filter_legs.get(j);

                    ComparisonResult comparisonResult = MessageComparator.compare(mleg, fleg, settings);

                    if (comparisonResult == null) {
						equality[i][j] = false;
					} else {
                        int failed_count = ComparisonUtil.getResultCount(comparisonResult, StatusType.FAILED) + ComparisonUtil.getResultCount(comparisonResult, StatusType.CONDITIONALLY_FAILED);
						equality[i][j] = failed_count == 0;

					}
					logger.trace("equality[{}][{}] == {}", i, j, equality[i][j]);
				}
				if (equality[i][j] == false) {
					return false;
				}
			}
		}

		// here we will generate all possible permutations of arr:
		for (int i = k; i < arr.length; i++) {
			swap(arr, i, k);
			boolean ret = findPermute(arr, k + 1, equality, message_legs, filter_legs, settings);
			if (ret) {
				return ret;
			}
			swap(arr, k, i);
		}

		if (k == arr.length - 1) {
			// here we get final permutation:
			return true;
		}
		return false;
	}

	private static void swap(int[] arr, int i, int k) {
		int tmp = arr[i];
		arr[i] = arr[k];
		arr[k] = tmp;
	}

}
