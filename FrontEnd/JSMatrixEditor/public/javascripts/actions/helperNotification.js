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
import * as c from 'consts';
import TheAppState from 'state/TheAppState.js';

const helpers = {
    error(title, message, exception) {
        this.notify('danger', title, message);
        this.logError(title + ': ' + message, exception);
    },

    info(title, message, exception) {
        this.notify('info', title, message);
        this.logInfo(title + ': ' + message, exception);
    },

    warning(title, message, exception) {
        this.notify('warning', title, message);
        this.logWarning(title + ': ' + message, exception);
    },

    success(title, message) {
        this.notify('success', title, message);
    },

    notify(severity, title, message) {
        TheAppState.select('errors', 'list').push({
            severity: severity, // possible values: success info warning danger
            message: title,
            rootCause: message,
            visible: true
        });
    },

    logError(message, exception) {
        this.log('error', message, exception);
    },

    logWarning(title, message, exception) {
        this.log('warn', message, exception);
    },

    logInfo(title, message, exception) {
        this.log('info', message, exception);
    },

    log(level, message, exception) {
        let stacktrace = exception && exception.stack && exception.stack.split('\n') || [];
        window.serverLogger && window.serverLogger.log(level, message, stacktrace);
        console && console.error(message, exception);
    },

    /**
     *
     * @param {object} error
     * @param {string} object.message
     * @param {string} object.rootCause
     */
    addError(error) {
        TheAppState.select('errors', 'list').push({
            severity: 'danger',
            message: error.message,
            rootCause: error.rootCause,
            visible: true
        });
    }
};

export default helpers;
