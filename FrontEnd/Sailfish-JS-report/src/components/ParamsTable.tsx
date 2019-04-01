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
import { createSelector } from '../helpers/styleCreators';

const PADDING_LEVEL_VALUE = 10;

export interface IParamTableProps {
    params: Array<ActionParameter>;
    name: string;
}

interface IParamTableState {
    collapseParams: Array<TableNode>;
}

interface TableNode extends ActionParameter {
    // is subnodes visible
    isExpanded?: boolean;
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
            isExpanded: true
        } : root)
    }

    tooglerClick(root: TableNode) {
        root.isExpanded = !root.isExpanded;
        this.setState(this.state);
    }    
    
    render({ name }: IParamTableProps, { collapseParams }: IParamTableState) {
        return (<div class="params-table">
            <table>
                <tbody>
                    <tr>
                        <th colSpan={2}>{name}</th>
                    </tr>
                    {collapseParams.map((param) => this.renderNodes(param))}
                </tbody>
            </table>
        </div>);
    }

    private renderNodes(node: TableNode, paddingLevel: number = 1) : JSX.Element[] {
        if (node.subParameters && node.subParameters.length !== 0) {
            const subNodes = node.isExpanded ? 
                node.subParameters.reduce((list, node) => list.concat(this.renderNodes(node, paddingLevel + 1)), []) : 
                [];

            return [
                this.renderTooglerNode(node, paddingLevel),
                ...subNodes
            ]
        } else {
            return [this.renderValueNode(node.name, node.value, paddingLevel)];
        }
    }

    private renderValueNode(name: string, value: string, paddingLevel: number) : JSX.Element {

        const cellStyle = {
            paddingLeft: PADDING_LEVEL_VALUE * paddingLevel
        };

        return (
            <tr class="params-table-row-value">
                <td style={cellStyle}> 
                    {name}
                </td>
                <td style={cellStyle}>
                    {value}
                </td>
            </tr>
        )
    }

    private renderTooglerNode(node: TableNode, paddingLevel: number) : JSX.Element {

        const rootClass = createSelector(
                "params-table-row-toogler",
                node.isExpanded ? "expanded" : "collapsed"
            ),
            nameStyle = {
                paddingLeft: PADDING_LEVEL_VALUE * paddingLevel
            };

        return (
            <tr class={rootClass}>
                <td onClick={() => this.tooglerClick(node)}
                    colSpan={2}>
                    <p style={nameStyle}>
                        {node.name}
                    </p>
                </td>
            </tr>
        )
    }
}