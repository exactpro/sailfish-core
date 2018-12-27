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
package com.exactpro.sf.services;

public enum ServiceHandlerRoute {
    FROM_APP("fromApp"),
    FROM_ADMIN("fromAdmin"),
    TO_APP("toApp"),
    TO_ADMIN("toAdmin");

    private final boolean admin;
    private final boolean from;
    private final String alias;

    private ServiceHandlerRoute(String alias) {
        this.alias = alias;
        this.from = alias.startsWith("from");
        this.admin = alias.endsWith("Admin");
    }

    public String getAlias() {
        return alias;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isFrom() {
        return from;
    }

    public static ServiceHandlerRoute get(boolean from, boolean admin) {
        for(ServiceHandlerRoute route : values()) {
            if(route.from == from && route.admin == admin) {
                return route;
            }
        }

        throw new IllegalArgumentException("No route with properties - from: " + from + ", admin: " + admin);
    }
}
