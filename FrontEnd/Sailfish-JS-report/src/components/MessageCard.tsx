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

import { h, Component } from 'preact';
import Message from '../models/Message';
import { StatusType } from '../models/Status';
import Action from '../models/Action';
import { MessageRaw } from './MessageRaw';
import { copyTextToClipboard } from '../helpers/copyHandler';
import { showNotification } from '../helpers/showNotification';
import { getHashCode } from '../helpers/stringHash';
import { formatTime } from '../helpers/dateFormatter';
import { MessageCardActionChips } from './MessageCardActionChips';
import '../styles/messages.scss';

const HUE_SEGMENTS_COUNT = 36;

export interface MessageCardProps {
    message: Message;
    isSelected?: boolean;
    actions: Action[];
    status?: StatusType;
    rejectedMessagesCount?: number;
}

interface MessageCardState {
    showRaw: boolean;
}

export class MessageCard extends Component<MessageCardProps, MessageCardState> {

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

    render({ message, isSelected, actions, status, rejectedMessagesCount }: MessageCardProps, { showRaw }: MessageCardState) {
        const { msgName, timestamp, from, to, contentHumanReadable, raw } = message;

        const hueValue = this.calculateHueValue(from, to),
            rejectedTitle = message.content.rejectReason,
            labelsCount = this.getLabelsCount(message);

        const rootClass = [
                "message",
                (status || "").toLowerCase(),
                (isSelected ? "selected" : "")
            ].join(" "),
            contentClass = [
                "message-card-content",
                (status || "").toLowerCase()
            ].join(" "),
            showRawClass = [
                "message-card-content-controls-showraw-icon",
                (showRaw ? "expanded" : "hidden")
            ].join(" ");

        return (
            <div class={rootClass}>
                <div class="message-label">
                    { this.renderMessageTypeLabels(message) }
                </div>
                <div class="message-card">
                    <div class="message-card-header" data-lb-count={labelsCount}>
                        <div class="message-card-header-action">
                            {
                                rejectedMessagesCount && actions.length == 0 ? 
                                (
                                    <div class="message-card-header-action-rejected">
                                        <p>Rejected {rejectedMessagesCount}</p>
                                    </div>
                                )
                                : (
                                    <MessageCardActionChips
                                        actions={actions}
                                        selectedStatus={status}/>
                                )
                            }
                        </div>
                        <div class="message-card-header-timestamp-value">
                            <p>{formatTime(timestamp)}</p>
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
                        {
                            from && to ?
                                <div class="message-card-header-session-icon"
                                    style={{ filter: `invert(1) sepia(1) saturate(5) hue-rotate(${hueValue}deg)` }} />
                                : null
                        }
                        {/* DISABLED */}
                        <div class="message-card-header-prediction"
                            title="Not implemented">
                            <div class="message-card-header-prediction-icon"
                                onClick={() => alert("Not implemented...")} />
                        </div>
                    </div>
                    <div class={contentClass}>
                        {
                            (message.content.rejectReason !== null) ?
                            (
                                <div class="message-card-content-title">
                                    <p>{rejectedTitle}</p>
                                </div>
                            )
                            : null
                        }
                        <div class="message-card-content-human">
                            <p>{contentHumanReadable}</p>
                        </div>
                        <div class="message-card-content-controls">
                            {
                                (raw && raw !== "null") ?
                                    <div class="message-card-content-controls-showraw"
                                        onClick={e => this.showRaw()}>
                                        <div class="message-card-content-controls-showraw-title">
                                            <span>{showRaw ? "Close raw" : "Show raw"}</span>
                                        </div>
                                        <div class={showRawClass} />
                                    </div>
                                    : null
                            }
                            {
                                showRaw ?
                                    (<div class="message-card-content-controls-copy-all"
                                        onClick={() => this.copyToClipboard(raw)}
                                        title="Copy all raw content to clipboard">
                                        <div class="message-card-content-controls-copy-all-icon" />
                                        <div class="message-card-content-controls-copy-all-title">
                                            <span>Copy All</span>
                                        </div>
                                    </div>)
                                    : null
                            }
                        </div>
                        {
                            showRaw ?
                                <MessageRaw
                                    rawContent={raw}
                                    copyHandler={this.copyToClipboard} />
                                : null
                        }
                    </div>
                </div>
            </div>
        );
    }

    private renderMessageTypeLabels(message: Message): JSX.Element[] {
        let labels = [];

        if (message.content.rejectReason !== null) {
            labels.push(
                <div class="message-label-rejected">
                    <div class="message-label-rejected-icon"/>
                </div>
            );
        }

        if (message.content.admin) {
            labels.push(
                <div class="message-label-admin">
                    <div class="message-label-admin-icon"/>
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

    private copyToClipboard(text: string) {
        copyTextToClipboard(text);
        showNotification('Text copied to the clipboard!');
    }

    private calculateHueValue(from: string, to: string): number {
        return (getHashCode([from, to].filter(str => str).sort((a, b) => a.localeCompare(b)).join(''))
            % HUE_SEGMENTS_COUNT) * (360 / HUE_SEGMENTS_COUNT);
    }
}