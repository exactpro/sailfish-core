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
package com.exactpro.sf.scriptrunner;

public interface IScriptProgress {

	long getCurrentActions();

	long getCurrentExecutedActions();

	int getCurrentTC();

	int getExecutedTC();

	long getFailed();

    void setFailed(long failed);

	int getLoaded();

    long getConditionallyPassed();

	long getPassed();

    void setConditionallyPassed(long conditionallyPassed);

	void setPassed(long passed);

	long getTotalActions();

	long getTotalExecutedActions();

	void increaseFailed();

	void increasePassed();

    void increaseConditionallyPassed();

	void incrementActions();

	void incrementExecutedTC();

	void setCurrentActions(long i);

	void setCurrentTC(int executed);

	void setLoaded(int size);

	void setTotalActions(long i);

}
