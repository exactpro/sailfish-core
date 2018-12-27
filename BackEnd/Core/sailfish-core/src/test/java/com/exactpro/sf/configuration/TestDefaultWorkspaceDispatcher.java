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
package com.exactpro.sf.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.util.Utils.FileExtensionFilter;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceDispatcherBuilder;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.IWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.ResourceWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.WorkspaceLayerException;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.google.common.io.Files;

public class TestDefaultWorkspaceDispatcher {

	private final String LOGS_DIR_PATH = "logs";
	private final String CFG_DIR_PATH = "cfg";

	@Test
	public void testCRUD() throws IOException {
		File tmpDir = Files.createTempDir();
		File baseLayer = new File(tmpDir, "base");
		File topLayer = new File(tmpDir, "top");

		try {
			createWorkspaceStructure(baseLayer);

			IWorkspaceDispatcher wd = new DefaultWorkspaceDispatcherBuilder()
				.addWorkspaceLayer(baseLayer, DefaultWorkspaceLayout.getInstance())
				.addWorkspaceLayer(topLayer, DefaultWorkspaceLayout.getInstance())
				.build();

			// GET FOLDER #1: in any case - return top layer's folder:
			File expected = new File(topLayer, LOGS_DIR_PATH);
			File actual = wd.getFolder(FolderType.LOGS);
			compareFiles(expected, actual);

			// GET FILE #1: no such file
			try {
				wd.getFile(FolderType.LOGS, "test.xml");
				Assert.fail();
			} catch (FileNotFoundException ex) {
				// ok
			}

			// GET FILE #2: file in base layer
			expected = new File(new File(baseLayer, LOGS_DIR_PATH), "test.xml");
			expected.createNewFile();
			actual = wd.getFile(FolderType.LOGS, "test.xml");
			compareFiles(expected, actual);

			// GET FILE #3: file in top layer
			expected = new File(new File(topLayer, LOGS_DIR_PATH), "test.xml");
			expected.createNewFile();
			actual = wd.getFile(FolderType.LOGS, "test.xml");
			compareFiles(expected, actual);

			FileUtils.forceDelete(actual);

			// CREATE FOLDER #1:
			expected = new File(new File(topLayer, LOGS_DIR_PATH), "nested");
			actual = wd.createFolder(FolderType.LOGS, "nested");
			compareFiles(expected, actual);

			// CREATE FOLDER #2: nested folders
			String path = "x" + File.separator + "y" + File.separator + "z";
			expected = new File(new File(topLayer, LOGS_DIR_PATH), path);
			actual = wd.createFolder(FolderType.LOGS, path);
			compareFiles(expected, actual);

			// CREATE FILE #1:
			expected = new File(new File(topLayer, LOGS_DIR_PATH), "test2.xml");
			actual = wd.createFile(FolderType.LOGS, true, "test2.xml");
			compareFiles(expected, actual);
			// nothing in baseLayer
			expected = new File(new File(baseLayer, LOGS_DIR_PATH), "test2.xml");
			Assert.assertFalse(expected.exists());

			// CREATE FILE #2: in (non-exisitng) nested folder
			path = "nested2" + File.separator + "test3.xml";
			expected = new File(new File(topLayer, LOGS_DIR_PATH), path);
			actual = wd.createFile(FolderType.LOGS, true, path);
			compareFiles(expected, actual);
			// nothing in baseLayer
			expected = new File(new File(baseLayer, LOGS_DIR_PATH), path);
			Assert.assertFalse(expected.exists());

            // CREATE FILE #3: use None-system separator in path
            path = "nested3" + File.separator + "test4.xml";
            String pathOtherSys;
            if ('/' == File.separatorChar) {
                pathOtherSys = FilenameUtils.separatorsToWindows(path);
            } else {
                pathOtherSys = FilenameUtils.separatorsToUnix(path);
            }
            expected = new File(new File(topLayer, LOGS_DIR_PATH), path);
            actual = wd.createFile(FolderType.LOGS, true, pathOtherSys);
            compareFiles(expected, actual);
            // nothing in baseLayer
            expected = new File(new File(baseLayer, LOGS_DIR_PATH), path);
            Assert.assertFalse(expected.exists());

			// EXIST #1:
			Assert.assertTrue(wd.exists(FolderType.LOGS, "test.xml")); // in base layer
			Assert.assertTrue(wd.exists(FolderType.LOGS, "test2.xml"));
			Assert.assertFalse(wd.exists(FolderType.LOGS, "test3.xml"));
			Assert.assertTrue(wd.exists(FolderType.LOGS, "x" + File.separator + "y"));

			// LIST FILES #1: all
			Set<String> expectedList = new HashSet<>();
			expectedList.add("test.xml");
			expectedList.add("test2.xml");
			expectedList.add("nested");
			expectedList.add("nested2");
			expectedList.add("nested3");
			expectedList.add("x");
			Set<String> actualList = wd.listFiles(null, FolderType.LOGS);
			Assert.assertEquals(expectedList, actualList);

			// LIST FILES #2: FileFilter
			expectedList = new HashSet<>();
			expectedList.add("test.xml");
			expectedList.add("test2.xml");
			actualList = wd.listFiles(new FileExtensionFilter("xml"), FolderType.LOGS);
			Assert.assertEquals(expectedList, actualList);

			// REMOVE FILE #1: base layer
			expected = new File(new File(baseLayer, LOGS_DIR_PATH), "test.xml");
			Assert.assertTrue(expected.exists());
			wd.removeFile(FolderType.LOGS, "test.xml");
			Assert.assertFalse(expected.exists());

			expected.createNewFile();

			// REMOVE FILE #2: top layer
			expected = new File(new File(topLayer, LOGS_DIR_PATH), "test2.xml");
			Assert.assertTrue(expected.exists());
			wd.removeFile(FolderType.LOGS, "test2.xml");
			Assert.assertFalse(expected.exists());

			expected.createNewFile();

			// REMOVE FOLDER #1: remove folder view
			File baseRoot = new File (new File(baseLayer, LOGS_DIR_PATH), "nested");
			File topRoot = new File(new File(topLayer, LOGS_DIR_PATH), "nested");
			File expected1 = new File(baseRoot, "test.xml");
			File expected2 = new File(topRoot, "test2.xml");
			expected1.getParentFile().mkdirs();
			expected1.createNewFile();
			expected2.getParentFile().mkdirs();
			expected2.createNewFile();
			Assert.assertTrue(expected1.exists());
			Assert.assertTrue(expected2.exists());
			wd.removeFolder(FolderType.LOGS, "nested");
			Assert.assertFalse(expected1.exists());
			Assert.assertFalse(expected2.exists());
			Assert.assertTrue(baseRoot.exists());
			Assert.assertFalse(topRoot.exists());

		} finally {
			FileUtils.forceDelete(tmpDir);
		}
	}

	@Test
	public void testResourceLayer() throws IOException {
        String resourceLayer = getClass().getClassLoader().getResource("com/exactpro/sf/workspace").getFile();

        IWorkspaceLayout layout = new ResourceWorkspaceLayout("com/exactpro/sf/workspace");
	    
        IWorkspaceDispatcher wd = new DefaultWorkspaceDispatcherBuilder()
                .addWorkspaceLayer(new File(layout.getPath(null, FolderType.ROOT)), layout)
                .build();
        
        // GET FOLDER
        File expected = new File(resourceLayer, CFG_DIR_PATH);
        File actual = wd.getFolder(FolderType.CFG);
        compareFiles(expected, actual);
        
        // GET FILE 
        expected = Paths.get(resourceLayer, CFG_DIR_PATH, "services.xml").toFile();
        actual = wd.getFile(FolderType.CFG, "services.xml");
        compareFiles(expected, actual);

        // GET LIST FILES 
        expected = Paths.get(resourceLayer, CFG_DIR_PATH, "services.xml").toFile();
        actual = wd.getFile(FolderType.CFG, "services.xml");
        compareFiles(expected, actual);
        
        // GET OR CREATE FILE #1
        Set<String> expectedList = new HashSet<>();
        expectedList.add("cfg");
        expectedList.add("test");
        Set<String> actualList = wd.listFiles(DirectoryFileFilter.DIRECTORY, FolderType.ROOT);
        Assert.assertEquals(expectedList, actualList);
        
        // GET OR CREATE FILE #2
        try {
            wd.getOrCreateFile(FolderType.LOGS, "all.log");
            Assert.fail("Create missing file on the last read-only layer");
        } catch (WorkspaceLayerException e) {
            // ok
        }
        
        // GET OR CREATE FILE #2
        try {
            wd.getWritableFile(FolderType.CFG, "services.xml");
            Assert.fail("Get writable file on the last read-only layer");
        } catch (WorkspaceLayerException e) {
            // ok
        }
        
        // CREATE FOLDER
        try {
            wd.createFolder(FolderType.ROOT, "logs");
            Assert.fail("Create folder on the last read-only layer");
        } catch (WorkspaceLayerException e) {
            // ok
        }
        
        // CREATE #1 new file
        try {
            wd.createFile(FolderType.LOGS, false, "all.log");
            Assert.fail("Create file on the last read-only layer");
        } catch (WorkspaceLayerException e) {
            // ok
        }
        
        // CREATE #2 new file
        try {
            wd.createFile(FolderType.LOGS, true, "all.log");
            Assert.fail("Create or overwrite file on the last read-only layer");
        } catch (WorkspaceLayerException e) {
            // ok
        }
        
        // CREATE #3 overwrite file
        try {
            wd.createFile(FolderType.CFG, false, "services.xml");
            Assert.fail("Overwrite file on the last read-only layer");
        } catch (WorkspaceLayerException e) {
            // ok
        }
        
        // CREATE #4 overwrite file
        try {
            wd.createFile(FolderType.CFG, true, "services.xml");
            Assert.fail("Overwrite file on the last read-only layer");
        } catch (WorkspaceLayerException e) {
            // ok
        }
        
        // REMOVE FOLDER
        try {
            wd.removeFolder(FolderType.ROOT, "test");
            Assert.fail("Remove folder on the last read-only layer");
        } catch (WorkspaceLayerException e) {
            // ok
        }
        
        // REMOVE FILE
        try {
            wd.removeFile(FolderType.ROOT, "test", "empty.txt");
            Assert.fail("Remove file on the last read-only layer");
        } catch (WorkspaceLayerException e) {
            // ok
        }
        
        
    }
	
    @Test
	public void testSecurity() throws IOException {
		File tmpDir = Files.createTempDir();
		File baseLayer = new File(tmpDir, "base");
		File topLayer = new File(tmpDir, "./top"); // test bug with root path normalization

		try {
			createWorkspaceStructure(baseLayer);

			IWorkspaceDispatcher wd = new DefaultWorkspaceDispatcherBuilder()
				.addWorkspaceLayer(baseLayer, DefaultWorkspaceLayout.getInstance())
				.addWorkspaceLayer(topLayer, DefaultWorkspaceLayout.getInstance())
				.build();

			// GET ROOT/. - Ok
			wd.getFile(FolderType.ROOT, ".");
			
			// GET ROOT/..
			try {
				wd.getFile(FolderType.ROOT, "..");
				Assert.fail();
			} catch (WorkspaceSecurityException ex) {
				// ok
			}

			// GET ROOT/../xxx
			try {
				wd.getFile(FolderType.ROOT, "../xxx");
				Assert.fail();
			} catch (WorkspaceSecurityException ex) {
				// ok
			}
			
			// GET ROOT/aaa/.. - Ok
			
			try {
				wd.getFile(FolderType.ROOT, "aaa/..");
				Assert.fail();
			} catch (FileNotFoundException ex) {
				// ok, but no such file
			}

			// FIXME: check other methods
		} finally {
			FileUtils.forceDelete(tmpDir);
		}
	}

	/**
     * @param expected
     * @param actual
     */
    private static void compareFiles(File expected, File actual) {
        Assert.assertEquals(expected.getAbsolutePath(), actual.getAbsolutePath());
        Assert.assertTrue(actual.exists());
        Assert.assertEquals(expected.isDirectory(), actual.isDirectory());
        Assert.assertEquals(expected.isFile(), actual.isFile());
    }

    private static void createWorkspaceStructure(File rootDir) {
		for (FolderType folderType : FolderType.values()) {
			File targetDir = new File(DefaultWorkspaceLayout.getInstance().getPath(rootDir, folderType));
			if (!targetDir.exists()) {
				targetDir.mkdirs();
			}
		}
	}

}
