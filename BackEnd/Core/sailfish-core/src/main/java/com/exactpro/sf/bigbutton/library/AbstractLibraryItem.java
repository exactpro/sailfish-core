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
package com.exactpro.sf.bigbutton.library;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class AbstractLibraryItem implements Serializable {
	
	private void reportNotAllowed(Object item) {
		
		throw new IllegalNestedItemException(item.getClass().getSimpleName() 
				+ " is not allowed under " 
				+ this.getClass().getSimpleName());
		
	}
	
	public void addNested(Executor item) {
		
		reportNotAllowed(item);
		
	}
	
	public void addNested(Daemon item) {
		
		reportNotAllowed(item);
		
	}
	
	public void addNested(Script item) {
		
		reportNotAllowed(item);
		
	}
	
	public void addNested(Tag item) {
		
		reportNotAllowed(item);
		
	}

	public void addNested(Service item) {
		
		reportNotAllowed(item);
		
	}
	
	public void addNested(Globals item) {
		
		reportNotAllowed(item);
		
	}
	
}