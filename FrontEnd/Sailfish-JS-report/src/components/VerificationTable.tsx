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
import Entry from "../models/Entry";
import { StatusType } from "../models/Status";
import "../styles/tables.scss";
import { VerificationTableProps } from './VerificationTable';
import { connect } from 'preact-redux';
import AppState from "../state/AppState";

export interface VerificationTableProps {
    params: Entry[];
    fieldsFilter: StatusType[];
}

interface VerificationTableState {
    nodes: TableNode[];
}

interface TableNode extends Entry {
    //is subnodes hidden
    isCollapsed?: boolean;
}

class VerificationTableBase extends Component<VerificationTableProps, VerificationTableState> {

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

    renderParams(node: TableNode, fieldsFilter: StatusType[], padding: number = 1) {

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
                        (param) => this.renderParams(param, fieldsFilter, padding + 1) as Element)
            ]
            );
        } else {
            const className = ["ver-table-row-value", !fieldsFilter.includes(status) ? "transparent" : ""].join(" ").toLowerCase();
            return (
                <tr class={className}>
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

    setCollapseStatus(isCollapsed: boolean) {
        this.setState({
            nodes: this.state.nodes.map(
                node => node.subEntries ? this.setNodeCollapseStatus(node, isCollapsed) : node)
        });
    }

    setNodeCollapseStatus(node: TableNode, isCollapsed: boolean): TableNode {
        return {
            ...node,
            isCollapsed: isCollapsed,
            subEntries: node.subEntries ? node.subEntries.map(
                subNode => subNode.subEntries ? this.setNodeCollapseStatus(subNode, isCollapsed) : 
                subNode) : null
        }
    }

    render({ fieldsFilter }: VerificationTableProps, { nodes }: VerificationTableState) {
        return (
            <div class="ver-table">
                <div class="ver-table-header">
                    <div class="ver-table-header-name">
                        <h5>Comparison Table</h5>
                    </div>
                    <div class="ver-table-header-control">
                        <span class="ver-table-header-control-button"
                            onClick={() => this.setCollapseStatus(true)}>
                            Collapse
                        </span>
                        <span> | </span>
                        <span class="ver-table-header-control-button"
                            onClick={() => this.setCollapseStatus(false)}>
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
                        {nodes.map((param) => this.renderParams(param, fieldsFilter))}
                    </tbody>
                </table>
            </div>
        )
    }
}

export const VerificationTable = connect(
    (state: AppState) => ({
        fieldsFilter: state.fieldsFilter
    }),
    dispatch => ({})
)(VerificationTableBase);