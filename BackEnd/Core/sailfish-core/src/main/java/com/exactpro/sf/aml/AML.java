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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.generator.GeneratedScript;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixReader;
import com.exactpro.sf.aml.iomatrix.SimpleCell;
import com.exactpro.sf.aml.reader.AMLReader;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.exactpro.sf.aml.writer.AMLWriter;
import com.exactpro.sf.common.adapting.IAdapterManager;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.IProgressListener;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.ScriptRunException;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.languagemanager.LanguageManager;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.google.common.collect.ListMultimap;

/**
 * Implementation for Automation Matrix Language.
 *
 * @author dmitry.guriev
 *
 */
public class AML {

    /**
     * 1) several user sessions
     * 2) Include blocks
     * 3) references to previous actions
     * 4) java formulas with references
     */

    private static Logger logger = LoggerFactory.getLogger(AML.class);

    private final static String EOL = System.getProperty("line.separator");
    public final static String INCLUDE_BLOCK_OLD = "Include Block";
    public final static String INCLUDE_BLOCK = INCLUDE_BLOCK_OLD.replace(" ", "");
    public final static SailfishURI INCLUDE_BLOCK_URI;

    public final static String PACKAGE_NAME = "com.exactpro.sf.testscript.matrix";
    public final static String CLASS_NAME = "Matrix";
    public final static String PACKAGE_PATH = AML.PACKAGE_NAME.replace(".", File.separator) + File.separator;

    private final static long M10 = 10*1024*1024L;

    static {
        try {
            INCLUDE_BLOCK_URI = new SailfishURI(null, null, INCLUDE_BLOCK);
        } catch(SailfishURIException e) {
            throw new EPSCommonException(e);
        }
    }

    private final AMLSettings amlSettings;
    private AlertCollector alertCollector;

    private List<AMLTestCase> testCases;
    private AMLMatrix matrix;

    private List<IProgressListener> progressListeners;

    private final IWorkspaceDispatcher workspaceDispatcher;
    private final IAdapterManager adapterManager;
    private final IEnvironmentManager environmentManager;
    private final IDictionaryManager dictionaryManager;
    private final IStaticServiceManager staticServiceManager;
    private final LanguageManager languageManager;
    private final IActionManager actionManager;
    private final IUtilityManager utilityManager;

    private final String compilerClassPath;

	public AML(AMLSettings settings, IWorkspaceDispatcher workspaceDispatcher, IAdapterManager adapterManager,
			IEnvironmentManager environmentManager, IDictionaryManager dictionaryManager,
			IStaticServiceManager staticServiceManager, LanguageManager languageManager, IActionManager actionManager,
			IUtilityManager utilityManager, String compilerClassPath) throws AMLException {
	    if(ToolProvider.getSystemJavaCompiler() == null) {
            throw new AMLException("No Java compiler found. You're probably using JRE instead of JDK");
        }
        this.alertCollector = new AlertCollector();
        this.amlSettings = settings;
        this.workspaceDispatcher = workspaceDispatcher;
        this.adapterManager = adapterManager;
        this.dictionaryManager = dictionaryManager;
        this.staticServiceManager = staticServiceManager;
        this.environmentManager = environmentManager;
        this.languageManager = languageManager;
        this.actionManager = actionManager;
        this.utilityManager = utilityManager;
        this.compilerClassPath = compilerClassPath;
        this.progressListeners = new LinkedList<>();
    }

    public GeneratedScript run(ScriptContext scriptContext, String fileEncoding) throws AMLException, IOException, InterruptedException
    {
        try(AdvancedMatrixReader reader = initReader(this.amlSettings.getMatrixPath(), fileEncoding)) {
            if(this.alertCollector.getCount(AlertType.ERROR) != 0) {
            	this.matrix = new AMLMatrix(new ArrayList<SimpleCell>(), new ArrayList<AMLBlock>());
                throw new AMLException("Errors detected on reader initialization", this.alertCollector);
            }
            return run(scriptContext, AMLReader.read(reader, this.amlSettings.isSkipOptional()));
        } catch(AMLException e) {
            throw e;
        } catch(Exception e) {
            throw new AMLException(e.getMessage(), e, this.alertCollector);
        }
        finally {
            this.progressListeners.clear();
        }
    }

    public GeneratedScript run(ScriptContext scriptContext, AMLMatrix matrix) throws AMLException, IOException, InterruptedException
    {
        this.matrix = matrix;

        AMLBlockUtility.resolveActionURIs(matrix.getBlocks(), actionManager, environmentManager.getConnectionManager(), scriptContext.getEnvironmentName());
        SailfishURI languageURI = AMLBlockUtility.detectLanguage(matrix.getBlocks(), languageManager.getLanguageURIs(), amlSettings.getLanguageURI(), actionManager);

        onDetermineLanguage(languageURI);
        amlSettings.setLanguageURI(languageURI);

        ListMultimap<AMLBlockType, AMLTestCase> blocks = AMLConverter.convert(matrix, amlSettings, actionManager);
        blocks = AMLBlockProcessor.process(blocks, amlSettings, actionManager);
        this.testCases = blocks.get(AMLBlockType.TestCase);

        onProgressChanged(30);

        ICodeGenerator codeGen = languageManager.getLanguageFactory(languageURI).getGenerator();

        try {
            codeGen.init(workspaceDispatcher,
                         adapterManager,
                         environmentManager,
                         dictionaryManager,
                         staticServiceManager,
                         actionManager,
                         utilityManager,
                         scriptContext,
                         amlSettings,
                         progressListeners,
                         compilerClassPath);

            for (IPreprocessor preprocessor : this.amlSettings.getPreprocessors()) {
                if (!preprocessor.preprocess(scriptContext.getEnvironmentName(), this, matrix, this.alertCollector))
                    throw new AMLException("Preprocessor " + preprocessor.getName() + " detect errors", this.alertCollector);
            }

            GeneratedScript script = null;

            try {
                script = codeGen.generateCode(blocks.get(AMLBlockType.TestCase), blocks.get(AMLBlockType.BeforeTCBlock), blocks.get(AMLBlockType.AfterTCBlock));
            } finally {
                this.alertCollector.add(codeGen.getAlertCollector());
            }

            for (IValidator validator : this.amlSettings.getValidators()) {
                if (!validator.validate(matrix, actionManager, languageURI, this.alertCollector))
                    throw new AMLException("Validator " + validator.getName() + " detect errors", this.alertCollector);
            }

            return script;
        } finally {
            if (codeGen != null)
                codeGen.cleanup();
        }
    }

    public AlertCollector getAlertCollector() {
        return this.alertCollector;
    }

    protected AdvancedMatrixReader initReader(String matrixFile, String fileEncoding) throws IOException {
        File file = null;
        try {
            file = workspaceDispatcher.getFile(FolderType.REPORT, matrixFile);
        } catch (FileNotFoundException ex) {
            this.alertCollector.add(new Alert("Input CSV/XLS file not found: "+matrixFile));
            return null;
        }

        AdvancedMatrixReader reader = null;
        try {
            reader = new AdvancedMatrixReader(file, fileEncoding);
        } catch (AMLException e) {
            this.alertCollector.add(new Alert(e.getMessage()));
        }

        if (reader != null && !reader.hasNext()) {
            this.alertCollector.add(new Alert("Input CSV/XLS/XLSX file is empty: "+matrixFile));
        }

        return reader;
    }

    public static void compileScript(GeneratedScript script, File binFolderPath, TestScriptDescription description, String compilerClassPath) throws InterruptedException
    {
        Thread.sleep(0); // let interrupt script compilation
        if (description != null) {
            logger.debug("compileScript: {}", description);
        }

        List<String> option = new LinkedList<>();
        option.add("-g");
        option.add("-classpath");

        String cp = compilerClassPath + File.pathSeparator + binFolderPath.getAbsolutePath();

        logger.debug("Classpath: {}", cp);

        option.add(cp);
        option.add("-d");
        option.add(binFolderPath.getAbsolutePath());

        logger.debug("Compiling java files: {}", script.getFilesList());
        int filesSize = 1 + script.getFilesList().size();
        int filesCount = 0;
        List<File> javaFiles = new LinkedList<>();
        long fLength = 0;
        for (File file : script.getFilesList() )
        {
            fLength += file.length();
            javaFiles.add(file);
            filesCount++;

            if (fLength > M10) {
                doCompile(option, javaFiles);
                if (description != null) {
                    description.setProgress(70+30*filesCount/filesSize);
                }
                javaFiles.clear();
                fLength = file.length();
            }
            logger.trace(file.getAbsolutePath());
        }

        logger.debug(script.getMainFile().getAbsolutePath());
        javaFiles.add(script.getMainFile());

        doCompile(option, javaFiles);
        if (description != null) {
            description.setProgress(100);
        }
    }

    private static void doCompile(List<String> option, List<File> javaFiles) throws InterruptedException
    {
        logger.debug("doCompile");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        for (int i = 0; i < javaFiles.size(); i++) {
            if (!javaFiles.get(i).exists()) {
                throw new ScriptRunException("Could not find file: '" + javaFiles.get(i) + "'");
            }
        }

        Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(
                javaFiles.toArray(new File[javaFiles.size()])
        );

        Thread.sleep(0); // let interrupt script compilation
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.flush();
        boolean isCompiled = compiler.getTask(printWriter, fileManager,null, option, null, units).call();

        if (!isCompiled)
            throw new ScriptRunException("Could not compile sources: " + EOL + writer.toString());
    }

    public void cleanup()
    {
        if (this.testCases != null) {
            this.testCases.clear();
            this.testCases = null;
        }
    }

    public void writeMatrix(AMLMatrix matrix) {
        writeMatrix(null, matrix);
    }

    public void writeMatrix(String namePrefix, AMLMatrix matrix) {
		try {
            String matrixPath = amlSettings.getOrigMatrixPath();
            File matrixFile = null;

            if(namePrefix == null) {
                matrixFile = workspaceDispatcher.createFile(FolderType.MATRIX, true, matrixPath);
            } else {
                matrixFile = workspaceDispatcher.createFile(FolderType.MATRIX,
                                                            true,
                                                            FilenameUtils.getPath(matrixPath),
                                                            namePrefix +
                                                            new SimpleDateFormat("_yyyy-MM-dd_HH-mm-ss_").format(new Date()) +
                                                            FilenameUtils.getName(matrixPath));
            }

            AMLWriter.write(matrixFile, matrix);
		} catch (Exception e) {
            logger.error(e.getMessage(), e);
		}
    }

    public void addProgressListener(IProgressListener iProgressListener) {
        this.progressListeners.add(iProgressListener);
    }

    public void removeProgressListener(IProgressListener iProgressListener) {
        this.progressListeners.remove(iProgressListener);
    }

    private void onProgressChanged(int i) {
        for (IProgressListener listener : this.progressListeners) {
            listener.onProgressChanged(i);
        }
    }

    private void onDetermineLanguage(SailfishURI languageURI) {
        for (IProgressListener listener : this.progressListeners) {
            listener.onDetermineLanguage(languageURI);
        }
    }

    /**
     * Used in test purposes only.
     * @return
     */
    protected List<AMLTestCase> getTestCases() {
        return this.testCases;
    }

    /*
     * used in MatrixEditor
     */
    public AMLMatrix getFullMatrix() {
        return matrix;
    }

}
