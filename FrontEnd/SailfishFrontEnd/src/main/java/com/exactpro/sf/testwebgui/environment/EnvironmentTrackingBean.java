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
package com.exactpro.sf.testwebgui.environment;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.scriptrunner.IEnvironmentListener;
import com.exactpro.sf.services.ChangeEnvironmentEvent;
import com.exactpro.sf.services.EnvironmentEvent;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.testwebgui.BeanUtil;

public class EnvironmentTrackingBean implements IEnvironmentListener {

	private List<String> environmentList;

	public EnvironmentTrackingBean() {
		this.environmentList = new CopyOnWriteArrayList<>();

        this.environmentList.addAll(BeanUtil.getSfContext().getConnectionManager().getEnvironmentList());
        this.environmentList.add(0, ServiceName.DEFAULT_ENVIRONMENT);

        BeanUtil.getSfContext().getConnectionManager().subscribeForEvents(this);
	}

	public List<String> getEnvironmentList() {
		return Collections.unmodifiableList(this.environmentList);
	}

	@Override
	public void onEvent(ServiceEvent event) {}

	@Override
	public void onEvent(EnvironmentEvent event) {
		if (event instanceof ChangeEnvironmentEvent) {
			updateEnvironmentList((ChangeEnvironmentEvent) event);
		}
	}

	private void updateEnvironmentList(ChangeEnvironmentEvent event) {

		switch (event.getStatus()) {
			case ADDED :

				this.environmentList.add(event.getName());
				break;

			case DELETED :

				this.environmentList.remove(event.getName());
				break;

			case RENAMED :

				for (int i = 0; i < this.environmentList.size(); i++) {
					if (this.environmentList.get(i).equals(event.getName())) {
						this.environmentList.set(i, event.getNewEnvName());
						break;
					}
				}

				break;
		}
	}
}
