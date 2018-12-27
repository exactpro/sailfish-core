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
package com.exactpro.sf.storage.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;

public class HibernateFactory {

    private static final String HIBERNATE_CFG = "hibernate.cfg.xml";

	private static volatile HibernateFactory instance;

	private static final Object lock = new Object();

	private Map<File, SessionFactory> sessionFactories;

	private HibernateFactory () {
		this.sessionFactories = new HashMap<>();
	}

	public static HibernateFactory getInstance() {
		if (instance == null) {
			synchronized (lock) {
				if (instance == null) {
					instance = new HibernateFactory();
				}
			}
		}
		return instance;
	}

    @SuppressWarnings("deprecation")
    public SessionFactory getSessionFactory(IWorkspaceDispatcher workspaceDispatcher) throws WorkspaceStructureException, FileNotFoundException {

		SessionFactory sessionFactory = null;

		synchronized (sessionFactories) {

            File cfgFolder = workspaceDispatcher.getFolder(FolderType.CFG);
            File fDescr = workspaceDispatcher.getFile(FolderType.CFG, HIBERNATE_CFG);
            sessionFactory = sessionFactories.get(cfgFolder);

			if (sessionFactory == null) {
				try {
					Configuration config = new Configuration();
                    sessionFactory = config.configure(fDescr).buildSessionFactory();
					sessionFactories.put(cfgFolder, sessionFactory);
				} catch ( Throwable e ) {
					throw new EPSCommonException("Could not initialize DB Storage", e);
				}
			}
		}

		return sessionFactory;

	}

}
