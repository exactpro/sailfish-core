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
import SmartHeatmap from './SmartHeatmap';
import Heatmap from './Heatmap';
import { ScrollHint } from '../../models/util/ScrollHint';

const SCROLLBAR_TRACK_WIDTH = 11;

interface HeatmapScrollbarProps {
    selectedElements: Map<number, StatusType>;
    heightMapper?: (index: number) => number;
    elementsCount: number;
    height?: number;
    width?: number;
    onScroll?: React.UIEventHandler<any>;
    scrollHints?: ScrollHint[];
}

export default class HeatmapScrollbar extends React.Component<HeatmapScrollbarProps> {

    private scrollbar = React.createRef<Scrollbars>();

    scrollToTop() {
        this.scrollbar.current?.scrollToTop();
    }

    scrollTop(scrollOptions: ScrollToOptions) {
        this.scrollbar.current?.scrollTop(scrollOptions.top);
    }

    render() {
        const { children, selectedElements, elementsCount, height, width, onScroll, heightMapper, scrollHints = [] } = this.props,
            style = height !== undefined || width !== undefined ? { height, width } : undefined;

        return (
            <div className="heatmap" style={style}>
                <Scrollbars
                    style={style}
                    renderThumbVertical={props => (
                        <div {...props} className="heatmap-scrollbar-thumb" />
                    )}
                    renderTrackVertical={({ style, ...props }) => (
                        <div {...props} className="heatmap-scrollbar-track" style={{ ...style, width: SCROLLBAR_TRACK_WIDTH }} />
                    )}
                    onScroll={onScroll}
                    ref={this.scrollbar}>
                    <div className="heatmap-wrapper">
                        { /* We need wrapper with position relative to work properly with absolute position childs. */ }
                        <div style={{ position: 'relative', width: '100%', height: '100%' }}>
                            {children}
                        </div>
                    </div>
                </Scrollbars>
                {
                    heightMapper ?
                        <SmartHeatmap
                            elementsCount={elementsCount}
                            selectedElements={selectedElements}
                            elementHeightMapper={heightMapper}
                            scrollHints={scrollHints}
                            /> :
                        <Heatmap
                            elementsCount={elementsCount}
                            selectedElements={selectedElements} />
                }
            </div>
        )
    }
}
