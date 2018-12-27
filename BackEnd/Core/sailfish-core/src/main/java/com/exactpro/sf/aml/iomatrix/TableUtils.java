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
package com.exactpro.sf.aml.iomatrix;

public class TableUtils {

    public static String columnAdress(int col) {

        if (col <= 26) {
            return Character.toString((char) (col + 64));
        }

        int div = col / 26;
        int mod = col % 26;
        if (mod == 0) {
            mod = 26;
            div--;
        }

        return columnAdress(div) + columnAdress(mod);
    }
}
