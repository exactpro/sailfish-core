import { h, Component } from "preact";
import Entry from "../models/Entry";
import { StatusType } from "../models/Status";
import "../styles/tables.scss";

export interface VerificationTableProps {
    params: Entry[];
    filterFields: StatusType[];
}

interface VerificationTableState {
    nodes: TableNode[];
}

interface TableNode extends Entry {
    //is subnodes hidden
    isCollapsed?: boolean;
}

export default class VerificationTable extends Component<VerificationTableProps, VerificationTableState> {

    constructor(props: VerificationTableProps) {
        super(props);
        this.state = {
            nodes: props.params ? props.params.map((param) => this.paramsToNodes(param)) : []
        }
    }

    paramsToNodes(root: Entry): TableNode {
        return root.subEntries ? {
            ...root,
            subEntries: root.subEntries.map((param) => this.paramsToNodes(param)),
            isCollapsed: false
        } : root;
    }

    renderParams(node: TableNode, filterFields: StatusType[], padding: number = 1) {

        const { subEntries, isCollapsed, expected, name, status, actual } = node;

        if (subEntries) {
            return ([
                <tr class="ver-table-row-toggler">
                    <td onClick={() => this.tooglerClick(node)}
                        colSpan={4}>
                        <p style={{ marginLeft: 10 * (padding - 1) }}>
                            {(isCollapsed ? "+  " : "-  ") + name}
                        </p>
                    </td>
                </tr>,
                isCollapsed ? null :
                    subEntries.map(
                        (param) => this.renderParams(param, filterFields, padding + 1) as Element)
            ]
            );
        } else if (filterFields.includes(status)) {
            return (
                <tr class="ver-table-row-value">
                    <td style={{ paddingLeft: 10 * padding }}>
                        {name}
                    </td>
                    <td>
                        {expected}
                    </td>
                    <td>
                        {actual}
                    </td>
                    <td class={"ver-table-row-value-status " + (status || "").toLowerCase()}>
                        {status}
                    </td>
                </tr>
            );
        }
    }

    tooglerClick(root: TableNode) {
        root.isCollapsed = !root.isCollapsed;
        this.setState(this.state);
    }

    render({ filterFields }: VerificationTableProps, { nodes }: VerificationTableState) {
        return (
            <div class="ver-table">
                <table>
                    <thead>
                        <th>Name</th>
                        <th class="ver-table-flexible">Expected</th>
                        <th class="ver-table-flexible">Actual</th>
                        <th>Status</th>
                    </thead>
                    <tbody>
                        {nodes.map((param) => this.renderParams(param, filterFields))}
                    </tbody>
                </table>
            </div>
        )
    }
}