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
package com.exactpro.sf.configuration.suri;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class SailfishURIAdapter extends XmlAdapter<String, SailfishURI> {
    @Override
    public SailfishURI unmarshal(String v) throws Exception {
        // sanitize value in case of loading old settings
        return SailfishURI.parse(SailfishURIUtils.sanitize(v));
    }

    @Override
    public String marshal(SailfishURI v) throws Exception {
        return v.toString();
    }
}
