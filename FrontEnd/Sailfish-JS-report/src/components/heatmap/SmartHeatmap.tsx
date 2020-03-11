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
import { createStyleSelector } from '../../helpers/styleCreators';
import { rangeSum } from '../../helpers/range';
import ResizeObserver from "resize-observer-polyfill";
import ScrollHintWindow from '../ScrollHintWindow';
import { ScrollHint } from '../../models/util/ScrollHint';

interface SmartHeatmapProps {
    elementsCount: number;
    selectedElements: Map<number, StatusType>;
    elementHeightMapper: (index: number) => number;
    scrollHints?: ScrollHint[];
}

function SmartHeatmap({ selectedElements, scrollHints, ...props }: SmartHeatmapProps) {
    const [rootHeight, setRootHeight] = React.useState<number>(0);
    const root = React.useRef<HTMLDivElement>();

    React.useEffect(() => {
        const resizeObserver = new ResizeObserver(elements => {
            const nextRootHeight = elements[0]?.contentRect.height;

            if (nextRootHeight != rootHeight) {
                setRootHeight(nextRootHeight);
            }
        });

        resizeObserver.observe(root.current);

        return () => {
            resizeObserver.unobserve(root.current);
        }
    }, []);

    return (
        <div className="heatmap-scrollbar smart" ref={root}>
            {
                Array.from(selectedElements).map(([index, status]) => (
                    <SmartHeatmapElement
                        {...props}
                        rootHeight={rootHeight}
                        key={index}
                        index={index}
                        status={status}
                        scrollHint={scrollHints.find(scrollHint => scrollHint.index === index) ?? null}
                    />
                ))
            }
        </div>
    )
}

interface SmartHeatmapElementProps extends Omit<SmartHeatmapProps, 'selectedElements'> {
    index: number;
    status: StatusType;
    rootHeight: number;
    scrollHint: ScrollHint | null;
}

const SmartHeatmapElement = ({ index, elementHeightMapper, elementsCount, rootHeight, status, scrollHint }: SmartHeatmapElementProps) => {
    const topOffset = rangeSum(0, index - 1, elementHeightMapper),
        elementHeight = elementHeightMapper(index),
        totalHeight = rangeSum(0, elementsCount, elementHeightMapper),
        scale = rootHeight / totalHeight,
        topOffsetScaled = topOffset * scale,
        elementHeightScaled = elementHeight * scale;

    const className = createStyleSelector("heatmap-scrollbar-item", "smart", status),
        style: React.CSSProperties = {
            top: topOffsetScaled,
            height: elementHeightScaled,
            right: 0,
            left: 0,
            position: 'absolute',
        };

    return (
        <div style={style}>
            <div className={className} />
            {scrollHint && <ScrollHintWindow scrollHint={scrollHint} />}
        </div>
    );
};

export default SmartHeatmap;
