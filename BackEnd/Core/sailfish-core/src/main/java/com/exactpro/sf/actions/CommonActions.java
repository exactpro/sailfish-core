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
package com.exactpro.sf.actions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.DeflaterInputStream;
import java.util.zip.InflaterOutputStream;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.messages.service.Checkpoint;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.util.DateTimeUtility;

/**
 * This class contains common actions used by SailFish services
 */

@MatrixActions
@ResourceAliases({"CommonActions"})
public class CommonActions extends AbstractCaller {

	private static final Logger logger = LoggerFactory.getLogger(CommonActions.class);

	public static final String WAIT_TILL_TIME_ARG_DATE = "till_date";
    public static final String FIRST_ARG = "firstArg";
    public static final String SECOND_ARG = "secondArg";

	@CommonColumns({
        @CommonColumn(value = Column.Reference, required = true)
    })
	@ActionMethod
	public CheckPoint GetCheckPoint(IActionContext actionContext)
	{
		IService[] clients = actionContext.getServiceManager().getStartedServices();

        // Special message is needed to show checkpoints in 'All
        // Messages' table in HTML Report

        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setName(actionContext.getReference());
        checkpoint.setDescription(actionContext.getDescription());

        IMessage checkpointMsg = checkpoint.getMessage();

        checkpointMsg.getMetaData().setServiceInfo(actionContext.getServiceManager().getServiceInfo(ServiceName.parse(actionContext.getServicesNames().iterator().next())));
        checkpointMsg.getMetaData().setDictionaryURI(ServiceUtil.SERVICE_DICTIONARY_URI);

        actionContext.storeMessage(checkpointMsg);

		CheckPoint checkPoint = new CheckPoint(actionContext.getReference().startsWith("!"), checkpointMsg.getMetaData().getId());
		for (IService client : clients)
		{
			if (client instanceof IInitiatorService)
			{
                IServiceHandler handler = client.getServiceHandler();
                ISession isession = ((IInitiatorService) client).getSession();
                if (isession != null) {
                    handler.registerCheckPoint(isession, ServiceHandlerRoute.FROM_APP, checkPoint);
                    handler.registerCheckPoint(isession, ServiceHandlerRoute.FROM_ADMIN, checkPoint);
				}
				/* TODO: The exception doesn't work properly. To think about it.
				else {
					throw new EPSCommonException("Only [" +
						CollectorServiceHandler.class.getCanonicalName() +
						"] has been supported for today as IServiceHandler implementation.");
				}
				*/
			}
		}

		return checkPoint;
	}

	@CommonColumns({
        @CommonColumn(value = Column.Reference, required = true)
    })
	@ActionMethod
	public CheckPoint GetAdminCheckPoint(IActionContext actionContext)
	{
		return GetCheckPoint(actionContext);

//		IService[] clients = settings.getScriptContext().getEnvironmentManager().getConnectionManager().getStartedServices();
//		CheckPoint checkPoint = new CheckPoint();
//		for (IService client : clients)
//		{
//			if (client instanceof IInitiatorService)
//			{
//				if ( client.getServiceHandler() instanceof CollectorServiceHandler )
//				{
//					CollectorServiceHandler handler = (CollectorServiceHandler) client.getServiceHandler();
//					if (handler == null) {
//						throw new EPSCommonException("CollectorServiceHandler is null for service: "+client.getName()+" of type "+client.getClass().getCanonicalName());
//					}
//					ISession isession = getSession((IInitiatorService)client, client.getName());
//					int cp = handler.getAdminCheckPoint(isession);
//
//					checkPoint.putApp(client.getName(), cp);
//					settings.getScriptContext().getScriptConfig().getLogger()
//					.info("Got admin checkpoint ["+checkPoint+"] for client ["+client.getName()+"].");
//				} else {
//					//TODO: throw exception
//				}
//			}
//		}
//
//		IScriptReport report = settings.getScriptContext().getReport();
//		String id = settings.getId() == null ? "" : settings.getId()+" ";
//		report.createAction(id + "GetAdminCheckPoint", "", null);
//		report.closeAction(new StatusDescription(StatusType.PASSED, "", updateTestCaseStatus));
//
//		return checkPoint;
	}

	@CommonColumns({
        @CommonColumn(value = Column.Timeout, required = true)
    })
	@ActionMethod
	public void Sleep(IActionContext actionContext) throws InterruptedException
	{
		actionContext.getLogger().info("sleep {}", actionContext.getTimeout());
		Thread.sleep(actionContext.getTimeout());
	}

    @CustomColumns({
        @CustomColumn(value = WAIT_TILL_TIME_ARG_DATE, required = true)
    })
    @ActionMethod
	public void WaitTillTime(IActionContext actionContext, HashMap<?, ?> inputData) throws InterruptedException {
        Object o = inputData.get(WAIT_TILL_TIME_ARG_DATE);
        LocalDateTime date;
        if (o instanceof TemporalAccessor) {
            date = DateTimeUtility.toLocalDateTime((TemporalAccessor) o);
        } else if (o instanceof String) {
            String dateFormat = (String) o;
            date = DateUtil.modifyTemporal(DateTimeUtility.nowLocalDateTime(), dateFormat);
        } else {
            throw new IllegalArgumentException("Field " + WAIT_TILL_TIME_ARG_DATE + " have got value not instance of Date or String");
        }
        long milliseconds = DateUtil.getMilliseconds(date);
        actionContext.getLogger().info("WaitTillTime {}", date);
        if (System.currentTimeMillis() >= milliseconds) {
            String errorDescription = date + " has already passed, now " + DateTimeUtility.nowLocalDateTime();
            throw new EPSCommonException(errorDescription);
        } else {
            Thread.sleep(milliseconds - System.currentTimeMillis());
        }
	}

	@CommonColumns({
        @CommonColumn(value = Column.MessageType, required = true),
        @CommonColumn(value = Column.Reference, required = true)
    })
	@ActionMethod
	public <T> T System_DefineMessage(IActionContext actionContext, T message)
	{
		Logger logger = actionContext.getLogger();
        logger.debug("System_DefineMessage id={} description=\"{}\"", actionContext.getId(), actionContext.getDescription());

		return message;
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
	public void CheckLogon(IActionContext actionContext) throws InterruptedException
	{
		ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());
		IInitiatorService client = actionContext.getServiceManager().getService(serviceName);
		ISession isession = getSession(client, serviceName.toString());
		Logger logger = actionContext.getLogger();

		long endTime = System.currentTimeMillis() + actionContext.getTimeout();
		while (endTime > System.currentTimeMillis())
		{
			if (logger.isDebugEnabled()) {
				logger.debug("CheckLogon start while");
			}
			if (isession.isLoggedOn()) {
				logger.debug("Service {} is logged on.", serviceName);
				return;
			}

            Thread.sleep(1);
	    }

		logger.debug("Service {} is not logged on.", serviceName);

		throw new EPSCommonException("Service "+serviceName+" is not logged on.");
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
	public void CleanMessages(IActionContext actionContext)
	{
		ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());
		IInitiatorService client = (IInitiatorService)actionContext.getServiceManager().getService(serviceName);
		client.getServiceHandler().cleanMessages(ServiceHandlerRoute.values());
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
	public void CleanAppMessages(IActionContext actionContext)
	{
		ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());
		IInitiatorService client = (IInitiatorService)actionContext.getServiceManager().getService(serviceName);
		ISession isession = client.getSession();

		if ( isession != null )
		{
            IServiceHandler handler = client.getServiceHandler();
			if (handler == null) {
				throw new EPSCommonException("CollectorServiceHandler is null for service: "+client.getName()+" of type "+client.getClass().getCanonicalName());
			}
			handler.cleanMessages(ServiceHandlerRoute.TO_APP, ServiceHandlerRoute.FROM_APP);
		}
	}

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
	public void CleanAdminMessages(IActionContext actionContext)
	{
		ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());
		IInitiatorService client = (IInitiatorService)actionContext.getServiceManager().getService(serviceName);
		ISession isession = client.getSession();

		if ( isession != null )
		{
            IServiceHandler handler = client.getServiceHandler();
			if (handler == null) {
				throw new EPSCommonException("CollectorServiceHandler is null for service: "+client.getName()+" of type "+client.getClass().getCanonicalName());
			}
			handler.cleanMessages(ServiceHandlerRoute.TO_ADMIN, ServiceHandlerRoute.FROM_ADMIN);
		}
	}

	@CustomColumns({
        @CustomColumn(value = FIRST_ARG, required = true),
        @CustomColumn(value = SECOND_ARG, required = true)
    })
	@ActionMethod
	public void Compare(IActionContext actionContext, HashMap<?, ?> inputData) throws InterruptedException
	{
		Object first = inputData.get(FIRST_ARG);
		Object second = inputData.get(SECOND_ARG);

		if(first == null) {
			throw new IllegalArgumentException("Field " + FIRST_ARG + " have no value");
		}
		if(second == null) {
			throw new IllegalArgumentException("Field " + SECOND_ARG + " have no value");
		}

		if(!first.equals(second)) {
		    throw new EPSCommonException("Comparison failed: " + first + " is not equal " + second);
		}
	}

	private static ISession getSession(IInitiatorService client, String serviceName) {
		ISession session = client.getSession();
		if ( session == null ) {
			throw new EPSCommonException("Session is not opened: "+serviceName);
		}
		return session;
	}

	@Description(
			"Pause script and wait untill user resume script. "+
			"If #timeout parameter is specified wait number of milliseconds and continue execution.")
	@CommonColumns({
        @CommonColumn(value = Column.Description, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public void AskForContinue(IActionContext actionContext) throws InterruptedException
	{
		actionContext.pauseScript(actionContext.getTimeout(), actionContext.getDescription());
	}

	@Description("Use this action to stop SailFish till time you need.<br>" +
        "Specify time in the column TimeSync<br>" +
        "Time can be absolute (UTC) or relative.<br>" +
        "Examples:<br>" +
        "absolutely - 16:56:00<br>" +
        "relatively - 16:56:00+1m (where 1m = 1 minute)<br>" +
        "17:00:00+3h (where 3h = 3 hours)<br>" +
	    "17:13:00+25s (where 25s = 25 seconds)<br>")
	@CustomColumns({
        @CustomColumn(value = "TimeSync", required = true)
    })
	@ActionMethod
	public void TimeSync(IActionContext actionContext, HashMap<?, ?> inputData) throws Exception
	{
        final DateTimeFormatter df = DateTimeUtility.createFormatter("HH:mm:ss");

		final int s_ms = 1000;
		final int m_ms = 60000;
		final int h_ms = 3600000;

		String value = ((String) inputData.get("TimeSync")).trim();
		final StringTokenizer t = new StringTokenizer(value, "+");

		final List<String> tokens = new ArrayList<>();
		while (t.hasMoreTokens()) {
			tokens.add(t.nextToken().trim());
		}

        LocalDateTime p = null;
		try {
            p = DateTimeUtility.toLocalDateTime(LocalTime.parse(tokens.get(0), df));
        } catch (DateTimeParseException pe) {
			throw pe;
		}

        LocalDateTime currentLocalDateTime = DateTimeUtility.nowLocalDateTime();
        p = p.withYear(currentLocalDateTime.getYear());
        p = p.withMonth(currentLocalDateTime.getMonth().getValue());
        p = p.withDayOfMonth(currentLocalDateTime.getDayOfMonth());

        long abs = DateUtil.getMilliseconds(p);

		if (tokens.size() > 1) {
			for (int i = 1; i < tokens.size(); i++) {
				String token = tokens.get(i);
				int mul = Integer.parseInt(token.substring(0, token.length()-1));
				String smul2 = token.substring(token.length()-1, token.length());
				int mul2 = 0;
				if ("h".equals(smul2)) mul2 = h_ms;
				else if ("m".equals(smul2)) mul2 = m_ms;
				else if ("s".equals(smul2)) mul2 = s_ms;
				abs += mul*mul2;
			}
		}

		actionContext.getLogger().info("time sync till {}", p);
        if (System.currentTimeMillis() >= abs) {
            String errorDescription = p + " has already passed, now " + DateTimeUtility.nowLocalDateTime();
            throw new EPSCommonException(errorDescription);
        } else {
            Thread.sleep(abs - System.currentTimeMillis());
        }
    }

	@CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
	@ActionMethod
	public HashMap<?,?> GetSettings(IActionContext actionContext) {

		IService client = ActionUtil.getService(actionContext, IService.class);

		ICommonSettings serviceSettings = client.getSettings();

		try {
			return (HashMap<?, ?>) BeanUtils.describe(serviceSettings);
		} catch(Exception e) {
			logger.error("Could not describe service settings object", e);
			throw new EPSCommonException("Could not describe service settings object", e);
		}
	}

	@CommonColumns({
        @CommonColumn(value = Column.Reference, required = true)
    })
	@ActionMethod
	public String GetEnvironmentName(IActionContext actionContext) {
		return actionContext.getEnvironmentName();
	}

    /**
	 * Runs a script
	 *
	 * @param actionContext - an implementation of {@link IActionContext}
	 * @param inputData - hash map of script parameters - must contain value of column "PathArgs" <br />
	 * Value of "PathArgs" must contain path to script and its arguments, separated by space.<br />
	 * @throws Exception - throws an exception
	 */
	@CustomColumns({
        @CustomColumn(value = "PathArgs", required = true)
    })
	@ActionMethod
	public void RunScript(IActionContext actionContext, HashMap<?, ?> inputData) throws Exception {
        String command = (String) inputData.get("PathArgs");
        command = preparePath(command);
        String[] pathArgs = toPathArgs(command);

        logger.debug("Script run args: {}", (Object)pathArgs);
        execProcess(actionContext.getReport(), pathArgs);
    }

    private void execProcess(IActionReport report, String... cmdarray) throws Exception {
        Process process = Runtime.getRuntime().exec(cmdarray);

        try(BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            StringBuilder stdOutBuilder = new StringBuilder();
            StringBuilder stdErrBuilder = new StringBuilder();

            int exitCode = process.waitFor();
            String line = null;

            while((line = stdOutReader.readLine()) != null) {
                stdOutBuilder.append(line);
                stdOutBuilder.append(System.lineSeparator());
            }

            while((line = stdErrReader.readLine()) != null) {
                stdErrBuilder.append(line);
                stdErrBuilder.append(System.lineSeparator());
            }

            String stdOut = stdOutBuilder.toString();
            String stdErr = stdErrBuilder.toString();

            logger.debug("Script run stdout: {}", stdOut);
            logger.debug("Script run stderr: {}", stdErr);

            StatusType status = exitCode != 0 ? StatusType.FAILED : StatusType.PASSED;
            report.createMessage(status, MessageLevel.INFO, "Script run stdout: " + stdOut);

            if(StringUtils.isNotBlank(stdErr)) {
                report.createMessage(StatusType.FAILED, MessageLevel.ERROR, "Script run stderr: " + stdErr);
            }

            if(exitCode != 0) {
                throw new Exception("Exit code: " + exitCode);
            }
        }
    }

	/**
     * Saves BASE64 content into file
     *
     * @param actionContext
     *            - an implementation of {@link IActionContext}
     * @param inputData
     *            - hash map of script parameters - must contain value of column
     *            "FileAlias" and "Content" <br />
     * @throws Exception
     *             - throws an exception
     */
    @CustomColumns({
        @CustomColumn(value = "FileAlias", required = true),
        @CustomColumn(value = "Content", required = true),
        @CustomColumn(value = "Compress", required = false)
        })
    @ActionMethod
    public void SaveBase64Content(IActionContext actionContext, HashMap<?, ?> inputData) throws Exception {

        String content = (String) inputData.get("Content");

        try (InputStream bais = new ByteArrayInputStream(content.getBytes());
             InputStream b64 = new Base64InputStream(bais, false);) {
            saveContent(actionContext, inputData, b64);
        }
    }

    /**
     * Saves field content into file
     *
     * @param actionContext
     *            - an implementation of {@link IActionContext}
     * @param inputData
     *            - hash map of script parameters - must contain value of column
     *            "FileAlias" and "Content" <br />
     * @throws Exception
     *             - throws an exception
     */
    @CustomColumns({
        @CustomColumn(value = "FileAlias", required = true),
        @CustomColumn(value = "Content", required = true),
        @CustomColumn(value = "Append")
        })
    @ActionMethod
    @Description("Save specified \"Content\" into file mapped by \"FileAlias\". "
            + "If specified \"Append\" param (y, yes) then content will be appended, by default overwritten")
    public void SaveContent(IActionContext actionContext, HashMap<?, ?> inputData) throws Exception {

        String content = (String) inputData.get("Content");

        try (InputStream bais = new ByteArrayInputStream(content.getBytes())) {
            saveContent(actionContext, inputData, bais);
        }
    }

    /**
     * Encode base64 text. Returns BASE64 string.
     *
     * @param actionContext
     *            - an implementation of {@link IActionContext}
     * @param inputData
     *            - hash map of script parameters - must contain value of column
     *            "FileName" and "Content" <br />
     * @throws Exception
     *             - throws an exception
     */
    @Description("Encode file to base64.<br/>"+
            "<b>FileAlias</b> - file to encode.<br/>" +
            "<b>Compress</b> - do compress result string (y / Y / n / N).<br/>")
    @CommonColumns({ @CommonColumn(value = Column.Reference, required = true) })
    @CustomColumns({ @CustomColumn(value = "FileAlias", required = true), @CustomColumn(value = "Compress", required = false)})
    @ActionMethod
    public String LoadBase64Content(IActionContext actionContext, HashMap<?, ?> inputData) throws Exception {

        String fileAlias = (String) inputData.get("FileAlias");
        Boolean compress = BooleanUtils.toBoolean((String) inputData.get("Compress"));
        IDataManager dataManager = actionContext.getDataManager();
        SailfishURI target = SailfishURI.parse(fileAlias);

        if (dataManager.exists(target)) {

            byte[] result;

            try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
                try (OutputStream b64 =  new Base64OutputStream(os, true, -1, new byte[0]);
                        InputStream dm = dataManager.getDataInputStream(target);
                        InputStream is = compress ? new DeflaterInputStream(dm) : dm) {
                    IOUtils.copy(is, b64);
                }
                os.flush();
                result = os.toByteArray();
            }

            return new String(result);
        } else {
            throw new EPSCommonException(String.format("Specified %s file alias does not exists", fileAlias));
        }
    }

    private void saveContent(IActionContext actionContext, HashMap<?, ?> inputData, InputStream source) throws Exception {

        String fileAlias = (String) inputData.get("FileAlias");
        boolean compress = BooleanUtils.toBoolean((String) inputData.get("Compress"));
	    boolean append = BooleanUtils.toBoolean((String) inputData.get("Append"));

        IDataManager dataManager = actionContext.getDataManager();

        SailfishURI target = SailfishURI.parse(fileAlias);

        if (dataManager.exists(target)) {
            try (OutputStream dm = dataManager.getDataOutputStream(target, append); OutputStream os = compress ? new InflaterOutputStream(dm) : dm) {
                IOUtils.copy(source, os);
                actionContext.getReport().createMessage(StatusType.NA, MessageLevel.INFO, String.format("Content has been saved by %s", fileAlias));
            }
        } else {
            throw new EPSCommonException(String.format("Specified %s file alias does not exists", fileAlias));
        }
    }

    /**
     * Prepare path to script running. Inline environment variable, e.g. ~ or
     * $HOME
     *
     * @param path
     *            - script path
     */
    private static String preparePath(String path) {
        if (path.startsWith("~")) {
            path = System.getProperty("user.home") + path.substring(1);
        } else if (path.startsWith("$HOME")) {
            path = System.getProperty("user.home") + path.substring(5);
        } else if (path.startsWith("$")) {
            String var = path.substring(1, path.indexOf(File.separator));
            if (System.getenv().containsKey(var)) {
                path = path.replace(path.substring(0, path.indexOf(File.separator)), System.getenv(var));
            } else {
                throw new RuntimeException("Can't find environment variable " + var);
            }
        }
        return path;
    }

    /**
     * Construct array of commandline parameters
     *
     * @param command - String with path to script and arguments
     */
    private static String[] toPathArgs(String command) {
        Character quote = null;
        List<String> args = new ArrayList<>();
        char[] aca = command.toCharArray();
        int start = 0;
        for (int i = 0; i < aca.length; i++) {
            if (aca[i] == ' ' && quote == null) {
                args.add(new String(aca, start, i - start));
                start = i + 1;
                continue;
            } else if (quote != null && aca[i] == quote){
                String arg = new String(aca, start, i - start + 1);
                arg = arg.replace(quote.toString(), "");
                args.add(arg);
                start = i + 1;
                i++;
                quote = null;
                continue;
            }
            if ((aca[i] == '\'' || aca[i] == '"')&& quote == null) {
                quote = aca[i];
            }
        }
        args.add(new String(aca, start, aca.length - start));
        //remove empty args
        Iterator<String> it = args.iterator();
        while(it.hasNext()){
            if(it.next().trim().isEmpty())
                it.remove();
        }
        String[] result = new String[args.size()];
        for (int i = 0; i < args.size(); i++) {
            result[i] = args.get(i).trim();
        }
        return result;
    }

    /**
     * Runs a script with args on column
     *
     * @param actionContext - an implementation of {@link IActionContext}
     * @param inputData - hash map of script parameters - must contain value of column "Command" <br />
     * Value of "Command" must contain path to script, its arguments must be in column with it name.<br />
     * @throws Exception - throws an exception
     */
    @CustomColumns({
        @CustomColumn(value = "Command", required = true)
    })
    @ActionMethod
    public void RunScriptWithArgs(IActionContext actionContext, HashMap<?, ?> inputData) throws Exception {
        String command = preparePath((String) inputData.get("Command"));
        List<String> pathArgs = new ArrayList<>();

        pathArgs.add(command);

        for(Object arg:inputData.keySet()) {
            if(!((String)arg).equals("Command")) {
                pathArgs.add((String)arg + "=" +inputData.get(arg));
            }
        }

        String[] pathArgArray = new String[pathArgs.size()];

        //List.toArray do not work right
        for(int i = 0; i<pathArgs.size(); i++){
            pathArgArray[i] = pathArgs.get(i);
        }

        logger.debug("Script run args: {}", (Object)pathArgArray);
        execProcess(actionContext.getReport(), pathArgArray);
    }

	/**
	 * Check message tags with mvel expressions
	 *
	 * @param actionContext - an implementation of {@link IActionContext}
	 * @param filterData - hash map of mvel expressions against it is required to check message <br />
	 * @throws Exception - throws an exception
	 */
	@Description(
			"Check message tags with mvel expressions.<br>" +
			"For every field that contains value this action " +
			"will try to evaluate that value and return boolean result.<br>" +
			"Logical and equality operators are following:<br>" +
			"'=='  Equal to<br>" +
			"'!='  Not equal to<br>" +
			"'>'  Greater than<br>" +
			"'>='  Greater than or equal to<br>" +
			"'<'  Less than<br>" +
			"'<='  Less than or equal to<br>" +
			"'&&'  AND<br>" +
			"'||'  OR<br>" +
			"'^'  exclusive OR<br>" +
			"Example: ${Logon1.HeartBtInt} + \" != 10 &&  \" + ${Logon1.HeartBtInt} +  \" < 1000 &&  \" + ${Logon1.HeartBtInt} +  \" > 200\""
	)
    @ActionMethod
	public void CheckMessage(IActionContext actionContext, HashMap<?, ?> filterData) throws Exception {


        ComparisonResult compResult = new ComparisonResult("CheckMessage");

		compResult.setStatus(StatusType.PASSED);

		Throwable problem = null;

		for ( Map.Entry<?, ?> entry : filterData.entrySet() ) {
            ComparisonResult curResult = new ComparisonResult(entry.getKey().toString());

			curResult.setExpected("True");
            curResult.setActual(entry.getValue());

			try {
				Boolean result = (Boolean)MVEL.eval(entry.getValue().toString());

				if ( result )
					curResult.setStatus(StatusType.PASSED);
				else {
					curResult.setStatus(StatusType.FAILED);
					compResult.setStatus(StatusType.FAILED);
				}
			}
			catch ( Throwable th ) {
				curResult.setStatus(StatusType.FAILED);
				compResult.setStatus(StatusType.FAILED);
				problem = th;

				actionContext.getLogger().error("CheckMessage problem", th);
			}

            compResult.addResult(curResult);
		}


		boolean addToReport = actionContext.isAddToReport();

		if (addToReport)
		{
			int countPassed = ComparisonUtil.getResultCount(compResult, StatusType.PASSED);

			countPassed += ComparisonUtil.getResultCount(compResult, StatusType.CONDITIONALLY_PASSED);

			int countFailed = ComparisonUtil.getResultCount(compResult, StatusType.FAILED);

			countFailed += ComparisonUtil.getResultCount(compResult, StatusType.CONDITIONALLY_FAILED);

			int countNA = ComparisonUtil.getResultCount(compResult, StatusType.NA);

			IActionReport report = actionContext.getReport();
            report.createVerification(compResult.getStatus(), "CheckMessage. Failed/Passed/NA: " + countFailed + "/" + countPassed + "/" + countNA,
                    "", "", compResult, problem);

			if ( compResult.getStatus() == StatusType.FAILED) {
				throw new EPSCommonException("Verification failed");
			}
		}
	}

    @Description("Create message with pair column / value. Value will have type String if it is not contain AML constructions.")
    @ActionMethod
    public HashMap<?, ?> SetVariables(IActionContext actionContext, HashMap<?, ?> message) throws Exception {
        if (message.size() > 0) {
            return message;
        }
        return null;
    }
}