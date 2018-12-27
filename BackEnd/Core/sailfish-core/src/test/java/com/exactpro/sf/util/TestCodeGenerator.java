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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.CodeGenUtils;
import com.exactpro.sf.common.util.CodeGenerator;

public class TestCodeGenerator extends EPSTestCase {

    private final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.java");;
    private final List<String> baseOption = Arrays.asList("-g", "-classpath", System.getProperty("java.class.path"), "-d");
    private final Path basePath = BASE_DIR.resolve("build").resolve("generated");
    private final CodeGenerator codeGenerator = new CodeGenerator();
    private final IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader(); 
    private final IDictionaryStructure dictionary;

    {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("testCodeGen.xml")) {
            dictionary = loader.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Dictionary loading failure", e);
        }
    }
    
    @Test
    public void testCodeGenerator() throws Exception {
        doCompile("full", 10, false, false);
        doCompile("admin", 6, true, false);
        doCompile("full_underscore", 10, false, true);
        doCompile("admin_underscore", 6, true, true);
    }
    
    private void doCompile(String testName, int countElements, boolean adminOnly, boolean underscoreAsPackageSeparator) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Path baseFolder = basePath.resolve(testName);
        if (Files.exists(baseFolder)) {
            Files.walk(baseFolder)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
        
        Path sourceFolder = baseFolder.resolve("src");
        Path compileFolder = baseFolder.resolve("bin");
        Files.createDirectories(compileFolder);
        
        codeGenerator.generate(sourceFolder.toString(), new String[] { "com", "exactpro", "sf", "pack" + System.currentTimeMillis(), "messages" }, dictionary, adminOnly, underscoreAsPackageSeparator);
        Assert.assertEquals(testName + ": file count", countElements, Files.walk(sourceFolder)
            .filter(path -> Files.isRegularFile(path))
            .count());
        
        List<Path> javaFiles = Files.find(sourceFolder, Integer.MAX_VALUE, (path, basicFileAttributes) -> matcher.matches(path.getFileName()))
                .collect(Collectors.toList());
        
        List<String> option = new ArrayList<>(baseOption);
        option.add(compileFolder.toString());
        
        
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(
                javaFiles.stream()
                    .map(Path::toFile)
                    .collect(Collectors.toList())
                    .toArray(new File[javaFiles.size()])
        );

        StringWriter writer = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            Assert.assertTrue(testName + ": " + writer.toString(), compiler.getTask(printWriter, fileManager,null, option, null, units).call());
        }
        
        try (URLClassLoader classLoader = new URLClassLoader(new URL[] { compileFolder.toUri().toURL() }, getClass().getClassLoader())) {
            for (Path path : javaFiles) {
                Assert.assertEquals(testName + ": contains underscore '" + path + "'", underscoreAsPackageSeparator, !path.getFileName().toString().contains("_"));
                
                String name = path.getFileName().toString();
                boolean isField = CodeGenUtils.COMPONENTS_SUB_PACKAGE.equals(path.getParent().getFileName().toString());
                if (underscoreAsPackageSeparator) {
                    name = path.getParent().getFileName().toString() + "_" + path.getFileName().toString();
                    isField = CodeGenUtils.COMPONENTS_SUB_PACKAGE.equals(path.getParent().getParent().getFileName().toString());
                }
                
                name = FilenameUtils.getBaseName(name);
                if (isField) {
                    Assert.assertNotNull(testName + ": check filed '" + name + "'", dictionary.getFieldStructure(name));
                } else {
                    Assert.assertNotNull(testName + ": check message '" + name + "'", dictionary.getMessageStructure(name));
                    
                    String className = sourceFolder.relativize(path).toString();
                    className = className.substring(0, className.length() - ".java".length()).replace(File.separatorChar, '.');
                    Class<?> clazz = classLoader.loadClass(className);
                    BaseMessage baseMessage = (BaseMessage) clazz.newInstance();
                    Assert.assertEquals(testName + ": check message type '" + name + "'", name, baseMessage.getMessage().getName());
                }
            }
        }
    }
}
