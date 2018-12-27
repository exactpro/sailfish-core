/*******************************************************************************
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

package com.exactpro.sf.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EMailUtil {
    private static final String PRE = "<pre style=\"font-size:12p; font-family: arial,sans-serif;\">"; //Save prefix/postfix indent
    private static final String CLOSE_PRE = "</pre>";

    public static final char DELIMITER = ';';

    public static final String BR = "<br/>";
    public static final String TABLE = "<table border=\"1\">";
    public static final String CLOSE_TABLE = "</table>";
    public static final String TR = "<tr>";
    public static final String CLOSE_TR = "</tr>";
    public static final String TD = "<td>";
    public static final String GREEN_TD = "<td style=\"color:green;\">";
    public static final String ORANGE_TD = "<td style=\"color:orange;\">";
    public static final String RED_TD = "<td style=\"color:red;\">";
    public static final String CLOSE_TD = "</td>";

    public static List<String> parseRecipients(String recipients) {
        return Arrays.stream(StringUtils.split(recipients, DELIMITER))
                     .map(String::trim)
                     .filter(StringUtils::isNotEmpty)
                     .collect(Collectors.toList());
    }

    public static String createSubSubject(String mainSubject, String... subSubjects) {
        StringBuilder subject = new StringBuilder(mainSubject);

        subject.append(" ");

        for (String subSubject : subSubjects) {
            subject.append("[")
                   .append(subSubject)
                   .append("]");
        }

        return subject.toString();
    }

    public static String createHtmlText(String text) {
        StringBuilder htmlText = new StringBuilder();

        return htmlText.append(PRE)
                       .append(text)
                       .append(CLOSE_PRE)
                       .toString();
    }

    private static String createTd(String text, String tdStyle) {
        StringBuilder htmlText = new StringBuilder();

        return htmlText.append(tdStyle)
                       .append(text)
                       .append(CLOSE_TD)
                       .toString();
    }

    public static String createTd(String text) {
        return createTd(text, TD);
    }

    public static String createRedTd(String text) {
        return createTd(text, RED_TD);
    }

    public static String createGreenTd(String text) {
        return createTd(text, GREEN_TD);
    }

    public static String createOrangeTd(String text) {
        return createTd(text, ORANGE_TD);
    }

}
