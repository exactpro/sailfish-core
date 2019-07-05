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
import Scrollbars from 'react-custom-scrollbars';
import { StatusType } from '../../models/Status';
import '../../styles/heatmap.scss';
import { rangeSum } from '../../helpers/range';
import { createSelector } from '../../helpers/styleCreators';
import SmartHeatmap from './SmartHeatmap';
import ChunkedHeatmap from './ChunkedHeatmap';
import Heatmap from './Heatmap';

const MIN_HEATMAP_ELEMENT_HEIGHT = 8;
const SCROLLBAR_TRACK_WIDTH = 11;

interface HeatmapScrollbarProps {
    selectedElements: Map<number, StatusType>;
    heightMapper?: (index: number) => number;
    elementsCount: number;
    height?: number;
    width?: number;
    onScroll?: React.UIEventHandler<any>;
}

interface HeatmapScrollbarState {
    rootHeight: number;
}

export class HeatmapScrollbar extends React.Component<HeatmapScrollbarProps, HeatmapScrollbarState> {

    private scrollbar = React.createRef<Scrollbars>();
    private root = React.createRef<HTMLDivElement>();

    constructor(props) {
        super(props);

        this.state = { rootHeight: 0 }
    }

    scrollToTop() {
        this.scrollbar.current && this.scrollbar.current.scrollToTop();
    }

    scrollTop(top: number = 0) {
        this.scrollbar.current && this.scrollbar.current.scrollTop(top);
    }

    setScrollTop(value: number) {
        this.scrollbar.current && this.scrollbar.current.scrollTop(value);
    }

    componentDidMount() {
        // at the first mount we can't get real component height, 
        // so we need to rerender component with new root element scroll height

        this.setState({
            rootHeight: this.root && this.root.current.scrollHeight
        });
    }

    render() {
        const { children, selectedElements, elementsCount, height, width, onScroll, heightMapper } = this.props, 
            rootHeight = height !== undefined ? height : this.state.rootHeight,
            style = height !== undefined || width !== undefined ? { height, width } : undefined;

        return (
            <div className="heatmap" ref={this.root} style={style}> 
                <Scrollbars
                    style={style}
                    renderThumbVertical={props => <div {...props} className="heatmap-scrollbar-thumb" />}
                    renderTrackVertical={({ style, ...props }) =>
                        <div {...props} className="heatmap-scrollbar-track" style={{ ...style, width: SCROLLBAR_TRACK_WIDTH }} />
                    }
                    onScroll={onScroll}
                    ref={this.scrollbar}
                >
                    <div className="heatmap-wrapper">
                        {children}
                    </div>
                </Scrollbars>
                {
                    heightMapper ?
                        <SmartHeatmap
                            elementsCount={elementsCount}
                            selectedElements={selectedElements}
                            rootHeight={rootHeight}
                            elementHeightMapper={heightMapper}/> : 
                        rootHeight ? 
                            renderHeatmap(elementsCount, selectedElements, rootHeight) : 
                            <div/>
                }
            </div>
        )
    }
}

function renderHeatmap(elementsCount: number, selectedElements: Map<number, StatusType>, rootHeight: number): React.ReactNode {
    // here we calculate how much heatmap elements we can render without overlaping
    const maxElementsCount = rootHeight / MIN_HEATMAP_ELEMENT_HEIGHT;

    return elementsCount > maxElementsCount ?
        <ChunkedHeatmap
            elementsCount={elementsCount}
            selectedElements={selectedElements}
            chunkSize={Math.ceil(elementsCount / maxElementsCount)}/> :
        <Heatmap
            elementsCount={elementsCount}
            selectedElements={selectedElements}/>
}
