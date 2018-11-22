import { h } from "preact";
import Action from "../models/Action";
import ParamsTable from "./ParamsTable";
import ExpandablePanel from "./ExpandablePanel";
import CompasionTable from "./ComparisonTable";
import "../styles/action.scss";
import Verification from '../models/Verification';

interface CardProps {
    action: Action;
}

export const ActionCard = ({ action }: CardProps) => {
    const {
        name,
        description,
        parameters,
        verifications
    } = action;
    return (
        <ExpandablePanel>
            <div class="card-header">
                <h3>{name}</h3>
                <p>{description}</p>
            </div>
            <div class="card-body">
                <ExpandablePanel>
                    <h4>Input parameters</h4>
                        <ParamsTable
                            params={parameters}
                            name={name}/>
                </ExpandablePanel>
                {
                    verifications.map(verification => renderVerification(verification))
                }
            </div>
        </ExpandablePanel>)
}

const renderVerification = ({name, status, entries}: Verification) => {
    const className = "card-body-verification " + 
        (status ? status.status.toLowerCase() : "");

    return (
        <div class={className}>
            <ExpandablePanel>
                <h4>{"Verification — " + name + " — " + status.status}</h4>
                <CompasionTable params={entries}/>
            </ExpandablePanel>
        </div>
    )
}