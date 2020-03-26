/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

import * as React from 'react';
import * as Raw from '../../helpers/rawFormatter';
import { copyTextToClipboard } from '../../helpers/copyHandler';
import { showNotification } from '../../helpers/showNotification';
import useSelectListener from '../../hooks/useSelectListener';
import '../../styles/messages.scss';
import SearchableContent from "../search/SearchableContent";
import { keyForMessage } from "../../helpers/keys";

const COPY_NOTIFICATION_TEXT = 'Text copied to the clipboard!';

interface Props {
    rawContent: string;
    messageId: number;
}

export function MessageRaw({ rawContent, messageId }: Props) {
    const hexadecimalRef = React.useRef<HTMLPreElement>();
    const humanReadableRef = React.useRef<HTMLPreElement>();
    const [hexSelectionStart, hexSelectionEnd] = useSelectListener(hexadecimalRef);
    const [humanSelectionStart, humanSelectionEnd] = useSelectListener(humanReadableRef);

    const decodedRawContent = React.useMemo(
        () => Raw.decodeBase64RawContent(rawContent),
        [rawContent]
    );

    const [
        offset,
        hexadecimal,
        humanReadable,
        beautifiedHumanReadable
    ] = Raw.getRawContent(decodedRawContent);

    const renderHumanReadable = (content: string) => {
        if (hexSelectionStart == hexSelectionEnd) {
            return (
                <SearchableContent
                    contentKey={keyForMessage(messageId, 'rawHumanReadable')}
                    content={content}/>
            );
        }

        const [startOffset, endOffset] = Raw.mapOctetOffsetsToHumanReadableOffsets(hexSelectionStart, hexSelectionEnd);

        return (
            <React.Fragment>
                {content.slice(0, startOffset)}
                <mark className="mc-raw__highlighted-content">
                    {content.slice(startOffset, endOffset)}
                </mark>
                {content.slice(endOffset)}
            </React.Fragment>
        );
    };

    const renderOctet = (content: string) => {
        if (humanSelectionStart == humanSelectionEnd) {
            return (
                <SearchableContent
                    contentKey={keyForMessage(messageId, 'rawHex')}
                    content={content}/>
            );
        }

        const [startOffset, endOffset] = Raw.mapHumanReadableOffsetsToOctetOffsets(humanSelectionStart, humanSelectionEnd);

        return (
            <React.Fragment>
                {content.slice(0, startOffset)}
                <mark className="mc-raw__highlighted-content">
                    {content.slice(startOffset, endOffset)}
                </mark>
                {content.slice(endOffset)}
            </React.Fragment>
        );
    };

    const copyAll = () => copyHandler(Raw.getAllRawContent(decodedRawContent));

    return (
        <div className="mc-raw">
            <div className="mc-raw">
                <div className="mc-raw__title">Raw message</div>
                <div className="mc-raw__copy-all"
                    onClick={copyAll}
                    title="Copy all raw content to clipboard">
                    <div className="mc-raw__copy-icon" />
                    <div className="mc-raw__copy-title">
                        <span>Copy All</span>
                    </div>
                </div>
            </div>
            <div className="mc-raw__content">
                <div className="mc-raw__column secondary">
                    <pre>{offset}</pre>
                </div>
                <div className="mc-raw__column primary">
                    <pre className="mc-raw__content-part"
                         ref={hexadecimalRef}>
                        {renderOctet(hexadecimal)}
                    </pre>
                    <div className="mc-raw__copy-btn   mc-raw__copy-icon"
                        onClick={() => copyHandler(hexadecimal)}
                        title="Copy to clipboard" />
                </div>
                <div className="mc-raw__column primary">
                    <pre className="mc-raw__content-part"
                        ref={humanReadableRef}>
                        {renderHumanReadable(beautifiedHumanReadable)}
                    </pre>
                    <div className="mc-raw__copy-btn   mc-raw__copy-icon"
                        onClick={() => copyHandler(humanReadable)}
                        title="Copy to clipboard" />
                </div>
            </div>
        </div>
    )
}

function copyHandler(content: string) {
    copyTextToClipboard(content);
    showNotification(COPY_NOTIFICATION_TEXT);
}
