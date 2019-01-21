import { h } from "preact";
import Action from "../models/Action";
import ParamsTable from "./ParamsTable";
import ExpandablePanel from "./ExpandablePanel";
import "../styles/action.scss";
import { getSecondsPeriod } from "../helpers/dateFormatter";

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
    const rootClassName = ["action-card", status.status.toLowerCase(), (isRoot && !isSelected ? "root" : null),
        (isSelected ? "selected" : null)].join(' '),
        headerClassName = ["action-card-header", status.status.toLowerCase()].join(' ');

    const time = getSecondsPeriod(startTime, finishTime);

    return (
        <div class={rootClassName}
            onClick={e => {
                if (!onSelect) return;
                onSelect(action);
                // here we cancel handling by parent divs
                e.cancelBubble = true;
            }}
            key={action.id}>
            <ExpandablePanel>
                <div class={headerClassName}>
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