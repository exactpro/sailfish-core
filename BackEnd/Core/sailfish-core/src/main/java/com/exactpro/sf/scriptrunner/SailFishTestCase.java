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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.storage.ScriptRun;

public class SailFishTestCase
{
	static final Logger logger = LoggerFactory.getLogger(SailFishTestCase.class);

	private IScriptReport report = null;
	private ScriptRun scriptRun = null;
	protected ScriptContext context = null;

	protected IScriptReport getReport()
	{
		return this.report;
	}

	protected ScriptRun getScriptRun()
	{
		return this.scriptRun;
	}

	protected ScriptContext getContext()
	{
		return this.context;
	}

	public void setReport(IScriptReport report)
	{
		this.report = report;
	}

	public void setScriptRun(ScriptRun scriptRun)
	{
		this.scriptRun = scriptRun;
	}

	public void setScriptContext(ScriptContext context)
	{
		this.context = context;
	}

	/**
	 * This check does not give a 100% guarantee to catch the InterruptedException
	 * @throws InterruptedException
	 */
	protected void checkInterrupted() throws InterruptedException
	{
	    if (Thread.currentThread().isInterrupted()) {
	        throw new InterruptedException();
	    }
	}

    protected static Exception collectExceptions(Exception current, Exception throwsException) {
        if (current == null) {
            return throwsException;
        }
        if (throwsException == null) {
            return current;
        }
        throwsException.addSuppressed(current);
        return throwsException;
    }
}
