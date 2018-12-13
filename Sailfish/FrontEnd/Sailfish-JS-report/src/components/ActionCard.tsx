import { h } from "preact";
import Action from "../models/Action";
import ParamsTable from "./ParamsTable";
import ExpandablePanel from "./ExpandablePanel";
import CompasionTable from "./VerificationTable";
import "../styles/action.scss";
import Verification from '../models/Verification';
import MessageAction from '../models/MessageAction';

interface CardProps {
    action: Action;
    onSelect?: (action: Action) => void;
    children?: JSX.Element[];
    isSelected?: boolean;
    isRoot?: boolean;
}

export const ActionCard = ({ action, children, isSelected, onSelect, isRoot }: CardProps) => {
    const {
        name,
        status,
        description,
        parameters,
        startTime,
        finishTime
    } = action;
    const className = ["action-card", status.status.toLowerCase(), (isRoot && !isSelected ? "root" : null),
        (isSelected ? "selected" : null)].join(' ');

    let time = "";
    if (startTime && finishTime) {
        const date =  new Date(new Date(finishTime).getTime() - new Date(startTime).getTime());
        time = `${date.getSeconds()}.${date.getMilliseconds()}s`;
    }

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
                    <div class="action-card-header-name">
                        <h3>{name}</h3>
                        <p>{description}</p>
                    </div>
                    <div class="action-card-header-status">
                        <h3>{status.status.toUpperCase()}</h3>
                        <h3>{time}</h3>
                    </div>
                </div>
                <div class="action-card-body">
                    <div class="action-card-body-params">
                        <ExpandablePanel>
                            <h4>Input parameters</h4>
                            <ParamsTable
                                params={parameters}
                                name={name} />
                        </ExpandablePanel>
                    </div>
                    {
                        // rendering inner actions
                        {children}
                    }
                </div>
            </ExpandablePanel>
        </div>)
}