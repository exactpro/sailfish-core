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
package com.exactpro.sf.scriptrunner.junit40;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.AddToReport;
import com.exactpro.sf.aml.AfterMatrix;
import com.exactpro.sf.aml.BeforeMatrix;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.ExecutionSequence;
import com.exactpro.sf.aml.Hash;
import com.exactpro.sf.aml.Id;
import com.exactpro.sf.aml.Reference;
import com.exactpro.sf.aml.Type;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.IScriptConfig;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.SailFishTestCase;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.ScriptRunException;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.storage.ScriptRun;

public class SFJUnitRunner
{
	private static Logger logger = LoggerFactory.getLogger(SFJUnitRunner.class);

	public void run(Class<? extends SailFishTestCase> testcaseClass, ScriptContext context) throws ScriptRunException
	{
		List<TestCaseDescription> testcaseDescriptions = new ArrayList<>();

		List<Method> methodsCalledBeforeScript = new ArrayList<>();
		List<Method> methodsCalledAfterScript = new ArrayList<>();

		List<Method> methodsCalledBeforeTestCase = new ArrayList<>();
		List<Method> methodsCalledAfterTestCase = new ArrayList<>();

		for (Method method : testcaseClass.getMethods()) {

			Annotation[] annotations = method.getAnnotations();

            AMLBlockType type = null;

            String reference = null;

            boolean addToReport = true;

			String description = null;

			String id = null;

            int hash = 0;

			int seqNum = 0;

			int matrixOrder = 0;

			for ( Annotation annotation : annotations )
			{
                Class<? extends Annotation> annotationType = annotation.annotationType();

                if(annotationType == Description.class)
				{
					description = ((Description)annotation).value();
				}
                else if(annotationType == Id.class)
				{
					id = ((Id)annotation).value();
				}
                else if(annotationType == Hash.class) {
                    hash = ((Hash)annotation).value();
                }
                else if(annotationType == ExecutionSequence.class)
				{
					ExecutionSequence execSeq = (ExecutionSequence)annotation;

					seqNum = execSeq.order();

					matrixOrder = execSeq.matrixOrder();
				}
                else if(annotationType == BeforeMatrix.class)
				{
					methodsCalledBeforeScript.add(method);
				}
                else if(annotationType == AfterMatrix.class)
				{
					methodsCalledAfterScript.add(method);
                } else if(annotationType == AddToReport.class) {
                    addToReport = ((AddToReport)annotation).value();
                } else if(annotationType == Type.class) {
                    type = ((Type)annotation).value();
                } else if(annotationType == Reference.class) {
                    reference = StringUtils.stripToNull(((Reference)annotation).value());
                }
			}

            if(type == AMLBlockType.BeforeTCBlock) {
                methodsCalledBeforeTestCase.add(method);
            } else if(type == AMLBlockType.AfterTCBlock) {
                methodsCalledAfterTestCase.add(method);
            }

            if(type == AMLBlockType.TestCase || type == AMLBlockType.FirstBlock || type == AMLBlockType.LastBlock) {
                testcaseDescriptions.add(new TestCaseDescription(reference, seqNum, matrixOrder, description, method, id, hash, type, addToReport));
			}
		}

		Collections.sort(testcaseDescriptions, new TestExecutionComparator());

        context.getScriptProgress().setLoaded((int)testcaseDescriptions.stream().filter(TestCaseDescription::isAddToReport).count());
		context.getScriptProgress().setCurrentActions(0);
		context.getScriptProgress().setTotalActions(0);

		try {
			// Execute code before script
			// create ScriptRun,... Init report
			executeBeforeScript(context);

			for (Method method : methodsCalledBeforeScript) {
				method.invoke(null);
			}

			for (TestCaseDescription testcaseDescription : testcaseDescriptions) {
				SailFishTestCase sfTestCase = null;

				boolean failed = false;
                Throwable exception = null;
				try
				{
					// Create Report's TestCase before @Before actions:
					// that actions should be inside 'TestCase' section
					onTestCaseStart(context, testcaseDescription);

					try
					{
						sfTestCase = testcaseClass.newInstance();
						sfTestCase.setReport(context.getReport());
						sfTestCase.setScriptContext(context);
						sfTestCase.setScriptRun(context.getScriptRun());

						executeBeforeTestCase(context, testcaseDescription);

                        if(testcaseDescription.getType() == AMLBlockType.TestCase) {
                            //execute code before testcase
                            for(Method method : methodsCalledBeforeTestCase) {
                                method.invoke(sfTestCase);
                            }
						}
					} catch (Throwable e) {
						logger.error(e.getMessage(), e);
						throw new ScriptRunException("Problem during testcase initialization", e);
					}

					// Execute TestCase:
                    testcaseDescription.getMethod().invoke(sfTestCase);
				}
                catch (InvocationTargetException e) {
                    exception = (e.getCause() != null) ? e.getCause() : e;
                    failed = true;
                } catch (Throwable e) {
                    exception = e;
                    failed = true;
                }
				finally
				{
                    if(testcaseDescription.getType() == AMLBlockType.TestCase) {
                        // execute code after testcase
                        for(Method method : methodsCalledAfterTestCase) {
                            try {
                                method.invoke(sfTestCase);
                            } catch(Throwable e) {
                                logger.error(e.getMessage(), e);
                                exception = e;
                                failed = true;
                            }
                        }
                    }
				}

                boolean conditionallyPassed = context.isConditionallyPassed();

				StatusDescription statusDescription;
                if (failed) {
                    statusDescription = new StatusDescription(StatusType.FAILED, exception.getMessage(), exception, context.getKnownBugs());
                    onTestCase(context, statusDescription);
                    context.getScriptProgress().increaseFailed();
                } else if (conditionallyPassed) {
                    statusDescription = new StatusDescription(StatusType.CONDITIONALLY_PASSED, "", context.getKnownBugs());
                    onTestCase(context, statusDescription);
                    context.getScriptProgress().increaseConditionallyPassed();
                } else {
                    statusDescription = new StatusDescription(StatusType.PASSED, "");
                    onTestCase(context, statusDescription);

                    if(testcaseDescription.isAddToReport()) {
                        context.getScriptProgress().increasePassed();
                    }
				}
			}

		}
		catch (Throwable e) {
			logger.error(e.getMessage(), e);

			if (e instanceof InvocationTargetException) {
				e = (e.getCause() != null) ? e.getCause() : e;
			}

			IScriptReport report = context.getReport(); // we believe, that executeBeforeScript was called
			report.createException(e);

			throw new ScriptRunException("Problem during testscript execution", e);
		} finally {
		    Throwable exception = null;
				// Execute code after testscript
				for (Method method : methodsCalledAfterScript) {
                    try {
                        method.invoke(null);
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                        exception = e;
                    }
				}

			try {
				executeAfterScript(context);
			} catch (RuntimeException e) {
				logger.error(e.getMessage(), e);
				exception = e;
			}

            if (exception != null) {
                throw new ScriptRunException("Problem during testscript finalization", exception);
            }
		}
	}


	private static final class TestCaseDescription
	{
        private final String reference;

		private final int seqnum;

		private final int matrixOrder;

		private final String description;

        private final Method method;

		private final String id;

		private final int hash;

        private final AMLBlockType type;

        private final boolean addToReport;

        public TestCaseDescription(String reference, int seqnum, int matrixOrder, String description, Method method, String id, int hash, AMLBlockType type, boolean addToReport)
		{
            this.reference = reference;

			this.seqnum = seqnum;

			this.matrixOrder = matrixOrder;

			this.description = description;

			this.method = method;

			this.id = id;

			this.hash = hash;

            this.type = type;

            this.addToReport = addToReport;
		}

        public boolean hasReference() {
            return method.getAnnotation(Reference.class) != null;
        }

        public String getReference() {
            return reference;
        }

		public int getSeqnum() {
			return seqnum;
		}


        public int getMatrixOrder() {
            return matrixOrder;
        }


		public String getDescription() {
			return description;
		}


		public Method getMethod() {
			return method;
		}


		public String getId() {
			return id;
		}

        public int getHash() {
            return hash;
        }

        public AMLBlockType getType() {
            return type;
        }

		public boolean isAddToReport() {
            return addToReport;
        }


        @Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("TestCaseDescription [seqnum=").append(seqnum);
			builder.append(", matrixOrder=").append(matrixOrder);
			builder.append(", description=").append(description);
			builder.append(", method=").append(method);
            builder.append(", type=").append(type);
			builder.append(", id=").append(id).append("]");
			return builder.toString();
		}

	}

	private static class TestExecutionComparator implements Comparator<TestCaseDescription>
	{
		@Override
        public int compare(TestCaseDescription o1, TestCaseDescription o2)
		{
			if ( o1.getSeqnum() < o2.getSeqnum() )
				return -1;
			else if ( o1.getSeqnum() > o2.getSeqnum() )
				return 1;
			return 0;
		}
	}

	private void executeBeforeScript(ScriptContext scriptContext)
	{
		IEnvironmentManager environmentManager = scriptContext.getEnvironmentManager();
		IScriptConfig scriptConfig = scriptContext.getScriptConfig();
		String scriptName = scriptConfig.getName();
		String description = scriptConfig.getDescription();

		scriptContext.setInterrupt(false);

		scriptConfig.getLogger().info("TestScript started");

		ScriptRun scriptRun = environmentManager.getMessageStorage().openScriptRun("TestScript", description);

		scriptContext.setScriptRun(scriptRun);

		// Init Report
		if (description == null || description.equals(""))
			description = IScriptReport.NO_DESCRIPTION;
		IScriptReport report = scriptContext.getReport();
		report.createReport(scriptContext, scriptName, description, scriptRun.getId().longValue(), scriptContext.getEnvironmentName(), scriptContext.getUserName());
	}




	private void executeAfterScript(ScriptContext scriptContext) {
		String status = "failed";

		IEnvironmentManager environmentManager = scriptContext.getEnvironmentManager();

		try {

			environmentManager.getMessageStorage().closeScriptRun(scriptContext.getScriptRun());

			status = "success";

		} catch (Throwable e) {
		    scriptContext.getScriptConfig().getLogger().error("Exception during script closing", e);
		} finally {
		    scriptContext.getScriptConfig().getLogger().info("shutdown {}", status);
		}
	}


	private void executeBeforeTestCase(ScriptContext context, TestCaseDescription testcaseDescription) throws Throwable {
	    logger.debug("executeBeforeTestCase start");
		cleanupServices(context.getEnvironmentManager().getConnectionManager(), context.getServiceList());
        context.reset();
		logger.debug("executeBeforeTestCase end");
	}

	private void onTestCaseStart(ScriptContext context, TestCaseDescription testcaseDescription) throws Throwable {
        if(!testcaseDescription.isAddToReport()) {
            return;
        }

		IScriptReport listener = context.getReport();
		listener.createTestCase(
                testcaseDescription.hasReference() ? testcaseDescription.getReference() : testcaseDescription.getMethod().getName(),
				testcaseDescription.getDescription(),
				testcaseDescription.getSeqnum(),
				testcaseDescription.getMatrixOrder(),
				testcaseDescription.getId(),
                testcaseDescription.getHash(),
                testcaseDescription.getType());
	}

    private void onTestCase(ScriptContext context, StatusDescription statusDescription) {
        logger.debug("onTestCase {} start", statusDescription.getStatus());

        IScriptReport listener = context.getReport();
        listener.setOutcomes(context.getOutcomeCollector());

        if(listener.isTestCaseCreated()) {
            listener.closeTestCase(statusDescription);
        }

        cleanupServices(context.getEnvironmentManager().getConnectionManager(), context.getServiceList());

        logger.debug("onTestCase {} end", statusDescription.getStatus());
    }

	private void cleanupServices(IConnectionManager connectionManager, List<String> services){
	    logger.debug("cleaning up services: {}", services);
		connectionManager.cleanup(services);
		logger.debug("cleanup completed");
	}

}
