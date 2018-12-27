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

import java.util.List;

public interface ITestCase {

	/*getters and setters*/

	String getId();
	void setId(String id);

	boolean isExecutable();
	void setExecutable(boolean isExecutable);

	String getDescription();
	void setDescription(String description);

    AMLBlockType getBlockType();
    void setBlockType(AMLBlockType blockType);

	List<? extends IAction> getActions();

	/*other methods*/
	IAction getAction(int index);
	IAction findActionByRef(String lineRef);

    long getUID();

    /**
     * Hash code of matrix model representation of this test case
     */
    int getHash();

	ITestCase clone();

    String getReference();
    void setReference(String reference);
    boolean hasReference();
}
