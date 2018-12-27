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

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.util.AbstractTest;

public class TestScriptRunner extends AbstractTest
{
	private static final String WORK_FOLDER = "build/temp";
	private static final SailfishURI LANGUAGE = SailfishURI.unsafeParse("AML_v2");

	private AbstractScriptRunner runner;

	@Before
	public void setUp() throws Exception
	{
		File workFolder = new File(WORK_FOLDER);

		if ( !workFolder.exists() )
			workFolder.mkdir();

		runner = new SyncScriptRunner(
				SFLocalContext.getDefault().getWorkspaceDispatcher(),
				SFLocalContext.getDefault().getDictionaryManager(),
				SFLocalContext.getDefault().getActionManager(),
				SFLocalContext.getDefault().getUtilityManager(),
				SFLocalContext.getDefault().getLanguageManager(),
				new PreprocessorLoader(SFLocalContext.getDefault().getDataManager()),
				new ValidatorLoader(),
				new ScriptRunnerSettings(),
				SFLocalContext.getDefault().getStatisticsService(),
				SFLocalContext.getDefault().getEnvironmentManager(),
				SFLocalContext.getDefault().getTestScriptStorage(),
				SFLocalContext.getDefault().getAdapterManager(),
				SFLocalContext.getDefault().getStaticServiceManager(),
				SFLocalContext.getDefault().getCompilerClassPath());

	}

	@Test
	public void testScriptRunner() throws InterruptedException
	{
		String scriptSettingsPath = "scriptrunner" + File.separator + "script.xml";
		String scriptMatrixPath = "matrixes" + File.separator + "test.csv";

		long id = runner.enqueueScript(scriptSettingsPath, scriptMatrixPath, "Desc", "matrix", "", false, false, true, false, false, false, LANGUAGE, "UTF-8", "default", System.getProperty("user.name"), null, null, null, null, SFLocalContext.getDefault());

		if ( id == -1 )
			throw new RuntimeException("Script was not added to the queue");
	}

    @Test
	public void test3TestScripts() throws InterruptedException
	{
		String scriptSettingsPath = "scriptrunner" + File.separator + "script.xml";
		String scriptMatrixPath = "matrixes" + File.separator + "test.csv";

        long id1 = runner.enqueueScript(scriptSettingsPath, scriptMatrixPath, "Desc1", "Matrix1", "", false, false, true, false, false, false, LANGUAGE, "UTF-8", "default", System.getProperty("user.name"), null, null, null, null, SFLocalContext.getDefault());
		if ( id1 == -1 )
			Assert.assertEquals(0, id1);

		long id2 = runner.enqueueScript(scriptSettingsPath, scriptMatrixPath, "Desc2", "Matrix2", "", false, false, true, false, false, false, LANGUAGE, "UTF-8", "default", System.getProperty("user.name"), null, null, null, null, SFLocalContext.getDefault());
		if ( id2 == -1 )
			Assert.assertEquals(0, id2);

		long id3 = runner.enqueueScript(scriptSettingsPath, scriptMatrixPath, "Desc3", "Matrix3", "", false, false, true, false, false, false, LANGUAGE, "UTF-8", "default", System.getProperty("user.name"), null, null, null, null, SFLocalContext.getDefault());
		if ( id3 == -1 )
			Assert.assertEquals(0, id3);


		boolean finished = false;

		TestScriptDescription descr1 = null;
		TestScriptDescription descr2 = null;
		TestScriptDescription descr3 = null;

		while ( !finished )
		{
			descr1 = runner.getTestScriptDescription(id1);

			descr2 = runner.getTestScriptDescription(id2);

			descr3 = runner.getTestScriptDescription(id3);

            if (!descr1.isLocked() && !descr2.isLocked() && !descr3.isLocked()) {
                finished = true;
            }

			Thread.sleep(100);

		}

		Assert.assertEquals(TestScriptDescription.ScriptState.FINISHED, descr1.getState());

		Assert.assertEquals(TestScriptDescription.ScriptStatus.INIT_FAILED, descr1.getStatus()); //TODO: Check possibility add custom actions for this test

		Assert.assertEquals(TestScriptDescription.ScriptState.FINISHED, descr2.getState());

		Assert.assertEquals(TestScriptDescription.ScriptStatus.INIT_FAILED, descr2.getStatus());

		Assert.assertEquals(TestScriptDescription.ScriptState.FINISHED, descr3.getState());

		Assert.assertEquals(TestScriptDescription.ScriptStatus.INIT_FAILED, descr3.getStatus());
	}

	@After
	public void tearDown() throws Exception {
		if ( runner != null )
			runner.dispose();
	}
}
