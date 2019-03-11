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

export class HeatmapScrollbar extends Component<HeatmapScrollbarProps> {

    private scrollbar: Scrollbars;

    scrollToTop() {
        this.scrollbar && this.scrollbar.scrollToTop();
    }

    render({children, selectedElements}: HeatmapScrollbarProps) {
        return (
            <div class="heatmap">
                <Scrollbars
                    renderThumbVertical={props => <div {...props} className="heatmap-scrollbar-thumb"/>}
                    renderTrackVertical={({style, ...props}) => 
                        <div {...props} className="heatmap-scrollbar-track" style={{ ...style, width: SCROLLBAR_TRACK_WIDTH }}/>
                    }
                    ref={ref => this.scrollbar = ref}
                    >
                    <div class="heatmap-wrapper">
                        {children}
                    </div>
                </Scrollbars>
                <div class="heatmap-scrollbar">
                    {renderHeatmap(children.length, selectedElements)}
                </div>
            </div>
        )
    }
}

function renderHeatmap(elementsCount: number, selectedElements: Map<number, StatusType>): JSX.Element[] {
    // here we calculate how much heatmap elements we can render without overlaping
    const maxElementsCount = document.body.scrollHeight / MIN_HEATMAP_ELEMENT_HEIGHT;

    return elementsCount > maxElementsCount ? 
        renderBigHeatmap(elementsCount, selectedElements, Math.ceil(elementsCount / maxElementsCount)) :
        renderSmallHeatmap(elementsCount, selectedElements);
}

function renderSmallHeatmap(elementsCount: number, selectedElements: Map<number, StatusType>): JSX.Element[] {
    let resultHeatmap : JSX.Element[] = [];

    for (let i = 0; i < elementsCount; i++) {
        resultHeatmap.push(
            <div class={"heatmap-scrollbar-item " + (selectedElements.get(i) || "").toLowerCase()}/>            
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

    let resultHeatmap : JSX.Element[] = [];

    for (let i = 0; i < elementsCount; i++) {
        if (selectedElements.get(i)) { 
            resultHeatmap.push(
                <div class={"heatmap-scrollbar-item " + selectedElements.get(i).toLowerCase()}/>            
            );

            // after we added heatmap element to the list, we should skip other elements in chunk
            i += chunkSize - ((i + 1) % chunkSize);
        } else if ((i + 1) % chunkSize == 0) {
            // no selected elements in current chunk, render empty heatmap element
            resultHeatmap.push(
                <div class="heatmap-scrollbar-item"/>            
            );
        }
    }

    return resultHeatmap;
}