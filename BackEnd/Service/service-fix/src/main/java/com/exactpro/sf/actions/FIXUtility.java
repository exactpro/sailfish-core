/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.actions;

import java.time.LocalDateTime;

class FIXUtility {
    protected static long extractMillisecFromMinut(LocalDateTime localDateTime) {
        int minut = localDateTime.getMinute();
        int second = localDateTime.getSecond();
        int millisec = localDateTime.getNano() / 1_000_000;

        return millisec + (second * 1_000) + (minut * 60 * 1_000);
    }
}
