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

import com.exactpro.sf.common.codecs.AbstractCodec
import com.exactpro.sf.common.messages.IMessageFactory
import com.exactpro.sf.common.util.ICommonSettings
import com.exactpro.sf.configuration.dictionary.ITCHDictionaryValidatorFactory
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator
import com.exactpro.sf.configuration.factory.ITCHMessageFactory
import com.exactpro.sf.externalapi.DictionaryType
import com.exactpro.sf.services.MessageHelper
import com.exactpro.sf.services.itch.ITCHCodec
import com.exactpro.sf.services.itch.ITCHCodecSettings
import com.exactpro.sf.services.itch.ITCHMessageHelper

class ExternalItchCodecFactory : AbstractExternalMinaCodecFactory() {
    override val codecClass: Class<out AbstractCodec> = ITCHCodec::class.java
    override val messageFactoryClass: Class<out IMessageFactory> = ITCHMessageFactory::class.java
    override val settingsClass: Class<out ICommonSettings> = ITCHCodecSettings::class.java
    override val messageHelperClass: Class<out MessageHelper> = ITCHMessageHelper::class.java
    override val messageHelperParams: Map<String, String> = mapOf(ITCHMessageHelper.FIELD_MARKET_DATA_GROUP_NAME to "0",
            ITCHMessageHelper.FIELD_SEQUENCE_NUMBER_NAME to "0",
            ITCHMessageHelper.FIELD_MESSAGE_COUNT_NAME to "1",
            ITCHMessageHelper.FIELD_LENGTH_NAME to "0"
    )
    override val dictionaryValidators: Map<DictionaryType, IDictionaryValidator> =
        mapOf(DictionaryType.MAIN to ITCHDictionaryValidatorFactory().createDictionaryValidator())
}