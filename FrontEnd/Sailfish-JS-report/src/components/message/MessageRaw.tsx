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

const COPY_NOTIFICATION_TEXT = 'Text copied to the clipboard!';

export interface MessageRawProps {
    rawContent: string;
}

export function MessageRaw({ rawContent }: MessageRawProps) {
    const [hexSelectionStart, setHexSelectionStart] = React.useState<number | null>(null);
    const [hexSelectionEnd, setHexSelectionEnd] = React.useState<number | null>(null);
    const [humanSelectionStart, setHumanSelectionStart] = React.useState<number | null>(null);
    const [humanSelectionEnd, setHumanSelectionEnd] = React.useState<number | null>(null);
    const hexadecimalRef = React.useRef<HTMLPreElement>();
    const humanReadableRef = React.useRef<HTMLPreElement>();

    const decodedRawContent = Raw.decodeBase64RawContent(rawContent);
    const [
        offset,
        hexadecimal,
        humanReadable,
        beautifiedHumanReadable
    ] = Raw.getRawContent(decodedRawContent);

    useSelectListener(hexadecimalRef, e => {
        const sel = window.getSelection();
        if (sel.rangeCount === 0 || sel.anchorNode != sel.focusNode || !hexadecimalRef.current.contains(sel.anchorNode)) {
            setHexSelectionStart(null);
            setHexSelectionEnd(null);
            return;
        }
        const range = sel.getRangeAt(0);
        setHexSelectionStart(range.startOffset);
        setHexSelectionEnd(range.endOffset);
    });

    useSelectListener(humanReadableRef, e => {
        const sel = window.getSelection();
        if (sel.rangeCount === 0 || sel.anchorNode != sel.focusNode || !humanReadableRef.current.contains(sel.anchorNode)) {
            setHumanSelectionStart(null);
            setHumanSelectionEnd(null);
            return
        }
        const range = sel.getRangeAt(0);
        setHumanSelectionStart(range.startOffset);
        setHumanSelectionEnd(range.endOffset);
    });

    const renderHumanReadable = (content: string) => {
        if (hexSelectionStart === null || hexSelectionEnd === null) {
            return content;
        }
        const [startOffset, endOffset] = Raw.mapOctetOffsetsToHumanReadableOffsets(hexSelectionStart, hexSelectionEnd);

        return (<React.Fragment>
                {content.slice(0, startOffset)}
                <strong className="highlited">{content.slice(startOffset, endOffset)}</strong>
                {content.slice(endOffset)}
            </React.Fragment>
        );
    }

    const renderOctet = (content: string) => {
        if (humanSelectionStart === null || humanSelectionEnd === null) {
            return content;
        }
        const [startOffset, endOffset] = Raw.mapHumanReadableOffsetsToOctetOffsets(humanSelectionStart, humanSelectionEnd);

        return (<React.Fragment>
                {content.slice(0, startOffset)}
                <strong className="highlited">{content.slice(startOffset, endOffset)}</strong>
                {content.slice(endOffset)}
            </React.Fragment>
        );
    }

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
                    <pre ref={hexadecimalRef}>{renderOctet(hexadecimal)}</pre>
                    <div className="mc-raw__copy-btn   mc-raw__copy-icon"
                        onClick={() => copyHandler(hexadecimal)}
                        title="Copy to clipboard" />
                </div>
                <div className="mc-raw__column primary">
                    <pre ref={humanReadableRef}>{renderHumanReadable(beautifiedHumanReadable)}</pre>
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
