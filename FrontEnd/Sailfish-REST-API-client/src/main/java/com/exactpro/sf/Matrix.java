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
package com.exactpro.sf;

import java.util.Date;

public class Matrix {

	private int id;
	private String name;
	private Date date;
	
	public Matrix(int id, String name, Date date) {
		this.id = id;
		this.name=name;
		this.date=date;
	}
	
	public Matrix(){}

	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	protected void setId(int id) {
		this.id = id;
	}
	
	protected void setDate(Date date){
		this.date=date;
	}
	
	public Date getDate(){
		return date;
	}
	/* 
	// Static members
	protected static Matrix fromXml(Node n, DateFormat dateFormat) throws ParseException {
		Element el = (Element)n;
		
		Matrix mat = new Matrix(Integer.parseInt(Util.getTextContent(el, "id")));
		
		String val = Util.getTextContent(el, "name");
		if (val != null)
			mat.name = val;
		
		val = Util.getTextContent(el, "date");
		if (val != null){
			mat.date = val;
		}
		
		return mat;
	}*/
	
}
