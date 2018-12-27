/*******************************************************************************
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

package com.exactpro.sf.services;

import com.exactpro.sf.configuration.workspace.DefaultWorkspaceDispatcherBuilder;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FileSessionStorageTest {

    private static IWorkspaceDispatcher workspaceDispatcher;

    private FileSessionStorage sessionManager;

    @BeforeClass
    public static void initWorkspace() throws IOException {
        workspaceDispatcher = new DefaultWorkspaceDispatcherBuilder()
                .addWorkspaceLayer(Files.createTempDirectory("test").toFile(), DefaultWorkspaceLayout.getInstance()).build(true);
    }

    @Before
    public void setUp() throws Exception {
        sessionManager = new FileSessionStorage(workspaceDispatcher, "dummy", "myservice");
    }

    @After
    public void tearDown() throws Exception {
        sessionManager.flush();
    }

    @Test
    public void aTestFlush() {

        sessionManager.putSessionProperties(new HashMap<String, String>() {{
            put("Test", RandomStringUtils.randomAlphabetic(10));
        }});
    }

    @Test
    public void bTestUpdate() {

        sessionManager.putSessionProperty("Test", "changed");
        sessionManager.putSessionProperty("Test2", "new");
    }

    @Test
    public void cRestore() {

        Assert.assertEquals("changed", sessionManager.readSessionProperty("Test"));
        Assert.assertEquals("new", sessionManager.readSessionProperty("Test2"));

        sessionManager.putSessionProperties(new HashMap<String, String>() {{
            put("Test", "updated");
            put("Test2", "updated");
        }});

        Assert.assertEquals("updated", sessionManager.readSessionProperty("Test"));
        Assert.assertEquals("updated", sessionManager.readSessionProperty("Test2"));
    }
}