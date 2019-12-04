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
import '../styles/carousel.scss';
import { createBemBlock, createBemElement } from '../helpers/styleCreators';

export interface SelectionCarouselProps {
    itemsCount: number;
    currentIndex: number;
    next: () => any;
    prev: () => any;
    isEnabled?: boolean;
}

function SelectionCarousel({ itemsCount, currentIndex, next, prev, isEnabled = true }: SelectionCarouselProps) {
    return (
        <div className={createBemBlock("carousel", isEnabled ? null : "disabled")}>
            <div className={createBemElement("carousel", "icon", "prev", isEnabled ? null : "disabled")}
                title="Go to previous"
                onClick={() => isEnabled && prev()} />
            <p className="carousel__title">
                {currentIndex} of {itemsCount}
            </p>
            <div className={createBemElement("carousel", "icon", "next", isEnabled ? null : "disabled")}
                title="Go to next"
                onClick={() => isEnabled && next()} />
        </div>
    )
}

export default SelectionCarousel;
