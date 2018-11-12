import { h, render, Component } from "preact";
import Action from "../models/Action";
import "../styles/action.scss";
import ParamsTable from "./ParamsTable";
import ExpandablePanel from "./ExpandablePanel";
import CompasionTable from "./ComparsionTable";

interface ICardProps {
    action: Action;
}

interface ICardState {
    status: string;
}

export default class ActionCard extends Component<ICardProps, ICardState> {

    constructor(props: ICardProps) {
        super(props);
        this.state = {
            status: props.action.status.toLowerCase()
        };
    }

    render({ action }: ICardProps, { status }: ICardState) {
        return (<div class={" card-root-" + status}>
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
                    </div>)}/>
        </div>)
    }
}