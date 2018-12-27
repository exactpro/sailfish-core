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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLBlockBrace;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixReader;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixWriter;
import com.exactpro.sf.aml.iomatrix.IMatrixReader;
import com.exactpro.sf.aml.iomatrix.IMatrixWriter;
import com.exactpro.sf.aml.iomatrix.SimpleCell;
import com.exactpro.sf.aml.iomatrix.SimpleCellUtils;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.storage.IMatrix;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONMatrixEditor {

	private static final Logger logger = LoggerFactory.getLogger(JSONMatrixEditor.class);

	private static class MetaData {
		private List<SimpleCell> header; // at start of test case
		private int offset; // line number
		private int count;
		private String testCaseName;

		public int getOffset() {
			return offset;
		}
		public void setOffset(int offset) {
			this.offset = offset;
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		public String getTestCaseName() {
			return testCaseName;
		}
		public void setTestCaseName(String testCaseName) {
			this.testCaseName = testCaseName;
		}
		public List<SimpleCell> getHeader() {
			return header;
		}
		public void setHeader(List<SimpleCell> header) {
			this.header = new ArrayList<>();
			this.header.addAll(header);
		}

	}

	// Just alias for Jackson
	public static class Lines extends ArrayList<String[]> {
        private static final long serialVersionUID = -509012897386430224L;
	}

	private final IMatrix matrix;

	private final List<SimpleCell[]> lines = new ArrayList<>();
	private final List<MetaData> metadata = new ArrayList<>();

	public JSONMatrixEditor(IMatrix matrix) {
		this.matrix = matrix;

        try(IMatrixReader reader = AdvancedMatrixReader.getReader(new File(SFLocalContext.getDefault().getWorkspaceDispatcher().getFolder(FolderType.MATRIX), matrix.getFilePath()))) {
			while (reader.hasNext()) {
				SimpleCell[] line = reader.readCells();
				lines.add(line);
			}

			updateMetaData();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}

	}

	private void updateMetaData() {
		metadata.clear();

		int tcs = 0; // Number of TestCases (not blocks)
		int lineNumber = 0;

		List<SimpleCell> headers = null;

		for (SimpleCell[] line : lines) {
			if (headers == null) {
				headers = new ArrayList<>();
				headers.addAll(Arrays.asList(line));
				continue;
			}

			lineNumber++;

			SimpleCell actionCell = getValue(line, headers, "#action");
			String action = "";
			if (actionCell != null) {
				action = actionCell.getValue().trim();
			}
			AMLBlockBrace brace = AMLBlockBrace.value(action);

			if (brace == null) {
				// FIXME: define headers
				// Here we can add '#'-fields (if they are not present at first)
				continue;
			} else if (brace.isStart()) {
				MetaData md = new MetaData();
				md.setHeader(headers);

				if(brace == AMLBlockBrace.TestCaseStart) {
					tcs++;
				}

				md.setOffset(lineNumber);

				// if description is empty and reference is not, return reference
				String trueDescription = getNonEmptyVelue(
						getValue(line, headers, "#description"),
						getValue(line, headers, "#reference"),
						getValue(line, headers, "#id"));

				String testCaseName = trueDescription; //+ " (line: " + lineNumber + ")";
				if(brace == AMLBlockBrace.TestCaseStart) {
					testCaseName = tcs + ": " + testCaseName;
				}

				md.setTestCaseName(testCaseName);

				metadata.add(md);
			} else {
				MetaData md = metadata.get(metadata.size()-1);
				md.setCount(lineNumber - md.getOffset());
			}
		}
	}

	private SimpleCell getValue(SimpleCell[] line, List<SimpleCell> headers, String key) {
		int idx = -1;
		for (int i=0; i<headers.size(); i++) {
			if (key.equals(headers.get(i).getValue())){
				idx = i;
				break;
			}
		}
		if (idx == -1) {
			return null;
		}
		if (idx >= line.length) {
			return null;
		}
		return line[idx];
	}

	private void setValue(SimpleCell[] line, List<SimpleCell> headers, String key, SimpleCell value) {
		int idx = -1;
		for (int i=0; i<headers.size(); i++) {
			if (key.equals(headers.get(i).getValue())){
				idx = i;
				break;
			}
		}
		if (idx == -1) {
			throw new IndexOutOfBoundsException("Can't find column " + key);
		}
		line[idx] = value;
	}

	private String getNonEmptyVelue(SimpleCell ... cells) {
		for (SimpleCell cell : cells) {
			if (cell == null) {
				continue;
			}
			if (cell.getValue().trim().isEmpty()) {
				continue;
			}
			return cell.getValue();
		}
		return "";
	}

	public String toJSON(String testCaseName)throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		List<String[]> result = new ArrayList<>();

		MetaData md = getMetaData(testCaseName);
		String[] headers = SimpleCellUtils.dropStyles(md.getHeader()).toArray(new String[0]);
		result.add(headers);
		int max = 0;
		// FIXME: styles
		List<SimpleCell[]> lines = getLines(testCaseName);
		for (SimpleCell[] line: lines) {
			result.add(SimpleCellUtils.dropStyles(line));
			if (line.length > max) {
			    max = line.length;
			}
		}

		if (result.get(0).length < max) {
		    List<String> header = new ArrayList<>(max);
		    for(int i=0; i<max; i++) {
		        if (i < result.get(0).length) {
		            header.add(result.get(0)[i]);
		        } else {
		            header.add("");
		        }
		    }

		    result.set(0, header.toArray(new String[0]));
		}


		return mapper.writeValueAsString(result);
	}

	public void fromJSON(String str, String testCaseName) throws JsonParseException, JsonMappingException, IOException {
		List<SimpleCell[]> oldLines = getLines(testCaseName);
		if (oldLines == null) {
			throw new RuntimeException("Testcase " + testCaseName + " doesn't exist");
		}

		ObjectMapper mapper = new ObjectMapper();

		Lines newLines = mapper.readValue(str, Lines.class);
		newLines.remove(0); // remove headers

		newLines.remove(newLines.size()-1);

		oldLines.clear();
		for (String[] line : newLines) {
			for (int i=0; i<line.length; i++) {
				// handson table return null for new lines
				if (line[i] == null) {
					line[i] = "";
				}
			}
			oldLines.add(SimpleCellUtils.addStyles(line));
		}

		updateMetaData();
	}

	public void flush() throws Exception {
        final File matrixFile = new File(SFLocalContext.getDefault().getWorkspaceDispatcher().getFolder(FolderType.MATRIX), matrix.getFilePath());

		try (IMatrixWriter writer = AdvancedMatrixWriter.getWriter(matrixFile)) {
			for (SimpleCell[] line : lines) {
				writer.writeCells(line);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException("Can't save matrix into file", e);
		}
	}

	public Set<String> getTestcaseNames() {
		final Set<String> testCaseNames = new LinkedHashSet<>();

		for (MetaData md : metadata) {
			testCaseNames.add(md.getTestCaseName());
		}

		return testCaseNames;
	}

	public void addTestcase(String testCaseName) {
		final Set<String> testCaseNames = getTestcaseNames();
		if (testCaseNames.contains(testCaseName)) { // FIXME: add logic for AML-auto-generated TestCase names?
			throw new RuntimeException("Testcase " + testCaseName + " already exists");
		} else {
			// FIXME: HEADERS:
			List<SimpleCell> headers = Arrays.asList(lines.get(0));
			// Add 'TestCaseStart' + TestCaseEnd'
			SimpleCell[] start = new SimpleCell[headers.size()];
			setValue(start, headers, "#action", new SimpleCell(AMLBlockBrace.TestCaseStart.getName()));

			SimpleCell[] end = new SimpleCell[headers.size()];
			setValue(end, headers, "#action", new SimpleCell(AMLBlockBrace.TestCaseEnd.getName()));

			lines.add(start);
			lines.add(end);

			updateMetaData();
		}

	}

	public void removeTestcase(String testCaseName) {
		List<SimpleCell[]> lines = getLines(testCaseName);

		if (lines == null) {
			throw new RuntimeException("Testcase " + testCaseName + " doesn't exist");
		}

		lines.clear(); // remove lines

		updateMetaData();
	}

	/*
	 * The returned list is backed by this list, so non-structural changes in
	 * the returned list are reflected in this list, and vice-versa.
	 */
	private List<SimpleCell[]> getLines(String testCaseName) {
		MetaData md = getMetaData(testCaseName);
		if (md == null) {
			return null;
		}
		return lines.subList(md.getOffset(), md.getOffset() + md.getCount()+1);
	}

	private MetaData getMetaData(String testCaseName) {
		for (MetaData md : metadata) {
			if (testCaseName.equals(md.getTestCaseName())) {
				return md;
			}
		}
		return null;
	}
}