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
    private int loaded;
    private int executedTC;
    private int currentTC;
    private long currentActions;
    private long currentExecutedActions;
    private long totalActions;
    private long totalExecutedActions;

    private long passed;
    private long conditionallyPassed;
    private long failed;



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

        listener.onProgressChanged(testScriptRunId);
	}

	@Override
	public int getExecutedTC() {
		return executedTC;
	}

	public void setExecutedTC(int executed)
	{
		executedTC = executed;

        listener.onProgressChanged(testScriptRunId);
	}

	@Override
	public void incrementExecutedTC()
	{
		executedTC++;

        listener.onProgressChanged(testScriptRunId);
	}

	@Override
	public int getCurrentTC() {
		return currentTC;
	}

	@Override
	public void setCurrentTC(int executed)
	{
		currentTC = executed;

        listener.onProgressChanged(testScriptRunId);
	}

	@Override
	public long getCurrentActions() {
		return currentActions;
	}

	@Override
	public void setCurrentActions(long actions)
	{
		currentActions = actions;

        listener.onProgressChanged(testScriptRunId);
	}

	@Override
	public long getCurrentExecutedActions() {
		return currentExecutedActions;
	}


	public void setCurrentExecutedActions(long executedActions)
	{
		currentExecutedActions = executedActions;

        listener.onProgressChanged(testScriptRunId);
	}

	@Override
	public long getTotalActions() {
		return totalActions;
	}

	@Override
	public void setTotalActions(long totalActions)
	{
		this.totalActions = totalActions;

        listener.onProgressChanged(testScriptRunId);
	}

	@Override
	public long getTotalExecutedActions() {
		return totalExecutedActions;
	}

	public void setTotalExecutedActions(long totalExecutedActions)
	{
		this.totalExecutedActions = totalExecutedActions;

        listener.onProgressChanged(testScriptRunId);
	}

	@Override
	public void incrementActions()
	{
		currentExecutedActions++;
		totalExecutedActions++;

        listener.onProgressChanged(testScriptRunId);
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
        listener.onProgressChanged(testScriptRunId);
	}

	@Override
	public void increasePassed()
	{
		++this.passed;

        listener.onProgressChanged(testScriptRunId);
	}

	@Override
	public long getFailed() {
		return failed;
	}

    @Override
    public void setFailed(long failed) {
        this.failed = failed;
        listener.onProgressChanged(testScriptRunId);
    }

    @Override
	public void increaseFailed() {
		++this.failed;

        listener.onProgressChanged(testScriptRunId);
	}

    @Override
    public long getConditionallyPassed() {
        return conditionallyPassed;
    }

    @Override
    public void setConditionallyPassed(long conditionallyPassed) {
        this.conditionallyPassed = conditionallyPassed;
        listener.onProgressChanged(testScriptRunId);
    }

    @Override
    public void increaseConditionallyPassed() {
        ++this.conditionallyPassed;
        listener.onProgressChanged(testScriptRunId);
    }

}
