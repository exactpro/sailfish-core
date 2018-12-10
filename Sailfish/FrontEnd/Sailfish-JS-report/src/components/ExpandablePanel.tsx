import { h, Component, PreactDOMAttributes } from "preact";
import "../styles/panel.scss"

interface IPanelProps {
    header?: JSX.Element;
    body?: JSX.Element;
    isExpanded?: boolean;
    children: JSX.Element[];
}

interface IPanelState {
    isExpanded: boolean;
}

export default class ExpandablePanel extends Component<IPanelProps, IPanelState> {
    constructor(props: IPanelProps) {
        super(props);
        this.state = {
            isExpanded: props.isExpanded !== undefined ? props.isExpanded : false
        };
    }

    expandePanel() {
        this.setState({isExpanded: !this.state.isExpanded})

    }

    render({ header, body, children }: IPanelProps, { isExpanded }: IPanelState) {
        const iconClass = ["expandable-panel-header-icon", (isExpanded ? "expanded" : "hidden")].join(' ');
        return (<div class="expandable-panel-root">
            <div class="expandable-panel-header">
                <div class={iconClass} 
                    onClick={e => this.expandePanel()}/>
                {header || children[0]}
            </div>
            {isExpanded ?
                <div className="expandable-panel-body">
                        {body || children.slice(1)}
                </div>
                : null}
        </div>)
    }
}