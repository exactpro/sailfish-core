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

import com.exactpro.sf.common.util.EPSCommonException;

/**
 * @author sergey.smirnov
 *
 */
public class ServiceListenerException extends EPSCommonException {
    private static final long serialVersionUID = 4247469707193494152L;

    /**
    *
    */
   public ServiceListenerException() {
   }

   /**
    * @param message
    */
   public ServiceListenerException(String message) {
       super(message);
   }

   /**
    * @param cause
    */
   public ServiceListenerException(Throwable cause) {
       super(cause);
   }

   /**
    * @param message
    * @param cause
    */
   public ServiceListenerException(String message, Throwable cause) {
       super(message, cause);
   }

   public ServiceListenerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
       super(message, cause, enableSuppression, writableStackTrace);
   }
}
