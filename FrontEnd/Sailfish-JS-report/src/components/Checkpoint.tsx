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
import "../styles/checkpoint.scss";
import { createStyleSelector } from '../helpers/styleCreators';

export interface CheckpointStateProps {
    name: string;
    index: number;
    description?: string;
    isSelected: boolean;
}

export interface CheckpointDispatchProps {
    clickHandler?: () => any;
}

interface CheckpointProps extends CheckpointStateProps, CheckpointDispatchProps {}

const Checkpoint = ({ name, index, isSelected, clickHandler = () => {}, description = '' }: CheckpointProps) => {

    const rootClassName = createStyleSelector(
        "checkpoint", 
        isSelected ? "selected" : ""
    );

    return (
        <div className={rootClassName}
            onClick={() => clickHandler()}>
            <div className="checkpoint-icon" />
            <div className="checkpoint-index">{index}</div>
            <div className="checkpoint-name">{name}</div>
            <div className="checkpoint-description">{description}</div>
        </div>
    )
};

export default Checkpoint;
