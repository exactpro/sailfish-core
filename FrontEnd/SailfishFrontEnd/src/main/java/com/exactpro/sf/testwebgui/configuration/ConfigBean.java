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
package com.exactpro.sf.testwebgui.configuration;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.EJB3DTDEntityResolver;
import org.hibernate.internal.util.xml.ErrorLogger;
import org.primefaces.context.RequestContext;
import org.primefaces.event.TabChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.ValidateRegex;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.configuration.ILoggingConfiguration;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.embedded.configuration.ServiceStatus;
import com.exactpro.sf.scriptrunner.EnvironmentSettings;
import com.exactpro.sf.scriptrunner.EnvironmentSettings.RelevantMessagesSortingMode;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.auth.PasswordHasher;
import com.exactpro.sf.storage.auth.User;
import com.exactpro.sf.storage.auth.UserRole;
import com.exactpro.sf.util.JdbcUtils;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name="configBean")
@SessionScoped
@SuppressWarnings("serial")
public class ConfigBean implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ConfigBean.class);

	private static final String PREFFERED_TEST_QUERY = "hibernate.c3p0.preferredTestQuery";

	private static final String VALIDATION_FAILED = "validationFailed";

	private static final String DEFAULT_SECTION = "database";

	private static final String HIBERNATE_CFG_FILE = "hibernate.cfg.xml";

    private static final String LOG_PROPERTIES_FILE = "log.properties";

    private String logConfigText;

	private String newDBName;

	private boolean dbChangesApplied = false;

	private ConfigModel configModel = new ConfigModel();

	private Configuration config;

	private SFLogConfigurationVisualizer logVisualizer;

	private String workspacePath;

	private Set<User> users;
	private User newUser;
	private User selectedUser;
	private Set<UserRole> selectedUserRoles;

	private String oldPassword;
	private String newPassword;
	private String newPasswordConfirm;

	private List<EnvironmentEntity> environmentParamsList;

    private boolean createIndividualAppenders;
    private String individualAppendersThreshold;

	private List<ComponentInfo> infos;

	private static String DEFAULT_PASSWORD = "Welcome";

	private ServiceStatus conStatus = ServiceStatus.Connected;
	private String conErrorText;

	private String section;

	public ConfigBean() {

	}

    //refresh state after deserialization
    private Object readResolve()  {
        init();
        return this;
    }

	@PostConstruct
	public void init() {

        ISFContext sfContext = BeanUtil.getSfContext();
        if(sfContext == null) {
            logger.error("SFLocalContext is null\n");
            logVisualizer = new SFLogConfigurationVisualizer("");
        } else {
            workspacePath = "";

            logConfigText = readConfig();
            ILoggingConfiguration loggingConfiguration = BeanUtil.getSfContext().getLoggingConfigurator().getConfiguration();
            createIndividualAppenders = loggingConfiguration.isIndividualAppendersEnabled();
            individualAppendersThreshold = loggingConfiguration.getIndividualAppendersThereshold();

            logVisualizer = new SFLogConfigurationVisualizer(logConfigText);

            try {

                File fDescr = BeanUtil.getSfContext().getWorkspaceDispatcher().getFile(FolderType.CFG, HIBERNATE_CFG_FILE);
                config = new Configuration();
                config.configure(fDescr);

            } catch (FileNotFoundException e) {
                logger.error("Hibernate configuration file not found", e);
            }

            configModel.applyConfig(config);

            EnvironmentSettings environmentSettings = sfContext.getEnvironmentManager().getEnvironmentSettings();
            BeanMap environmentBeanMap = new BeanMap(environmentSettings);
            environmentParamsList = new ArrayList<>();
            try {
                for (Object key : environmentBeanMap.keySet()) {
                    if (environmentBeanMap.getWriteMethod(key.toString()) != null) {
                        Field field = environmentSettings.getClass().getDeclaredField(key.toString());
                        Description description =  field.getAnnotation(Description.class);
                        ValidateRegex validateRegex = field.getAnnotation(ValidateRegex.class);
                        String descriptionValue = description != null ? description.value() : null;
                        String regex = validateRegex != null ? validateRegex.regex() : null;

                        EnvironmentEntity environmentEntity = new EnvironmentEntity(key.toString(), environmentBeanMap.get(key), regex, descriptionValue);
                        environmentParamsList.add(environmentEntity);
                    }
                }
            } catch (NoSuchFieldException e) {
                logger.error(e.getMessage(), e);
            }

            refresh();
        }

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public Object convert(@SuppressWarnings("rawtypes") Class type, Object value) {
                return EnvironmentSettings.StorageType.parse(value.toString());
            }
        }, EnvironmentSettings.StorageType.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public Object convert(@SuppressWarnings("rawtypes") Class type, Object value) {
                return RelevantMessagesSortingMode.parse(value.toString());
            }
        }, RelevantMessagesSortingMode.class);

		this.infos = createComponentsInfo();
	}

	public void preRenderView() {
		if (BeanUtil.getSfContext() == null) {
			BeanUtil.addErrorMessage("SFContext error", "SFContext is not initialized correctly. See log file for details.");
		}
	}

    public void onPageLoad() {
        if (StringUtils.isEmpty(this.section)) {
            this.section = DEFAULT_SECTION;
        }
        RequestContext.getCurrentInstance().execute("setSectionVisible('" + this.section + "')");
    }

	public void changeSection() {
	    String reqSection = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("section");
	    if (StringUtils.isEmpty(reqSection)) {
	        reqSection = DEFAULT_SECTION;
	    }
	    this.section = reqSection;
        RequestContext.getCurrentInstance().execute("setSectionVisible('" + this.section + "')");
	}

	public void refresh() {
		logger.info("refresh invoked {}", getUser());
		this.users = BeanUtil.getSfContext().getAuthStorage().getUsers();
	}

	public void createNewUser() {
		logger.info("createNewUser invoked {}", getUser());
		this.newUser = new User();

		this.selectedUserRoles = new HashSet<>();

		for (String role : BeanUtil.getSfContext().getAuthStorage().getRoles()) {
			this.selectedUserRoles.add(new UserRole(role, role.equals("user")));
		}

	}

	public void add() {
		logger.info("add invoked {}", getUser());
		try {

			this.newUser.setRegistered(new Date());
			this.newUser.setPassword(PasswordHasher.getHash(this.newUser.getPassword()));

			for (UserRole role : this.selectedUserRoles) {
				if (role.isHasRole()) {
					this.newUser.getRoles().add(role.getName());
				}
			}

			BeanUtil.getSfContext().getAuthStorage().addUser(this.newUser);

			this.newUser = null;
			this.selectedUserRoles.clear();

			refresh();

		} catch (StorageException e) {
			logger.error("AppUser adding error:{}", e);
			BeanUtil.addErrorMessage("User adding error", "Could not add new Entity");
		}

	}

	public void delete(User user){
		logger.info("delete invoked {} user[{}]", getUser(), user);
		try {
			BeanUtil.getSfContext().getAuthStorage().removeUser(user.getName());
			refresh();

		} catch (StorageException e) {
			logger.error("AppUser removing error:{}", e);
		}
	}

	public void delete() {
		logger.info("delete invoked {} selectedUser[{}]", getUser(), selectedUser);
		try {

			BeanUtil.getSfContext().getAuthStorage().removeUser(selectedUser.getName());
			refresh();

		} catch (StorageException e) {
			logger.error("AppUser removing error:{}", e);
		}
	}

	public void save() {
		logger.info("save invoked {} selectedUserRoles[{}]", getUser(), selectedUserRoles);
		try {
			if(newPassword != null && !newPassword.trim().isEmpty()) {
				this.selectedUser.setPassword(PasswordHasher.getHash(newPassword));
				newPassword = "";
			}

            for(UserRole role : selectedUserRoles) {
                if(role.isHasRole()) {
                    selectedUser.getRoles().add(role.getName());
                } else {
                    selectedUser.getRoles().remove(role.getName());
                }
            }

			BeanUtil.getSfContext().getAuthStorage().updateUser(this.selectedUser);

			this.selectedUser.getRoles().clear();

			this.selectedUserRoles.clear();

			refresh();

		} catch (StorageException e) {
			logger.error("AppUser save error:{}", e.getMessage(), e);
			BeanUtil.addErrorMessage("User saving error", "Could not update an Entity");
		}
	}

	public void changePassword() {
		logger.info("changePassword invoked {} oldPassword[{}], newPassword[{}], newPassword[{}]", getUser(), oldPassword, newPassword, newPassword);

		User user = BeanUtil.getObject(BeanUtil.KEY_USER, User.class);

		if (PasswordHasher.getHash(this.oldPassword).equals(user.getPassword())) {

			if (this.newPassword.equals(this.newPasswordConfirm)) {

				if (!this.newPassword.equals(this.oldPassword)) {

					user.setPassword(PasswordHasher.getHash(this.newPassword));

					BeanUtil.getSfContext().getAuthStorage().updateUser(user);

					this.oldPassword = null;
					this.newPassword = null;
					this.newPasswordConfirm = null;

					RequestContext.getCurrentInstance().execute("changePassDialog.hide();");

					BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Password changed successfully", "");

				} else {
					logger.error("New Password must be different than Old Password!");
					BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "New Password must be different than Old Password!");
					RequestContext rContext = RequestContext.getCurrentInstance();
					rContext.addCallbackParam(VALIDATION_FAILED, true);
				}

			} else {
				logger.error("Confirm New Password field is wrong!");
				BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "Confirm New Password field is wrong!");
				RequestContext rContext = RequestContext.getCurrentInstance();
				rContext.addCallbackParam(VALIDATION_FAILED, true);
			}

		} else {
			logger.error("Old Password is wrong!");
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "Old Password is wrong!");
			RequestContext rContext = RequestContext.getCurrentInstance();
			rContext.addCallbackParam(VALIDATION_FAILED, true);
		}

	}

	public void resetUserPassword() {
		logger.info("resetUserPassword invoked {}", getUser());


		User user = this.selectedUser;
		user.setPassword(PasswordHasher.getHash(DEFAULT_PASSWORD));

		BeanUtil.getSfContext().getAuthStorage().updateUser(user);
		BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Password was resetting successfully", "");

	}

	public Set<UserRole> getRoles(User user){
		return this.selectedUserRoles;
	}

	public void setRoles(Set<UserRole> roles){
		logger.info("setRoles invoked {} roles[{}]", getUser(), roles);
		selectedUserRoles = roles;
	}

	private String readConfig() {

		logger.info("readConfig: begin");
		StringBuilder text = new StringBuilder();
		try {
			File file = BeanUtil.getSfContext().getWorkspaceDispatcher().getFile(FolderType.CFG, LOG_PROPERTIES_FILE);
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
				String str;
				while ((str = br.readLine()) != null) {
					text.append("\n").append(str);
				}
			}
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		logger.info("readConfig: end");

		return text.toString();
	}

	public void applySettings() {

		logger.info("applySettings invoked {}", getUser());

		Document document = null;
        File hibFile = null;

		try {
            hibFile = BeanUtil.getSfContext().getWorkspaceDispatcher().getFile(FolderType.CFG, HIBERNATE_CFG_FILE);

            try (InputStream hibStream = new BufferedInputStream(new FileInputStream(hibFile))) {

                SAXReader xmlReader = new SAXReader();
                xmlReader.setEntityResolver(new EJB3DTDEntityResolver());
                ErrorLogger errorLogger = new ErrorLogger(hibFile.getAbsolutePath());
                xmlReader.setErrorHandler(errorLogger);

                document = xmlReader.read(new InputSource(hibStream));

            }

        } catch (IOException e) {
            logger.error("Hibernate file isn't found", e);
        } catch (DocumentException e) {
            logger.error("Xml reading error", e);
        }

		if (document != null) {

			Element sfNode = document.getRootElement().element("session-factory");
			findPropertyNode(sfNode, AvailableSettings.URL).setText(createProtocolUrl(this.configModel));
			findPropertyNode(sfNode, AvailableSettings.USER).setText(this.configModel.getUserName());
			findPropertyNode(sfNode, AvailableSettings.PASS).setText(this.configModel.getPassword());
			findPropertyNode(sfNode, AvailableSettings.DRIVER).setText(this.configModel.getDriverClass());
			findPropertyNode(sfNode, AvailableSettings.DIALECT).setText(this.configModel.getDialect());
			findPropertyNode(sfNode, PREFFERED_TEST_QUERY).setText(this.configModel.getPreferredTestQuery());

			try {
                hibFile = BeanUtil.getSfContext().getWorkspaceDispatcher().createFile(FolderType.CFG, true, HIBERNATE_CFG_FILE);
    			try (OutputStream os = new FileOutputStream(hibFile)) {

    				OutputFormat format = OutputFormat.createPrettyPrint();

    				XMLWriter xmlWriter = new XMLWriter(os, format);
    				xmlWriter.write(document);

    				this.dbChangesApplied = true;

    				logger.debug("Hibernate configuration file {} successfully updated", hibFile.getAbsolutePath());

    				BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "INFO", "Configuration changes successfully applied");

    			} catch (IOException e) {
    				logger.error("Xml writing error", e);
    			}
			} catch (WorkspaceStructureException | WorkspaceSecurityException e) {
			    logger.error("Hibernate file can't be created", e);
			}
		}
	}

	public void revertChanges() {

		if (!this.configModel.equals(this.config)) {
			this.configModel.applyConfig(this.config);
			applySettings();
		}

		this.dbChangesApplied = false;

		this.conStatus = ServiceStatus.Connected;
		RequestContext.getCurrentInstance().update("connectionStatusForm");
		RequestContext.getCurrentInstance().update("buttonsForm");
	}

	public void preCheckConnection() {

		if (this.configModel.equals(this.config)) {
			if (!ServiceStatus.Connected.equals(this.conStatus)) {
				this.conStatus = ServiceStatus.Connected;
				RequestContext.getCurrentInstance().update("connectionStatusForm");
				RequestContext.getCurrentInstance().update("buttonsForm");
			}
			BeanUtil.addInfoMessage("Info", "Changes has not yet been made");
			return;
		}

		this.conStatus = ServiceStatus.Checking;
		RequestContext.getCurrentInstance().update("connectionStatusForm");
		RequestContext.getCurrentInstance().update("buttonsForm");
	}

	public void checkConnection() {

		if (this.configModel.equals(this.config)) {
			return;
		}

		File fDescr;
		try {
			fDescr = BeanUtil.getSfContext().getWorkspaceDispatcher().getFile(FolderType.CFG, HIBERNATE_CFG_FILE);
		} catch (FileNotFoundException | WorkspaceSecurityException e) {
			logger.error("Hibernate file loading error", e);
			return;
		}

		final Configuration configClone = new Configuration();
		configClone.configure(fDescr);
		this.configModel.applyToConfiguration(configClone);

		try {

			JdbcUtils.executeTestQuery(this.configModel.driverClass, createProtocolUrl(configModel), this.configModel.getUserName(),
			        this.configModel.getPassword(), this.configModel.getPreferredTestQuery());

			this.conStatus = ServiceStatus.Connected;

			applySettings();

		} catch (ClassNotFoundException | SQLException e1) {
			this.conStatus = ServiceStatus.Error;
			this.conErrorText = e1.getMessage();
		}

		RequestContext.getCurrentInstance().update("connectionStatusForm");
		RequestContext.getCurrentInstance().update("buttonsForm");
	}

	public boolean isConfigWasChanged() {
		return !this.configModel.equals(this.config);
	}

	private Node findPropertyNode(Element contextNode, String propertyName) {
		return contextNode.selectSingleNode("//property[@name='" + propertyName + "']");
	}

	public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

	private String createProtocolUrl(ConfigModel model) {
	    return JdbcUtils.buildConnectionUrl(model.getProtocol(), model.getSubProtocol(), model.getHost(),
	            model.getPort(), model.getPath(), model.getQuery());
	}

	public void updateLogConfig() {
		logConfigText = logVisualizer.toString();
		saveLogConfig();
	}

	public void updateLogConfigModel() {
		logVisualizer = new SFLogConfigurationVisualizer(logConfigText);
	}

	public void restoreSettings() {
        ILoggingConfiguration loggingConfiguration = BeanUtil.getSfContext().getLoggingConfigurator().getConfiguration();
        createIndividualAppenders = loggingConfiguration.isIndividualAppendersEnabled();
	    individualAppendersThreshold = loggingConfiguration.getIndividualAppendersThereshold();
		logConfigText = readConfig();
		updateLogConfigModel();
	}

	public void saveLogConfig() {

		logger.info("saveLogConfig invoked {}", getUser());

		File config = null;
		try {
			 config = BeanUtil.getSfContext().getWorkspaceDispatcher().createFile(FolderType.CFG, true, LOG_PROPERTIES_FILE);
		} catch(WorkspaceStructureException ex) {
			logger.error(ex.getMessage(), ex);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "Could not save log config - " + ex.getMessage());
			return;
		}

		try (BufferedWriter bufferedWriter = new BufferedWriter(
				new OutputStreamWriter(
				new FileOutputStream(config), "UTF-8"))) {

	    	bufferedWriter.write(logConfigText);

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "Could not save log config - " + ex.getMessage());
			return;
		}

        ILoggingConfigurator loggingConfigurator = BeanUtil.getSfContext().getLoggingConfigurator();
        try {
            if (createIndividualAppenders) {
                loggingConfigurator.enableIndividualAppenders();
            } else {
                loggingConfigurator.disableIndividualAppenders();
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "Could not apply individual adapter setting - " + ex.getMessage());
            return;
        }

		BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "INFO", "Successfully saved" );
		logger.info("saveLogConfig: end");

	}

	public ConfigModel getConfigModel() {
		return configModel;
	}

	public void createDB(){
		logger.info("createDB invoked {}", getUser());
	}

	public void removeDB(){
		logger.info("removeDB invoked {}", getUser());
	}

	public void cleanDB(){
		logger.info("cleanDB invoked {}", getUser());
		try {
			BeanUtil.getSfContext().getMessageStorage().clear();
			BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "INFO", "Database successfully cleared");
		} catch (Exception e) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "Error during cleaning database - " + e.getMessage());
			logger.error(e.getMessage(), e);
		}
	}

	public String getLogConfigText() {
		return logConfigText;
	}

	public void setLogConfigText(String logConfigText) {
		logger.info("setLogConfigText invoked {}", getUser());
		this.logConfigText = logConfigText;
	}

	public String getNewDBName() {
		return newDBName;
	}

	public void setNewDBName(String newDBName) {
		logger.info("setNewDBName invoked {} query[{}]", getUser(), newDBName);
		this.newDBName = newDBName;
	}

	public Set<User> getUsers() {
		return users;
	}

	public void setUsers(Set<User> users) {
		logger.info("setUsers invoked {} users[{}]", getUser(), users);
		this.users = users;
	}

	public User getNewUser() {
		return newUser;
	}

	public void setNewUser(User newUser) {
		logger.info("setNewUser invoked {} newUser[{}]", getUser(), newUser);
		this.newUser = newUser;
	}

	public User getSelectedUser() {
		return selectedUser;
	}

	public void setSelectedUser(User selectedUser) {
		logger.info("setSelectedUser invoked {} selectedUser[{}]", getUser(), selectedUser);

		this.selectedUser = selectedUser;

		this.selectedUserRoles = new HashSet<>();

        for(String role : BeanUtil.getSfContext().getAuthStorage().getRoles()) {
            boolean hasRole = null != selectedUser && selectedUser.getRoles().contains(role);
            this.selectedUserRoles.add(new UserRole(role, hasRole));
		}
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getNewPasswordConfirm() {
		return newPasswordConfirm;
	}

	public void setNewPasswordConfirm(String newPasswordConfirm) {
		this.newPasswordConfirm = newPasswordConfirm;
	}

	public String getWorkspacePath() {
		return workspacePath;
	}

	public void setWorkspacePath(String workspacePath) {
		logger.info("setWorkspacePath invoked {} workspacePath[{}]", getUser(), workspacePath);
		this.workspacePath = workspacePath;
	}

    public void switchWorkspace() {
		logger.info("switchWorkspace invoked {}", getUser());
		try {
			BeanUtil.getSfContext().getWorkspaceDispatcher().selectWorkspace(this.workspacePath, DefaultWorkspaceLayout.getInstance());
			((HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true)).invalidate();
		} catch (UnsupportedOperationException | FileNotFoundException e) {
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "Error during switching workspace: " + e.getMessage());
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).toString();
	}

	protected String getUser(){
		return System.getProperty("user.name");
	}

    public boolean isCreateIndividualAppenders() {
        return createIndividualAppenders;
    }

    public String getIndividualAppendersThreshold() {
	    return individualAppendersThreshold;
    }

    public void setIndividualAppendersThreshold(String individualAppendersThreshold) {
	    this.individualAppendersThreshold = individualAppendersThreshold;
	    try {
            BeanUtil.getSfContext().getLoggingConfigurator().getConfiguration().setIndividualAppendersThreshold(individualAppendersThreshold);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "Could not save config - " + ex.getMessage());
            return;
        }
    }

    public void setCreateIndividualAppenders(boolean createIndividualAppenders) {
        this.createIndividualAppenders = createIndividualAppenders;
    }

	public SFLogConfigurationVisualizer getLogVisualizer() {
		return logVisualizer;
	}

	public void setLogVisualizer(SFLogConfigurationVisualizer logVisualizer) {
		this.logVisualizer = logVisualizer;
	}

	public void onTabChange(TabChangeEvent event) {
		String tabTitle = event.getTab().getTitle();
		logConfigText = readConfig();
		if(tabTitle.equals("As text")) {
			//
		} else {
			updateLogConfigModel();
		}
	}

    public void onApplyEnvParams() {
        logger.info("onApplyEnvParams: start");
        try {

        	EnvironmentSettings environmentSettings = BeanUtil.getSfContext().getEnvironmentManager().getEnvironmentSettings();
        	BeanMap environmentBeanMap = new BeanMap(environmentSettings);

            for (EnvironmentEntity entry : environmentParamsList) {
            	Class<?> type = environmentBeanMap.getType(entry.getParamName());
            	Object convertedValue = BeanUtilsBean.getInstance().getConvertUtils().convert(entry.getParamValue(), type);
                environmentBeanMap.put(entry.getParamName(), convertedValue);
            }
            BeanUtil.getSfContext().getEnvironmentManager().updateEnvironmentSettings((EnvironmentSettings) environmentBeanMap.getBean());
            BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "INFO", "Successfully saved. Some options will be applied only after Sailfish restart");
        } catch (Exception e){
            logger.error(e.getMessage(), e);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "ERROR", e.getMessage());
        }
        logger.info("onApplyEnvParams: end");
    }

    public List<EnvironmentEntity> getEnvironmentParamsList() {
        return environmentParamsList;
    }

    public List<ComponentInfo> getComponentsBuildNumbers() {
        return this.infos;
    }

	public List<ComponentInfo> createComponentsInfo() {

	    List<ComponentInfo> infos = new ArrayList<>();

        for (IVersion v : BeanUtil.getSfContext().getPluginVersions()) {

            if (v.isGeneral()) {
                continue;
            }

            ComponentInfo info = new ComponentInfo();

            info.setName(v.getAlias());
            info.setVersion(v.buildVersion());

            infos.add(info);
        }

        return infos;
    }

	public boolean isDbChangesApplied() {
		return dbChangesApplied;
	}

	public void setDbChangesApplied(boolean dbChangesApplied) {
		this.dbChangesApplied = dbChangesApplied;
	}

	public ServiceStatus getConStatus() {
		return conStatus;
	}

	public String getConErrorText() {
		return conErrorText;
	}

	public class ComponentInfo implements Serializable {
		private String name;
		private String version;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getVersion() {
            return version;
        }

		public void setVersion(String version) {
            this.version = version;
        }
	}

	public class ConfigModel implements Serializable {

		private String driverClass;
		private String dialect;
		private String preferredTestQuery;
		private String userName;
		private String password;
		private String protocol;
		private String subProtocol;
		private String host;
		private String port;
		private String path;
		private String query;

		public String getDriverClass() {
			return driverClass;
		}
		public void setDriverClass(String driverClass) {
			this.driverClass = driverClass;
		}
		public String getDialect() {
			return dialect;
		}
		public void setDialect(String dialect) {
			this.dialect = dialect;
		}
		public String getPreferredTestQuery() {
			return preferredTestQuery;
		}
		public void setPreferredTestQuery(String preferredTestQuery) {
			this.preferredTestQuery = preferredTestQuery;
		}
		public String getUserName() {
			return userName;
		}
		public void setUserName(String userName) {
			this.userName = userName;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		public String getProtocol() {
			return protocol;
		}
		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}
		public String getSubProtocol() {
			return subProtocol;
		}
		public void setSubProtocol(String subProtocol) {
			this.subProtocol = subProtocol;
		}
		public String getHost() {
			return host;
		}
		public void setHost(String host) {
			this.host = host;
		}
		public String getPort() {
			return port;
		}
		public void setPort(String port) {
			this.port = port;
		}
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
		}
		public String getQuery() {
			return query;
		}
		public void setQuery(String query) {
			this.query = query;
		}

		public void applyToConfiguration(Configuration configuration) {
			configuration.setProperty(AvailableSettings.USER, userName);
			configuration.setProperty(AvailableSettings.PASS, password);
			configuration.setProperty(AvailableSettings.DRIVER, driverClass);
			configuration.setProperty(AvailableSettings.DIALECT, dialect);
			configuration.setProperty(PREFFERED_TEST_QUERY, preferredTestQuery);
			configuration.setProperty(AvailableSettings.URL, createProtocolUrl(this));
		}

		public void applyConfig(Configuration configuration) {

			Properties prop = configuration.getProperties();

			try {
    			String url = prop.getProperty(AvailableSettings.URL, "");

    			URI full = new URI(url);
				URI uri = new URI(full.getSchemeSpecificPart());
				setProtocol(full.getScheme());
				setSubProtocol(uri.getScheme());
				setHost(uri.getHost());
				int intPort = uri.getPort();
				if (intPort == -1) {
					port = "";
				} else {
					port = String.valueOf(intPort);
				}
				path = uri.getPath().replace("/", "");
				query = uri.getQuery();
			} catch (URISyntaxException e) {
				logger.error("Could not parse hibernate url.", e);
			}

			driverClass = prop.getProperty(AvailableSettings.DRIVER);
			dialect = prop.getProperty(AvailableSettings.DIALECT);
			preferredTestQuery = prop.getProperty(PREFFERED_TEST_QUERY, "SELECT 1;");

			userName = prop.getProperty(AvailableSettings.USER, "sailfish");
			password = prop.getProperty(AvailableSettings.PASS, "999");

		}

		public boolean equals(Configuration configuration) {

			Properties prop = configuration.getProperties();

			if (!driverClass.equals(prop.getProperty(AvailableSettings.DRIVER))) {
				return false;
			}
			if (!dialect.equals(prop.getProperty(AvailableSettings.DIALECT))) {
				return false;
			}
			if (!preferredTestQuery.equals(prop.getProperty(PREFFERED_TEST_QUERY))) {
				return false;
			}
			if (!userName.equals(prop.getProperty(AvailableSettings.USER))) {
				return false;
			}
			if (!password.equals(prop.getProperty(AvailableSettings.PASS))) {
				return false;
			}
			if (!createProtocolUrl(this).equals(prop.getProperty(AvailableSettings.URL))) {
				return false;
			}

			return true;
		}
	}
}