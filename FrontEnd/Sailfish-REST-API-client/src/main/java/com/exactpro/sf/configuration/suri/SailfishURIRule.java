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

public enum SailfishURIRule {
    REQUIRE_PLUGIN("plugin alias cannot be empty") {
        @Override
        public boolean check(SailfishURI uri) {
            return uri.getPluginAlias() != null;
        }
    },
    REQUIRE_CLASS("class alias cannot be empty") {
        @Override
        public boolean check(SailfishURI uri) {
            return uri.getClassAlias() != null;
        }
    },
    REQUIRE_RESOURCE("resource name cannot be empty") {
        @Override
        public boolean check(SailfishURI uri) {
            return uri.getResourceName() != null;
        }
    };

    public abstract boolean check(SailfishURI uri);

    private final String description;

    SailfishURIRule(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
