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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.configuration.suri.SailfishURI;

/**
 * This class keep settings for AML.
 * @author dmitry.guriev
 *
 */
public class AMLSettings {
	private String baseDir;    // relative (to REPORT folder) path
	private String matrixPath; // relative (to REPORT folder) path to matrix (copy)
	private String origMatrixPath; // relative (to MATRIX folder) path to matrix (original)
	private String srcDir;  // "src"

	private SailfishURI languageURI;
	private String testCasesRange;
	private boolean continueOnFailed;
	private boolean autoStart;
	private boolean autoRun;
	private boolean runNetDumper;
	private boolean skipOptional;
	private List<IValidator> validators;
	private List<IPreprocessor> preprocessors;
	private boolean suppressAskForContinue = false;
	private IOutputStreamFactory outputStreamFactory = new IOutputStreamFactory.DefaultOutputStreamFactory();
	private Map<String, String> staticVariables;

	public AMLSettings() {
		this.validators = new ArrayList<IValidator>();
		this.preprocessors = new ArrayList<IPreprocessor>();
	}

	/**
	 * Set project directory.
	 * relative (to REPORT folder) path
	 * @param baseDir
	 */
	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	/**
	 * Get project directory.
	 * relative (to REPORT folder) path
	 * @return
	 * @throws AMLException
	 */
	public String getBaseDir() throws AMLException {
		if (baseDir == null) {
			throw new AMLException("BaseDir not defined.");
		}
		return baseDir;
	}

	/**
	 * Set path to matrix on the file system.
	 * relative (to REPORT folder) path to matrix (copy)
	 * @param matrixPath
	 */
	public void setMatrixPath(String matrixPath) {
		this.matrixPath = matrixPath;
	}

	/**
	 * Get path to matrix on the file system.
	 * relative (to REPORT folder) path to matrix (copy)
	 * @return
	 * @throws AMLException
	 */
	public String getMatrixPath() throws AMLException {
		if (matrixPath == null) {
			throw new AMLException("MatrixPath not defined.");
		}
		return matrixPath;
	}

	/**
	 * relative (to MATRIX folder) path to matrix (original)
	 */
	public String getOrigMatrixPath() {
		return origMatrixPath;
	}

	/**
	 * relative (to MATRIX folder) path to matrix (original)
	 */
	public void setOrigMatrixPath(String origMatrixPath) {
		this.origMatrixPath = origMatrixPath;
	}

	/**
	 * Set directory for generated sources. Should be the same as in build.xml
	 * @param srcDir
	 */
	public void setSrcDir(String srcDir) {
		this.srcDir = srcDir;
	}

	/**
	 * Get directory for generated sources. Should be the same as in build.xml
	 * @return
	 */
	public String getSrcDir() {
		return this.srcDir;
	}

	/**
	 * Set range of the test cases to be executes.<br>
	 * Format:<br>
	 * 1-3, 5, 7-<br>
	 * This means to execute test cases 1, 2, 3, 5, 7 and all after 7.<br>
	 * Index of the first test case is 1.<br>
	 * If parameter string is empty or null than all test cases will be executed.
	 *
	 * @param range String representation of number test cases to be executed
	 */
	public void setTestCasesRange(String range)
	{
		this.testCasesRange = range;
	}

	/**
	 * Get range of the test cases to be executes.<br>
	 * Format:<br>
	 * 1-3, 5, 7-<br>
	 * This means to execute test cases 1, 2, 3, 5, 7 and all after 7.<br>
	 * Index of the first test case is 1.<br>
	 * If return string is empty or null than all test cases will be executed.
	 *
	 * @return String representation of number test cases to be executed
	 */
	public String getTestCasesRange() {
		return this.testCasesRange;
	}

	public void setContinueOnFailed(boolean b) {
		this.continueOnFailed = b;
	}

	public boolean getContinueOnFailed() {
		return this.continueOnFailed;
	}

	public void setAutoStart(boolean b) {
		this.autoStart = b;
	}

	public boolean getAutoStart() {
		return this.autoStart;
	}

	public void setAutoRun(boolean b) {
		this.autoRun = b;
	}

	public boolean getAutoRun() {
		return this.autoRun;
	}

	/**
	 * This method can contain AUTO
	 * @return Language name
	 */
	public SailfishURI getLanguageURI() {
		return this.languageURI;
	}

	public void setLanguageURI(SailfishURI languageURI) {
		this.languageURI = languageURI;
	}

	public void addValidator(IValidator validator) {
		this.validators.add(validator);
	}

	public List<IValidator> getValidators()
	{
		return this.validators;
	}

	public void addPreprocessor(IPreprocessor preprocessor) {
		this.preprocessors.add(preprocessor);
	}

	public List<IPreprocessor> getPreprocessors() {
		return preprocessors;
	}

	public boolean isSuppressAskForContinue() {
		return suppressAskForContinue ;
	}

	public void setSuppressAskForContinue(boolean  b) {
		this.suppressAskForContinue = b;
	}

	public IOutputStreamFactory getOutputStreamFactory() {
		return outputStreamFactory;
	}

	public void setOutputStreamFactory(IOutputStreamFactory outputStreamFactory) {
		this.outputStreamFactory = outputStreamFactory;
	}

    public Map<String, String> getStaticVariables() {
        return staticVariables;
    }

    public void setStaticVariables(Map<String, String> staticVariables) {
        this.staticVariables = staticVariables;
    }

	public boolean isRunNetDumper() {
		return runNetDumper;
	}

	public void setRunNetDumper(boolean runNetDumper) {
		this.runNetDumper = runNetDumper;
	}

    public boolean isSkipOptional() {
        return skipOptional;
    }

    public void setSkipOptional(boolean skipOptional) {
        this.skipOptional = skipOptional;
    }
}
