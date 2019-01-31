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

export function copyTextToClipboard(str: string) {
    // dirty hack here - we create new invisible element, select it and copy text to the clipboard
    const element = document.createElement('textarea');

    element.value = str;
    element.setAttribute('readonly', '');
    element.style.position = 'absolute';
    element.style.left = '-9000px';

    document.body.appendChild(element);

    // using select API, copy inner text to clipboard
    element.select();

    document.execCommand('copy');
    document.body.removeChild(element);
}