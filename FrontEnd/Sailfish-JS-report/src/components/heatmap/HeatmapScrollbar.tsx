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
import topWindow, { isTopWindowAvailable } from '../../helpers/getWindow';

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

export default class HeatmapScrollbar extends React.Component<HeatmapScrollbarProps, HeatmapScrollbarState> {

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
                    onScrollStart={this.onScrollStart}
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
                        <Heatmap
                            elementsCount={elementsCount}
                            selectedElements={selectedElements}/>
                }
            </div>
        )
    }

    private onScrollStart = () => {
        // 'react-custom-scrollbars library is using document.addEventListenter('mouseup', ...) to handle user's LMB release,
        // but it doesn't work when scrollbars displayed in iframe and user releases LMB out of iframe window.
        // https://github.com/malte-wessel/react-custom-scrollbars/blob/b353cc4956d6154d6a100f34c3a6202c75434186/src/Scrollbars/index.js#L328
        // Using 'top.window.document' instead of 'document' to handle mouse events can be a solution. 

        if (!isTopWindowAvailable) {
            return;
        }

        // Also, we need 'ts-ignore' here because type declarations for this library doesn't allow us to access private properties.
        // @ts-ignore
        if (this.scrollbar.current.dragging) {
            topWindow.document.addEventListener('mouseup', this.onMouseUp);
        }
    }

    private onMouseUp = () => {
        // @ts-ignore
        this.scrollbar.current.handleDragEnd();
        topWindow.document.removeEventListener('mouseup', this.onMouseUp);
    }
}
