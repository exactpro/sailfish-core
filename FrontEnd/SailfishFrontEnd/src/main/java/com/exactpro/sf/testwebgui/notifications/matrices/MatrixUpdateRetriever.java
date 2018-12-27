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
package com.exactpro.sf.testwebgui.notifications.matrices;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.storage.MatrixUpdateListener;
import com.exactprosystems.webchannels.IUpdateRequestListener;
import com.exactprosystems.webchannels.IUpdateRetriever;

@SuppressWarnings("deprecation")
public class MatrixUpdateRetriever implements IUpdateRetriever, MatrixUpdateListener {

	private static final Logger logger = LoggerFactory.getLogger(MatrixUpdateRetriever.class);

	private List<IUpdateRequestListener> listeners;

	public MatrixUpdateRetriever() {
		listeners = new CopyOnWriteArrayList<>();
	}

	private void notifyListener(IUpdateRequestListener listener, MatrixUpdateEvent event) {
		try {
			listener.onEvent(event);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
    public void registerUpdateRequest(IUpdateRequestListener listener) {

		logger.debug("Listener {} register in MatrixUpdateRetriever {}", listener, this);

		if (listener instanceof MatrixUpdateSubscriber) {
			listeners.add(listener);
		} else {
			throw new RuntimeException("Listener = " + listener + " is not instance of " + MatrixUpdateSubscriber.class);
		}
	}

	@Override
	public void unregisterUpdateRequest(IUpdateRequestListener listener) {

		logger.debug("Listener {} unregister in MatrixUpdateRetriever {}", listener, this);

		if (listener instanceof MatrixUpdateSubscriber) {
			listeners.remove(listener);
		} else {
			throw new RuntimeException("Listener = " + listener + " is not instance of " + MatrixUpdateSubscriber.class);
		}

	}

	@Override
	public void synchronizeUpdateRequest(IUpdateRequestListener listener) {
		// Not needed yet, because it sends only notifications about updates
	}

	@Override
	public void onEvent() {

		MatrixUpdateEvent event = new MatrixUpdateEvent();

		for (IUpdateRequestListener listener : listeners){
			notifyListener(listener, event);
		}

	}

	@Override
	public void destroy() {
		listeners.clear();
	}

}
