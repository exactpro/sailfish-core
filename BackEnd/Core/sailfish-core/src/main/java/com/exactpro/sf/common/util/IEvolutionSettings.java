/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.common.util;

/**
 * This interface is used to inform the Sailfish and its components
 * to use specific logic to support the interaction with Evolution project.
 */
public interface IEvolutionSettings extends ICommonSettings {
    /**
     * Indicate whether the component should use logic for evolution.
     * @return true if the component should use logic for evolution.
     */
    boolean isEvolutionSupportEnabled();

    default void setEvolutionSupportEnabled(boolean evolutionSupportEnabled){}
}
