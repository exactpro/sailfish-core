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

import java.io.InputStream;

import com.exactpro.sf.center.IVersion;

/**
 * @author sergey.smirnov
 *
 */
public class LoadableManagerContext implements ILoadableManagerContext {

    private IVersion version;
    private String folder;
    private ClassLoader[] classLoaders;
    private InputStream stream;
    
    public LoadableManagerContext() {
        
    }
    
    public LoadableManagerContext(IVersion version, String folder, InputStream stream, ClassLoader...classLoaders) {
        this.version = version;
        this.folder = folder;
        this.stream = stream;
        this.classLoaders = classLoaders;
    }
    
    /* (non-Javadoc)
     * @see com.exactpro.sf.configuration.ILoadableManagerContext#getPluginVersion()
     */
    @Override
    public IVersion getVersion() {
        return version; 
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.configuration.ILoadableManagerContext#getPluginFolder()
     */
    @Override
    public String getResourceFolder() {
        return folder;
    }

    /**
     * @param version the pluginVersion to set
     */
    public LoadableManagerContext setVersion(IVersion version) {
        this.version = version;
        return this;
    }

    /**
     * @param pluginFolder the pluginFolder to set
     */
    public LoadableManagerContext setResourceFolder(String pluginFolder) {
        this.folder = pluginFolder;
        return this;
    }

    /**
     * @param classLoaders the classLoaders to set
     */
    public LoadableManagerContext setClassLoaders(ClassLoader... classLoaders) {
        this.classLoaders = classLoaders;
        return this;
    }

    /**
     * @return the classLoaders
     */
    public ClassLoader[] getClassLoaders() {
        return classLoaders;
    }

    /**
     * @return the stream
     */
    public InputStream getResourceStream() {
        return stream;
    }

    /**
     * @param stream the stream to set
     */
    public LoadableManagerContext setResourceStream(InputStream stream) {
        this.stream = stream;
        return this;
    }    
    

}
