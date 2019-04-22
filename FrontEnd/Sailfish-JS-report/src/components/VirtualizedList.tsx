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

import {h, Component} from 'preact';
import { Scrollbars } from 'preact-custom-scrollbars';
import { AutoSizer, List, CellMeasurer, CellMeasurerCache } from 'react-virtualized';

interface VirtualizedListProps {
    elementRenderer: (idx: number) => JSX.Element;
    rowCount: number;
    itemSpacing?: number;
}

export class VirtualizedList extends Component<VirtualizedListProps> {
    
    public readonly measurerCache = new CellMeasurerCache({
        defaultHeight: 50,
        fixedWidth: true
    });

    public forceUpdateList : Function;

    render({ rowCount }: VirtualizedListProps) {
        return (
            <AutoSizer>
                {({ height, width }) => (
                        <List
                            height={height}
                            width={width}
                            rowCount={rowCount}
                            deferredMeasurementCache={this.measurerCache}
                            rowHeight={this.measurerCache.rowHeight}
                            rowRenderer={this.rowRenderer}
                            ref={ref => this.forceUpdateList = ref ? ref.forceUpdateGrid.bind(ref) : null}/>
                    )
                }
            </AutoSizer>
        )
    }

    private rowRenderer = ({ index, key, parent, style }) => {
        return (
            <CellMeasurer
                cache={this.measurerCache}
                columnIndex={0}
                rowIndex={index}
                parent={parent}
                key={key}>
                    <div style={ { ...style, paddingTop: this.props.itemSpacing / 2, paddingBottom: this.props.itemSpacing / 2 }}>
                        {this.props.elementRenderer(index)}
                    </div>
            </CellMeasurer>
        )
    }

    componentDidMount() {
        // We need to recalculate cells height after the DOM actual render, 
        // because it looks like in chromium browsers element's width with 'break-word' 
        // property calculating after react-virtualized measured row's heights.
        // After double RAF we can be shure, that DOM is fully rendered, and react-virtaulized can measure rows correct.

        // Related issue: https://github.com/bvaughn/react-virtualized/issues/896

        // Besides that, it fixes bug, related with invallid rendering, when data (test case) is changed (but it looks realy bad) 

        window.requestAnimationFrame(() =>{
            window.requestAnimationFrame(() => { 
                this.measurerCache.clearAll();
                this.forceUpdateList();
            });
        });
    }
}
