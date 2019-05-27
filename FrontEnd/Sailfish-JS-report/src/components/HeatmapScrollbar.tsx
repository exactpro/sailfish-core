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

import { h, Component } from 'preact';
import { Scrollbars } from 'preact-custom-scrollbars';
import { StatusType } from '../models/Status';
import '../styles/heatmap.scss';

const MIN_HEATMAP_ELEMENT_HEIGHT = 8;
const SCROLLBAR_TRACK_WIDTH = 11;

interface HeatmapScrollbarProps {
    children: JSX.Element[];
    selectedElements: Map<number, StatusType>;
}

interface HeatmapScrollbarState {
    rootHeight: number;
}

export class HeatmapScrollbar extends Component<HeatmapScrollbarProps, HeatmapScrollbarState> {

    private scrollbar: Scrollbars;
    private root: HTMLElement;

    constructor(props) {
        super(props);

        this.state = { rootHeight: 0 }
    }

    scrollToTop() {
        this.scrollbar && this.scrollbar.scrollToTop();
    }

    componentDidMount() {
        // at the first mount we can't get real component height, 
        // so we need to rerender component with new root element scroll height

        this.setState({
            rootHeight: this.root && this.root.scrollHeight
        });
    }

    render({ children, selectedElements }: HeatmapScrollbarProps, { rootHeight }: HeatmapScrollbarState) {
        return (
            <div class="heatmap" ref={ref => this.root = ref}>
                <Scrollbars
                    renderThumbVertical={props => <div {...props} className="heatmap-scrollbar-thumb" />}
                    renderTrackVertical={({ style, ...props }) =>
                        <div {...props} className="heatmap-scrollbar-track" style={{ ...style, width: SCROLLBAR_TRACK_WIDTH }} />
                    }
                    onScrollStart={this.onScrollStart}
                    ref={ref => this.scrollbar = ref}
                >
                    <div class="heatmap-wrapper">
                        {children}
                    </div>
                </Scrollbars>
                <div class="heatmap-scrollbar">
                    {
                        rootHeight ? 
                            renderHeatmap(children.length, selectedElements, rootHeight) : 
                            null
                    }
                </div>
            </div>
        )
    }

    private onScrollStart = () => {
        // '(p)react-custom-scrollbars library is using document.addEventListenter('mouseup', ...) to handle user's LMB release,
        // but it doesn't work when scrollbars displayed in iframe and user releases LMB out of iframe window.
        // https://github.com/malte-wessel/react-custom-scrollbars/blob/b353cc4956d6154d6a100f34c3a6202c75434186/src/Scrollbars/index.js#L328
        // Using 'top.window.document' instead of 'document' to handle mouse events can be a solution. 

        // Also, we need 'ts-ignore' here because type declarations for this library doesn't allow us to access private properties.
        // @ts-ignore
        if (this.scrollbar.dragging) {
            top.window.document.addEventListener('mouseup', this.onMouseUp);
        }
    }

    private onMouseUp = () => {
        // @ts-ignore
        this.scrollbar.handleDragEnd();
        top.window.document.removeEventListener('mouseup', this.onMouseUp);
    }
}

function renderHeatmap(elementsCount: number, selectedElements: Map<number, StatusType>, rootHeight: number): JSX.Element[] {
    // here we calculate how much heatmap elements we can render without overlaping
    const maxElementsCount = rootHeight / MIN_HEATMAP_ELEMENT_HEIGHT;

    return elementsCount > maxElementsCount ?
        renderBigHeatmap(elementsCount, selectedElements, Math.ceil(elementsCount / maxElementsCount)) :
        renderSmallHeatmap(elementsCount, selectedElements);
}

function renderSmallHeatmap(elementsCount: number, selectedElements: Map<number, StatusType>): JSX.Element[] {
    let resultHeatmap: JSX.Element[] = [];

    for (let i = 0; i < elementsCount; i++) {
        resultHeatmap.push(
            <div class={"heatmap-scrollbar-item " + (selectedElements.get(i) || "").toLowerCase()} />
        );
    }

    return resultHeatmap;
}

function renderBigHeatmap(elementsCount: number, selectedElements: Map<number, StatusType>, chunkSize: number): JSX.Element[] {
    // this fucntion is used for rendering heatmap only for big lists
    // the idea is that we divide the list of elements into chunks and render only one heatmap element for each chunk

    // WARNING : we can render only one element in a chunk,
    // so if chunk contains multiple selected elements with different status,
    // it will render ONLY FIRST status element in a chunk
    // In current version several selected elements can't have different statuses, so we can safely use this trick

    let resultHeatmap: JSX.Element[] = [];

    for (let i = 0; i < elementsCount; i++) {
        if (selectedElements.get(i)) {
            resultHeatmap.push(
                <div class={"heatmap-scrollbar-item " + selectedElements.get(i).toLowerCase()} />
            );

            // after we added heatmap element to the list, we should skip other elements in chunk
            i += chunkSize - ((i + 1) % chunkSize);
        } else if ((i + 1) % chunkSize == 0) {
            // no selected elements in current chunk, render empty heatmap element
            resultHeatmap.push(
                <div class="heatmap-scrollbar-item" />
            );
        }
    }

    return resultHeatmap;
}