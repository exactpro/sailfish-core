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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.aml.reader.struct.ExecutionMode;
import com.exactpro.sf.embedded.statistics.StatisticsService;

/**
 * Represent a single test case from matrix.
 * @author dmitry.guriev
 *
 */
@SuppressWarnings("serial")
public class AMLTestCase implements ITestCase, Cloneable, Serializable {

    private final List<AMLAction> actions = new ArrayList<>();
    private String reference;
	private long line;
	private String id;
    private ExecutionMode executionMode = ExecutionMode.EXECUTABLE;
	private String description = "";
	private int execOrder;
	private int matrixOrder;
    private boolean failOnUnexpectedMessage;
    private boolean addToReport = true;
    private AMLBlockType blockType = AMLBlockType.TestCase;
	private final long uid;
	private final int hash;

	public AMLTestCase(String id, long uid, int hash) {
		this.id = id;
        this.uid = uid;
		this.hash = hash;
	}

	public void addAction(int index, AMLAction action) throws AMLException {
	    try {
    	    if(action.hasReference()) {
    	        String ref = action.getReference();
    	        AMLAction a = findActionByRef(ref);

    	        if(a != null) {
    	            throw new AMLException("Duplicated reference found in lines: " + a.getLine() + ", " + action.getLine() + ": '" + ref + "'");
    	        }
    	    }

    	    if(action.hasReferenceToFilter()) {
                String ref = action.getReferenceToFilter();
                AMLAction a = findActionByRef(ref);

                if(a != null) {
                    throw new AMLException("Duplicated reference found in lines: " + a.getLine() + ", " + action.getLine() + ": '" + ref + "'");
                }
    	    }
	    } finally {
            actions.add(index, action);
	    }
	}

	public void addAction(AMLAction action) throws AMLException{
        addAction(actions.size(), action);
	}

	@Override
    public List<AMLAction> getActions() {
        return actions;
	}

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public boolean hasReference() {
        return StringUtils.isNotEmpty(reference);
	}

	@Override
    public AMLAction getAction(int i) {
        return actions.get(i);
	}

	public void removeAction(int i) {
        actions.remove(i);
	}

	public void addAllActions(int i, List<AMLAction> actions) {
		this.actions.addAll(i, actions);
	}

	public long getLine() {
        return line;
	}

	public void setLine(long line) {
		this.line = line;
	}

	@Override
    public String getDescription() {
        return description;
	}

	@Override
    public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Return action with specified reference.
	 * @param lineRef reference
	 * @return action in test case or null if no reference found
	 */
	@Override
    public AMLAction findActionByRef(String lineRef)
	{
        for(AMLAction action : actions) {
            if((action.getExecutionMode() == ExecutionMode.EXECUTABLE || action.getExecutionMode() == ExecutionMode.OPTIONAL)
                    && (lineRef.equals(action.getReference())
                    || lineRef.equals(action.getReferenceToFilter()))) {
                return action;
            }
        }

        return null;
	}

	@Override
    public void setId(String id) {
		this.id = id;
	}

	@Override
    public String getId() {
		return id;
	}

	@Override
    public void setExecutionMode(ExecutionMode executionMode) {
		this.executionMode = executionMode;
	}

	@Override
    public ExecutionMode getExecutionMode() {
        return executionMode;
	}

	public boolean isEmpty() {
        return actions.isEmpty();
	}

	public void setExecOrder(int order) {
		execOrder = order;
	}

	public int getExecOrder() {
		return execOrder;
	}

	public int getMatrixOrder() {
        return matrixOrder;
    }

    public void setMatrixOrder(int matrixOrder) {
        this.matrixOrder = matrixOrder;
    }

    public void clear() {
        for(AMLAction a : actions) {
			a.clear();
		}
	}

	public boolean isFailOnUnexpectedMessage() {
		return failOnUnexpectedMessage;
	}

	public void setFailOnUnexpectedMessage(boolean failOnUnexpectedMessage) {
		this.failOnUnexpectedMessage = failOnUnexpectedMessage;
	}

    public boolean isAddToReport() {
        return addToReport;
    }

    public void setAddToReport(boolean addToReport) {
        this.addToReport = addToReport;
    }

    @Override
    public AMLBlockType getBlockType() {
        return blockType;
    }

    @Override
    public void setBlockType(AMLBlockType blockType) {
        this.blockType = blockType;
    }

    @Override
    public long getUID() {
        return uid;
    }

    @Override
    public int getHash() {
        return hash;
    }

    public boolean isOptional() {
        if (executionMode == ExecutionMode.OPTIONAL) {
            return true;
        }
        for (AMLAction action : actions) {
            if (action.isOptional()) {
                return true;
            }
        }
        return false;
    }

    public String getTagsAsString() {
        return isOptional() ? "{\"" + StatisticsService.OPTIONAL_TAG_NAME + "\"}" : "{}";
    }

    @Override
	public AMLTestCase clone() {

        AMLTestCase that = new AMLTestCase(id, uid, hash);

        for(AMLAction a : actions) {
			that.actions.add(a.clone());
		}
        that.reference = reference;
        that.line = line;
        that.description = description;
        that.executionMode = executionMode;
        that.execOrder = execOrder;
        that.matrixOrder = matrixOrder;
        that.blockType = blockType;
        that.addToReport = addToReport;

		return that;
	}

	@Override
	public String toString() {
		ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

		sb.append("line", line);
        sb.append("reference", reference);
		sb.append("executionMode", executionMode);
		sb.append("description", description);
		sb.append("execOrder", execOrder);
		sb.append("matrixOrder", matrixOrder);
		sb.append("id", id);

		return sb.toString();
	}

}
