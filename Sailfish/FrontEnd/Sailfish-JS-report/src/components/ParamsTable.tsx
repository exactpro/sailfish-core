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
    
    constructor(props : IParamTableProps) {
        super(props);
        console.log(props);
        this.state = {
            rootNode: this.paramsToNodes(props.params)
        }
    }

    paramsToNodes = (root: ActionParameter) => (root.SubParameters ? {
        ...root,
        SubParameters: root.SubParameters.map(this.paramsToNodes),
        isCollapsed: false
    } as TableNode : root as TableNode)

    tooglerClick(...args) {
        console.log(args);
    }

    renderParams = (root: TableNode) => (root.SubParameters ? (
        root.isCollapsed ? 
            <tr class="table-row-toogler">
                <td onClick={() => root.isCollapsed = !root.isCollapsed}>{root.Name}</td>
            </tr> : [
            <tr class="table-row-toogler">
                <td onClick={() => root.isCollapsed = !root.isCollapsed}>{root.Name}</td>
            </tr>,
            ...root.SubParameters.map((param) => this.renderParams(param) as Element)
        ] ) : (
            <tr class="table-row-value">
                <td>{root.Name}</td>
                <td>{root.Value}</td>
            </tr>
        )
    );


    render({params} : IParamTableProps, {rootNode} : IParamTableState) {
        const { renderParams } = this;
        return (<div class="table-root">
            <table>
                <tbody>
                    <tr>
                        <th colSpan={2}>{params.Name}</th>
                    </tr>
                    {renderParams(rootNode)}
                </tbody>
            </table>
        </div>);
    }
}