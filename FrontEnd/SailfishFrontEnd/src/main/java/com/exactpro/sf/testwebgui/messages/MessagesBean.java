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
package com.exactpro.sf.testwebgui.messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.data.SortEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.download.AppZip;

@ManagedBean(name="messagesBean")
@SessionScoped
@SuppressWarnings("serial")
public class MessagesBean implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(MessagesBean.class);

    private Boolean showAdmin = false;

    private List<String> selectedOptions;

    private Map<String, String> options;

    private Map<String, String> columns_name;

    private String whereStatement;

    private String showCount = "100";

    //load from js, look builder-basic.js
    private String queries = "";

    private List<String> storedQueries = Collections.emptyList();

    private MessagesLazyModel messageLazyModel;

	private boolean includeRawMessage;

    private int rowsPerPage;
    private String timeFilter;
    private String nameFilter;
    private String fromFilter;
    private String toFilter;
    private String contentFilter;

    private String sortField;
    private String sortOrder;

    private int limitNumber;

    private boolean highlightEnabled = true;
    private boolean humanSelected = false;

	public MessagesBean() {

		logger.debug("MessagesBean creation started.");

		options = new LinkedHashMap<>();
		options.put("Timestamp", "timestamp");
		options.put("Name","name" );
		options.put("From","from" );
		options.put("To","to");
		options.put("Content", "content");

		columns_name = new LinkedHashMap<>();
		columns_name.put("timestamp", "Timestamp");
		columns_name.put("name","Name" );
		columns_name.put("from","From" );
		columns_name.put("to","To");
		columns_name.put("content", "Content");

		selectedOptions = new ArrayList<>();
		selectedOptions.add("name");
		selectedOptions.add("timestamp");
		selectedOptions.add("from");
		selectedOptions.add("to");
		selectedOptions.add("content");

        rowsPerPage = 15;
        sortOrder = "ascending";
	}

	public void preRenderView() {
		if (this.messageLazyModel == null) {
			this.messageLazyModel = new MessagesLazyModel();
		}
	}

	public void getByQuery() {

		logger.debug("getByQuery invoked {} whereStatement[{}]", getUser(), whereStatement);
		String whereStatementCleared = removeComments(whereStatement);

		if(!checkLimit(showCount)) {
			BeanUtil.addErrorMessage("Limit error", "Limit must be a positive integer value");
			return;
		}
		this.limitNumber = Integer.parseInt(showCount);

		try {
		    IMessageStorage messageStorage = BeanUtil.getSfContext().getMessageStorage();

			Iterable<MessageRow> messageList = messageStorage.getMessages(0, 1, whereStatementCleared);
			Iterator<MessageRow> messagesIterator = messageList.iterator();

			String firstMsgId = null;
			if (!messagesIterator.hasNext()) {
				firstMsgId = String.valueOf(Long.MAX_VALUE);
			} else {
				firstMsgId = messagesIterator.next().getID();
			}

			this.messageLazyModel.reinit(firstMsgId, limitNumber, whereStatementCleared);

	    } catch (StorageException e) {
		    BeanUtil.addErrorMessage("Query error", e.getCause().getMessage());
	    } catch (IllegalArgumentException e) {
		    BeanUtil.addErrorMessage("Limit error", e.getMessage());
	    } catch (NullPointerException e) {
		    BeanUtil.addErrorMessage("SFContext error", "SFContext is not initialized correctly. See log file for details.");
	    }
	}

	private boolean checkLimit(String limit) {

		int number;

		try {
			number = Integer.parseInt(limit);
		} catch (NumberFormatException e) {
			return false;
		}

		if(number < 1) {
			return false;
		}

		return true;
	}

	private static String removeComments(String sql) {

		String cleared = sql;
		int beginPos;
		int endPos;

		while ((beginPos = cleared.indexOf("/*")) >= 0) {
			if ((endPos = cleared.indexOf("*/")) > beginPos+1) {
				cleared = cleared.substring(0, beginPos) + cleared.substring(endPos+2);
			} else {
				break;
			}
		}

    	return cleared;

	}

	//TODO: This method should be load and save all messages instead of one page
    public StreamedContent getResultsInCSV() {
		logger.info("getResultsInCSV invoked {}", getUser());

        if(selectedOptions.size() == 0 && !includeRawMessage) {
            BeanUtil.addWarningMessage("No columns selected", "Select at least one column");
            return null;
        }

    	if(this.messageLazyModel.getRowCount() != 0) {
			try {
				String fileNameCsv = "query_result_" + UUID.randomUUID().toString();
				File temp = File.createTempFile(fileNameCsv, ".csv");
				CsvMessageWriter writer = new CsvMessageWriter(selectedOptions, includeRawMessage);
				writer.writeAndClose(temp, this.messageLazyModel.load(0, this.messageLazyModel.getRowCount()));

				File zipFile = new File(temp.getParent(), "messages.zip");
				AppZip appZip = new AppZip();
				appZip.generateFileList(temp);
				appZip.zipIt(zipFile.getAbsoluteFile().toString());

				InputStream stream = new FileInputStream(zipFile);
				return new DefaultStreamedContent(stream, "application/zip", "Messages.zip");
			} catch (IOException e) {
				logger.error("Could not export messages", e);
				BeanUtil.addErrorMessage("Could not export messages", e.getMessage());
				return null;
			}
    	} else {
			BeanUtil.addWarningMessage("No data to export", "Table messages is empty");
		}
    	return null;
    }

    public void onColumnChange(ValueChangeEvent e) {
		logger.debug("onColumnChange invoked {} ValueChangeEvent[{}]", getUser(), e);
        if(e.getNewValue() instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> value = (List<String>)e.getNewValue();
            selectedOptions = value;
        }
    }

	public boolean isRendered(String column) {
		boolean result = selectedOptions.contains(column);
		return result;
	}

    public void saveRows(){
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, String> map = context.getExternalContext().getRequestParameterMap();
        String rowsStr = (String) map.get("rowsPerPage");
        rowsPerPage = Integer.parseInt(rowsStr);
    }

    public void saveFilter() {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, String> map = context.getExternalContext().getRequestParameterMap();
        String column = (String) map.get("filterColumn");
        String value = (String) map.get("filterValue");

        if(column.equals("time")) {
            timeFilter = value;
        } else if(column.equals("name")) {
            nameFilter = value;
        } else if(column.equals("from")) {
            fromFilter = value;
        } else if(column.equals("to")) {
            toFilter = value;
        } else if(column.equals("content")) {
            contentFilter = value;
        }

    }

    public void onSort(SortEvent event) {
        String column = event.getSortColumn().getClientId();
        this.sortField = column;
        this.sortOrder = event.isAscending() ? "ascending" : "descending";
    }

    public void loadSort() {

        DataTable dataTable = (DataTable) FacesContext.getCurrentInstance().getViewRoot().findComponent("form:table");

        if(this.sortField == null || this.sortField.equals("")) {
            dataTable.setValueExpression("sortBy", null);
            return;
        }

        String elRaw = null;
        if(this.sortField.equals("table:name")) {
            elRaw = "#{message.name}";
        } else if(this.sortField.equals("table:from")) {
            elRaw = "#{message.from}";
        } else if(this.sortField.equals("table:to")) {
            elRaw = "#{message.to}";
        } else if(this.sortField.equals("table:content")) {
            elRaw = "#{message.content}";
        } else if(this.sortField.equals("table:timestamp")) {
            elRaw = "#{message.timestamp}";
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ELContext elContext = facesContext.getELContext();
        ExpressionFactory elFactory = facesContext.getApplication().getExpressionFactory();
        ValueExpression valueExpresion = elFactory.createValueExpression(elContext, elRaw, Date.class);

        dataTable.setSortOrder(this.sortOrder);
        dataTable.setValueExpression("sortBy", valueExpresion);
    }

    public void removeFilters() {
        this.timeFilter = "";
        this.nameFilter = "";
        this.fromFilter = "";
        this.toFilter = "";
        this.contentFilter = "";
        this.sortField = "";
    }

    public void queriesToList(ValueChangeEvent event){
        queries = (String) event.getNewValue();
        if (queries.length() == 0)
            storedQueries = Collections.emptyList();
        else
            storedQueries = Arrays.asList(queries.split(";"));
    }

    public List<String> completeText(String query) {
        if(query.length() == 0){
            return new ArrayList<>(storedQueries);
        }
        List<String> results = new ArrayList<>();
        for(String storedQuery : storedQueries){
            if(storedQuery.startsWith(query)){
                results.add(storedQuery);
            }
        }
        return results;
    }

	public Boolean getShowAdmin() {
		return showAdmin;
	}

	public void setShowAdmin(Boolean showAdmin) {
		this.showAdmin = showAdmin;
	}

	public String getWhereStatement() {
		return whereStatement;
	}

	public void setWhereStatement(String whereStatement) {
		this.whereStatement = whereStatement;
	}

	public void setSelectedOptions(List<String> selectedOptions) {
		this.selectedOptions = selectedOptions;
	}

	public List<String> getSelectedOptions() {
		return selectedOptions;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public String getShowCount() {
		return showCount;
	}

	public void setShowCount(String showCount) {
		this.showCount = showCount;
	}

	public MessagesLazyModel getMessageLazyModel() {
		return messageLazyModel;
	}

	protected String getUser(){
		return System.getProperty("user.name");
	}

	public boolean isIncludeRawMessage() {
		return includeRawMessage;
	}

	public void setIncludeRawMessage(boolean includeRawMessage) {
		this.includeRawMessage = includeRawMessage;
	}

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
    }

    public String getTimeFilter() {
        return timeFilter;
    }

    public void setTimeFilter(String timeFilter) {
        this.timeFilter = timeFilter;
    }

    public String getNameFilter() {
        return nameFilter;
    }

    public void setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
    }

    public String getFromFilter() {
        return fromFilter;
    }

    public void setFromFilter(String fromFilter) {
        this.fromFilter = fromFilter;
    }

    public String getToFilter() {
        return toFilter;
    }

    public void setToFilter(String toFilter) {
        this.toFilter = toFilter;
    }

    public String getContentFilter() {
        return contentFilter;
    }

    public void setContentFilter(String contentFilter) {
        this.contentFilter = contentFilter;
    }

    public boolean isHighlightEnabled() {
        return highlightEnabled;
    }

    public void setHighlightEnabled(boolean highlightEnabled) {
        this.highlightEnabled = highlightEnabled;
    }

    public String getQueries() {
        return queries;
    }

    public void setQueries(String queries) {
        this.queries = queries;
    }

    public boolean isHumanSelected() {
        return humanSelected;
    }

    public void setHumanSelected(boolean humanSelected) {
        this.humanSelected = humanSelected;
    }
}
