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

import {h, Component} from "preact";
import "../styles/messages.scss";
import { MessageCardProps, MessageCard } from "./MessageCard";
import Message from '../models/Message';
import { MessageCardActionChips } from "./MessageCardActionChips";
import { StatusType } from "../models/Status";
import { createSelector, createBemBlock } from '../helpers/styleCreators';

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

            const expandButtonClass = createBemBlock(
                "mc-expand-btn", 
                props.message.content.rejectReason != null ? "rejected" : null
            );

            return (
                <div style={{position: "relative"}}>
                    <MessageCard {...props}/>
                    <div class={expandButtonClass}>
                        <div class="mc-expand-btn__icon" onClick={this.expandButtonHandler}/>
                    </div>
                </div>
            );
        }

        const rootClass = createBemBlock(
            "message-card",
            props.status,
            props.isSelected ? "selected" : null
        );

        return (
            <div class={rootClass}
                onClick={() => props.selectHandler(props.message)}>
                <div class="message-card__labels">
                    {this.renderMessageTypeLabels(props.message)}
                </div>
                <div class="message-card__header   mc-header small"
                    data-lb-count={this.getLabelsCount(props.message)}>
                    <div class="mc-header__info">
                        <MessageCardActionChips
                            actions={props.actions}
                            selectedStatus={props.status}
                            selectHandler={status => props.selectHandler(props.message, status)}/>
                    </div>
                    <div class="mc-header__name">Name</div>
                    <div class="mc-header__name-value">{props.message.msgName}</div>
                    <div class="mc-header__expand">
                        <div class="mc-header__expand-icon" onClick={this.expandButtonHandler}/>
                    </div>
                </div>
            </div>
        );
    }

    private renderMessageTypeLabels(message: Message): JSX.Element[] {
        let labels = [];

        if (message.content.rejectReason !== null) {
            labels.push(
                <div class="mc-label rejected">
                    <div class="mc-label__icon rejected" style={{marginTop: "10px"}}/>
                </div>
            );
        }

        if (message.content.admin) {
            labels.push(
                <div class="mc-label admin">
                    <div class="mc-label__icon admin" style={{marginTop: "10px"}}/>
                </div>
            )
        }

        return labels;
    }

    private getLabelsCount(message: Message) {
        let count = 0;

        if (message.content.rejectReason != null) {
            count++;
        }

        if (message.content.admin) {
            count++;
        }

        return count;
    }

    private expandButtonHandler = () => {
        this.setState({
            isExpanded: !this.state.isExpanded
        });
    }
}