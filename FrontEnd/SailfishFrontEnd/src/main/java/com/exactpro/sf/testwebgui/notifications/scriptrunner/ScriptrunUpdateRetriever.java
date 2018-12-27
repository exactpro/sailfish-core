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
package com.exactpro.sf.testwebgui.notifications.scriptrunner;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.scriptrunner.IScriptRunListener;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactprosystems.webchannels.DefaultUpdateRetriever;
import com.exactprosystems.webchannels.IUpdateRequestListener;

@SuppressWarnings("deprecation")
public class ScriptrunUpdateRetriever extends DefaultUpdateRetriever implements IScriptRunListener {

	private static final Logger logger = LoggerFactory.getLogger(ScriptrunUpdateRetriever.class);

	private static final String BLOCK_ID_PREFIX = "eps-result-";
	private static final String EMPTY_BLOCK_ID = "empty";

    private ISFContext context;
	private List<IUpdateRequestListener> listeners;

	public ScriptrunUpdateRetriever(ISFContext context){
	    super(IUpdateRequestListener.class);
	    this.context = context;
		listeners = new CopyOnWriteArrayList<>();
	}

	private void notifyListener(IUpdateRequestListener listener, ScriptrunUpdateEvent event) {
		try {
			listener.onEvent(event);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private ScriptrunUpdateEvent formatEvent(TestScriptDescription descr) {
        ScriptrunUpdateEvent newEvent;
        if(descr == null){
            newEvent = new ScriptrunUpdateEvent(ScriptrunEventHTMLBuilder.getEmptyMessage());
            newEvent.setDivId(EMPTY_BLOCK_ID);
            newEvent.setScriptRunId(0L);
        } else{
            String htmlMarkup = ScriptrunEventHTMLBuilder.buildHTMLEvent(descr, context);
            newEvent = new ScriptrunUpdateEvent(htmlMarkup);
            newEvent.setDivId(BLOCK_ID_PREFIX + descr.getId());
            newEvent.setScriptRunId(descr.getId());
            newEvent.setState(descr.getState());
            newEvent.setStatus(descr.getStatus());
        }
		return newEvent;
	}

	@Override
	public void registerUpdateRequest(IUpdateRequestListener listener) {

		logger.debug("Listener {} register in ScriptrunUpdateRetriever {}", listener, this);

		if (listener instanceof ScriptrunUpdateSubscriber) {
			listeners.add(listener);
		}

	}

	@Override
	public void unregisterUpdateRequest(IUpdateRequestListener listener) {

		logger.debug("Listener {} unregister in ScriptrunUpdateRetriever {}", listener, this);

		if (listener instanceof ScriptrunUpdateSubscriber) {
			listeners.remove(listener);
		}
	}

	@Override
	public void synchronizeUpdateRequest(IUpdateRequestListener listener) {
		// Not needed, because initial states generates in jsf
	}

    public String getCurrentStateSnapshot(Comparator<TestScriptDescription> comparator) {

		if(context == null) {
			return "";
		}

		List<TestScriptDescription> descriptions = context.getScriptRunner().getDescriptions();

		if (descriptions.isEmpty()) {
			return ScriptrunEventHTMLBuilder.getEmptyMessage();
		}
        descriptions = descriptions.stream().sorted(comparator).collect(Collectors.toList());

		StringBuilder sb = new StringBuilder();

		for(TestScriptDescription descr : descriptions) {
			if (descr != null) {
				sb.append(ScriptrunEventHTMLBuilder.buildHTMLEvent(descr, context));
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	@Override
	public void destroy() {
		listeners.clear();
	}

	@Override
	public void onScriptRunEvent(TestScriptDescription descr) {

		ScriptrunUpdateEvent event = formatEvent(descr);

		for (IUpdateRequestListener listener : listeners){
			notifyListener(listener, event);
		}

	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).toString();
	}

}
