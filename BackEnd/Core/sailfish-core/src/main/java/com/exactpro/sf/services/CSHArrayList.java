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
package com.exactpro.sf.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.CheckPoint;

public class CSHArrayList<E> extends ArrayList<E> {
	private static final Logger logger = LoggerFactory.getLogger(CSHArrayList.class);
	private static final long serialVersionUID = -2751265570161216250L;

	private static final AtomicLong HASH_COUNTER = new AtomicLong(0);
	private final long id = HASH_COUNTER.incrementAndGet();

	private final Map<CheckPoint, Integer> checkPointToIndex = new HashMap<>();

	public CSHArrayList() {
		logger.debug("create handler list: {}", id);
	}

	public void addCheckPoint(CheckPoint checkPoint) {
	    putCheckPoint(checkPoint, super.size());
	}

	protected void putCheckPoint(CheckPoint checkPoint, int index) {
	    checkPointToIndex.put(checkPoint, index);
	}

    public int getIndex(CheckPoint checkPoint) {
        Integer index = checkPointToIndex.get(checkPoint);
        return index != null ? index : 0;
    }

    public List<E> subList(int fromIndex) {
        int size = super.size();

        if(fromIndex < size) {
            return new ArrayList<>(super.subList(fromIndex, size));
        }

        return new ArrayList<>();
    }

	public long getID() {
		return id;
	}

	@Override
	public void clear() {
		logger.debug("clear handler list: {}", id);
		super.clear();
		checkPointToIndex.clear();
	}
}
