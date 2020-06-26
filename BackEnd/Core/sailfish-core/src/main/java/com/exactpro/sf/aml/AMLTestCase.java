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
public class AMLTestCase implements Cloneable, Serializable {

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

    public List<AMLAction> getActions() {
        return actions;
	}

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public boolean hasReference() {
        return StringUtils.isNotEmpty(reference);
	}

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

    public String getDescription() {
        return description;
	}

    public void setDescription(String description) {
		this.description = description;
	}

    private static boolean checkActionReference(AMLAction action, String reference) {
        ExecutionMode executionMode = action.getExecutionMode();
        return (executionMode == ExecutionMode.EXECUTABLE || executionMode == ExecutionMode.OPTIONAL) &&
                (reference.equals(action.getReference()) || reference.equals(action.getReferenceToFilter()));
    }

	/**
     * Returns the first found action with specified reference.
	 * @param lineRef reference
	 * @return action in test case or null if no reference found
	 */
    public AMLAction findActionByRef(String lineRef)
	{
        for(AMLAction action : actions) {
            if (checkActionReference(action, lineRef)) {
                return action;
            }
        }

        return null;
	}

    /**
     * Searches for an action closest to the specified one by reference.
     * Actions before the specified action have a higher priority than actions after it
     * @param dependant depending action
     * @param dependencyReference target action reference
     * @return closest action with the specified reference or {@code null} if there is no such action
     */
    public AMLAction findClosestAction(AMLAction dependant, String dependencyReference) {
        int dependantIndex = actions.indexOf(dependant);

        if (dependantIndex >= 0) {
            for (int i = dependantIndex - 1; i >= 0; i--) {
                AMLAction action = actions.get(i);

                if (checkActionReference(action, dependencyReference)) {
                    return action;
                }
            }

            for (int i = dependantIndex; i < actions.size(); i++) {
                AMLAction action = actions.get(i);

                if (checkActionReference(action, dependencyReference)) {
                    return action;
                }
            }
        }

        return null;
    }

    public void setId(String id) {
		this.id = id;
	}

    public String getId() {
		return id;
	}

    public void setExecutionMode(ExecutionMode executionMode) {
		this.executionMode = executionMode;
	}

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

    public AMLBlockType getBlockType() {
        return blockType;
    }

    public void setBlockType(AMLBlockType blockType) {
        this.blockType = blockType;
    }

    public long getUID() {
        return uid;
    }

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
