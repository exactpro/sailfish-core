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

package com.exactpro.sf.bigbutton.library;

import java.util.Arrays;
import java.util.Optional;

public enum BigButtonAction {

    Interrupt("interrupt"), //interrupt bb
    Skip("skip"), //skip script list
    SendEmail("send_email");

    private final String action;

    private BigButtonAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public static BigButtonAction parse(String actionName){
        Optional<BigButtonAction> action = Arrays.stream(BigButtonAction.values())
                                      .filter(bigButtonAction -> bigButtonAction.getAction().equalsIgnoreCase(actionName))
                                      .findFirst();

        if(!action.isPresent()){
            throw new IllegalArgumentException("Action not supported: " + actionName);
        }
        return action.get();
    }
}
