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
package com.exactpro.sf.center;

public interface IVersion {
    
    String CORE_VERSION_PROPERTY = "core_version";
    
    String GENERAL = "General";
    
    String buildShortVersion();
    
    String buildVersion();
    
    int getMajor();
    
    int getMinor();
    
    int getMaintenance();
    
    int getBuild();
    
    String getAlias();
    
    String getBranch();
    
    String getRevision();
    
    /** This variable contains minimum core revision for plug-ins and real revision for core */
    int getMinCoreRevision();
    
    boolean isGeneral();

    /** If true some of the plugin components maybe missed */
    default boolean isLightweight() {
        return false;
    }

    String getArtifactName();

    /**
     * The method returns {@link String} with artifactVersion that is using to identify the artifact from Deployer response
     * @return artifact version in format major.minor.maintenance.build
     */
    default String getArtifactVersion() {
        //TODO probably, we should remove that method when the Sailfish stops using deployer by itself
        return String.valueOf(getMajor() == -1 ? 0 : getMajor())
                + '.'
                + (getMinor() == -1 ? 0 : getMinor())
                + '.'
                + (getMaintenance() == -1 ? 0 : getMaintenance())
                + '.'
                + (getBuild() == -1 ? 0 : getBuild());
    }
}
