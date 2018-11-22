import { h, Component } from "preact";
import Entry from "../models/Entry";
import "../styles/messages.scss"

export interface IComTableProps {
    params: Array<Entry>;
}

interface IComTableState {
    nodes: Array<TableNode>;
}

interface TableNode extends Entry {
    //is subnodes hidden
    isCollapsed?: boolean;
}

export default class ComparsionTable extends Component<IComTableProps, IComTableState> {

    constructor(props: IComTableProps) {
        super(props);
        this.state = {
            nodes: props.params ? props.params.map((param) => this.paramsToNodes(param)) : []
        }
    }

    paramsToNodes(root: Entry) : TableNode {
        return root.subEntries ? {
            ...root,
            subEntries: root.subEntries.map((param) => this.paramsToNodes(param)),
            isCollapsed: false
        } : root;
    }

    renderParams(root: TableNode, padding: number = 1) {

        return (root.subEntries ? ([
            <tr class="table-row-toogler">
                <td onClick={() => this.tooglerClick(root)}
                    colSpan={4}>
                    <p style={{ marginLeft: 10 * padding }}>
                        {(root.isCollapsed ? "+  " : "-  ") + root.name}
                    </p>
                </td>
            </tr>,
            root.isCollapsed ? null :
                root.subEntries.map(
                    (param) => this.renderParams(param, padding + 1) as Element)
        ]
        ) : (
                <tr class="table-row-value">
                    <td style={{ paddingLeft: 10 * padding }}> 
                        {root.name}
                    </td>
                    <td style={{ paddingLeft: 10 * padding }}>
                        {root.expected}
                    </td>
                    <td style={{ paddingLeft: 10 * padding }}>
                        {root.actual}
                    </td>
                    <td style={{ paddingLeft: 10 * padding }}>
                        {root.status}
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