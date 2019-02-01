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

import { h } from 'preact';
import { splitRawContent } from '../helpers/rawFormatter';
import '../styles/messages.scss';

export interface MessageRawProps {
    rawContent: string;
    copyHandler: (text: string) => any;
}

export const MessageRaw = ({rawContent, copyHandler}: MessageRawProps) => {
    
    const [offset, hexadecimal, humanReadable] = splitRawContent(rawContent);

    return (
        <div class="message-card-content-raw">
            <div class="message-card-content-raw-numbers">
                <pre>{offset}</pre>
            </div>
            <div class="message-card-content-raw-symbols">
                <pre>{hexadecimal}</pre>
                <div class="message-card-content-raw-copy-btn"
                    onClick={() => copyHandler(hexadecimal)}
                    title="Copy to clipboard"/>
            </div>
            <div class="message-card-content-raw-string">
                <pre>{humanReadable}</pre>
                <div class="message-card-content-raw-copy-btn"
                    onClick={() => copyHandler(humanReadable)}
                    title="Copy to clipboard"/>
            </div>
        </div>
    )
}