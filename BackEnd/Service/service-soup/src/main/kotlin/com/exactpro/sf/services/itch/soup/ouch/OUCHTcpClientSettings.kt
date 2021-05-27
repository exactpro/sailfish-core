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

package com.exactpro.sf.services.itch.soup.ouch

import com.exactpro.sf.aml.Ignore
import com.exactpro.sf.services.RequiredParam
import com.exactpro.sf.services.itch.soup.SOUPTcpClientSettings
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement
class OUCHTcpClientSettings : SOUPTcpClientSettings() {

    @Ignore
    private var member: String? = ""

    @Ignore
    private var version: String? = ""

    @Ignore
    private var msgLength: Int = 0

    @Ignore
    private var marketDataGroup: Byte = 0

    @RequiredParam
    private var requestedSequenceNumber: Long = 0

    @RequiredParam
    private var requestedSession: String? = ""

    override fun getMember(): String? = member

    override fun setMember(member: String?) {
        this.member = member
    }

    override fun getVersion(): String? = version

    override fun setVersion(version: String?) {
        this.version = version
    }

    override fun getMsgLength(): Int = msgLength

    override fun setMsgLength(msgLength: Int) {
        this.msgLength = msgLength
    }

    override fun getMarketDataGroup(): Byte = marketDataGroup

    override fun setMarketDataGroup(marketDataGroup: Byte) {
        this.marketDataGroup = marketDataGroup
    }

    override fun getRequestedSequenceNumber(): Long = requestedSequenceNumber

    override fun setRequestedSequenceNumber(requestedSequenceNumber: Long) {
        this.requestedSequenceNumber = requestedSequenceNumber
    }

    override fun getRequestedSession(): String? = requestedSession

    override fun setRequestedSession(requestedSession: String?) {
        this.requestedSession = requestedSession
    }
}
