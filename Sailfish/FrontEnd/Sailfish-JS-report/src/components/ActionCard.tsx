import { h } from "preact";
import Action from "../models/Action";
import ParamsTable from "./ParamsTable";
import ExpandablePanel from "./ExpandablePanel";
import CompasionTable from "./ComparsionTable";
import "../styles/action.scss";

interface CardProps {
    action: Action;
}

export const ActionCard = ({ action }: CardProps) => {
    return (
        <ExpandablePanel>
            <div class="card-header">
                <h3>{action.name}</h3>
                <p>{action.description}</p>
            </div>
            <div class="card-body">
                <ExpandablePanel>
                    <h4>Input parameters</h4>
                        <ParamsTable
                            params={action.parameters}
                            name={action.name}/>
                </ExpandablePanel>
                <ExpandablePanel>
                        <h4>Comparsion parameters</h4>
                        <CompasionTable params={action.verifications}/>
                </ExpandablePanel>
            </div>
        </ExpandablePanel>)
}