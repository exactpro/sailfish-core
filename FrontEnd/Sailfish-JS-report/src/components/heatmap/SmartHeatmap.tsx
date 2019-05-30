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
import { StatusType } from "../../models/Status";
import { createSelector } from '../../helpers/styleCreators';
import { rangeSum } from '../../helpers/range';

interface SmartHeatmapProps {
    elementsCount: number;
    selectedElements: Map<number, StatusType>;
    rootHeight: number;
    elementHeightMapper: (index: number) => number;
}

const SmartHeatmap = ({ selectedElements, ...props }: SmartHeatmapProps) =>  (
    <div className="heatmap-scrollbar smart">
        {
            Array.from(selectedElements).map(([index, status]) => (
                <SmartHeatmapElement
                    {...props}
                    index={index}
                    status={status}/>
            ))
        }
    </div>
)

interface SmartHeatmapElementProps extends Omit<SmartHeatmapProps, 'selectedElements'> {
    index: number;
    status: StatusType;
}

const SmartHeatmapElement = ({ index, elementHeightMapper, elementsCount, rootHeight, status }: SmartHeatmapElementProps) => {
    const topOffset = rangeSum(0, index - 1, elementHeightMapper),
        elementHeight = elementHeightMapper(index),
        totalHeight = rangeSum(0, elementsCount, elementHeightMapper),
        scale = rootHeight / totalHeight,
        topOffsetScaled = topOffset * scale,
        elementHeightScaled = elementHeight * scale;

    const className = createSelector("heatmap-scrollbar-item", "smart", status), 
        style: React.CSSProperties = { 
            top: topOffsetScaled,
            height: elementHeightScaled
        };

    return (
        <div 
            className={className}
            style={style}/>
    );
}

export default SmartHeatmap;
