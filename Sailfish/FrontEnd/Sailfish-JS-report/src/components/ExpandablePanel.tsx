import { h, Component } from "preact";
import "../styles/panel.scss"

interface IPanelProps {
    header: JSX.Element;
    body: JSX.Element;
    isCollapsed?: boolean;
}

interface IPanelState {
    isCollapsed: boolean;
}

export default class ExpandablePanel extends Component<IPanelProps, IPanelState> {
    constructor(props: IPanelProps) {
        super(props);
        this.state = {
            isCollapsed: props.isCollapsed !== undefined ? props.isCollapsed : true
        };
    }

    expandePanel() {
        this.setState({isCollapsed: !this.state.isCollapsed})
    }

    render({ header, body }: IPanelProps, { isCollapsed }: IPanelState) {
        return (<div class="expandable-panel-root">
            <div class="expandable-panel-header">
                <div class="expandable-panel-header-icon" onClick={() => this.expandePanel()}>
                    <h5>{isCollapsed ? "+" : "-"}</h5>
                </div>
                {header}
            </div>
            {isCollapsed ?
                null :
                <div className="expandable-panel-body">
                        {body}
                </div>}
        </div>)
    }
}