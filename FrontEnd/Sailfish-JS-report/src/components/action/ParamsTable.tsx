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
import { createStyleSelector } from '../../helpers/styleCreators';
import StateSaver from "../util/StateSaver";
import SearchableContent from '../search/SearchableContent';
import { keyForActionParameter } from '../../helpers/keys';
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import { getParamsExpandPath } from '../../helpers/search/getExpandPath';
import SearchResult from '../../helpers/search/SearchResult';

const PADDING_LEVEL_VALUE = 10;

interface OwnProps {
    actionId: number;
    params: ActionParameter[];
    name: string;
}

interface StateProps {
    expandPath: number[];
    searchResults: SearchResult;
}

interface RecoveredProps {
    nodes: TableNode[];
    saveState: (state: TableNode[]) => void;
}

interface Props extends Omit<OwnProps, 'params'>, StateProps, RecoveredProps {}

interface State {
    nodes: TableNode[];
}

interface TableNode extends ActionParameter {
    // is subnodes visible
    isExpanded?: boolean;
}

class ParamsTableBase extends React.Component<Props, State> {

    constructor(props: Props) {
        super(props);
        this.state = {
            nodes: props.nodes
        }
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

    updateExpandPath([currentIndex, ...expandPath]: number[], prevState: TableNode[]): TableNode[] {
        return prevState.map(
            (node, index) => index === currentIndex ? {
                ...node,
                isExpanded: true,
                subParameters: node.subParameters && this.updateExpandPath(expandPath, node.subParameters)
            } : node
        )
    }

    componentDidUpdate(prevProps: Props, prevState: State) {
        if (prevProps.expandPath !== this.props.expandPath && this.props.expandPath && this.props.expandPath.length > 0) {
            this.setState({
                nodes: this.updateExpandPath(this.props.expandPath, this.state.nodes)
            });      
        }
    }

    componentWillUnmount() {
        this.props.saveState(this.state.nodes);
    }
    
    render() {
        return (
            <div className="params-table">
                <table>
                    <tbody>
                        <tr>
                            <th colSpan={2}>{this.props.name}</th>
                        </tr>
                        {
                            this.state.nodes.map((nodes, index) => 
                                this.renderNodes(nodes, 0, keyForActionParameter(this.props.actionId, index))
                            )
                        }
                    </tbody>
                </table>
            </div>
        );
    }

    private renderNodes(node: TableNode, paddingLevel: number = 1, key: string) : React.ReactNodeArray {
        if (node.subParameters && node.subParameters.length !== 0) {
            const subNodes = node.isExpanded ? 
                node.subParameters.reduce(
                    (list, node, index) => 
                        list.concat(this.renderNodes(node, paddingLevel + 1, `${key}-${index}`)), []
                ) 
                : [];

            return [
                this.renderTooglerNode(node, paddingLevel, key),
                ...subNodes
            ]
        } else {
            return [this.renderValueNode(node.name, node.value, paddingLevel, key)];
        }
    }

    private renderValueNode(name: string, value: string, paddingLevel: number, key: string) : React.ReactNode {

        const cellStyle = {
            paddingLeft: PADDING_LEVEL_VALUE * paddingLevel
        };

        return (
            <tr className="params-table-row-value" key={key}>
                <td style={cellStyle}> 
                    {this.renderContent(`${key}-name`, name)}
                </td>
                <td style={cellStyle}>
                    {this.renderContent(`${key}-value`, value)}
                </td>
            </tr>
        )
    }

    private renderTooglerNode(node: TableNode, paddingLevel: number, key: string) : React.ReactNode {

        const rootClass = createStyleSelector(
                "params-table-row-toogler",
                node.isExpanded ? "expanded" : "collapsed"
            ),
            nameStyle = {
                paddingLeft: PADDING_LEVEL_VALUE * paddingLevel
            };

        return (
            <tr className={rootClass} key={key}>
                <td onClick={this.togglerClickHandler(node)}
                    colSpan={2}>
                    <p style={nameStyle}>
                        {this.renderContent(`${key}-name`, node.name)}
                    </p>
                    <span className="params-table-row-toogler-count">{node.subParameters.length}</span>
                </td>
            </tr>
        )
    }

    // we need this for optimization - render SearchableContent component only if it contains some search results
    private renderContent(contentKey: string, content: string): React.ReactNode {
        if (this.props.searchResults.size && this.props.searchResults.get(contentKey)) {
            return (
                <SearchableContent
                    contentKey={contentKey}
                    content={content}/>
            )
        } else {
            return content;
        }
    }
    
    private togglerClickHandler = (targetNode: TableNode) => (e: React.MouseEvent) => {
        this.setState({
            ...this.state,
            nodes: this.state.nodes.map(node => this.findNode(node, targetNode))
        });

        e.stopPropagation();
    }    
}

export const RecoverableParamsTable = ({ stateKey, ...props }: OwnProps & StateProps & {stateKey: string}) => (
    // at first table render, we need to generate table nodes if we don't find previous table's state 
    <StateSaver
        stateKey={stateKey}
        getDefaultState={() => props.params ? props.params.map(param => paramsToNodes(param)) : []}>
        {
            (state: TableNode[], stateSaver) => (
                <ParamsTableBase
                    {...props}
                    saveState={stateSaver}
                    nodes={state}/> 
            )
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

const ParamsTable = connect(
    (state: AppState, ownProps: OwnProps): StateProps => ({
        expandPath: getParamsExpandPath(state.selected.search.results, state.selected.search.index, ownProps.actionId),
        searchResults: state.selected.search.results
    })
)(RecoverableParamsTable);

export default ParamsTable;
