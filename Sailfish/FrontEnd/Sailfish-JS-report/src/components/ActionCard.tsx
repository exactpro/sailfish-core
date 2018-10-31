import { h, render, Component } from "preact";
import Action from "../models/Action";
import "../styles/action.scss";
import ParamsTable from "./ParamsTable";

interface ICardProps {
    action: Action;
}

interface ICardState {
    isCollapsed: boolean;
}

export default class ActionCard extends Component<ICardProps, ICardState> {

    constructor(props: ICardProps) {
        super(props);
        this.state = { isCollapsed: true };
    }

    headerClickHandler() {
        if (this.props.action.InputParameters) {
            this.setState({ isCollapsed: !this.state.isCollapsed });
        }
    }

    render({action}: ICardProps, {isCollapsed}: ICardState) {
        return (<div class="card-root">
            <div className="card-header" onClick={() => {this.headerClickHandler()}}>
                <h3>{action.Name}</h3>
            </div>
            { isCollapsed ? null : (
                <div class="card-body">
                    <ParamsTable params={action.InputParameters}/>
                </div>
            )}
        </div>)
    }
 }