/*
 * ****************************************************************************
 *  Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ****************************************************************************
 */

import * as React from 'react';
import {PredictionData} from '../../models/MlServiceResponse';

const COLOR_HUE = 0;
const COLOR_SATURATION_PERCENTAGE = 100;
const COLOR_LIGHTNESS_MAX_PERCENTAGE = 60;
const COLOR_LIGHTNESS_MIN_PERCENTAGE = 90;
const VALUE_THRESHOLD = 0.5;
const VALUE_STEP_COUNT = 10;

interface Props {
    className?: string;
    prediction: PredictionData;
}

export const MessagePredictionIndicator = ({prediction, className = "mc-label"}: Props) => {
    const predictionPercentage = Math.round(prediction.predictedClassProbability * 100);

    return (
        <div className={className}
             style={{backgroundColor: `hsl(${COLOR_HUE}, ${COLOR_SATURATION_PERCENTAGE}%, ${getLightness(prediction.predictedClassProbability)}%)`}}>
            <div className="ml__prediction-icon"/>
            <div className="ml__prediction-percentage">{predictionPercentage === 100 ? null : predictionPercentage}</div>
        </div>
    )
};

function getLightness(value: number) {
    const valueFiltered = (value > VALUE_THRESHOLD)
        ? Math.round(((value - VALUE_THRESHOLD) / VALUE_THRESHOLD) * VALUE_STEP_COUNT) / VALUE_STEP_COUNT
        : 0;

    return COLOR_LIGHTNESS_MIN_PERCENTAGE - (valueFiltered * (COLOR_LIGHTNESS_MIN_PERCENTAGE - COLOR_LIGHTNESS_MAX_PERCENTAGE));
}
