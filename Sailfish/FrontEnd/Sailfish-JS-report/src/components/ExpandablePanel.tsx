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

    render({ header, body }: IPanelProps, { isCollapsed }: IPanelState) {
        return (<div class="expandable-panel-root">
            <div class="expandable-panel-header" onClick={() => {
                this.setState({ isCollapsed: !this.state.isCollapsed })
            }}>
                <h5>{isCollapsed ? "+" : "-"}</h5>
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