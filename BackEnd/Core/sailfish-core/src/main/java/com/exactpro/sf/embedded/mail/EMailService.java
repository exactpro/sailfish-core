package com.exactpro.sf.embedded.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.activation.CommandMap;
import javax.activation.DataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.embedded.IEmbeddedService;
import com.exactpro.sf.embedded.configuration.ServiceStatus;
import com.exactpro.sf.embedded.mail.configuration.EMailServiceSettings;
import com.exactpro.sf.storage.IMapableSettings;
import com.exactpro.sf.util.EMailUtil;

public class EMailService implements IEmbeddedService{
    private final Logger logger = LoggerFactory.getLogger(EMailService.class);

    private volatile EMailServiceSettings settings;
    private volatile ServiceStatus status = ServiceStatus.Disconnected;
    private volatile String errorMsg = "";

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public EMailService() {
        this.settings = new EMailServiceSettings();

        MailcapCommandMap mailcapCommandMap = new MailcapCommandMap();

        mailcapCommandMap.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mailcapCommandMap.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mailcapCommandMap.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mailcapCommandMap.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mailcapCommandMap.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");

        CommandMap.setDefaultCommandMap(mailcapCommandMap);
    }

    @Override
    public void init() {
        if (settings.isServiceEnabled()) {
            checkConnection();
            return;
        }
        status = ServiceStatus.Disconnected;
        errorMsg = "";
    }

    @Override
    public void tearDown() {
        //nothing to close
    }

    public void send(String subject, String body, String htmlBody,
                     List<String> additionalRecipients,
                     List<File> attachments) {
        if (!ServiceStatus.Connected.equals(this.status)) {
            logger.warn("EMail service in not the connected status. Please check its configurations");
            return;
        }

        Email mailer = null;

        if(StringUtils.isNotEmpty(htmlBody)) {
            mailer = new HtmlEmail();
        } else if (attachments != null && !attachments.isEmpty()) {
            mailer = new MultiPartEmail();
        } else {
            mailer = new SimpleEmail();
        }

        List<String> recipients = setTransportSettings(mailer);

        if (attachments != null && !attachments.isEmpty()) {
            for (final File attachment : attachments) {
                if (attachment != null) {
                    try {
                        ((MultiPartEmail) mailer).attach(new Attachment(attachment.getName(), attachment), attachment.getName(), "");
                    } catch (EmailException e) {
                        throw new EPSCommonException(e);
                    }
                }
            }
        }

        try {
            if (additionalRecipients != null && !additionalRecipients.isEmpty()) {
                recipients.addAll(additionalRecipients);
            }
            for (String recipient : recipients) {
                mailer.addTo(recipient);
            }

            mailer.setSubject(subject);

            if (htmlBody != null) {
                HtmlEmail htmlEmail = (HtmlEmail) mailer;
                htmlEmail.setHtmlMsg(htmlBody);

                if (body != null) {
                    htmlEmail.setTextMsg(body);
                }
            } else {
                mailer.setMsg(body);
            }

            mailer.send();
        } catch (Exception e) {
            logger.error("Cannot send a message", e);
            throw new EPSCommonException("Cannot send a message", e);
        }
    }

    public EMailServiceSettings getSettings() {
        try {
            this.lock.readLock().lock();
            return settings;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void setSettings(IMapableSettings settings) {
        this.lock.writeLock().lock();
        this.settings = (EMailServiceSettings) settings;
        this.lock.writeLock().unlock();
    }

    public void preCheckConnection() {
        this.status = ServiceStatus.Checking;
    }

    public void checkConnection() {
        Transport transport = null;
        try {
            Email email = new SimpleEmail();
            setTransportSettings(email);
            Session mailSession = email.getMailSession();
            transport = mailSession.getTransport("smtp");
            transport.connect();
            status = transport.isConnected() ? ServiceStatus.Connected : ServiceStatus.Disconnected;
        } catch (Exception e) {
            logger.error("Error upon checking a connection to smtp server", e);
            status = ServiceStatus.Error;
            errorMsg = e.getMessage();
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    logger.error("Error upon closing a connection to smtp server", e);
                }
            }
        }
    }

    @Override
    public boolean isConnected(){
        return this.status.equals(ServiceStatus.Connected);
    }

    @Override
    public ServiceStatus getStatus() {
        return status;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }


    private List<String>  setTransportSettings(Email email) {
        try {
            this.lock.readLock().lock();

            email.setHostName(settings.getSmtpHost());
            email.setSmtpPort(settings.getSmtpPort());
            String username = settings.getUsername();
            email.setAuthentication(username, settings.getPassword());
            email.setFrom(username);
            email.setSSLOnConnect(settings.isUseSSL());
            email.setSocketTimeout(settings.getTimeout());
            email.setSocketConnectionTimeout(settings.getTimeout());

            return EMailUtil.parseRecipients(settings.getRecipients());
        } catch (EmailException e) {
            logger.error("Cannot set the email settings", e);
            throw new EPSCommonException("Cannot set the email settings", e);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private static class Attachment implements DataSource {
        private final String name;
        private final File resourceStream;

        public Attachment(String name, File resourceStream) {
            this.name = name;
            this.resourceStream = resourceStream;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(resourceStream);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getContentType() {
            return "application/zip";
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
