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
        <ExpandablePanel
            header={(
                <div class="card-header">
                    <h3>{action.name}</h3>
                    <p>{action.description}</p>
                </div>)}
            body={(
                <div class="card-body">
                    <ExpandablePanel
                        header={(
                            <h4>Input parameters</h4>
                        )}
                        body={(
                            <ParamsTable
                                params={action.parameters}
                                name={action.name}/>
                        )}/>
                    <ExpandablePanel
                        header={(
                            <h4>Comparsion parameters</h4>
                        )}
                        body={(
                            <CompasionTable params={action.verifications}/>
                        )}/>
                </div>)}/>)
}