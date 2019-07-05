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
import { StatusType } from '../../models/Status';
import range from '../../helpers/range';
import { createSelector } from '../../helpers/styleCreators';

interface ChunkedHeatmapProps {
    elementsCount: number;
    selectedElements: Map<number, StatusType>;
    chunkSize: number;
}

// this fucntion is used for rendering heatmap only for big lists
// we divide the list of elements into chunks and render only one heatmap element for each chunk

// WARNING : we can render only one element in a chunk,
// so if chunk contains multiple selected elements with different status,
// it will render ONLY FIRST status element in a chunk
// In current version several selected elements can't have different statuses, so we can safely use this trick
const ChunkedHeatmap = ({ elementsCount, selectedElements, chunkSize}: ChunkedHeatmapProps) => (
    <div className="heatmap-scrollbar">
        {
            range(0, elementsCount).map(i => {
                if (selectedElements.has(i)) {
                    const element = selectedElements.get(i);

                    // after we added heatmap element to the list, we should skip other elements in chunk
                    i += chunkSize - ((i + 1) % chunkSize);

                    return (
                        <div className={createSelector("heatmap-scrollbar-item", element)} />
                    );
        
                } else if ((i + 1) % chunkSize == 0) {
                    // no selected elements in current chunk, render empty heatmap element
                    return (
                        <div className="heatmap-scrollbar-item" />
                    );
                }
            })
        }
    </div>
)

export default ChunkedHeatmap;
