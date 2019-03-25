/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import { h, Component } from "preact";
import ActionParameter from "../models/ActionParameter";
import '../styles/tables.scss';

export interface IParamTableProps {
    params: Array<ActionParameter>;
    name: string;
}

interface IParamTableState {
    collapseParams: Array<TableNode>;
}

interface TableNode extends ActionParameter {
    // is subnodes hidden
    isCollapsed?: boolean;
}

export default class ParamsTable extends Component<IParamTableProps, IParamTableState> {

    constructor(props: IParamTableProps) {
        super(props);
        this.state = {
            collapseParams: props.params ? props.params.map((param) => this.paramsToNodes(param)) : []
        }
    }

    paramsToNodes(root: ActionParameter) : TableNode {
        return (root.subParameters ? {
            ...root,
            subParameters: root.subParameters.map(parameter => this.paramsToNodes(parameter)),
            isCollapsed: false
        } : root)
    }

    tooglerClick(root: TableNode) {
        root.isCollapsed = !root.isCollapsed;
        this.setState(this.state);
    }

    renderParams(root: TableNode, padding: number = 1) {

        return (root.subParameters && root.subParameters.length !== 0 ? ([
            <tr class="params-table-row-toogler">
                <td onClick={() => this.tooglerClick(root)}
                    colSpan={2}>
                    <p style={{ marginLeft: 10 * padding }}>
                        {(root.isCollapsed ? "+  " : "-  ") + root.name}
                    </p>
                </td>
            </tr>,
            root.isCollapsed ? null :
                root.subParameters.map(
                    (param) => this.renderParams(param, padding + 1) as Element)
        ]
        ) : (
                <tr class="params-table-row-value">
                    <td style={{ paddingLeft: 10 * padding }}> 
                        {root.name}
                    </td>
                    <td style={{ paddingLeft: 10 * padding }}>
                        {root.value}
                    </td>
                </tr>
            )
        )
    }


    render({ name }: IParamTableProps, { collapseParams }: IParamTableState) {
        return (<div class="params-table">
            <table>
                <tbody>
                    <tr>
                        <th colSpan={2}>{name}</th>
                    </tr>
                    {collapseParams.map((param) => this.renderParams(param))}
                </tbody>
            </table>
        </div>);
    }
}