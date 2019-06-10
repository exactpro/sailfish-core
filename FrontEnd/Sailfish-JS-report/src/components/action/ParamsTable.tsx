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

import * as React from 'react';
import ActionParameter from "../../models/ActionParameter";
import '../../styles/tables.scss';
import { createSelector } from '../../helpers/styleCreators';
import StateSaver, { RecoverableElementProps } from "../util/StateSaver";

const PADDING_LEVEL_VALUE = 10;

interface ParamsTableOwnProps {
    params: ActionParameter[];
    name: string;
    onExpand: () => void;
}

interface ParamTableProps {
    nodes: TableNode[];
    name: string;
    onExpand: () => void;
    saveState: (state: TableNode[]) => void;
}

interface ParamTableState {
    collapseParams: TableNode[];
}

interface RecoverableTableProps extends ParamsTableOwnProps, RecoverableElementProps {}

interface TableNode extends ActionParameter {
    // is subnodes visible
    isExpanded?: boolean;
}

export default class ParamsTable extends React.Component<ParamTableProps, ParamTableState> {

    constructor(props: ParamTableProps) {
        super(props);
        this.state = {
            collapseParams: props.nodes
        }
    }

    tooglerClick(targetNode: TableNode) {
        this.setState({
            ...this.state,
            collapseParams: this.state.collapseParams.map(node => this.findNode(node, targetNode))
        });
    }    

    findNode(node: TableNode, targetNode: TableNode): TableNode {
        if (node === targetNode) {
            return {
                ...targetNode,
                isExpanded: !targetNode.isExpanded
            };
        }

        return {
            ...node,
            subParameters: node.subParameters && node.subParameters.map(subNode => this.findNode(subNode, targetNode))
        };
    }

    componentDidUpdate(prevProps, prevState: ParamTableState) {
        if (prevState.collapseParams !== this.state.collapseParams) {
            this.props.onExpand();
        }
    }

    componentWillUnmount() {
        this.props.saveState(this.state.collapseParams);
    }
    
    render() {
        return (
            <div className="params-table">
                <table>
                    <tbody>
                        <tr>
                            <th colSpan={2}>{this.props.name}</th>
                        </tr>
                        {this.state.collapseParams.map(nodes => this.renderNodes(nodes))}
                    </tbody>
                </table>
            </div>
        );
    }

    private renderNodes(node: TableNode, paddingLevel: number = 1, index = 0) : JSX.Element[] {
        if (node.subParameters && node.subParameters.length !== 0) {
            const subNodes = node.isExpanded ? 
                node.subParameters.reduce(
                    (list, node, index) => list.concat(this.renderNodes(node, paddingLevel + 1, index)), []
                ) 
                : [];

            return [
                this.renderTooglerNode(node, paddingLevel, index),
                ...subNodes
            ]
        } else {
            return [this.renderValueNode(node.name, node.value, paddingLevel, index)];
        }
    }

    private renderValueNode(name: string, value: string, paddingLevel: number, index = 0) : JSX.Element {

        const cellStyle = {
            paddingLeft: PADDING_LEVEL_VALUE * paddingLevel
        };

        return (
            <tr className="params-table-row-value" key={`${paddingLevel}-${index}`}>
                <td style={cellStyle}> 
                    {name}
                </td>
                <td style={cellStyle}>
                    {value}
                </td>
            </tr>
        )
    }

    private renderTooglerNode(node: TableNode, paddingLevel: number, index = 0) : JSX.Element {

        const rootClass = createSelector(
                "params-table-row-toogler",
                node.isExpanded ? "expanded" : "collapsed"
            ),
            nameStyle = {
                paddingLeft: PADDING_LEVEL_VALUE * paddingLevel
            };

        return (
            <tr className={rootClass} key={`${paddingLevel}-${index}`}>
                <td onClick={() => this.tooglerClick(node)}
                    colSpan={2}>
                    <p style={nameStyle}>
                        {node.name}
                    </p>
                    <span className="params-table-row-toogler-count">{node.subParameters.length}</span>
                </td>
            </tr>
        )
    }
}

export const RecoverableParamsTable = ({ stateKey, ...props }: RecoverableTableProps) => (
    <StateSaver
        stateKey={stateKey}>
        {
            (state, stateSaver) => state ? 
                <ParamsTable
                    {...props}
                    saveState={stateSaver}
                    nodes={state as TableNode[]}/> 
                :
                <ParamsTable
                    {...props}
                    saveState={stateSaver}
                    nodes={props.params ? props.params.map(param => paramsToNodes(param)) : []}/>
        }
    </StateSaver>
)

function paramsToNodes(root: ActionParameter) : TableNode {
    return (root.subParameters ? {
        ...root,
        subParameters: root.subParameters.map(parameter => paramsToNodes(parameter)),
        isExpanded: true
    } : root)
}
