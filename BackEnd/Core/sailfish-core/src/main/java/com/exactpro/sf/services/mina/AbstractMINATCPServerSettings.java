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

package com.exactpro.sf.services.mina;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.RequiredParam;
import com.exactpro.sf.services.codecs.ICodecSettings;
import com.exactpro.sf.services.util.ServiceUtil;

public class AbstractMINATCPServerSettings extends  AbstractMINASettings implements ICodecSettings {

    @Description("Service`s hostname. Example: localhost")
    @RequiredParam
    private String host = "localhost";

    @Description("Service`s port. Example: 8888")
    @RequiredParam
    private int port = 8888;

    @Description("Fields value for which allow receive messages.\n"
            + "Example: '1, A' or '"+ ServiceUtil.ALIAS_PREFIX + "dataA'")
    private String filterValues;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public SailfishURI getDictionaryURI() {
        return dictionaryName;
    }

    @Override
    public String getFilterValues() {
        return filterValues;
    }

    public void setFilterValues(String filterValues) {
        this.filterValues = filterValues;
    }
}
