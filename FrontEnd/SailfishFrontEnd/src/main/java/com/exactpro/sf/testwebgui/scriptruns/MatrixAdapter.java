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
package com.exactpro.sf.testwebgui.scriptruns;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.storage.DefaultMatrix;
import com.exactpro.sf.storage.IMatrix;

public class MatrixAdapter implements Serializable, Cloneable {

	private static final long serialVersionUID = -7538488603270652995L;

	private Long matrixId;

	private boolean continueOnFailed  = true;
	private boolean autoStart 		  = false;
	private String range;
	private IView view;

	private long lastScriptRun;

	public MatrixAdapter(Long matrixId) {
		this.matrixId = matrixId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MatrixAdapter) {
			MatrixAdapter matrixAdapter = (MatrixAdapter)obj;
			return matrixId != null ?
					(matrixAdapter.getMatrixId() != null ? matrixId.equals(matrixAdapter.getMatrixId()) : false) :
						matrixAdapter.getMatrixId() == null;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return matrixId != null ? matrixId.hashCode() : 0;
	}

	public boolean isContinueOnFailed() {
		return continueOnFailed;
	}

	public void setContinueOnFailed(boolean continueOnFailed) {
		this.continueOnFailed = continueOnFailed;
	}

	public boolean isAutoStart() {
		return autoStart;
	}

	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	public boolean getReloadEnabled(){
		return getIMatrix().getReloadEnabled();
	}

	public SailfishURI getLanguageURI(){
		return getIMatrix().getLanguageURI();
	}

	public long getLastScriptRun() {
		return lastScriptRun;
	}

	public void setLastScriptRun(long lastScriptRun) {
		this.lastScriptRun = lastScriptRun;
	}

	public String getRange() {
		return range;
	}

	public void setRange(String range) {
		this.range = range;
	}

	public Long getMatrixId() {
		return matrixId;
	}

	public String getName() {
		return getIMatrix().getName();
	}

	public void setName(String name) {
		getIMatrix().setName(name);
	}

	public String getCreator() {
		return getIMatrix().getCreator();
	}

	public String getFilePath() {
		return getIMatrix().getFilePath();
	}

	public void setFilePath(String filePath) {
		getIMatrix().setFilePath(filePath);
	}

	public Date getDate() {
		return getIMatrix().getDate();
	}

	public void setDate(Date date) {
		getIMatrix().setDate(date);
	}

	public String getScriptSettindsPath() {
		return getIMatrix().getScriptSettindsPath();
	}

	public String getLink() {
		return getIMatrix().getLink();
	}

	public void setLink(String link) {
		getIMatrix().setLink(link);
	}

	public SailfishURI getProviderURI() {
		return getIMatrix().getProviderURI();
	}

	public void setProviderURI(SailfishURI providerURI) {
		getIMatrix().setProviderURI(providerURI);
	}

	public InputStream readStream(){
		IWorkspaceDispatcher wd = SFLocalContext.getDefault().getWorkspaceDispatcher();
		return getIMatrix().readStream(wd);
	}

	public void writeStream(InputStream stream){
		IWorkspaceDispatcher wd = SFLocalContext.getDefault().getWorkspaceDispatcher();
		getIMatrix().writeStream(wd, stream);
	}

	protected IMatrix getIMatrix() {
		if (view != null) {
			IMatrix matrix = view.getMatrixHolder().getMatrixById(matrixId);
			if (matrix == null) {
				return new DefaultMatrix();
			}
			return view.getMatrixHolder().getMatrixById(matrixId);
		} else {
			throw new IllegalStateException("View in null");
		}
	}

	public void setView(IView view) {
		this.view = view;
	}

	@Override
	public MatrixAdapter clone() throws CloneNotSupportedException {
		MatrixAdapter clone = (MatrixAdapter)super.clone();
		clone.setContinueOnFailed(continueOnFailed);
		clone.setAutoStart(autoStart);
		clone.setRange(range);
		clone.setLastScriptRun(lastScriptRun);
		return clone;
	}

}
