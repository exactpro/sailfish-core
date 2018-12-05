import { h } from "preact";
import Action from "../models/Action";
import ParamsTable from "./ParamsTable";
import ExpandablePanel from "./ExpandablePanel";
import CompasionTable from "./ComparisonTable";
import "../styles/action.scss";
import Verification from '../models/Verification';
import MessageAction from '../models/MessageAction';

interface CardProps {
    action: Action;
    onSelect?: (action: Action) => void;
    onMessageSelect: (id: number) => void;
    selectedMessage: number;
    children?: JSX.Element[];
    isSelected?: boolean;
}

export const ActionCard = ({ action, onMessageSelect, selectedMessage, children, isSelected, onSelect }: CardProps) => {
    const {
        name,
        description,
        parameters,
    } = action;
    const className = ["action-card", action.status.status.toLowerCase(),
        (isSelected ? "selected" : "")].join(' ');

    return (
        <div class={className}
            onClick={e => {
                if (!onSelect) return;
                onSelect(action);
                // here we cancel handling by parent divs
                e.cancelBubble = true;
            }}
            key={action.id}>
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
                            name={name} />
                    </ExpandablePanel>
                    {
                        // rendering inner actions
                        {children}
                    }
                </div>
            </ExpandablePanel>
        </div>)
}