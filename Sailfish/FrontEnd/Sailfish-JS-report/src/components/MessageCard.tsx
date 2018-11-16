import {h} from 'preact';
import '../styles/messages.scss';
import Message from '../models/Message';

interface MessageCardProps {
    message: Message;
    isSelected?: boolean;
}

export const MessageCard = ({message, isSelected}: MessageCardProps) => {
    const {
        msgName, status, timestamp, from, to, contentHumanReadable
    } = message;
    const rootClass = "message-card-root " + status.toLowerCase() + 
        (isSelected ? " selected" : "");
    return(
        <div class={rootClass}>
            <div class="message-card-header">
                <div class="message-card-header-timestamp"> 
                    <p>{timestamp}</p>
                </div>
                <div class="message-card-header-name">
                    <span>Message Name</span>
                    <p>{msgName}</p>
                </div>
                <div class="message-card-header-from">
                    <span>From</span>
                    <p>{from}</p>
                </div>
                <div class="message-card-header-to">
                    <span>To</span>
                    <p>{to}</p>
                </div>
            </div>
            <div class="message-card-content">
                <p>{contentHumanReadable}</p>
            </div>
        </div>
    )
}