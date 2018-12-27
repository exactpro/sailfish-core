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
package com.exactpro.sf.testwebgui.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Date;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.apache.catalina.Container;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.mvel2.math.MathProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.SFContextSettings;
import com.exactpro.sf.center.SFException;
import com.exactpro.sf.center.impl.CoreVersion;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceDispatcherBuilder;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.embedded.machinelearning.MachineLearningService;
import com.exactpro.sf.embedded.mail.EMailService;
import com.exactpro.sf.embedded.mail.configuration.EMailServiceSettings;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.configuration.StatisticsServiceSettings;
import com.exactpro.sf.scriptrunner.IEnvironmentListener;
import com.exactpro.sf.scriptrunner.IScriptRunListener;
import com.exactpro.sf.storage.IAuthStorage;
import com.exactpro.sf.storage.IMapableSettings;
import com.exactpro.sf.storage.IMappableSettingsSerializer;
import com.exactpro.sf.storage.MatrixUpdateListener;
import com.exactpro.sf.storage.auth.PasswordHasher;
import com.exactpro.sf.storage.auth.User;
import com.exactpro.sf.storage.util.PropertiesSettingsReaderSerializer;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.SFWebApplication;
import com.exactpro.sf.testwebgui.environment.EnumSetContainer;
import com.exactpro.sf.testwebgui.environment.EnvironmentTrackingBean;
import com.exactpro.sf.testwebgui.help.HelpContentHolder;
import com.exactpro.sf.testwebgui.notifications.events.LogSubscriber;
import com.exactpro.sf.testwebgui.notifications.events.WebLoggingAppender;
import com.exactpro.sf.testwebgui.scriptruns.MatrixHolder;

public class SFContextServlet implements Servlet {
	private static final Logger logger = LoggerFactory.getLogger(SFContextServlet.class);

	private static final String WORKSPACE_PATH = "workspace";

	private static final String CONFIG_FILE_NAME = "sf.cfg.xml";

    private volatile SFLocalContext sfLocalContext;

	private ServletConfig config;

	private String buildClassPathString() {

		String path = config.getServletContext().getRealPath("/") + "/WEB-INF/lib";
		File libsFolder = new File(path);

		if (!libsFolder.exists()) {
			logger.error("Could not set classpath. {} does not exists", path);
			return null;
		}

		File[] files = libsFolder.listFiles();

		String separator = System.getProperty("path.separator");
		StringBuilder cp = new StringBuilder();

		for (int i = 0; i < files.length; i++) {
			cp.append(files[i].getAbsolutePath());
			if (i != files.length - 1)
				cp.append(separator);
		}

		return cp.toString();

	}

	private String getWorkFolder() {
		return config.getServletContext().getRealPath("/");
	}

	private int determineTomcatHttpPort(ServletContext ctx) {

		try {
            Object o = FieldUtils.readField(ctx, "context", true);
            StandardContext sCtx = (StandardContext) FieldUtils.readField(o, "context", true);
            Container container = (Container) sCtx;

            Container c = container.getParent();
	        while (c != null && !(c instanceof StandardEngine)) {
	            c = c.getParent();
	        }

	        if (c != null) {
	            StandardEngine engine = (StandardEngine) c;
	            for (Connector connector : engine.getService().findConnectors()) {

	            	if(connector.getProtocol().startsWith("HTTP")); {
	            		return connector.getPort();
	            	}

	            }
	        }

        } catch (Throwable e) {
            logger.error("Could not determine http port", e);
        }

		return 0;

	}

	private void initServices(ISFContext sFcontext, ServletContext servletContext) {

        try {
            Map<String, String> allStoredOptions = sFcontext.getOptionsStorage().getAllOptions();

            StatisticsServiceSettings settings = new StatisticsServiceSettings();

            if (!loadMappableSettingsFromFile(settings, sFcontext.getWorkspaceDispatcher())) {
                settings.fillFromMap(allStoredOptions);
            }

            settings.setThisSfName(servletContext.getContextPath());

            settings.setThisSfHost(InetAddress.getLocalHost().getHostName());

            settings.setThisSfPort(Integer.toString(determineTomcatHttpPort(servletContext)));

            StatisticsService statisticsService = sFcontext.getStatisticsService();

            statisticsService.setSettings(settings);

            try {
                statisticsService.init();
            } catch(Throwable t) {

			    logger.error("Init of statistics service failed", t);

    		}

    		try {
                MachineLearningService service = sFcontext.getMachineLearningService();
                service.init();
            } catch(Throwable t) {

                logger.error("Init of statistics service failed", t);

            }

            initEmailService(sFcontext, allStoredOptions);
	    } catch(Throwable t) {
            logger.error("Init options failed", t);
            throw new SFException("Problem during reading settings for embedded services", t);
		}

	}

    private void initEmailService(ISFContext sFcontext, Map<String, String> allStoredOptions) {
        try {
            EMailServiceSettings settings = new EMailServiceSettings();
            settings.fillFromMap(allStoredOptions);
            EMailService eMailService = sFcontext.getEMailService();
            eMailService.setSettings(settings);
            eMailService.init();

        } catch(Throwable t) {
            logger.error("Init of email service failed", t);
        }
    }

    /**
     *
     * @param settings
     * @param wd
     * @return true if settings successfully loaded, else return false
     */
    private boolean loadMappableSettingsFromFile(IMapableSettings settings, IWorkspaceDispatcher wd) {
        try {
            IMappableSettingsSerializer settingsReader = new PropertiesSettingsReaderSerializer();
            settingsReader.readMappableSettings(settings, wd, FolderType.CFG);
            return true;
        } catch (Exception e) {
            logger.error("Can't load settings {} from file", settings.settingsName());
            return false;
        }
    }

    private String getExceptionMessageChain(Throwable throwable) {

	    StringBuilder sb = new StringBuilder();
	    while (throwable != null) {
	        sb.append(throwable.getMessage());
	        throwable = throwable.getCause();

	        if(throwable != null) {
	        	sb.append(" > ");
	        }
	    }
	    return sb.toString();

	}

	private void setFatalError(String message) {

		SFWebApplication app = SFWebApplication.getInstance();

		if(app != null) {

			app.setFatalError(true);

			if(message.contains("Connections could not be acquired from the underlying database!")) {

				message = "Could not connect to DB: " + message;

			}

			app.setFatalErrorMessage(message);

		}

	}

	//TODO: Create separate hibernate configuration for connection to user storage
	private void initAuthentication(IAuthStorage storage) {
        if(!storage.roleExists(IAuthStorage.ADMIN)) {
            storage.addRole(IAuthStorage.ADMIN);
            }

        if(!storage.roleExists(IAuthStorage.USER)) {
            storage.addRole(IAuthStorage.USER);
            }

        if(!storage.userExists(IAuthStorage.ADMIN)) {
            User admin = new User();

            admin.setName(IAuthStorage.ADMIN);
            admin.setPassword(PasswordHasher.getHash(IAuthStorage.ADMIN));
            admin.setFirstName("default admin account");
            admin.setLastName("...");
                admin.setRegistered(new Date());
                admin.setEmail("admin@admin.com");

            admin.getRoles().add(IAuthStorage.ADMIN);
            admin.getRoles().add(IAuthStorage.USER);

            storage.addUser(admin);
        }
    }

	@Override
	public void init(ServletConfig config) throws ServletException {
	    try {
    		this.config = config;

    		String workspacePath = config.getServletContext().getInitParameter(WORKSPACE_PATH);
    		if (workspacePath == null) {
    		    workspacePath = config.getInitParameter(WORKSPACE_PATH);
        		if (workspacePath == null) {
        			System.err.println("Could not find " + WORKSPACE_PATH + " parameter in the 'context' or 'servlet init' parameters");
        		}
    		}

    		IWorkspaceDispatcher wd = createWorkspaceDispatcher(workspacePath);

            // ----------- log4j init
            // allow to use ${sf.log.dir} in log.properties
            File sfLogDir = wd.createFolder(FolderType.LOGS,  "");
            System.setProperty("sf.log.dir", sfLogDir.getAbsolutePath());

            File fullLogConfig = wd.getFile(FolderType.CFG, "log.properties");
            CustomPropertyConfigurator.configureAndWatch(fullLogConfig.getAbsolutePath());

            if (logger.isInfoEnabled()) {
                logger.info("Sailfish {}", new CoreVersion());
            }

            // Read settings
            XMLConfiguration sfConfig = new XMLConfiguration() {
                @Override
                protected Transformer createTransformer() throws TransformerException {
                    Transformer transformer = super.createTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                    return transformer;
                }
            };
			try {
				File configFile = wd.getFile(FolderType.CFG, CONFIG_FILE_NAME);
				try (InputStream stream = new FileInputStream(configFile)) {
					sfConfig.load(stream);
				}
			} catch (ConfigurationException | WorkspaceSecurityException e) {
				throw new SFException("Could not read [" + CONFIG_FILE_NAME + "] configuration file", e);
			} catch (FileNotFoundException e) {
	    		logger.info("Use default settings for Sailfish");
			}

			File configFileOnLastLayer = new File(wd.getFolder(FolderType.CFG), CONFIG_FILE_NAME);
			sfConfig.setFile(configFileOnLastLayer);
			sfConfig.setAutoSave(true);

    		// ----------- SFContextSettings init
    		SFContextSettings sfContextSettings = new SFContextSettings();
            sfContextSettings.setConfig(sfConfig);
    		sfContextSettings.setCompilerClassPath(buildClassPathString());
            sfContextSettings.setAuthEnabled(isAuthEnabled(config));

    		// ----------- SFLocalContext init
    		logger.debug("Before create context");
    		logger.debug("workspace: {}", workspacePath);

    		try {
    		    sfLocalContext = SFLocalContext.createContext(wd, sfContextSettings);
                BigDecimal comparisonPrecision = sfLocalContext.getEnvironmentManager().getEnvironmentSettings().getComparisonPrecision();
                MathProcessor.COMPARISON_PRECISION = comparisonPrecision;
                MessageComparator.COMPARISON_PRECISION = comparisonPrecision.doubleValue();
    		} catch (Throwable ex) {
    			logger.error("Failed to create SFLocalContext instance.", ex);
    			setFatalError(getExceptionMessageChain(ex));
    		}

            // ---------- Global singleton init
            SFWebApplication.getInstance().init(fullLogConfig.getAbsolutePath(), sfLocalContext);

            FacesContext.getCurrentInstance()
                        .getExternalContext()
                        .getApplicationMap()
                        .put("sfWebApp", SFWebApplication.getInstance());

    		if(SFLocalContext.getDefault() == null) {
    			logger.error("SFLocalContext.createContext returns null\n");
    			logger.error("Application initialization finished with error\n");
    			return;
    		}
    		logger.debug("SFLocalContext.createContext invoked.");

    		// Authentication DB init
    		initAuthentication(sfLocalContext.getAuthStorage());

    		// Hibernate DB service init
            initServices(sfLocalContext, config.getServletContext());

    		// ---------- subscribe SFWebApplication to events
    		sfLocalContext.getMatrixStorage().subscribeForUpdates((MatrixUpdateListener)SFWebApplication.getInstance().getMatrixUpdateRetriever());
    		sfLocalContext.getConnectionManager().subscribeForEvents((IEnvironmentListener) SFWebApplication.getInstance().getEnvironmentUpdateRetriever());
            sfLocalContext.getTestScriptStorage().setScriptRunListener((IScriptRunListener) SFWebApplication.getInstance().getScriptrunsUpdateRetriever());
            sfLocalContext.getScriptRunner().addScriptRunListener((IScriptRunListener) SFWebApplication.getInstance().getScriptrunsUpdateRetriever());
            sfLocalContext.getScriptRunner().addScriptRunListener(sfLocalContext.getTestScriptStorage());
            sfLocalContext.getScriptRunner().testScriptsInitFromWD();
    		WebLoggingAppender.registerSubscriber((LogSubscriber)SFWebApplication.getInstance().getEventRetriever());
    		MatrixHolder matrixHolder = new MatrixHolder(wd, sfLocalContext.getMatrixStorage(), sfLocalContext.getMatrixProviderHolder());

            HelpContentHolder helpContentHolder = new HelpContentHolder(sfLocalContext);
            sfLocalContext.getDictionaryManager().subscribeForEvents(helpContentHolder);
    		EnumSetContainer enumSetContainer = new EnumSetContainer(sfLocalContext.getDictionaryManager());
    		FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().put(BeanUtil.WORKSPACE_DISPATCHER, wd);
    		FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().put(BeanUtil.MATRIX_HOLDER, matrixHolder);
    		FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().put(BeanUtil.MATRIX_PROVIDER_HOLDER, sfLocalContext.getMatrixProviderHolder());
    		FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().put(BeanUtil.HELP_CONTENT_HOLDER, helpContentHolder);
    		FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().put("sfContext", sfLocalContext);
    		FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().put(BeanUtil.ENUM_SET_CONTAINER, enumSetContainer);
            FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().put(BeanUtil.SESSION_MODELS_MAPPER, new SessionModelsMapper());
            FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().put(BeanUtil.ENVIRONMENT_TRACKING_BEAN, new EnvironmentTrackingBean());

            BeanUtil.setServletContext(config.getServletContext());

    		logger.info("Application initialization finished\n");
	    } catch (Throwable e) {
	        logger.error(e.getMessage(), e);
	        e.printStackTrace();
	        setFatalError(getExceptionMessageChain(e));
	    }
	}

    // FIXME: find a better way to check
    public boolean isAuthEnabled(ServletConfig config) {
        String webXml = String.valueOf(config.getServletContext().getAttribute("org.apache.tomcat.util.scan.MergedWebXml"));
        return StringUtils.contains(webXml, "login-config");
	}

	private IWorkspaceDispatcher createWorkspaceDispatcher(String paths) throws IOException {
		if (paths == null) {
			paths = getWorkFolder();
		}

		String[] workspaces = paths.split(File.pathSeparator);

		DefaultWorkspaceDispatcherBuilder builder = new DefaultWorkspaceDispatcherBuilder();

		//Deploy folder should be first
		builder.addWorkspaceLayer(new File(getWorkFolder()), DefaultWorkspaceLayout.getInstance());

		for (String workspacePath : workspaces) {
			if (workspacePath.trim().equals(".")) {
				workspacePath = getWorkFolder();
			}
			System.out.println("Add workspace layer " + workspacePath);
			builder.addWorkspaceLayer(new File(workspacePath), DefaultWorkspaceLayout.getInstance());
		}

		return builder.build();
	}

	@Override
	public void destroy() {

		logger.debug("Destroing servlet");

		try {
		    sfLocalContext.dispose();
			sfLocalContext.getMatrixStorage().unSubscribeForUpdates((MatrixUpdateListener)SFWebApplication.getInstance().getMatrixUpdateRetriever());
			sfLocalContext.getConnectionManager().unSubscribeForEvents((IEnvironmentListener)SFWebApplication.getInstance().getEnvironmentUpdateRetriever());
			sfLocalContext.getScriptRunner().removeScriptRunListener((IScriptRunListener)SFWebApplication.getInstance().getScriptrunsUpdateRetriever());
			WebLoggingAppender.unRegisterSubscriber((LogSubscriber)SFWebApplication.getInstance().getEventRetriever());
			SFWebApplication.getInstance().getMatrixUpdateRetriever().destroy();
			SFWebApplication.getInstance().getEnvironmentUpdateRetriever().destroy();
			SFWebApplication.getInstance().getScriptrunsUpdateRetriever().destroy();
			SFWebApplication.getInstance().getEventRetriever().destroy();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		CustomPropertyConfigurator.stopWatch();
		logger.debug("Servlet destroyed");
	}

	@Override
	public ServletConfig getServletConfig() {
		return this.config;
	}

	@Override
	public String getServletInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		throw new SFException("Incorrect State. This servlet only initialize SF context");
	}
}
