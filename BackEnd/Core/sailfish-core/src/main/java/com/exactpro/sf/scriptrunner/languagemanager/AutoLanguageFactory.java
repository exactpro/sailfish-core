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
package com.exactpro.sf.scriptrunner.languagemanager;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import com.exactpro.sf.aml.ICodeGenerator;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.languagemanager.exceptions.LanguageManagerException;

public class AutoLanguageFactory implements ILanguageFactory {
    public static final SailfishURI URI;

    static {
        try {
            URI = new SailfishURI(IVersion.GENERAL, null, "Auto");
        } catch(SailfishURIException e) {
            throw new LanguageManagerException(e);
        }
    }

    @Override
    public void init(ClassLoader... pluginClassLoaders) {
    }

    @Override
    public String getName() {
        return URI.getResourceName();
    }

    @Override
    public Object getReader() {
        return null;
    }

    @Override
    public ICodeGenerator getGenerator() {
        return null;
    }

    @Override
    public ICompatibilityChecker getChecker() {
        return new ICompatibilityChecker() {
            @Override
            public boolean isCompatible(Method method) {
                return false;
            }
        };
    }

    @Override
    public ClassLoader createClassLoader(URL binFolder, ClassLoader parent) {
        return new URLClassLoader(new URL[] { binFolder }, this.getClass().getClassLoader());
    }
}
