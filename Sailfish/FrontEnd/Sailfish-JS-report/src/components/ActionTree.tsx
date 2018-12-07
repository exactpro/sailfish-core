import { h, Component } from 'preact';
import Action, { ActionNode } from '../models/Action';
import { ActionTreeProps } from './ActionTree';
import { ActionCard } from './ActionCard';
import CompasionTable from "./VerificationTable";
import MessageAction from '../models/MessageAction';
import Verification from '../models/Verification'
import '../styles/action.scss';
import ExpandablePanel from './ExpandablePanel';
import Link from '../models/Link';
import { StatusType } from '../models/Status';


export interface ActionTreeProps {
    action: ActionNode;
    actionSelectHandler: (action: Action) => void;
    messageSelectHandler: (id: number) => void;
    selectedMessageId: number;
    selectedActionId: number;
    filterFields: StatusType[];
}

export class ActionTree extends Component<ActionTreeProps, {}> {


    shouldComponentUpdate(nextProps: ActionTreeProps) {
        if (nextProps.action !== this.props.action) return true;

        if (nextProps.action.actionNodeType === "action") {
            return (nextProps.action as Action).id === nextProps.selectedActionId || this.props.selectedActionId === (nextProps.action as Action).id;
        } else {
            return true;
        }
    }

    render(props: ActionTreeProps): JSX.Element {
        return this.ActionNode(props);
    }

    ActionNode(props: ActionTreeProps): JSX.Element {
        const { actionSelectHandler, messageSelectHandler, selectedActionId, selectedMessageId, filterFields } = props;

        switch (props.action.actionNodeType) {
            case 'action': {
                const action = props.action as Action;
                return (
                    <ActionCard action={action}
                        isSelected={action.id === selectedActionId}
                        onSelect={actionSelectHandler}>
                        {
                            action.subNodes ? action.subNodes.map(
                                action => <this.ActionNode {...props} action={action} />) : null
                        }
                    </ActionCard>
                );
            }

            case 'message': {
                const messageAction = props.action as MessageAction;

                return this.renderMessageAction(messageAction);
            }

            case 'verification': {
                const verification = props.action as Verification;

                return this.renderVerification(verification, messageSelectHandler,
                    verification.messageId === selectedMessageId, filterFields)
            }

            case 'link': {
                const { link } = props.action as Link;

                return (
                    <div class="action-card">
                        <h3>{"Link : " + link}</h3>
                    </div>
                );
            }

            default: {
                return null;
            }
        }
    }

    renderMessageAction({ message, level, exception, color, style }: MessageAction) {
        if (exception) {
            return (
                <div class="action-card">
                    <ExpandablePanel>
                        <div class="action-card-header">
                            {/* italic style value - only for fontStyle css property
                            bold style value - only for fontWeight css property */}
                            <h3 style={{
                                color: (color || "").toLowerCase(),
                                fontStyle: (style || "").toLowerCase(),
                                fontWeight: (style || "").toLowerCase()
                            }}>{message} - {level}</h3>
                        </div>
                        <div class="action-card-body">
                            <p>{exception && exception.stacktrace}</p>
                        </div>
                    </ExpandablePanel>
                </div>
            );
        } else {
            return (
                <div class="action-card">
                    <div class="action-card-header">
                        {/* italic style value - only for fontStyle css property
                        bold style value - only for fontWeight css property */}
                        <h3 style={{
                            color: (color || "").toLowerCase(),
                            fontStyle: (style || "").toLowerCase(),
                            fontWeight: (style || "").toLowerCase()
                        }}>{message} - {level}</h3>
                    </div>
                </div>
            );
        }
    }

    renderVerification({ name, status, entries, messageId }: Verification,
        selectHandelr: Function, isSelected: boolean, filterFields: StatusType[]) {

        const className = ["action-card-body-verification",
            (status ? status.status.toLowerCase() : ""),
            (isSelected ? "selected" : "")].join(' ');

        return (
            <div class="action-card">
                <div class={className}
                    onClick={e => {
                        selectHandelr(messageId);
                        // here we cancel handling by parent divs
                        e.cancelBubble = true;
                    }}>
                    <ExpandablePanel>
                        <h4>{"Verification — " + name + " — " + status.status}</h4>
                        <CompasionTable params={entries} filterFields={filterFields} />
                    </ExpandablePanel>
                </div>
            </div>
        )
    }
}