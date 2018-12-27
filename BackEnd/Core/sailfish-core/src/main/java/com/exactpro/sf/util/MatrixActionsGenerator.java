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
package com.exactpro.sf.util;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XsdDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.actions.ActionClassTemplate;

import java.io.*;
import java.util.List;

public class MatrixActionsGenerator {
    private static final String actionPackage = "com.exactpro.sf.actions";
    private static final String messagePackageName = "com.exactpro.sf.messages";
    private static final String defaultUncheckedFields = "com.exactpro.sf.common.impl.messages.DefaultMessageFactory.getFactory().getUncheckedFields()";

	public static void generate(
			String genDir,
			String actionPackage,
			String messagesPackageName,
			final IDictionaryStructure dictionary,
			String uncheckedFields)
	throws IOException {

		List<IMessageStructure> messages = dictionary.getMessageStructures();

		StringWriter strWriter = new StringWriter();
		String typeNamespace = messagesPackageName + "." + dictionary.getNamespace().toLowerCase();
		ActionClassTemplate actionTemplate = new ActionClassTemplate();

		actionTemplate.render(
				strWriter,
				actionPackage,
				dictionary.getNamespace(),
				typeNamespace,
				messages,
                uncheckedFields
		);

		String fileName = genDir
		+ ("." + actionPackage + "." + dictionary.getNamespace().toUpperCase() + "_SndRcvMatrixActions")
		.replace('.', File.separatorChar);
		fileName += ".java";
		String contents = strWriter.toString();
		writeFile(contents, fileName);
	}

	private static void writeFile(String contents, String fileName) {
		File file = new File(fileName);

		if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
			throw new EPSCommonException("Can not create \""
					+ file.getParentFile().getAbsolutePath() + "\" folder");
		}

		FileWriter writer = null;

		try {
			writer = new FileWriter(file);

			writer.write(contents);
		} catch (IOException e) {
			throw new EPSCommonException(e);
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				throw new EPSCommonException(e);
			}
		}

	}

	static IDictionaryStructure loadDictionary(String dictionaryFile)
	throws IOException {
		
		IDictionaryStructureLoader loader = null;
		
		if(dictionaryFile.toLowerCase().endsWith(".xml")){
			loader = new XmlDictionaryStructureLoader();
		}else if (dictionaryFile.toLowerCase().endsWith("xsd")) {
			loader = new XsdDictionaryStructureLoader();
		}else {
			throw new EPSCommonException("This parameter is not supported.");
		}
		
		try (InputStream in = new FileInputStream(dictionaryFile)) {
			return loader.load(in);
    	}
	}

	public static void main(String[] args) throws IOException {

		String dictionaryFile = args[0];
		IDictionaryStructure dictionary = loadDictionary(dictionaryFile);

		String genDir = args[1];

		String msgPack = messagePackageName;
		if (args.length > 2) {
			msgPack = args[2];
		}

		String actionPack = actionPackage;
		if (args.length > 3) {
			actionPack = args[3];
		}
        String uncheckedFields = defaultUncheckedFields;
        if (args.length > 4) {
            uncheckedFields = args[4];
        }

		generate(genDir, actionPack, msgPack, dictionary, uncheckedFields);
	}
}
