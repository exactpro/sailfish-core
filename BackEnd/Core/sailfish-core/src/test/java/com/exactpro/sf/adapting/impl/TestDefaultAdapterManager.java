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
package com.exactpro.sf.adapting.impl;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.adapting.IAdapterFactory;
import com.exactpro.sf.configuration.DefaultAdapterManager;
import com.exactpro.sf.util.AbstractTest;

public class TestDefaultAdapterManager extends AbstractTest
{
	public interface ILabelProvider
	{
		String getLabel();
	}


	public class FileLabelProvider implements ILabelProvider
	{
		private File file;

		public FileLabelProvider(File file)
		{
			if ( file == null )
				throw new NullPointerException("file");

			this.file = file;
		}


		@Override
		public String getLabel()
		{
			String label = file.getName() + " [" + file.exists() + "]";

			return label;
		}

	}

	@Test
	public void testAdapterManager()
	{
		DefaultAdapterManager adapterManager = DefaultAdapterManager.getDefault();

		adapterManager.registerAdapters(new IAdapterFactory() {

			@Override
			public Object getAdapter(Object adaptableObject,
					Class<?> adapterType)
			{
				return new FileLabelProvider((File)adaptableObject);
			}

			@Override
			public Class<?>[] getAdapterList() {
				return new Class<?>[]{ILabelProvider.class};
			}

			}, File.class);


		File file = new File("max.txt");

		ILabelProvider provider = (ILabelProvider)adapterManager.getAdapter(file, ILabelProvider.class);

		Assert.assertEquals("max.txt [false]", provider.getLabel());

	}

}
