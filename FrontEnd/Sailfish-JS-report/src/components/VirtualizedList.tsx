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
import { AutoSizer, List, CellMeasurer, CellMeasurerCache, OnScrollParams } from 'react-virtualized';
import RemeasureHandler from './util/RemeasureHandler';
import { StatusType } from '../models/Status';
import { HeatmapScrollbar } from './heatmap/HeatmapScrollbar';
import { raf } from '../helpers/raf';

interface VirtualizedListProps {
    elementRenderer: (idx: number, measure?: () => void) => React.ReactNode;
    rowCount: number;
    itemSpacing?: number;

    // for heatmap scrollbar
    selectedElements: Map<number, StatusType>;
    
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

    private list = React.createRef<List>();
    private scrollbar = React.createRef<HeatmapScrollbar>();

    private lastWidth = 0
    private lastScrollTop = 0;

    public updateList() {
        this.list.current && this.list.current.forceUpdateGrid();
    }

    public remeasureRow(index: number) {
        this.measurerCache.clear(index, 0);
        this.updateList();
    }

    public remeasureAll() {
        this.measurerCache.clearAll();
        this.updateList();
    }

    componentDidUpdate(prevProps: VirtualizedListProps) {
        // Here we handle a situation, when primitive value of Number object doesn't changed 
        // and passing new index value in List doesn't make any effect (because it requires primitive value).
        // So we need to scroll List manually.
        if (prevProps.scrolledIndex !== this.props.scrolledIndex && this.props.scrolledIndex != null) {
            // We need raf here because in some cases scroling happened before remeasuring row heights,
            // so we need to wait until component is complete rerender after remeasuring changed rows, 
            // and then scroll to selected row.
            // Without it List will calculate wrong scrollTop because it contains outdated information about row's heights.
            raf(() => {
                this.list.current.scrollToRow(+this.props.scrolledIndex);
            });
        }
    }

    componentDidMount() {
        if (this.props.scrolledIndex != null) {
            // we need raf here, because in componentDidMount virtualized list is not complete its render
            raf(() => {
                this.list.current.scrollToRow(+this.props.scrolledIndex);
            });
        }
    }

    render() {
        const { rowCount, selectedElements } = this.props;

        return (
            <AutoSizer onResize={this.onResize}>
                {({ height, width }) => (
                    <HeatmapScrollbar
                        height={height}
                        width={width}
                        elementsCount={rowCount}
                        selectedElements={selectedElements}
                        onScroll={this.scrollbarOnScroll}
                        heightMapper={index => this.measurerCache.rowHeight({ index })}
                        ref={this.scrollbar}>
                        <List
                            height={height}
                            width={width}
                            rowCount={rowCount}
                            deferredMeasurementCache={this.measurerCache}
                            rowHeight={this.measurerCache.rowHeight}
                            rowRenderer={this.rowRenderer}
                            onScroll={this.listOnScroll}
                            scrollToAlignment="center"
                            ref={this.list}
                            style={{ overflowX: "visible", overflowY: "visible", paddingRight: 15 }}/>
                    </HeatmapScrollbar>
                )}
            </AutoSizer>
        )
    }

    private rowRenderer = ({ index, key, parent, style }) => (
        <CellMeasurer
            cache={this.measurerCache}
            columnIndex={0}
            rowIndex={index}
            parent={parent}
            key={key}>
            {
                ({ measure }) => (
                    <RemeasureHandler 
                        itemSpacing={this.props.itemSpacing}
                        style={style}
                        measureHandler={measure}>
                        {this.props.elementRenderer(index, measure)}
                    </RemeasureHandler>
                )
            }
        </CellMeasurer>
    )

    private onResize = ({ width }) => {
        if (width !== this.lastWidth) {
            this.lastWidth = width;
            this.measurerCache.clearAll();
            this.updateList();
        }
    }

    private scrollbarOnScroll = ({ target }) => {
        const { scrollTop, scrollLeft } = target;

        this.lastScrollTop = scrollTop;
        this.list.current.Grid.handleScrollEvent({ scrollTop, scrollLeft });
    }

    private listOnScroll = ({ scrollTop }: OnScrollParams) => {
        // Updating scrollTop for scrollbar when scroll trriggered by scrolledIndex change
        if (scrollTop !== this.lastScrollTop) {
            this.scrollbar.current && this.scrollbar.current.scrollTop(scrollTop);
        }
    }
}
