/*
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
 */
package com.exactpro.sf.util;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class Configuration2Utils {
	private Configuration2Utils() { }

	public static void readConfig(XMLConfiguration configuration, File configFile) throws ConfigurationException, IOException {
		try (InputStream inputStream = Files.newInputStream(configFile.toPath())) {
			new FileHandler(configuration).load(inputStream);
		}
	}

	public static XMLConfiguration readConfig(File configFile) throws ConfigurationException, IOException {
		XMLConfiguration configuration = new XMLConfiguration();
		readConfig(configuration, configFile);
		return configuration;
	}

	public static ImmutableNode createNode(String name) {
		return new ImmutableNode.Builder().name(name).create();
	}

	public static void addChildNodeAndUpdate(HierarchicalConfiguration<ImmutableNode> config, ImmutableNode newNode) {
		ImmutableNode updatedRoot = new ImmutableNode.Builder()
				.addChildren(getChildrenOfRootNode(config))
				.addChild(newNode)
				.create();
		config.getNodeModel().setRootNode(updatedRoot);
	}

	private static List<ImmutableNode> getChildrenOfRootNode(HierarchicalConfiguration<ImmutableNode> config) {
		return config.getNodeModel()
				.getNodeHandler()
				.getRootNode()
				.getChildren();
	}
}
