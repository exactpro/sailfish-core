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
package com.exactpro.sf.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class FormatHelper {

    public static String buildFormatString(int precision) {
        StringBuilder formatString = new StringBuilder().append('#');
        if (precision > 0) {
            formatString.append('.');
            for (int i = 0; i < precision; i++) {
                formatString.append('#');
            }
        }
        return formatString.toString();
    }

    public static double formatDouble(String formatString, double d) {
        DecimalFormatSymbols fs = new DecimalFormatSymbols();
        fs.setDecimalSeparator('.');
        return Double.valueOf(new DecimalFormat(formatString, fs).format(d));
    }
}
