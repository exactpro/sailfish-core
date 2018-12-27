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
package com.exactpro.sf.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XsdDictionaryStructureLoader;
import com.exactpro.sf.messages.impl.templates.EnumFieldTypeTemplate;
import com.exactpro.sf.messages.impl.templates.MessageStructureTemplate;

/**
 * Generates classes from dictionaries
 *
 */
public class CodeGenerator {
	
    public void generate(String path, String[] distPackagePath, final IDictionaryStructure dictStructure, boolean adminOnly,
            boolean underscoreAsPackageSeparator) throws IOException {

        List<IMessageStructure> filteredMessages = filterMessages(dictStructure, adminOnly);
        for (IMessageStructure msgStruct : filteredMessages) {
            StringWriter strWriter = new StringWriter();
            String fullPackage = CodeGenUtils.getPackage(msgStruct, distPackagePath, underscoreAsPackageSeparator);
            String className = CodeGenUtils.getShortClass(msgStruct, underscoreAsPackageSeparator);

            if (generateMessage(strWriter, distPackagePath, underscoreAsPackageSeparator, fullPackage, className, msgStruct.getName(), dictStructure.getNamespace(), msgStruct)) {
                writeFile(path, className, strWriter.toString(), fullPackage);
            }
        }

        for (IFieldStructure fieldStructure : filterFields(dictStructure, filteredMessages, adminOnly)) {
            StringWriter strWriter = new StringWriter();

            String fullPackage = CodeGenUtils.getPackage(fieldStructure, distPackagePath, underscoreAsPackageSeparator);
            String className = CodeGenUtils.getShortClass(fieldStructure, underscoreAsPackageSeparator);

            if (generateFieldType(strWriter, fullPackage, className, dictStructure.getNamespace(), fieldStructure)) {
                writeFile(path, className, strWriter.toString(), fullPackage);
            }
        }
    }

    private List<IFieldStructure> filterFields(IDictionaryStructure dictStructure, List<IMessageStructure> filteredMessages, boolean adminOnly) {
        if (adminOnly) {
            return filteredMessages.stream()
                    .peek(msg -> System.out.println("Search fields in message '" + msg.getName() + "'"))
                    .flatMap(msg -> msg.getFields().stream())
                    .filter(field -> !field.isComplex())
                    .map(IFieldStructure::getReferenceName)
                    .distinct()
                    .filter(Objects::nonNull)
                    .peek(name -> System.out.println("Filed '" + name + "' for generate"))
                    .map(dictStructure::getFieldStructure)
                    .collect(Collectors.toList());
        }
        return dictStructure.getFieldStructures();
    }

    private List<IMessageStructure> filterMessages(IDictionaryStructure dictStructure, boolean adminOnly) {
        if (adminOnly) {
            return dictStructure.getMessageStructures().stream()
                    .filter(msg -> Boolean.TRUE.equals(msg.getAttributeValueByName("IsAdmin")))
                    .peek(msg -> System.out.println("Search fields in admin message '" + msg.getName() + "'"))
                    .flatMap(this::searchComplex)
                    .map(field -> ObjectUtils.defaultIfNull(field.getReferenceName(), field.getName()))
                    .distinct()
                    .map(dictStructure::getMessageStructure)
                    .peek(msg -> System.out.println("Message '" + msg.getName() + "' for generate"))
                    .collect(Collectors.toList());
        }
        return dictStructure.getMessageStructures();
    }
    
    private Stream<IFieldStructure> searchComplex(IFieldStructure structure) {
        return Stream.concat(
                Stream.of(structure),
                structure.getFields().stream()
                        .filter(field -> field.isComplex())
                        .flatMap(this::searchComplex));
    }
    
    private boolean generateMessage(Writer writer, String[] distPackagePath, boolean underscoreAsPackageSeparator, String fullPackage, String className, String messageName, String namespace, IMessageStructure msgStruct) 
			throws IOException
	{
		MessageStructureTemplate msgStructTemplate = new MessageStructureTemplate();
		
		Map<String, String> attributes = new HashMap<>();
		
		for (String name : msgStruct.getAttributes().keySet()) {
			attributes.put(name, msgStruct.getAttributeValueByName(name).toString());
		}
		msgStructTemplate.render(writer,
		                            distPackagePath,
		                            underscoreAsPackageSeparator,
									fullPackage, 
									className,
									messageName,
									namespace,
									msgStruct.getFields(),
									attributes);

		return true;
	}
	
	private boolean generateFieldType(Writer writer, String fullPackage, String className, String namespace, IFieldStructure fieldStructure) 
			throws IOException
	{
		if ( fieldStructure.isEnum() )
		{
			EnumFieldTypeTemplate enumFieldTypeTemplate = new EnumFieldTypeTemplate();
			
			enumFieldTypeTemplate.render(writer,
										fullPackage, 
										className, 
										CodeGenUtils.convertSimpleFieldTypeToJavaType(fieldStructure.getJavaType()),
										CodeGenUtils.convertSimpleFieldTypeToJavaObjectType(fieldStructure.getJavaType()),
										CodeGenUtils.isPrimitive(fieldStructure.getJavaType()), 
										fieldStructure.getValues());
			
			return true;
		}
		
		return false;
	}
	
    private void writeFile(String path, String className, String contents, String fullPackage) throws IOException {
        File file = Paths.get(path, fullPackage.replace('.', File.separatorChar), className + ".java").toFile();

        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
            throw new EPSCommonException("Could not create \"" + file.getParentFile().getAbsolutePath() + "\" folder");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(contents);
        }
    }
	
	public static void main(String[] args) throws IOException {
		String dictName = args[0];
		String distDir = args[1];
		String[] distPackagePath = args[2].split("\\.");
		boolean adminOnly = false;
		boolean underscoreAsPackageSeparator = false;
		
		if (args.length > 3) {
		    adminOnly = ArrayUtils.contains(args, "--admin-only");
		    underscoreAsPackageSeparator = ArrayUtils.contains(args, "--underscore-as-package-separator");
		}
		
		try {
			IDictionaryStructure dictionary;
			IDictionaryStructureLoader loader = null;

			if (dictName.toLowerCase().endsWith("xml")) {
				loader = new XmlDictionaryStructureLoader();
			} else if (dictName.toLowerCase().endsWith("xsd")) {
				loader = new XsdDictionaryStructureLoader();
			} else {
				throw new EPSCommonException("This parameter is not supported.");
			}
			
			try (InputStream in = new FileInputStream(dictName)) {
				dictionary = loader.load(in);
	    	}

			CodeGenerator codeGenerator = new CodeGenerator();
			codeGenerator.generate(distDir, distPackagePath, dictionary, adminOnly, underscoreAsPackageSeparator);
		} catch (RuntimeException e) {
			throw new EPSCommonException(e.getMessage() + " file " + dictName, e);
		}
	}
}