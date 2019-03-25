/******************************************************************************
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

const NOTIFICATION_TIMEOUT = 1500;

export function showNotification(text: string) {
    const element = document.createElement('div');
    element.className = 'notification';
    element.innerHTML = `<p>${text}</p>`;

    const root = document.getElementById('index');

    root.appendChild(element);

    window.setTimeout(() => {
        root.removeChild(element);
    }, NOTIFICATION_TIMEOUT);
}