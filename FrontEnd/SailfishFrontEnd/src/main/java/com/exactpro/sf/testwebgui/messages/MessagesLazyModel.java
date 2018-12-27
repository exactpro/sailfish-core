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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.util.JsonMessageConverter;
import com.exactpro.sf.testwebgui.BeanUtil;

public class MessagesLazyModel extends LazyDataModel<MessageAdapter> {

	private static final Logger logger = LoggerFactory.getLogger(MessagesLazyModel.class);

	private static final long serialVersionUID = -4428885446749217579L;

//    private final List<MessageAdapter> messages;


//    private boolean inited = false;
    private int numberOfRecords;
    private String commonWhereStatement = "";

    private int lastFirst;
    private int lastPageSize;
    private String lastSortField;
	private SortOrder lastSortOrder;
	private List<MessageAdapter> lastMessages;
	private String lastWhereStatement;

    public MessagesLazyModel() {
//        this.messages = new ArrayList<>();
    }

    public void reinit(String firstMsgId, int limitNumber, String whereStatement) {
    	this.numberOfRecords = limitNumber;
    	this.commonWhereStatement = buildCommonWhereSatment(firstMsgId, whereStatement);

    	this.lastSortField = "";
    	this.lastSortOrder = SortOrder.DESCENDING;
        this.setRowIndex(0);
    }

    @Override
    public List<MessageAdapter> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {

    	if (checkAndUpdateLoadParameters(first, pageSize, sortField, sortOrder)) {

	    	String currentWhereStatment = this.commonWhereStatement;

        	this.lastWhereStatement = currentWhereStatment;
        	this.lastMessages = load(first, pageSize);

        	this.setRowCount(this.lastMessages.isEmpty() ? 0 : this.numberOfRecords);

//    	    RequestContext.getCurrentInstance().addCallbackParam("totalRecords", 0);

        	this.setRowIndex(0);
    	}

    	return this.lastMessages;
    }

    public List<MessageAdapter> load(int first, int pageSize) {
    	List<MessageAdapter> messages = Collections.emptyList();

    	try {
    		logger.info("Start loading offset {} length {} query {}", first, pageSize, this.lastWhereStatement);

	    	int limit = Math.min(this.numberOfRecords, pageSize);

	        messages = loadMessages(BeanUtil.getSfContext().getMessageStorage().getMessages(first, limit, this.lastWhereStatement));

		} catch (StorageException e) {
	        BeanUtil.addErrorMessage("Query error", e.getCause().getMessage());
	    } catch (IllegalArgumentException e) {
	        BeanUtil.addErrorMessage("Limit error", e.getMessage());
	    } catch (NullPointerException e) {
	        BeanUtil.addErrorMessage("SFContext error", "SFContext is not initialized correctly. See log file for details.");
		} catch (Exception e) {
			if(e.getCause() != null) {
				BeanUtil.addErrorMessage("Error", e.getCause().getMessage());
			}
	    }

	    Set<String> fields = new TreeSet<String>();
	    if (this.lastSortField != null) {
	        fields.add(this.lastSortField);
	    } else {
	    	fields.add("timestamp");
	    }

	    Collections.sort(messages, new MessageSorter(fields, this.lastSortOrder));

	    logger.info("End loading offset {} length {} query {}", first, pageSize, this.lastWhereStatement);

	    return messages;
    }

    private List<MessageAdapter> loadMessages(Iterable<MessageRow> list) {
        List<MessageAdapter> messages = new ArrayList<>();

    	for (MessageRow message : list) {
            MessageAdapter messageAdapter = new MessageAdapter(message);
            if (StringUtils.startsWith(messageAdapter.getHumanReadable(), "{")) { //TODO: Added for backward compatibility, delete after a few months 
                try {
                    messageAdapter.setHumanReadable(JsonMessageConverter.fromJsonToHuman(
                            messageAdapter.getHumanReadable(), BeanUtil.getSfContext().getDictionaryManager(), true).toString());
                } catch (Exception e) {
                    logger.warn("Can not parse json message [{}]", message.getJson(), e);
                }
            }
            messages.add(messageAdapter);
        }

       return messages;
    }

    /**
     *
     * @param first
     * @param pageSize
     * @param sortField
     * @param sortOrder
     * @return <code>true</code> if and only if any parameter was
     *          changed; <code>false</code> otherwise
     */
    private boolean checkAndUpdateLoadParameters(int first, int pageSize, String sortField, SortOrder sortOrder) {
    	if (this.lastFirst != first || this.lastPageSize != pageSize ||
    			!StringUtils.equals(this.lastSortField, sortField) ||
                (this.lastSortOrder != sortOrder && sortField != null)) {
    				this.lastFirst = first;
    				this.lastPageSize = pageSize;
    				this.lastSortField = sortField;
    				this.lastSortOrder = sortField != null ? sortOrder : SortOrder.DESCENDING;

    				return true;
    			}
    	return false;
    }

    private String buildCommonWhereSatment(String firstMsgId, String whereStatement) {
		StringBuilder builder = new StringBuilder();
		if (!whereStatement.isEmpty()) {
			builder.append("( ").append(whereStatement).append(" ) AND ");
		}
		builder.append("( msg.id <= ").append(firstMsgId).append(" ) ");

		return builder.toString();
	}
}
