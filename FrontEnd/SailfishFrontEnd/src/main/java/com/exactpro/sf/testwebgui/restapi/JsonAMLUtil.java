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
package com.exactpro.sf.testwebgui.restapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.iomatrix.SimpleCell;
import com.exactpro.sf.aml.reader.struct.AMLBlock;
import com.exactpro.sf.aml.reader.struct.AMLElement;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.exactpro.sf.aml.visitors.IAMLElementVisitor;
import com.exactpro.sf.testwebgui.restapi.editor.MatrixWithHistory;
import com.exactpro.sf.testwebgui.restapi.json.JsonAMLError;
import com.exactpro.sf.testwebgui.restapi.json.editor.JsonMatrixLine;
import com.google.common.primitives.Longs;

public class JsonAMLUtil {
	
	public static JsonMatrixLine convertLine(AMLElement element, MatrixWithHistory matrix) throws AMLException {
		JsonAMLVisitor visitor = new JsonAMLVisitor(matrix);
		element.accept(visitor);
		return visitor.getLine();
	}
	
	public static boolean isBlockAction(JavaStatement action) {
		return action == JavaStatement.BEGIN_ELIF || action == JavaStatement.BEGIN_ELSE
				|| action == JavaStatement.BEGIN_IF || action == JavaStatement.BEGIN_LOOP;
	}
	
	private final static long[] NO_PATH = new long[0];
	public static List<JsonAMLError> getErrors(MatrixWithHistory matrix) {
		Collection<Alert> errors = matrix.getErrors();

		// uid -> path
		Map<Long, long[]> paths = new HashMap<>();

		// Find all UIDs that we should map to paths
		for (Alert error : errors) {
			paths.put(error.getUid(), NO_PATH);
		}

		// map uids to paths
		Long i = 0L;
		AMLMatrix mm = matrix.getMatrix();
		ArrayList<Long> path = new ArrayList<>();
		for (AMLBlock block : mm.getBlocks()) {
			path.clear();
			path.add(i++);
			buildMapping(path, block, paths);
		}

		// convert errors
		List<JsonAMLError> result = new ArrayList<JsonAMLError>(errors.size());
		for (Alert error : errors) {
			result.add(convertError(error, paths));
		}

		return result;
	}

	private static void buildMapping(ArrayList<Long> path, AMLBlock block, Map<Long, long[]> uids) {
		if (uids.containsKey(block.getUID())) {
			uids.put(block.getUID(), buildPath(path));
		}
		for (int i=0; i<block.getElements().size(); i++) {
			AMLElement element = block.getElement(i);
			
			if (element instanceof AMLBlock) {
				path.add((long)i);
				buildMapping(path, (AMLBlock) element, uids);
				path.remove( (int) (path.size() - 1)); // by index!
			} else {
				if (uids.containsKey(element.getUID())) {
					path.add((long)i);
					uids.put(element.getUID(), buildPath(path));
					path.remove( (int) (path.size() - 1)); // by index!
				}
			}
		}
	}

	private static JsonAMLError convertError(Alert error, Map<Long, long[]> paths) {
		List<long[]> errorPaths = new ArrayList<>();

		errorPaths.add(paths.get(error.getUid()));

		return new JsonAMLError(errorPaths, error.getReference(), error.getColumn(), error.getMessage(), error.getType() == AlertType.ERROR);
	}

	private static long[] buildPath(List<Long> path) {
		return Longs.toArray(path);
	}
	
	private final static class JsonAMLVisitor implements IAMLElementVisitor {
		
		private final MatrixWithHistory matrix;
		private JsonMatrixLine line;
		
		public JsonAMLVisitor(MatrixWithHistory matrix) {
			this.matrix = matrix;
		}

		@Override
		public void visit(AMLElement element) throws AMLException {
			final Map<String, String> actionValues = getActionValues(element);
			final Map<String, String> actionMetadata = getActionMetaData(element);
			
			if (matrix != null && !matrix.getErrors().isEmpty()) {
				Collection<Alert> errors = matrix.getErrors();
	        	if (errors != null && !errors.isEmpty()) {
					this.line = new JsonMatrixLine(
							actionValues,
							actionMetadata,
							getErrors(matrix),
							null);
					return;
	        	}
			}
			this.line = new JsonMatrixLine(actionValues, actionMetadata,  null);
		}

		private Map<String, String> getActionValues(AMLElement element) {
			final Map<String, String> actionValues = new HashMap<String, String>();
			for (Entry<String, SimpleCell> entry : element.getCells().entrySet()) {
				String value = (entry.getValue() != null) ? entry.getValue().getValue(): null;
				if (StringUtils.isNotEmpty(value)) {
					actionValues.put(entry.getKey(), value);
				}
			}
			return actionValues;
		}

		public JsonMatrixLine getLine() {
			return line;
		}

		@Override
		public void visit(AMLBlock block) throws AMLException {
			final Map<String, String> actionValues = getActionValues(block);
			final Map<String, String> actionMetadata = getActionMetaData(block);
			
			List<JsonMatrixLine> items = new LinkedList<JsonMatrixLine>();
			for (AMLElement inAction : block.getElements()) {
				inAction.accept(this);
				JsonMatrixLine line = this.getLine();
				items.add(line);
			}
			
			if (matrix != null && !matrix.getErrors().isEmpty()) {
				Collection<Alert> errors = matrix.getErrors();
	        	if (errors != null && !errors.isEmpty()) {
					this.line = new JsonMatrixLine(
							actionValues,
							actionMetadata,
							getErrors(matrix),
							items);
	        	}
			}
			this.line = new JsonMatrixLine(actionValues, actionMetadata, items);
		}

		private Map<String, String> getActionMetaData(AMLElement element) {
			final Map<String, String> actionMetadata = new HashMap<String, String>();
			actionMetadata.put("dictionary", element.getValue(Column.Dictionary));
			return actionMetadata;
		}
	}

}
