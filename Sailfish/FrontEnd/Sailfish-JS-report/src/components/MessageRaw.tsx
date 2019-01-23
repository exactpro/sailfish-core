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