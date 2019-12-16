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

package com.exactpro.sf.testwebgui;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class GuiUtil {
    private static final Pattern WORD_BOUNDARY = Pattern.compile("([a-z])([A-Z])"); // (g)(T)
    private static final Pattern WORD_BOUNDARY_UPPERCASE = Pattern.compile("([A-Z])([A-Z][a-z])"); // (G)(Tt)

    /**
     * Converts camel-case formatted variable name into a one where words are separated by spaces
     * @param name variable name
     * @return reformatted variable name
     */
    public static String getReadableName(String name) {
        name = StringUtils.capitalize(name); // httpMessage -> HttpMessage
        name = WORD_BOUNDARY.matcher(name).replaceAll("$1 $2"); // HttpMessage -> Http Message
        name = WORD_BOUNDARY_UPPERCASE.matcher(name).replaceAll("$1 $2"); // HTTPMessage -> HTTP Message
        return name;
    }
}
