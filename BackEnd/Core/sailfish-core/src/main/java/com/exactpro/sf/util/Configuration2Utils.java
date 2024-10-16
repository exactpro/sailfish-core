/******************************************************************************
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
 ******************************************************************************/
package com.exactpro.sf.util;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.util.List;

public class Configuration2Utils {
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
