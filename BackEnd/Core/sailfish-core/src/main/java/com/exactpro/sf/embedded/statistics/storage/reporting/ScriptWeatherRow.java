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
package com.exactpro.sf.embedded.statistics.storage.reporting;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ScriptWeatherRow implements Serializable {

	public enum Weather {

		Good("good"),
		Normal("normal"),
		Bad("bad");

		private final String value;

		private Weather(final String value) {

			this.value = value;

		}

		public String getValue() {
			return value;
		}

	}

	private String matrixName;

	private long passed;

	private long failed;

	private long conditionallyPassed;

	public ScriptWeatherRow() {

	}

	public ScriptWeatherRow(String matrixName, long passed, long conditionallyPassed, long failed) {
		super();
		this.matrixName = matrixName;
		this.passed = passed;
		this.conditionallyPassed = conditionallyPassed;
		this.failed = failed;
	}

	public String getWeatherString() {

		if(this.failed == 0) {

			return Weather.Good.getValue();

		}

		if(this.passed == 0 && this.conditionallyPassed == 0) {

			return Weather.Bad.getValue();

		}

		return Weather.Normal.getValue();

	}

	public String getMatrixName() {
		return matrixName;
	}

	public void setMatrixName(String matrixName) {
		this.matrixName = matrixName;
	}

	public long getPassed() {
		return passed;
	}

	public void setPassed(long passed) {
		this.passed = passed;
	}

	public long getFailed() {
		return failed;
	}

	public void setFailed(long failed) {
		this.failed = failed;
	}

    public long getConditionallyPassed() {
        return conditionallyPassed;
    }

    public void setConditionallyPassed(long conditionallyPassed) {
        this.conditionallyPassed = conditionallyPassed;
    }

}
