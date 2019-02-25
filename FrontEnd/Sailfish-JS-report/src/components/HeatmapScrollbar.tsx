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

import {h} from 'preact';
import { Scrollbars } from 'preact-custom-scrollbars';
import { StatusType } from '../models/Status';
import '../styles/heatmap.scss';

const SCROLLBAR_TRACK_WIDTH = 11;

interface HeatmapScrollbarProps {
    children: JSX.Element[];
    selectedElements: Map<number, StatusType>;
}

export const HeatmapScrollbar = ({children, selectedElements}: HeatmapScrollbarProps) => {
    return (
        <div class="heatmap">
            <Scrollbars
                renderThumbVertical={props => <div {...props} className="heatmap-scrollbar-thumb"/>}
                renderTrackVertical={({style, ...props}) => 
                    <div {...props} className="heatmap-scrollbar-track" style={{ ...style, width: SCROLLBAR_TRACK_WIDTH }}/>
                }
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

function renderHeatmap(elementsCount: number, selectedElements: Map<number, StatusType>): JSX.Element[] {
    let resultHeatmap : JSX.Element[] = [];

    for (let i = 0; i < elementsCount; i++) {
        resultHeatmap.push(
            <div class={"heatmap-scrollbar-item " + (selectedElements.get(i) || "").toLowerCase()}/>            
        )
    }

    return resultHeatmap;
}
