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
package com.exactpro.sf.aml;

import com.exactpro.sf.aml.generator.AlertCollector;

/**
 *
 * @author dmitry.guriev
 *
 */
public class AMLException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = -4327272682680648634L;
	private AlertCollector alertCollector = new AlertCollector();

	public AMLException()
	{
		super();
	}

	public AMLException(String message)
	{
		super(message);
	}

	public AMLException(Throwable cause)
	{
		super(cause);
	}

	public AMLException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public AMLException(String message, AlertCollector alertCollector) {
		super(message);
		this.alertCollector = alertCollector;
	}

    public AMLException(String message, Throwable cause, AlertCollector alertCollector) {
        super(message, cause);
        this.alertCollector = alertCollector;
    }

	public AlertCollector getAlertCollector() {
		return this.alertCollector;
	}
}
