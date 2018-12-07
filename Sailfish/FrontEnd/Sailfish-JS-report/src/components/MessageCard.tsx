import {h, Component} from 'preact';
import '../styles/messages.scss';
import Message from '../models/Message';

interface MessageCardProps {
    message: Message;
    isSelected?: boolean;
    selectedStatus?: string;
}

interface MessageCardState {
    showRaw: boolean;
}

export default class MessageCard extends Component<MessageCardProps, MessageCardState> {
    
    constructor(props: MessageCardProps) {
        super(props);
        this.state = {
            showRaw: false
        };
    }

    shouldComponentUpdate(nextProps: MessageCardProps, nextState: MessageCardState) {
        if (nextState !== this.state) return true;

        if (nextProps.message !== this.props.message || nextProps.selectedStatus !== this.props.selectedStatus) {
            return true;
        } 

        return nextProps.isSelected !== this.props.isSelected;
    }

    showRaw() {
        this.setState({
            ...this.state,
            showRaw: !this.state.showRaw
        });
    }
    
    render({message, isSelected, selectedStatus}: MessageCardProps, {showRaw}: MessageCardState) {
        const { msgName, timestamp, from, to, contentHumanReadable, raw } = message,
            rootClass = ["message-card", (selectedStatus || "").toLowerCase(), (isSelected ? "selected" : "")].join(" "),
            contentClass = ["message-card-content", (status || "").toLowerCase()].join(" "),
            showRawClass = ["message-card-content-showraw-icon", (showRaw ? "expanded" : "hidden")].join(" ");

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
                                return <pre>{row}<br/></pre>
                            })}
                        </div>)
                        : null
                    }
                    <div class="message-card-content-showraw"
                        onClick={e => this.showRaw()}>
                        <span>{showRaw ? "Hide raw" : "Show raw"}</span>
                        <div class={showRawClass}/>
                    </div>
                </div>
            </div>
        );
    }
}