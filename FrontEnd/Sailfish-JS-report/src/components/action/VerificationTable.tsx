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
import Entry from "../../models/Entry";
import { StatusType } from "../../models/Status";
import "../../styles/tables.scss";
import { connect } from 'preact-redux';
import AppState from "../../state/models/AppState";
import { createSelector } from '../../helpers/styleCreators';
import StateSaver, { RecoverableElementProps } from "./../util/StateSaver";

const PADDING_LEVEL_VALUE = 15;

interface VerificationTableOwnProps {
    params: Entry[];
    status: StatusType;
    onExpand: () => void;
}

interface VerificationTableProps {
    nodes: TableNode[];
    fieldsFilter: StatusType[];
    status: StatusType;
    onExpand: () => void;
    stateSaver: (state: TableNode[]) => void;
}

interface RecoverableVerificationTableProps extends VerificationTableOwnProps, RecoverableElementProps {}

interface VerificationTableState {
    nodes: TableNode[];
}

interface TableNode extends Entry {
    //is subnodes visible
    isExpanded?: boolean;
}

class VerificationTableBase extends Component<VerificationTableProps, VerificationTableState> {

    constructor(props: VerificationTableProps) {
        super(props);
        this.state = {
            nodes: props.nodes
        }
    }

    tooglerClick(targetNode: TableNode) {
        this.setState({
            ...this.state,
            nodes: this.state.nodes.map(rootNode => this.findNode(rootNode, targetNode))
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
            subEntries: node.subEntries && node.subEntries.map(subNode => this.findNode(subNode, targetNode))
        };
    }

    setExpandStatus(isCollapsed: boolean) {
        this.setState({
            nodes: this.state.nodes.map(
                node => node.subEntries ? this.setNodeExpandStatus(node, isCollapsed) : node)
        });
    }

    setNodeExpandStatus(node: TableNode, isExpanded: boolean): TableNode {
        return {
            ...node,
            isExpanded: isExpanded,
            subEntries: node.subEntries ? node.subEntries.map(
                subNode => subNode.subEntries ? this.setNodeExpandStatus(subNode, isExpanded) :
                    subNode) : null
        }
    }

    componentDidUpdate(prevProps: VerificationTableProps, prevState: VerificationTableState) {
        // handle expand state changing to remeasure card size
        if (this.state.nodes !== prevState.nodes) {
            this.props.onExpand();
        }
    }

    componentWillUnmount() {
        this.props.stateSaver(this.state.nodes);
    }

    render({ fieldsFilter, status }: VerificationTableProps, { nodes }: VerificationTableState) {

        const rootClass = createSelector("ver-table", status);

        return (
            <div class={rootClass}>
                <div class="ver-table-header">
                    <div class="ver-table-header-name">
                        <h5>Comparison Table</h5>
                    </div>
                    <div class="ver-table-header-control">
                        <span class="ver-table-header-control-button"
                            onClick={() => this.setExpandStatus(false)}>
                            Collapse
                        </span>
                        <span> | </span>
                        <span class="ver-table-header-control-button"
                            onClick={() => this.setExpandStatus(true)}>
                            Expand
                        </span>
                        <span> all groups</span>
                    </div>
                </div>
                <table>
                    <thead>
                        <th>Name</th>
                        <th class="ver-table-flexible">Expected</th>
                        <th class="ver-table-flexible">Actual</th>
                        <th>Status</th>
                    </thead>
                    <tbody>
                        {nodes.map((param) => this.renderTableNodes(param, fieldsFilter))}
                    </tbody>
                </table>
            </div>
        )
    }

    private renderTableNodes(node: TableNode, fieldsFilter: StatusType[], paddingLevel: number = 1) : JSX.Element[] {

        if (node.subEntries) {

            const subNodes = node.isExpanded ? 
                node.subEntries.reduce((lsit, node) => lsit.concat(this.renderTableNodes(node, fieldsFilter, paddingLevel + 1)), []) :
                [];

            return [this.renderTooglerNode(node, paddingLevel), ...subNodes];
        } else {
            return [this.renderValueNode(node, fieldsFilter, paddingLevel)];
        }
    }

    private renderValueNode({ name, expected, actual, status }: TableNode, fieldsFilter: StatusType[], paddingLevel: number): JSX.Element {

        const rootClassName = createSelector(
                "ver-table-row-value",
                !fieldsFilter.includes(status) ? "transparent" : null
            ),
            statusClassName = createSelector("ver-table-row-value-status", status);

        return (
            <tr class={rootClassName}>
                <td style={{ paddingLeft: PADDING_LEVEL_VALUE * paddingLevel }}>
                    {name}
                </td>
                <td class="ver-table-row-value-expected">
                    {expected}
                </td>
                <td class="ver-table-row-value-actual">
                    {actual}
                </td>
                <td class={statusClassName}>
                    {status}
                </td>
            </tr>
        );
    }

    private renderTooglerNode(node: TableNode, paddingLevel: number): JSX.Element {

        const className = createSelector(
            "ver-table-row-toggler",
            node.isExpanded ? "expanded" : "collapsed"
        );

        return (
            <tr class={className}>
                <td onClick={() => this.tooglerClick(node)}
                    colSpan={4}>
                    <p style={{ marginLeft: PADDING_LEVEL_VALUE * (paddingLevel - 1) }}>
                        {node.name}
                    </p>
                    <span class="ver-table-row-toggler-count">{node.subEntries.length}</span>
                </td>
            </tr>
        )
    }
}

export const VerificationTable = connect(
    (state: AppState) => ({
        fieldsFilter: state.filter.fieldsFilter
    }),
    dispatch => ({})
)(VerificationTableBase);

export const RecoverableVerificationTable = ({ stateKey, ...props }: RecoverableVerificationTableProps) => {
    return (
        <StateSaver
            stateKey={stateKey}>
            {
                // at first table render, we need to generate table nodes if we don't find previous table's state 
                (state, stateHandler) => state ? 
                    <VerificationTable
                        {...props}
                        nodes={state as TableNode[]}
                        stateSaver={stateHandler}/> :
                    <VerificationTable
                        {...props}
                        nodes={props.params ? props.params.map(param => paramsToNodes(param)) : []}
                        stateSaver={stateHandler}/>
            }
        </StateSaver>
    )
}

function paramsToNodes(root: Entry): TableNode {
    return root.subEntries ? {
        ...root,
        subEntries: root.subEntries.map((param) => paramsToNodes(param)),
        isExpanded: true
    } : root;
}
