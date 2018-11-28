import { h } from "preact";
import Action from "../models/Action";
import ParamsTable from "./ParamsTable";
import ExpandablePanel from "./ExpandablePanel";
import CompasionTable from "./ComparisonTable";
import "../styles/action.scss";
import Verification from '../models/Verification';

interface CardProps {
    action: Action;
    onMessageSelect: (id: number) => void;
    selectedMessage: number;
}

export const ActionCard = ({ action, onMessageSelect, selectedMessage }: CardProps) => {
    const {
        name,
        description,
        parameters,
        verifications
    } = action;
    return (
        <ExpandablePanel>
            <div class="action-card-header">
                <h3>{name}</h3>
                <p>{description}</p>
            </div>
            <div class="action-card-body">
                <ExpandablePanel>
                    <h4>Input parameters</h4>
                        <ParamsTable
                            params={parameters}
                            name={name}/>
                </ExpandablePanel>
                {
                    verifications && verifications.map(verification => 
                        renderVerification(verification, onMessageSelect, verification.messageId === selectedMessage))
                }
            </div>
        </ExpandablePanel>)
}

const renderVerification = ({name, status, entries, messageId}: Verification, 
    selectHandelr: Function, isSelected: boolean) => {

    const className = ["action-card-body-verification", (status ? status.status.toLowerCase() : ""), 
        (isSelected ? "selected" : "")].join(' ');

    return (
        <div class={className}
            onClick={e => {
                selectHandelr(messageId);
                e.cancelBubble = true;
            }}>
            <ExpandablePanel>
                <h4>{"Verification — " + name + " — " + status.status}</h4>
                <CompasionTable params={entries}/>
            </ExpandablePanel>
        </div>
    )
}