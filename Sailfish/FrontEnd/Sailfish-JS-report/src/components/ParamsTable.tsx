import { h, Component } from "preact";
import ActionParameter from "../models/ActionParameter";

interface IParamTableProps {
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
    } as TableNode : root as TableNode)

    tooglerClick(root: TableNode) {
        root.isCollapsed = !root.isCollapsed;
        this.setState(this.state);
    }

    renderParams(root: TableNode, margin: number = 1) {

        return (root.SubParameters ? ([
            <tr class="table-row-toogler">
                <td onClick={() => this.tooglerClick(root)}
                    colSpan={2}>
                    <p style={{ marginLeft: 10 * margin }}>
                        {(root.isCollapsed ? "+  " : "-  ") + root.Name}
                    </p>
                </td>
            </tr>,
            root.isCollapsed ? null :
                root.SubParameters.map(
                    (param) => {
                        return this.renderParams(param, margin + 1) as Element
                    })
        ]
        ) : (
                <tr class="table-row-value">
                    <td>
                        <p style={{ marginLeft: 10 * margin }}>{root.Name}</p>
                    </td>
                    <td>
                        <p style={{ marginLeft: 10 * margin }}>{root.Value}</p>
                    </td>
                </tr>
            )
        )
    }


    render({ params }: IParamTableProps, { rootNode }: IParamTableState) {
        const { renderParams } = this;
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