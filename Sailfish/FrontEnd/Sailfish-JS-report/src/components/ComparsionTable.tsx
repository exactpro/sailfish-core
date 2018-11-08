import { h, Component } from "preact";
import MessageParameter from "../models/MessageParameter";

export interface IComTableProps {
    params: Array<MessageParameter>;
}

interface IComTableState {
    nodes: Array<TableNode>;
}

interface TableNode extends MessageParameter {
    //is subnodes hidden
    isCollapsed?: boolean;
}

export default class ComparsionTable extends Component<IComTableProps, IComTableState> {

    constructor(props: IComTableProps) {
        super(props);
        this.state = {
            nodes: props.params.map((param) => this.paramsToNodes(param))
        }
    }

    paramsToNodes(root: MessageParameter) : TableNode {
        return root.SubParameters ? {
            ...root,
            SubParameters: root.SubParameters.map((param) => this.paramsToNodes(param)),
            isCollapsed: false
        } : root;
    }

    renderParams(root: TableNode, padding: number = 1) {

        return (root.SubParameters ? ([
            <tr class="table-row-toogler">
                <td onClick={() => this.tooglerClick(root)}
                    colSpan={4}>
                    <p style={{ marginLeft: 10 * padding }}>
                        {(root.isCollapsed ? "+  " : "-  ") + root.Name}
                    </p>
                </td>
            </tr>,
            root.isCollapsed ? null :
                root.SubParameters.map(
                    (param) => this.renderParams(param, padding + 1) as Element)
        ]
        ) : (
                <tr class="table-row-value">
                    <td style={{ paddingLeft: 10 * padding }}> 
                        {root.Name}
                    </td>
                    <td style={{ paddingLeft: 10 * padding }}>
                        {root.Expected}
                    </td>
                    <td style={{ paddingLeft: 10 * padding }}>
                        {root.Actual}
                    </td>
                    <td style={{ paddingLeft: 10 * padding }}>
                        {root.Result}
                    </td>
                </tr>
            )
        )
    }

    tooglerClick(root: TableNode) {
        root.isCollapsed = !root.isCollapsed;
        this.setState(this.state);
    }

    render({ params }: IComTableProps, { nodes }: IComTableState) {
        return (
            <div class="table-root">
                <table>
                    <thead>
                        <th>Name</th>
                        <th>Expected</th>
                        <th>Actual</th>
                        <th>Status</th>
                    </thead>
                    <tbody>
                        {nodes.map((param) => this.renderParams(param))}
                    </tbody>
                </table>
            </div>
        )
    }
}