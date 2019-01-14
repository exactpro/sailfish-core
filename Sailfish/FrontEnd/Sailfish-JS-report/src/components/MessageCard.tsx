import {h, Component} from 'preact';
import '../styles/messages.scss';
import Message from '../models/Message';
import { StatusType, statusValues } from '../models/Status';
import Action from '../models/Action';

interface MessageCardProps {
    message: Message;
    isSelected?: boolean;
    actionsMap: Map<number, Action>;
    status?: StatusType;
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

        if (nextProps.message !== this.props.message || nextProps.status !== this.props.status) {
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
    
    render({message, isSelected, actionsMap, status}: MessageCardProps, {showRaw}: MessageCardState) {
        const { msgName, timestamp, from, to, contentHumanReadable, raw } = message,
            rootClass = ["message-card", (status || "").toLowerCase(), (isSelected ? "selected" : "")].join(" "),
            contentClass = ["message-card-content", (status || "").toLowerCase()].join(" "),
            showRawClass = ["message-card-content-showraw-icon", (showRaw ? "expanded" : "hidden")].join(" ");

        const actions = [...actionsMap.values()];

        return(
            <div class={rootClass}>
                <div class="message-card-header">
                    <div class="message-card-header-action">
                        {statusValues.map(statusValue => {
                            //getting status chip element for each status
                            const statusCount = actions.filter(action => action.status.status === statusValue).length,
                                className = ["message-card-header-action-chip", statusValue.toLowerCase()].join(' ');

                            return statusCount ? (<div class={className}><p>{statusCount}</p></div>) : null;
                        })}
                    </div>
                    <div class="message-card-header-timestamp-value">
                        <p>{timestamp}</p>
                    </div>
                    <div class="message-card-header-name-value">
                        <p>{msgName}</p>
                    </div>
                    <div class="message-card-header-from-value">
                        <p>{from}</p>
                    </div>
                    <div className="message-card-header-to-value">
                        <p>{to}</p>
                    </div>
                    <div class="message-card-header-name">
                        <span>Name</span>
                    </div>
                    <div class="message-card-header-session">
                        <span>Session</span>
                    </div>
                    <div class="message-card-header-session-icon"/>
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