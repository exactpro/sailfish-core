/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
import com.exactpro.sf.configuration.dictionary.SOUPTcpDictionaryValidatorFactory
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator
import com.exactpro.sf.configuration.factory.SOUPMessageFactory
import com.exactpro.sf.externalapi.DictionaryType
import com.exactpro.sf.services.MessageHelper
import com.exactpro.sf.services.itch.soup.SOUPTcpCodec
import com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper
import com.exactpro.sf.services.itch.soup.SoupTcpCodecSettings

class ExternalSoupTcpCodecFactory : AbstractExternalMinaCodecFactory() {
    override val codecClass: Class<out AbstractCodec> = SOUPTcpCodec::class.java
    override val settingsClass: Class<out ICommonSettings> = SoupTcpCodecSettings::class.java
    override val messageFactoryClass: Class<out IMessageFactory> = SOUPMessageFactory::class.java
    override val messageHelperClass: Class<out MessageHelper> = SOUPTcpMessageHelper::class.java
    override val dictionaryValidators: Map<DictionaryType, IDictionaryValidator> = mapOf(
        DictionaryType.MAIN to SOUPTcpDictionaryValidatorFactory().createDictionaryValidator()
    )
}