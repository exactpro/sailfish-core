import { h, Component } from "preact";
import ActionParameter from "../models/ActionParameter";

export interface IParamTableProps {
    params: ActionParameter;
}

interface IParamTableState {
    rootNode: TableNode;
}

interface TableNode extends ActionParameter {
    // is subnodes hidden
    isCollapsed?: boolean;
}

export default class ParamsTable extends Component<IParamTableProps, IParamTableState> {

    constructor(props: IParamTableProps) {
        super(props);
        this.state = {
            rootNode: this.paramsToNodes(props.params)
        }
    }

    paramsToNodes = (root: ActionParameter) => (root.SubParameters ? {
        ...root,
        SubParameters: root.SubParameters.map(this.paramsToNodes),
        isCollapsed: false
    } : root)

    tooglerClick(root: TableNode) {
        root.isCollapsed = !root.isCollapsed;
        this.setState(this.state);
    }

    renderParams(root: TableNode, padding: number = 1) {

        return (root.SubParameters ? ([
            <tr class="table-row-toogler">
                <td onClick={() => this.tooglerClick(root)}
                    colSpan={2}>
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
                        {root.Value}
                    </td>
                </tr>
            )
        )
    }


    render({ params }: IParamTableProps, { rootNode }: IParamTableState) {
        return (<div class="table-root">
            <table>
                <tbody>
                    <tr>
                        <th colSpan={2}>{params.Name}</th>
                    </tr>
                    {this.renderParams(rootNode)}
                </tbody>
            </table>
        </div>);
    }
}