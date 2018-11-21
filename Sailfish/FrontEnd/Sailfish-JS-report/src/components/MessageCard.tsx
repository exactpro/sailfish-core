import {h, Component} from 'preact';
import '../styles/messages.scss';
import Message from '../models/Message';

interface MessageCardProps {
    message: Message;
    isSelected?: boolean;
}

interface MessageCardState {
    showRaw: boolean;
}

export default class MessageCard extends Component<MessageCardProps, MessageCardState> {
    
    constructor(props: MessageCardProps) {
        super(props);
        this.state = {
            showRaw: false
        }
    }

    showRaw() {
        this.setState({
            ...this.state,
            showRaw: !this.state.showRaw
        })
    }
    
    render({message, isSelected}: MessageCardProps, {showRaw}: MessageCardState) {
        const {
            msgName, status, timestamp, from, to, contentHumanReadable, raw
        } = message;
        const rootClass = "message-card-root " + 
            (status ? status.toLowerCase() : "") + 
            (isSelected ? " selected" : "");
        const contentClass = "message-card-content " + 
            (status ? status.toLowerCase() : "");

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
                <div class={contentClass}>
                    <div class="message-card-content-human">
                        <p>{contentHumanReadable}</p>
                    </div>
                    {
                        showRaw ?
                        (<div class="message-card-content-raw">
                            <span class="title">Raw message</span>
                            {raw.split('\n').map((row) => {
                                return <p>{row}<br/></p>
                            })}
                        </div>)
                        : null
                    }
                    <div class="message-card-content-showraw"
                        onClick={e => this.showRaw()}>
                        <span class="expand-button">{showRaw ? "Hide row" : "Show row"}</span>
                    </div>
                </div>
            </div>
        )
    }
}