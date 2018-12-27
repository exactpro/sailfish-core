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
package com.exactpro.sf.scriptrunner.impl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextColor;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.generator.AggregateAlert;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.scriptrunner.IReportStats;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.LoggerRow;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.OutcomeCollector;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.StatusDescription;

public class BroadcastScriptReport implements IScriptReport {

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

	private final List<IScriptReport> listeners;

	private boolean actionCreated = false;

	private boolean testCaseCreated = false;

	public BroadcastScriptReport(final List<IScriptReport> listeners) {
		this.listeners = new CopyOnWriteArrayList<>(listeners);
	}

	@Override
	public void createReport(ScriptContext scriptContext, String name, String description, long scriptRunId, String environmentName, String userName) {
		for (IScriptReport listener: listeners) {
			try {
			listener.createReport(scriptContext, name, description, scriptRunId, environmentName, userName);
			} catch (Exception e) {
				logger.error("Error while firing create report with {} listener", listener.getClass().getName(), e);
			}
		}
	}

	@Override
	public void addAlerts(Collection<AggregateAlert> alerts) {
		for (IScriptReport listener: listeners) {
			try {
			listener.addAlerts(alerts);
			} catch (Exception e) {
				logger.error("Error while firing add errors with {} listener", listener.getClass().getName(), e);
			}
		}
	}

	@Override
	public void closeReport() {
		for (IScriptReport listener: listeners) {
			try {
			listener.closeReport();
			} catch (Exception e) {
				logger.error("Error while firing close report with {} listener", listener.getClass().getName(), e);
			}
		}
		// Don't store this data in TestScriptDescription > ScriptContext > Report
		// you should save links to listeners's data in your code or extract it in theirs closeReport()
		listeners.clear();
	}

	@Override
	public void flush() {
		for (IScriptReport listener: listeners) {
			try {
			listener.flush();
			} catch (Exception e) {
				logger.error("Error while firing flush report with {} listener", listener.getClass().getName(), e);
			}
		}
	}

	@Override
    public void createTestCase(String reference, String description, int order, int matrixOrder, String tcId, int tcHash, AMLBlockType type) {
		for (IScriptReport listener: listeners) {
			try {
                listener.createTestCase(reference, description, order, matrixOrder, tcId, tcHash, type);
			} catch (Exception e) {
				logger.error("Error while firing create testcase with {} listener", listener.getClass().getName(), e);
			}
		}

        testCaseCreated = true;
	}

	@Override
	public void closeTestCase(StatusDescription status) {
		for (IScriptReport listener: listeners) {
			try {
			listener.closeTestCase(status);
			} catch (Exception e) {
				logger.error("Error while firing close testcase with {} listener", listener.getClass().getName(), e);
			}
		}

        testCaseCreated = false;
	}

    @Override
    public boolean isTestCaseCreated() {
        return testCaseCreated;
    }

    @Override
    public void createAction(String name, String serviceName, String action, String msg, String description, Object inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {
		try {
    		for (IScriptReport listener: listeners) {
    			try {
                    listener.createAction(name, serviceName, action, msg, description, inputParameters, checkPoint, tag, hash, verificationsOrder);
			    } catch (Exception e) {
				    logger.error("Error while firing create action with {} listener", listener.getClass().getName(), e);
			    }
    		}
		} finally {
            this.actionCreated = true;
        }
	}

	@Override
	public boolean isActionCreated() {
		return this.actionCreated;
	}

    @Override
    public void createAction(String name, String serviceName, String action, String msg, String description, List<Object> inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {
		try {
			for (IScriptReport listener: listeners) {
				try {
                    listener.createAction(name, serviceName, action, msg, description, inputParameters, checkPoint, tag, hash, verificationsOrder);
				} catch (Exception e) {
					logger.error("Error while firing create action with {} listener", listener.getClass().getName(), e);
				}
			}
		} finally {
			this.actionCreated = true;
		}
	}

	@Override
    public void closeAction(StatusDescription status, Object actionResult) {
	    try {
    		for (IScriptReport listener: listeners) {
    			try {
                    listener.closeAction(status, actionResult);
			    } catch (Exception e) {
				    logger.error("Error while firing close action with {} listener", listener.getClass().getName(), e);
			    }
    		}
	    } finally {
            this.actionCreated = false;
        }
	}

    @Override
    public void openGroup(String name, String description) {
        for (IScriptReport listener : listeners) {
        	try {
            listener.openGroup(name, description);
	        } catch (Exception e) {
		        logger.error("Error while firing open group with {} listener", listener.getClass().getName(), e);
	        }
        }
    }

    @Override
    public void closeGroup(StatusDescription status) {
        for (IScriptReport listener : listeners) {
        	try {
                listener.closeGroup(status);
	        } catch (Exception e) {
		        logger.error("Error while firing close group with {} listener", listener.getClass().getName(), e);
	        }
        }
    }

	@Override
    public void createVerification(String name, String description, StatusDescription status, ComparisonResult result) {
		for (IScriptReport listener: listeners) {
			try {
              listener.createVerification(name, description, status, result);
			} catch (Exception e) {
				logger.error("Error while firing create verification with {} listener", listener.getClass().getName(), e);
			}
		}
	}

	@Override
	public void createMessage(MessageLevel level, String... messages) {
		for (IScriptReport listener: listeners) {
			try {
			    listener.createMessage(level, messages);
			} catch (Exception e) {
				logger.error("Error while firing create message with {} listener", listener.getClass().getName(), e);
			}
		}
	}

	@Override
	public void createMessage(MessageLevel level, Throwable e, String... messages) {
		for (IScriptReport listener: listeners) {
			try {
			    listener.createMessage(level, e, messages);
			} catch (Exception ee) {
				logger.error("Error while firing create message with {} listener", listener.getClass().getName(), ee);
			}
		}
	}

    @Override
    public void createMessage(TextColor color, TextStyle style, String... messages) {
        for (IScriptReport listener : listeners) {
            try {
                listener.createMessage(color, style,  messages);
            } catch (Exception ee) {
                logger.error("Error while firing create message with {} listener", listener.getClass().getName(), ee);
            }
        }
    }

	@Override
	public void createException(Throwable cause) {
	    for (IScriptReport listener: listeners) {
	    	try {
                listener.createException(cause);
		    } catch (Exception e) {
			    logger.error("Error while firing create exception with {} listener", listener.getClass().getName(), e);
		    }
        }
	}

	@Override
    public void createTable(ReportTable table) {
		for (IScriptReport listener: listeners) {
			try {
                listener.createTable(table);
			} catch (Exception e) {
				logger.error("Error while firing create table with {} listener", listener.getClass().getName(), e);
			}
		}
	}

	@Override
    public void createParametersTable(String messageName, Object message) {
        for(IScriptReport listener : listeners) {
            try {
                listener.createParametersTable(messageName, message);
            } catch(Exception e) {
                logger.error("Error while firing create parameters table with {} listener", listener.getClass().getName(), e);
            }
        }
    }

    @Override
	public void createLogTable(List<String> header, List<LoggerRow> rows) {
		for (IScriptReport listener: listeners) {
			try {
			    listener.createLogTable(header, rows);
			} catch (Exception e) {
				logger.error("Error while firing create log table with {} listener", listener.getClass().getName(), e);
			}
		}
	}

	@Override
	public void setOutcomes(OutcomeCollector outcomes) {
		for (IScriptReport listener: listeners) {
			try {
			    listener.setOutcomes(outcomes);
			} catch (Exception e) {
				logger.error("Error while firing set outcomes with {} listener", listener.getClass().getName(), e);
			}
		}
	}

	@Override
	public void createLinkToReport(String linkToReport) {
		for (IScriptReport listener: listeners) {
			try {
			    listener.createLinkToReport(linkToReport);
			} catch (Exception e) {
				logger.error("Error while firing create link to report with {} listener", listener.getClass().getName(), e);
			}
		}
	}

	@Override
	public IReportStats getReportStats() {
		for (IScriptReport listener: listeners) {
			try {
			    IReportStats stats = listener.getReportStats();
			if (stats != null)
				return stats;
			} catch (Exception e) {
				logger.error("Error while firing get report stats with {} listener", listener.getClass().getName(), e);
			}
		}
		return null;
	}


}
