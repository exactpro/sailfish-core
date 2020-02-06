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
import Exception from '../models/Exception';
import { createStyleSelector } from '../helpers/styleCreators';
import StateSaver, { RecoverableElementProps } from './util/StateSaver';
import { stopPropagationHandler } from '../helpers/react';

interface Props {
    exception: Exception;
}

interface BaseProps extends Props {
    isExpanded: boolean;
    onExpand: () => any;
}

const ExceptionCardBase = ({ exception, isExpanded, onExpand }: BaseProps) => {
    return (
        <div className="status-panel failed">
            <div className="status-panel-exception-wrapper">
                <div className="status-panel-exception-header">
                    {
                        isExpanded ? (
                            <div>{exception.class}: </div>
                        ) : null
                    }
                    <div className={createStyleSelector("status-panel-exception-header-message", exception.message ? "" : "disabled")}>
                        {exception.message || "No exception message"}
                    </div>
                </div>
                <div className="status-panel-exception-expand" onClick={stopPropagationHandler(onExpand)}>
                    <div className="status-panel-exception-expand-title">More</div>
                    <div className={createStyleSelector("status-panel-exception-expand-icon", (isExpanded ? "expanded" : "hidden"))} />
                </div>
            </div>
            {
                isExpanded ? (
                    <div className="status-panel-exception-stacktrace">
                        <pre>{exception.stacktrace}</pre>
                    </div>
                ) : null
            }
        </div>
    )
}

export const ExceptionCard = ({ exception }: Props) => {
    const [isExpanded, setIsExpanded] = React.useState(false);

    return (
        <ExceptionCardBase
            exception={exception}
            isExpanded={isExpanded}
            onExpand={() => setIsExpanded(!isExpanded)}/>
    )
}

interface RecoverableProps extends Props, RecoverableElementProps {
    onExpand?: () => any;
}

export const RecoverableExceptionCard = ({ stateKey, ...props }: RecoverableProps) => (
    <StateSaver
        stateKey={stateKey}
        getDefaultState={() => false}>
        {
            (isExpanded: boolean, setIsExpanded: (boolean) => any) => (
                <ExceptionCardBase
                    {...props}
                    isExpanded={isExpanded}
                    onExpand={() => {
                        setIsExpanded(!isExpanded);
                        props.onExpand && props.onExpand();
                    }}/>
            )
        }
    </StateSaver>
)

export default ExceptionCard;
