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


public class ScriptProgress implements IScriptProgress
{
	private int loaded = 0;
	private int executedTC = 0;
	private int currentTC = 0;
	private long currentActions = 0;
	private long currentExecutedActions = 0;
	private long totalActions = 0;
	private long totalExecutedActions = 0;

	private long passed = 0;
    private long conditionallyPassed = 0;
	private long failed = 0;



	private final IScriptRunProgressListener listener;

	private final long testScriptRunId;

	public ScriptProgress(long testScriptRunId, IScriptRunProgressListener listener)
	{
		this.listener = listener;

		this.testScriptRunId = testScriptRunId;
	}


	@Override
	public int getLoaded() {
		return loaded;
	}

	@Override
    public void setLoaded(int loaded)
	{
		this.loaded = loaded;

		this.listener.onProgressChanged(this.testScriptRunId);
	}

	@Override
	public int getExecutedTC() {
		return executedTC;
	}

	public void setExecutedTC(int executed)
	{
		executedTC = executed;

		this.listener.onProgressChanged(this.testScriptRunId);
	}

	@Override
	public void incrementExecutedTC()
	{
		executedTC++;

		this.listener.onProgressChanged(this.testScriptRunId);
	}

	@Override
	public int getCurrentTC() {
		return currentTC;
	}

	@Override
	public void setCurrentTC(int executed)
	{
		currentTC = executed;

		this.listener.onProgressChanged(this.testScriptRunId);
	}

	@Override
	public long getCurrentActions() {
		return currentActions;
	}

	@Override
	public void setCurrentActions(long actions)
	{
		currentActions = actions;

		this.listener.onProgressChanged(this.testScriptRunId);
	}

	@Override
	public long getCurrentExecutedActions() {
		return currentExecutedActions;
	}


	public void setCurrentExecutedActions(long executedActions)
	{
		currentExecutedActions = executedActions;

		this.listener.onProgressChanged(this.testScriptRunId);
	}

	@Override
	public long getTotalActions() {
		return totalActions;
	}

	@Override
	public void setTotalActions(long totalActions)
	{
		this.totalActions = totalActions;

		this.listener.onProgressChanged(this.testScriptRunId);
	}

	@Override
	public long getTotalExecutedActions() {
		return totalExecutedActions;
	}

	public void setTotalExecutedActions(long totalExecutedActions)
	{
		this.totalExecutedActions = totalExecutedActions;

		this.listener.onProgressChanged(this.testScriptRunId);
	}

	@Override
	public void incrementActions()
	{
		currentExecutedActions++;
		totalExecutedActions++;

		this.listener.onProgressChanged(this.testScriptRunId);
	}

	public interface IScriptRunProgressListener
	{
		/**
		 * 
		 * @param id - TestScriptDescription.id
		 */
		void onProgressChanged(long id);
	}

	@Override
	public long getPassed() {
		return passed;
	}

    @Override
	public void setPassed(long passed) {
		this.passed = passed;
        this.listener.onProgressChanged(this.testScriptRunId);
	}

	@Override
	public void increasePassed()
	{
		++this.passed;

		this.listener.onProgressChanged(this.testScriptRunId);
	}

	@Override
	public long getFailed() {
		return failed;
	}

    @Override
    public void setFailed(long failed) {
        this.failed = failed;
        this.listener.onProgressChanged(this.testScriptRunId);
    }

    @Override
	public void increaseFailed() {
		++this.failed;

		this.listener.onProgressChanged(this.testScriptRunId);
	}

    @Override
    public long getConditionallyPassed() {
        return conditionallyPassed;
    }

    @Override
    public void setConditionallyPassed(long conditionallyPassed) {
        this.conditionallyPassed = conditionallyPassed;
        this.listener.onProgressChanged(this.testScriptRunId);
    }

    @Override
    public void increaseConditionallyPassed() {
        ++this.conditionallyPassed;
        this.listener.onProgressChanged(this.testScriptRunId);
    }

}
