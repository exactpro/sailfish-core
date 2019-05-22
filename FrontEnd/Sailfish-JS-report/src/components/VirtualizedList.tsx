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
import { AutoSizer, List, CellMeasurer, CellMeasurerCache } from 'react-virtualized';
import { raf } from '../helpers/raf';
import RemeasureHandler from './util/RemeasureHandler';

interface VirtualizedListProps {
    elementRenderer: (idx: number) => JSX.Element;
    rowCount: number;
    itemSpacing?: number;

    // Number objects is used here because in some cases (eg one message / action was selected several times by diferent entities)
    // We can't understand that we need to scroll to the selected entity again when we are comparing primitive numbers.
    // Objects and reference comparison is the only way to handle numbers changing in this case.
    scrolledIndex?: Number;
}

export class VirtualizedList extends React.Component<VirtualizedListProps> {
    
    private readonly measurerCache = new CellMeasurerCache({
        defaultHeight: 50,
        fixedWidth: true
    });

    private lastWidth = 0
    private forceUpdateList : Function;
    private scrollToIndex: (idx: number) => any;
    private remeasureHandler: (idx: number) => any;

    public updateList() {
        this.forceUpdateList && this.forceUpdateList();
    }

    public remeasureRow(index: number) {
        this.measurerCache.clear(index, 0);
        this.updateList();
    }

    public remeasureAll() {
        this.measurerCache.clearAll();
        this.updateList();
    }

    render() {
        const { rowCount, scrolledIndex } = this.props;

        return (
            <AutoSizer onResize={this.onResize}>
                {({ height, width }) => (
                    <List
                        height={height}
                        width={width}
                        rowCount={rowCount}
                        deferredMeasurementCache={this.measurerCache}
                        rowHeight={this.measurerCache.rowHeight}
                        rowRenderer={this.rowRenderer}
                        scrollToAlignment="center"
                        scrollToIndex={scrolledIndex != null ? +scrolledIndex : undefined}
                        ref={ref => {
                            if (!ref) {
                                return;
                            }

                            this.forceUpdateList = ref.forceUpdateGrid.bind(ref);
                            this.scrollToIndex = ref.scrollToRow.bind(ref);
                        }}/>
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
                    <RemeasureHandler 
                        itemSpacing={this.props.itemSpacing}
                        style={style}
                        measureHandler={() => this.remeasureHandler && this.remeasureHandler(index)}>
                        {this.props.elementRenderer(index)}
                    </RemeasureHandler>
            </CellMeasurer>
        )
    }

    private onResize = ({width}) => {
        if (width !== this.lastWidth) {
            this.lastWidth = width;
            this.measurerCache.clearAll();
            this.updateList();
        }
    }

    componentDidUpdate(prevProps: VirtualizedListProps) {
        // Here we handle a situation, when primitive value of Number object doesn't changed 
        // and passing new index value in List doesn't make any effect (because it requires primitive value).
        // So we need to scroll List manually.

        if (prevProps.scrolledIndex !== this.props.scrolledIndex && +prevProps.scrolledIndex === +this.props.scrolledIndex) {
            this.scrollToIndex(+this.props.scrolledIndex);
        }
    }

    componentDidMount() {
        // We need to recalculate cells height after the DOM actual render, 
        // because it looks like in chromium browsers element's width with 'break-word' 
        // property calculating after react-virtualized measured row's heights.
        // After double RAF we can be shure, that DOM is fully rendered, and react-virtaulized can measure rows correct.

        // Related issue: https://github.com/bvaughn/react-virtualized/issues/896

        // Besides that, it fixes bug, related with invallid rendering, when data (test case) is changed (but it looks realy bad) 

        raf(() => { 
            this.remeasureAll();
        }, 2);

        this.remeasureHandler = (index) => {
            this.remeasureRow(index);
        }
    }
}
