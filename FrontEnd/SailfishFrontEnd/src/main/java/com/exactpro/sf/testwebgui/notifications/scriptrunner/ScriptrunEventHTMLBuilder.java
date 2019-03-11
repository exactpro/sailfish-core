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
package com.exactpro.sf.testwebgui.notifications.scriptrunner;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.format.DateTimeFormatter;

import com.exactpro.sf.common.util.ErrorUtil;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.generator.AggregateAlert;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.GuiVersion;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.EnvironmentSettings;
import com.exactpro.sf.scriptrunner.IScriptProgress;
import com.exactpro.sf.scriptrunner.ScriptRunException;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptState;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptStatus;
import com.exactpro.sf.scriptrunner.ZipReport;
import com.exactpro.sf.util.DateTimeUtility;
import com.exactpro.sf.testwebgui.servlets.ReportServlet;

public class ScriptrunEventHTMLBuilder {

	private static final Logger logger = LoggerFactory.getLogger(ScriptrunEventHTMLBuilder.class);

	private static final String BLOCK_ID_PREFIX = "eps-result-";
    private static final int MAX_STRING_LENGTH = 4000;

    private static final String EXECUTED_CLASS_POSTFIX = "good";
	private static final String SOME_FAILES_CLASS_POSTFIX = "normal";
    private static final String SOME_CONDTIONALLY_PASSED_CLASS_POSTFIX = "cpnormal";
	private static final String FAILED_CLASS_POSTFIX = "bad";
	private static final String IN_PROGRESS_CLASS_POSTFIX = "running";
	private static final String TIME_PAUSED_CLASS_POSTFIX = "time-paused";
	private static final String PERMANENT_PAUSED_CLASS_POSTFIX = "permanent-paused";
	private static final String CANCELED_CLASS_POSTFIX = "canceled";

	private static final String EMPTY_MESSAGE =
			"<div class=\"eps-result-system-message\">" +
                 "<p> No test scripts have been run </p>" +
            "</div>";

	private static DateTimeFormatter dateFormat = DateTimeUtility.createFormatter("yyyy-MM-dd HH:mm:ss");

    private static String resourceFolderName = GuiVersion.BUILD + "_0";

	private ScriptrunEventHTMLBuilder() {

	}

    private static String formatProblemBlock(TestScriptDescription descr) {
    	StringBuilder sb = new StringBuilder();
    	sb.append("<div id=\""+ BLOCK_ID_PREFIX + descr.getId() + "-details" +"\" class=\"eps-details-ct\" style=\"display: none;\">");
    	if (hasProblem(descr)) {
	    	sb.append("<div class=\"eps-fail-details\">");
	    		sb.append(formatProblem(descr));
	    	sb.append("</div>");
    	}
    	if (hasWarning(descr)) {
	    	sb.append("<div class=\"eps-warn-details\">");
	    		sb.append(formatWarning(descr));
	    	sb.append("</div>");
    	}
    	sb.append("</div>");
    	return sb.toString();
    }

	private static String formatProblem(TestScriptDescription descr)
	{
		Throwable tw = descr.getCause();
		if (tw != null) {
			if (tw instanceof ScriptRunException) {
				if (tw.getCause() != null && tw.getCause() instanceof AMLException) {
					AMLException e = (AMLException) (tw.getCause());
					StringBuilder sb = new StringBuilder();

					if(e.getAlertCollector().getCount(AlertType.ERROR) > 0) {
					    formatAlert(e.getAlertCollector(), sb, AlertType.ERROR);
					} else {
					    sb.append("Found unknown errors during preparing script.<br>Returned message is \"" + escapeHtml4(e.getMessage()) + "\"");
					}

					return sb.toString();
				} else if(tw.getCause() != null && tw.getCause() instanceof InvocationTargetException){
					InvocationTargetException e = (InvocationTargetException) (tw.getCause());
					Throwable target = e.getTargetException();
					if(target.getCause() != null){
						return "Found error during running script: <li>" + escapeHtml4(target.getCause().getMessage());
					} else {
						return "Found error during running script: <li>" + escapeHtml4(target.getMessage());
					}
				}
			}
			String eText = escapeHtml4(tw.toString() + ErrorUtil.formatException(tw));
			return "<pre>" + eText + "</pre>";
		}
		return "";

	}

	private static Object formatWarning(TestScriptDescription descr) {
		AlertCollector collector = descr.getAlertCollector();
		if (collector == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		if(collector.getCount(AlertType.WARNING) > 0) {
			formatAlert(collector, sb, AlertType.WARNING);
		}
		return sb.toString();
	}

    private static void formatAlert(AlertCollector collector, StringBuilder sb, AlertType alertType) {
        Collection<AggregateAlert> aggregatedAlerts = collector.aggregate(alertType);
        sb.append("Found ").append(aggregatedAlerts.size()).append(' ')
            .append(alertType.toString().toLowerCase()).append("(s) during preparing script:");
        for (AggregateAlert aggregatedAlert : aggregatedAlerts) {
            if(sb.length() > MAX_STRING_LENGTH){
                sb.append("<li>...</li>");
                break;
            }
        	sb.append("<li>"+escapeHtml4(aggregatedAlert.toString())+"<br>");
        }
    }

    private static String formatStatusLink(TestScriptDescription descr, String status, String remainTime) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class='eps-result-status' title=\"Status\">");
    	if (hasProblemBlock(descr)) {
            sb.append(
                    "<a class='eps-outer-link eps-ajax-link' href=\"javascript:void(0)\" title='click to show/hide details' onclick=\"toggleDetails('"
                            + BLOCK_ID_PREFIX + descr.getId() + "-details" + "');return false;\" class=\"text-link\">" + status
                            + (remainTime != null ? "(" + remainTime + ")" : "") + "</a>");
    	} else {
            sb.append(status);
    	}
    	sb.append("</div>");
    	return sb.toString();
	}

	private static String formatReportLink(TestScriptDescription descr, ISFContext context) {
        String folderName = descr.getWorkFolder();
        if(!descr.isLocked()) {
			String link = folderName + "/report.html";
            String zipLink = folderName + "/" + folderName + ZipReport.ZIP;
            IWorkspaceDispatcher workspaceDispatcher = context.getWorkspaceDispatcher();

            if(!workspaceDispatcher.exists(FolderType.REPORT, link) && workspaceDispatcher.exists(FolderType.REPORT, zipLink)){
                link =  folderName + "/" + folderName + "/report.html";
            }

            link = ReportServlet.REPORT_URL_PREFIX + "/" + link;
            return "<div title='View report' class='eps-result-report-link'> "
                    + "<a class='eps-outer-link eps-event-link' href=\"report.xhtml?report=" + StringUtil.escapeURL(link)
                    + "\" class=\"text-link\" target=\"_blank\"> Report </a></div>";
		}
		return "";
	}

    private static String formatZipRequestLink(TestScriptDescription descr, ISFContext context ) {
            if (!descr.isLocked() && !context.getEnvironmentManager().getEnvironmentSettings().getReportOutputFormat().isEnableZip()) {
                String link = ReportServlet.REPORT_URL_PREFIX + "/" + StringUtil.escapeURL(descr.getWorkFolder()) + "?action=pack&script_id=" + descr.getId();
                return "<div title='Prepare zip' class='eps-result-zip-link'> "
                        + "<a class='eps-outer-link eps-ajax-link' href=\"javascript:void(0)\" onclick=\"$.ajax({ url: '" + link
                        + "', type: 'GET\'})\" class=\"text-link\">Zip</a></div>";
            }
        return "";
    }

	private static Object formatZipLink(TestScriptDescription descr, ISFContext context) {
		if(!descr.isLocked()) {
			String folderName = descr.getWorkFolder();
            String link = folderName + "/" + folderName + ZipReport.ZIP;

            link =  context.getWorkspaceDispatcher().exists(FolderType.REPORT, link) ||
                    context.getEnvironmentManager().getEnvironmentSettings().getReportOutputFormat().equals(
                    EnvironmentSettings.ReportOutputFormat.FILES) ?
                    ReportServlet.REPORT_URL_PREFIX + "/" + StringUtil.escapeURL(link) + "?action=zip&script_id=" + descr.getId() :
                    ReportServlet.REPORT_URL_PREFIX + "/" + StringUtil.escapeURL(folderName) + ZipReport.ZIP + "?action=zip&script_id=" + descr.getId();

            return "<div title='Download zip' class='eps-result-zip-link-ready'> " + "<a class='eps-outer-link' href=\"" + link
                    + "\" class=\"text-link\">Get Zip<span class='ui-icon ui-icon-arrowthickstop-1-s' style='display:  inline-block; vertical-align: bottom;'></span></a></div>";
		}
		return "";
	}

	private static String formatLogLink(TestScriptDescription descr, ISFContext context) {
		if(!descr.isLocked()) {
		    String folderName = StringUtil.escapeURL(descr.getWorkFolder());

			String log = folderName + "/scriptlog.html";

            String zipLink = folderName + "/" + folderName + ZipReport.ZIP;

            IWorkspaceDispatcher workspaceDispatcher = context.getWorkspaceDispatcher();

            if(!workspaceDispatcher.exists(FolderType.REPORT, log) && workspaceDispatcher.exists(FolderType.REPORT, zipLink)){
                log =  folderName + "/" + folderName + "/scriptlog.html";
            }

            log = ReportServlet.REPORT_URL_PREFIX + "/" + log;

            return "<div title='View log' class='eps-result-log-link'> " + "<a class='eps-outer-link eps-event-link' href=\"" + log
                    + "\" class=\"text-link\" target=\"_blank\">Log</a></div>";
		}
		return "";
	}

	private static String formatProgressBar(int progress, String text) {
		return "<div class=\"ui-progressbar ui-widget ui-widget-content\">\n" +
		"<div class=\"ui-progressbar-value ui-widget-header\" style=\"display:block;width:" + progress + "%; margin:0;\"></div>\n"+
			"<div class=\"ui-progressbar-label\" style=\"display:block; color: black;\">" + text + "</div>\n" +
		"</div>";

	}

	private static String formatPassedFailedColumn(TestScriptDescription descr) {

		if(descr.getContext() == null)
		{
			return "<div class='resultPassedFailed'></div>";
		}

		IScriptProgress progress = descr.getContext().getScriptProgress();

		long passed = progress.getPassed();
		long conditionallyPassed = progress.getConditionallyPassed();
		long failed = progress.getFailed();

		return "<div class='eps-result-passed-failed-info'><div title='passed' class='eps-result-passed-info'><img class='passedFailedImg' src='resources/sf/" + resourceFolderName + "/images/passed.png'/>" + passed +
                "</div><div title='conditionally_passed' class='eps-result-conditionally-passed-info'><img class='passedFailedImg' src='resources/sf/" + resourceFolderName + "/images/conditionally_passed.png'/>" + conditionallyPassed
                +
                "</div><div title='failed' class='eps-result-failed-info'>" +
               " <img class='passedFailedImg' src='resources/sf/" + resourceFolderName + "/images/failed.png' />" + failed + " </div></div>";
	}

	private static String getBlockClassPostfix(TestScriptDescription descr) {
		if(descr.getStatus() == ScriptStatus.INIT_FAILED || descr.getStatus() == ScriptStatus.RUN_FAILED)
			return FAILED_CLASS_POSTFIX;

		if(descr.getStatus() == ScriptStatus.EXECUTED || descr.getStatus() == ScriptStatus.INTERRUPTED) {
			if(descr.getContext().getScriptProgress().getFailed() != 0)
				return SOME_FAILES_CLASS_POSTFIX;
            if (descr.getContext().getScriptProgress().getConditionallyPassed() != 0)
                return SOME_CONDTIONALLY_PASSED_CLASS_POSTFIX;
			return EXECUTED_CLASS_POSTFIX;
		}
		return IN_PROGRESS_CLASS_POSTFIX;
	}

	public static String buildHTMLEvent(TestScriptDescription descr, ISFContext context) {

		switch (descr.getStatus()) {

			case CANCELED:    return buildCanceled(descr);

			case INIT_FAILED:
			case NOT_STARTED:
			case RUN_FAILED:  return buildFailed(descr, context);

                        case INTERRUPTED:
			case EXECUTED:    return buildExecuted(descr, context);

			case NONE:
		}

		switch (descr.getState()) {

			case READY:
				if (!descr.getAutoRun()) {
					return buildPending(descr, context);
				}
			case PREPARING:
			case INITIAL:
            case FINISHED:
			case RUNNING:  return buildInProgress(descr, context);
			case CANCELED: return buildCanceled(descr);
			case PENDING:  return buildPending(descr, context);
			case PAUSED:   return buildPaused(descr, context);
		}

		throw new RuntimeException("Unexpected state or status " + descr.getState() + ", " + descr.getStatus());
	}

	public static String getEmptyMessage() {
		return EMPTY_MESSAGE;
	}

	private static String buildExecuted(TestScriptDescription descr, ISFContext context) {

		String environmentName = descr.getContext().getEnvironmentName();
		String language = Objects.toString(descr.getLanguageURI(), "");
		String range = descr.getRange() == null || descr.getRange().length() == 0 ? "&infin;" : descr.getRange();
        String autoStart = descr.getAutoStart() ? "(A)" : "";

        StringBuilder sb = new StringBuilder();

		sb.append("<div id=\""+BLOCK_ID_PREFIX +descr.getId() + "\" class=\"eps-result-block eps-result-" + getBlockClassPostfix(descr) + "\">");

        sb.append("<div class='eps-result-info-left'>");

            sb.append("<span title='Script title' class=\"eps-script-name\">");
                sb.append(descr.getMatrixFileName());
            sb.append("</span>");

            sb.append("<div class='eps-result-group-id-date'>");
                sb.append("<div title='Script id' class='eps-result-id'> #" + descr.getId() + "</div>");
                sb.append("<div title='Script start date and time' class='eps-result-date-time'>" + dateFormat.format(DateTimeUtility.toLocalDateTime(descr.getTimestamp())) + "</div>");
            sb.append("</div>");

            sb.append("<div class='eps-result-group-env-aml'>");
                sb.append("<div class='eps-result-environment-name' title='Environment name'> " + environmentName + " <span title='Auto start' class='isAutoStartEnabled'>" + autoStart + "</span></div>");
                sb.append("<div class='eps-result-aml-version' title='Language'> " + language + "</div>");
				sb.append("<div class='eps-result-range' title='Range'>" + range + "</div>");
            sb.append("</div>");

        sb.append("</div>");

        sb.append("<div class='eps-result-info-right'>");

            sb.append(formatStatusLink(descr, descr.getStatus().getTitle(), null));
			sb.append(formatPassedFailedColumn(descr));
			sb.append(formatReportLink(descr, context));
			sb.append(formatLogLink(descr, context));
			sb.append(formatZipRequestLink(descr, context));
			sb.append(formatZipLink(descr, context));
			sb.append("<div class='eps-res-checkbox-wrapper'><input type='checkbox' id='check-" + descr.getId() + "' class='eps-res-checkbox'/><label for='check-" + descr.getId() + "'>&nbsp;</label></div>");


        sb.append("</div>");

        sb.append(formatProblemBlock(descr));

		sb.append("</div>");

		return sb.toString();
	}

	private static String buildCanceled(TestScriptDescription descr) {

		String environmentName = descr.getContext().getEnvironmentName();
		String language = Objects.toString(descr.getLanguageURI(), "");
		String range = descr.getRange() == null || descr.getRange().length() == 0 ? "&infin;" : descr.getRange();
        String autoStart = descr.getAutoStart() ? "(A)" : "";

		StringBuilder sb = new StringBuilder();

		sb.append("<div id=\""+BLOCK_ID_PREFIX +descr.getId() + "\" class=\"eps-result-block eps-result-" + CANCELED_CLASS_POSTFIX + "\">");

        sb.append("<div class='eps-result-info-left'>");

            sb.append("<span title='Script title' class=\"eps-script-name\">");
                sb.append(descr.getMatrixFileName());
            sb.append("</span>");

            sb.append("<div class='eps-result-group-id-date'>");
                sb.append("<div title='Script id' class='eps-result-id'> #" + descr.getId() + "</div>");
                sb.append("<div title='Script start date and time' class='eps-result-date-time'>" + dateFormat.format(DateTimeUtility.toLocalDateTime(descr.getTimestamp())) + "</div>");
            sb.append("</div>");

            sb.append("<div class='eps-result-group-env-aml'>");
            	sb.append("<div class='eps-result-environment-name' title='Environment name'> " + environmentName + " <span title='Auto start' class='isAutoStartEnabled'>" + autoStart + "</span></div>");
                sb.append("<div class='eps-result-aml-version' title='Language'> " + language + "</div>");
				sb.append("<div class='eps-result-range' title='Range'>" + range + "</div>");
            sb.append("</div>");

        sb.append("</div>");

        sb.append("<div class='eps-result-info-right'>");
            sb.append(formatStatusLink(descr, descr.getStatus().getTitle(), null));
			sb.append("<div class='eps-res-checkbox-wrapper'><input type='checkbox' id='check-" + descr.getId() + "' class='eps-res-checkbox'/><label for='check-" + descr.getId() + "'>&nbsp;</label></div>");
		sb.append("</div>");

		sb.append(formatProblemBlock(descr));

		sb.append("</div>");

		return sb.toString();
	}

	private static String buildFailed(TestScriptDescription descr, ISFContext context) {

		String environmentName = descr.getContext().getEnvironmentName();
		String language = Objects.toString(descr.getLanguageURI(), "");
		String range = descr.getRange() == null || descr.getRange().length() == 0 ? "&infin;" : descr.getRange();
        String autoStart = descr.getAutoStart() ? "(A)" : "";

		StringBuilder sb = new StringBuilder();

		sb.append("<div id=\""+BLOCK_ID_PREFIX +descr.getId() + "\" class=\"eps-result-block eps-result-" + FAILED_CLASS_POSTFIX + "\">");

        sb.append("<div class='eps-result-info-left'>");

            sb.append("<span title='Script title' class=\"eps-script-name\">");
                sb.append(descr.getMatrixFileName());
            sb.append("</span>");

            sb.append("<div class='eps-result-group-id-date'>");
                sb.append("<div title='Script id' class='eps-result-id'> #" + descr.getId() + "</div>");
                sb.append("<div title='Script start date and time' class='eps-result-date-time'>" + dateFormat.format(DateTimeUtility.toLocalDateTime(descr.getTimestamp())) + "</div>");
            sb.append("</div>");

            sb.append("<div class='eps-result-group-env-aml'>");
                sb.append("<div class='eps-result-environment-name' title='Environment name'> " + environmentName + " <span title='Auto start' class='isAutoStartEnabled'>" + autoStart + "</span></div>");
                sb.append("<div class='eps-result-aml-version' title='Language'> " + language + "</div>");
				sb.append("<div class='eps-result-range' title='Range'>" + range + "</div>");
            sb.append("</div>");

        sb.append("</div>");

        sb.append("<div class='eps-result-info-right'>");
            sb.append(formatStatusLink(descr, descr.getStatus().getTitle(), null));
            sb.append(formatPassedFailedColumn(descr));
            sb.append(formatReportLink(descr, context));
            sb.append(formatZipRequestLink(descr, context));
            sb.append(formatZipLink(descr, context));
			sb.append("<div class='eps-res-checkbox-wrapper'><input type='checkbox' id='check-" + descr.getId() + "' class='eps-res-checkbox'/><label for='check-" + descr.getId() + "'>&nbsp;</label></div>");
		sb.append("</div>");

        sb.append(formatProblemBlock(descr));

		sb.append("</div>");

		return sb.toString();
	}

	private static String buildInProgress(TestScriptDescription descr, ISFContext context) {

		String environmentName = descr.getContext().getEnvironmentName();
		String language = Objects.toString(descr.getLanguageURI(), "");
		String range = descr.getRange() == null || descr.getRange().isEmpty() ? "&infin;" : descr.getRange();
        String autoStart = descr.getAutoStart() ? "(A)" : "";

		StringBuilder sb = new StringBuilder();
		try {
			String progressBlock;
			String progressText;
			if(descr.getState() == ScriptState.RUNNING) {
				int finishedCount = descr.getContext().getScriptProgress().getExecutedTC();
				int loadedTC = descr.getContext().getScriptProgress().getLoaded();
				int progress = (int) Math.round(100.0 * finishedCount / loadedTC);
				progressText = finishedCount + " of " + loadedTC + " (" + progress + "%)";

				String report = ReportServlet.REPORT_URL_PREFIX + "/" + StringUtil.escapeURL(descr.getWorkFolder()) + "/report.html";
				progressBlock = "<a href=\"report.xhtml?report="+ report +"\">" + formatProgressBar(progress, progressText) + "</a>";
			}
			else {
				String progress = descr.getProgress();
                progressBlock = "".equals(progress) ? formatProgressBar(0, "0%") :
                        formatProgressBar(Integer.parseInt(progress), progress + "%" );
            }

			sb.append("<div id=\""+BLOCK_ID_PREFIX +descr.getId() + "\" class=\"eps-result-block eps-result-" + IN_PROGRESS_CLASS_POSTFIX + "\">");

            sb.append("<div class='eps-result-info-left'>");

                sb.append("<span title='Script title' class=\"eps-script-name\">");
                    sb.append(descr.getMatrixFileName());
                sb.append("</span>");

                sb.append("<div class='eps-result-group-id-date'>");
                    sb.append("<div title='Script id' class='eps-result-id'> #" + descr.getId() + "</div>");
                    sb.append("<div title='Script start date and time' class='eps-result-date-time'>" + dateFormat.format(DateTimeUtility.toLocalDateTime(descr.getTimestamp())) + "</div>");
                sb.append("</div>");

                sb.append("<div class='eps-result-group-env-aml'>");
                    sb.append("<div class='eps-result-environment-name' title='Environment name'> " + environmentName + " <span title='Auto start' class='isAutoStartEnabled'>" + autoStart + "</span></div>");
                    sb.append("<div class='eps-result-aml-version' title='Language'> " + language + "</div>");
					sb.append("<div class='eps-result-range' title='Range'>" + range + "</div>");
                sb.append("</div>");

            sb.append("</div>");

            sb.append("<div class='eps-result-info-right'>");
                sb.append(formatStatusLink(descr, descr.getState().name(), null));
                sb.append(formatPassedFailedColumn(descr));
                sb.append("<div class='eps-result-progress-bar'> " + progressBlock  + "</div>");
                sb.append("<div class='eps-result-pause-stop-link'> " + formatPauseScriptLink(descr.getId()) + formatStopScriptLink(descr.getId())  + "</div>");
                sb.append(formatLogLink(descr, context));
            sb.append("</div>");

            sb.append(formatProblemBlock(descr));

		    sb.append("</div>");
		}
		catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		return sb.toString();

	}

	private static String formatStopScriptLink(long id) {
		return "<button  class=\"ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only\" onclick=\"stopScript([{name:'id', value:'"+id+"'}]);return false;\" title=\"Stop script\" role=\"button\">" +
				"<span class=\"ui-button-icon-left ui-icon ui-icon-stop\"></span><span class=\"ui-button-text\">ui-button</span></button>";
	}

	private static String formatCompileScriptLink(long id) {
		return "<button  class=\"ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only\" onclick=\"compileScript([{name:'id', value:'"+id+"'}]);return false;\" title=\"Compile script\" role=\"button\">" +
				"<span class=\"ui-button-icon-left ui-icon ui-icon-suitcase\"></span><span class=\"ui-button-text\">ui-button</span></button>";
	}

	private static String formatRunCompileScriptLink(long id) {
		return "<button  class=\"ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only\" onclick=\"runCompiledScript([{name:'id', value:'"+id+"'}]);return false;\" title=\"Run script\" role=\"button\">" +
				"<span class=\"ui-button-icon-left ui-icon ui-icon-play\"></span><span class=\"ui-button-text\">ui-button</span></button>";
	}

	private static String formatResumeScriptLink(long id) {
		return "<button  class=\"ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only\" onclick=\"resumeScript([{name:'id', value:'"+id+"'}]);return false;\" title=\"Resume script\" role=\"button\">" +
				"<span class=\"ui-button-icon-left ui-icon ui-icon-seek-next\"></span><span class=\"ui-button-text\">ui-button</span></button>";
	}

	private static String formatNextStepScriptLink(long id) {
		return "<button  class=\"ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only\" onclick=\"nextStep([{name:'id', value:'"+id+"'}]);return false;\" title=\"Next Step\" role=\"button\">" +
				"<span class=\"ui-button-icon-left ui-icon ui-icon-seek-end\"></span><span class=\"ui-button-text\">ui-button</span></button>";
	}

	private static String formatPauseScriptLink(long id) {
		return "<button  class=\"ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only\" onclick=\"pauseScript([{name:'id', value:'"+id+"'}]);return false;\" title=\"Pause\" role=\"button\">" +
				"<span class=\"ui-button-icon-left ui-icon ui-icon-pause\"></span><span class=\"ui-button-text\">ui-button</span></button>";
	}

	private static String buildPending(TestScriptDescription descr, ISFContext context) {

		String environmentName = descr.getContext().getEnvironmentName();
		String language = Objects.toString(descr.getLanguageURI(), "");
        String autoStart = descr.getAutoStart() ? "(A)" : "";

		StringBuilder sb = new StringBuilder();
		try {
			String progressBlock;
			String progressText;
			if(descr.getState() == ScriptState.RUNNING) {
				int finishedCount = descr.getContext().getScriptProgress().getExecutedTC();
				int loadedTC = descr.getContext().getScriptProgress().getLoaded();
				int progress = (int) Math.round(100.0 * finishedCount / loadedTC);
				progressText = finishedCount + " of " + loadedTC + " (" + progress + "%)";

				String report = ReportServlet.REPORT_URL_PREFIX + "/" + StringUtil.escapeURL(descr.getWorkFolder())+ "/report.html";
				progressBlock ="<a href=\"report.xhtml?report="+ report +"\">" + formatProgressBar(progress, progressText) + "</a>";
			}
			else {
				String progress = descr.getProgress();
				progressBlock = "".equals(progress) ? formatProgressBar(0, "0%") :
						formatProgressBar(Integer.parseInt(progress), progress + "%" );
			}

			sb.append("<div id=\""+BLOCK_ID_PREFIX +descr.getId() + "\" class=\"eps-result-block eps-result-" + IN_PROGRESS_CLASS_POSTFIX + "\">");

            sb.append("<div class='eps-result-info-left'>");

                sb.append("<span title='Script title' class=\"eps-script-name\">");
                    sb.append(descr.getMatrixFileName());
                sb.append("</span>");

                sb.append("<div class='eps-result-group-id-date'>");
                    sb.append("<div title='Script id' class='eps-result-id'> #" + descr.getId() + "</div>");
                    sb.append("<div title='Script start date and time' class='eps-result-date-time'>" + dateFormat.format(DateTimeUtility.toLocalDateTime(descr.getTimestamp())) + "</div>");
                sb.append("</div>");

                sb.append("<div class='eps-result-group-env-aml'>");
                    sb.append("<div class='eps-result-environment-name' title='Environment name'> " + environmentName + " <span title='Auto start' class='isAutoStartEnabled'>" + autoStart + "</span></div>");
                    sb.append("<div class='eps-result-aml-version' title='Language'> " + language + "</div>");
                sb.append("</div>");

            sb.append("</div>");

            sb.append("<div class='eps-result-info-right'>");
                sb.append(formatStatusLink(descr, descr.getStatus().getTitle(), null));
                sb.append(formatPassedFailedColumn(descr));
                sb.append("<div class='eps-result-progress-bar'> " + progressBlock  + "</div>");
                sb.append("<div class='eps-result-pause-stop-link'> " + formatPauseScriptLink(descr.getId()) + formatStopScriptLink(descr.getId())  + "</div>");
                sb.append(formatLogLink(descr, context));

                if (descr.getState() == ScriptState.PENDING) {
                    sb.append("<div class='eps-result-compile-link'> " + formatCompileScriptLink(descr.getId())  + "</div>");
                } else {
                    sb.append("<div class='eps-result-run-link'> " + formatRunCompileScriptLink(descr.getId())  + "</div>");
                }

                sb.append(formatLogLink(descr, context));

            sb.append("</div>");

            sb.append(formatProblemBlock(descr));

			sb.append("</div>");
		}
		catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		return sb.toString();

	}


	private static String buildPaused(TestScriptDescription descr, ISFContext context) {

		String environmentName = descr.getContext().getEnvironmentName();
		String language = Objects.toString(descr.getLanguageURI(), "");
        String autoStart = descr.getAutoStart() ? "(A)" : "";

		StringBuilder sb = new StringBuilder();
		try {
			String progressBlock;
			String progressText;
			int finishedCount = descr.getContext().getScriptProgress().getExecutedTC();
			int loadedTC = descr.getContext().getScriptProgress().getLoaded();
			int progress = (int) Math.round(100.0 * finishedCount / loadedTC);
			progressText = finishedCount + " of " + loadedTC + " (" + progress + "%)";

			String report = ReportServlet.REPORT_URL_PREFIX + "/" + StringUtil.escapeURL(descr.getWorkFolder()) + "/report.html";
			progressBlock ="<a href=\"report.xhtml?report="+ report +"\">" + formatProgressBar(progress, progressText) + "</a>";

			String postfix = descr.getPauseTimeout() == 0 ? PERMANENT_PAUSED_CLASS_POSTFIX : TIME_PAUSED_CLASS_POSTFIX;

			String remainTime = "&infin;";
			if (descr.getPauseTimeout() > 0) {
				remainTime = String.valueOf(descr.getPauseTimeout()/1000);
			}

			sb.append("<div id=\""+BLOCK_ID_PREFIX +descr.getId() + "\" class=\"eps-result-block eps-result-" + postfix + "\">");

            sb.append("<div class='eps-result-info-left'>");

                sb.append("<span title='Script title' class=\"eps-script-name\">");
                    sb.append(descr.getMatrixFileName());
                sb.append("</span>");

                sb.append("<div class='eps-result-group-id-date'>");
                    sb.append("<div title='Script id' class='eps-result-id'> #" + descr.getId() + "</div>");
                    sb.append("<div title='Script start date and time' class='eps-result-date-time'>" + dateFormat.format(DateTimeUtility.toLocalDateTime(descr.getTimestamp())) + "</div>");
                sb.append("</div>");

                sb.append("<div class='eps-result-group-env-aml'>");
                    sb.append("<div class='eps-result-environment-name' title='Environment name'> " + environmentName + " <span title='Auto start' class='isAutoStartEnabled'>" + autoStart + "</span></div>");
                    sb.append("<div class='eps-result-aml-version' title='Language'> " + language + "</div>");
                sb.append("</div>");

            sb.append("</div>");

            sb.append("<div class='eps-result-info-right'>");

                sb.append(formatStatusLink(descr, descr.getState().name(), remainTime));
                sb.append(formatPassedFailedColumn(descr));
                sb.append("<div class='eps-result-progress-bar'> " + progressBlock  + "</div>");
                sb.append("<div class='eps-result-pause-stop-link'> " + formatPauseScriptLink(descr.getId()) + formatStopScriptLink(descr.getId())  + "</div>");
                sb.append("<div class='eps-links'> " + formatResumeScriptLink(descr.getId()) + formatNextStepScriptLink(descr.getId()) + formatStopScriptLink(descr.getId()) + "</div>");

                sb.append(formatLogLink(descr, context));

            sb.append("</div>");


            sb.append("<div id=\""+ BLOCK_ID_PREFIX + descr.getId() + "-details" +"\" class=\"eps-pause-details\" style=\"display: block;\">");
                sb.append("<span class=\"eps-script-says\">The script says:</span>");
                sb.append(buildPausedDescriptionAsTable(descr.getPauseReason()));
            sb.append("</div>");

            sb.append(formatProblemBlock(descr));

			sb.append("</div>");
		}
		catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		return sb.toString();

	}

	public static String buildPausedDescriptionAsTable(String description)
	{
		String[] descArray = description.split("\\n");
		StringBuilder result = new StringBuilder();
		boolean isTableOpened = false;

		for(String line : descArray)
		{
			boolean isTable = line.indexOf('|') > -1;

			if(isTable)
			{
				String [] cells = line.split("\\|");


				if(!isTableOpened)
				{
					result.append("<table class='eps-pause-result-table'>");
					isTableOpened = true;
				}


				StringBuilder row = new StringBuilder("<tr>");
				for(String cell: cells)
				{
					row.append("<td>" + cell + "</td>");
				}
				row.append("</tr>");
				result.append(row);
			}
			else
			{
				if(isTableOpened)
				{
					result.append("</table>");
					isTableOpened = false;
				}

				result.append("<div class='eps-result-description-as-text'>" + line + "</div>");
			}

		}

		if(isTableOpened)
		{
			result.append("</table>");
			isTableOpened = false;
		}

		return result.toString();
	}


    private static boolean hasProblem(TestScriptDescription descr) {
    	return descr.getCause() != null;
    }

    private static boolean hasWarning(TestScriptDescription descr) {
    	return descr.getAlertCollector() != null && descr.getAlertCollector().getCount(AlertType.WARNING) != 0;
    }

    private static boolean hasProblemBlock(TestScriptDescription descr) {
    	return hasProblem(descr) || hasWarning(descr);
    }

}
