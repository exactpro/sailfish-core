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
    isCollapsed: boolean;
    status: string;
}

export default class ActionCard extends Component<ICardProps, ICardState> {

    constructor(props: ICardProps) {
        super(props);
        this.state = {
            isCollapsed: true,
            status: props.action.Status.Status.toLowerCase()
        };
    }

    headerClickHandler() {
        if (this.props.action.InputParameters) {
            this.setState({ isCollapsed: !this.state.isCollapsed });
        }
    }

    render({ action }: ICardProps, { isCollapsed, status }: ICardState) {
        return (<div class={" card-root-" + status}>
            <ExpandablePanel
                header={(
                    <div class="card-header">
                        <h3>{action.Name}</h3>
                        <p>{action.Description}</p>
                    </div>)}
                body={(
                    <div class="card-body">
                        <ExpandablePanel
                            header={(
                                <h4>Input parameters</h4>
                            )}
                            body={(
                                <ParamsTable params={action.InputParameters}/>
                            )}/>
                        <ExpandablePanel
                            header={(
                                <h4>Comparsion parameters</h4>
                            )}
                            body={(
                                <CompasionTable params={action.ComparsionParameters}/>
                            )}/>
                    </div>)}/>
        </div>)
    }
}