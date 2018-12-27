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
package com.exactpro.sf.testwebgui.restapi.editor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.storage.DefaultMatrix;

public class MatrixWithHistory extends DefaultMatrix {

	private static final long serialVersionUID = 1357953303522030970L;

	private static final int MAX_HISTORY_SIZE = 11;

	@SuppressWarnings("serial")
	private static class MatrixWithHistoryEntry implements Serializable {
		private AMLMatrix matrix;
		private Collection<Alert> errors;
		private boolean commited = false;

		MatrixWithHistoryEntry(AMLMatrix matrix, Collection<Alert> errors) {
			super();
			this.matrix = matrix;
			this.errors = errors;
		}

		AMLMatrix getMatrix() {
			return matrix;
		}

		Collection<Alert> getErrors() {
			return errors;
		}

		boolean isCommited() {
			return commited;
		}

		void commit(AMLMatrix matrix, Collection<Alert> errors) {
			if (commited)
				throw new IllegalStateException("This snapshot had been already commited");

			// unmodifiable collections to prevent bugs
			this.matrix = matrix;
			this.errors = Collections.unmodifiableCollection(errors);
			commited = true;
		}

		void commit() {
			this.commit(this.matrix, this.errors);
		}

	}

	private LinkedList<MatrixWithHistoryEntry> history = new LinkedList<MatrixWithHistoryEntry>();
	private int position = -1;

	public MatrixWithHistory(Long id, String name, String description, SailfishURI languageURI, String filePath, Date date) {
		// we don't support editing of the Remote matrixes
		super(id, name, description, "Matrix Editor creator", languageURI, filePath, date, null, null);
	}

	public synchronized void newSnaphot(AMLMatrix matrix) {
		insertNewSnapshot(matrix, Collections.<Alert>emptyList());
	}

	public synchronized void commitSnapshot(AMLMatrix amlMatrix, Collection<Alert> errors) {
		history.get(position).commit(amlMatrix, errors);
	}

	public synchronized void newCommitedSnapshot(AMLMatrix amlMatrix, Collection<Alert> errors) {
		insertNewSnapshot(amlMatrix, errors);
		history.get(position).commit();
	}

	public synchronized boolean rollbackIfUncommited() {
		if (history.isEmpty() || history.get(position).isCommited()) {
			return false; // all ok
		}

		history.remove(position);
		if (position != 0)
			position--;

		rollbackIfUncommited();
		return true;
	}

	private synchronized void insertNewSnapshot(AMLMatrix matrix, Collection<Alert> errors) {
		if (position + 1 != history.size()) { // if we change after 'undo' - drop all changes after last point
			int fromIndex = position + 1;
			int toIndex = history.size() - 1;
			history.subList(fromIndex, toIndex).clear();
		}
		if (history.size() >= MAX_HISTORY_SIZE) {
			history.pollFirst();
		}
		history.add(new MatrixWithHistoryEntry(matrix, errors));
		position = history.size() - 1;
	}

	public synchronized void undo() throws Exception {
		if (position == 0) {
			throw new Exception("No more elements");
		}
		position--;
	}

	public synchronized void redo() throws Exception {
		if (position >= history.size() - 1) {
			throw new Exception("No more elements");
		}
		position++;
	}

	public synchronized AMLMatrix getMatrix() {
		return history.get(position).getMatrix();
	}

	public synchronized Collection<Alert> getErrors() {
		return history.get(position).getErrors();
	}

}