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
package com.exactpro.sf.externalapi.codec.impl

import com.exactpro.sf.common.messages.IMessageFactory
import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.util.ICommonSettings
import com.exactpro.sf.configuration.dictionary.impl.DefaultDictionaryValidator
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator
import com.exactpro.sf.externalapi.DictionaryType
import com.exactpro.sf.externalapi.DictionaryType.MAIN
import com.exactpro.sf.externalapi.codec.IExternalCodecFactory
import com.exactpro.sf.externalapi.codec.IExternalCodecSettings
import org.apache.commons.configuration.HierarchicalConfiguration

abstract class AbstractExternalCodecFactory : IExternalCodecFactory {
    protected abstract val settingsClass: Class<out ICommonSettings>
    protected abstract val messageFactoryClass: Class<out IMessageFactory>
    override val protocolName: String by lazy { messageFactoryClass.newInstance().protocol }
    protected open val dictionaryValidators: Map<DictionaryType, IDictionaryValidator> = emptyMap()

    override fun createSettings(): IExternalCodecSettings {
        return ExternalCodecSettings(settingsClass.newInstance())
    }

    override fun createSettings(dictionary: IDictionaryStructure): IExternalCodecSettings {
        return createSettings().apply {
            this[MAIN] = dictionary
        }
    }

    protected fun validateDictionaries(settings: IExternalCodecSettings) {
        val builder = StringBuilder()

        for (dictionaryType in settings.dictionaryTypes) {
            val dictionary = checkNotNull(settings[dictionaryType]) { "Dictionary type is not set: $dictionaryType" }

            val validator: IDictionaryValidator = dictionaryValidators[dictionaryType]
                ?: DefaultDictionaryValidator.INSTANCE

            val errors = validator.validate(dictionary, true, null)
            if (errors.isNotEmpty()) {
                errors.joinTo(
                    buffer = builder,
                    prefix = "Got following errors during '$dictionaryType' dictionary validation:${System.lineSeparator()}",
                    separator = System.lineSeparator()
                )
            }
        }

        check(builder.isEmpty()) { builder }
    }

    protected class DummyCodecSettings : ICommonSettings {
        override fun load(config: HierarchicalConfiguration?) {}
    }
}