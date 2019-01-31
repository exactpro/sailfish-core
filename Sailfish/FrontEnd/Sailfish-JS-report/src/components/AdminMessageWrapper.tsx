import {h, Component} from "preact";
import "../styles/messages.scss";
import { MessageCardProps, MessageCard } from "./MessageCard";
import Message from '../models/Message';

interface WrapperProps extends MessageCardProps {
    isExpanded: boolean;
}

interface WrapperState {
    isExpanded: boolean;
}

export class AdminMessageWrapper extends Component<WrapperProps, WrapperState> {

    constructor(props: WrapperProps) {
        super(props);

        this.state = {
            isExpanded: props.isExpanded
        }
    }

    componentWillReceiveProps(nextProps: WrapperProps) {
        if (this.props.isExpanded !== nextProps.isExpanded) {
            this.setState({isExpanded: nextProps.isExpanded})
        }
    }

    render(props: WrapperProps, {isExpanded}: WrapperState) {

        if (isExpanded) {
            return (
                <div style={{position: "relative"}}>
                    <MessageCard {...props}/>
                    <div class="message-expand">
                        <div class="message-expand-icon" onClick={this.expandButtonHandler}/>
                    </div>
                </div>
            );
        }

        const rootClass = [
            "message",
            (props.isSelected ? "selected" : "")
        ].join(' ');

        return (
            <div class={rootClass}>
                <div class="message-label">
                    {this.renderMessageTypeLabels(props.message)}
                </div>
                <div class="message-wrapper">
                    <div class="message-wrapper-actionchips"/>
                    <div class="message-wrapper-name">Name</div>
                    <div class="message-wrapper-name-value">{props.message.msgName}</div>
                    <div class="message-wrapper-expand">
                        <div class="message-wrapper-expand-icon" onClick={this.expandButtonHandler}/>
                    </div>
                </div>
            </div>
        );
    }

    private renderMessageTypeLabels(message: Message): JSX.Element[] {
        const labels = [];

        if (message.isAdmin) {
            labels.push(
                <div class="message-label-admin">
                    <div class="message-label-admin-icon" style={{marginTop: "10px"}}/>
                </div>
            )
        }

        if (message.isRejected) {
            labels.push(
                <div class="message-label-rejected">
                    <div class="message-label-rejected-icon" style={{marginTop: "10px"}}/>
                </div>
            );
        }

        return labels;
    }

    private expandButtonHandler = () => {
        this.setState({
            isExpanded: !this.state.isExpanded
        });
    }
}