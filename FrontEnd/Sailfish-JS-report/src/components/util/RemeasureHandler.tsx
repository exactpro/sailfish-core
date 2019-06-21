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

interface Props {
    style: any;
    itemSpacing: number;
    measureHandler: () => any;
    children: React.ReactNode;
}

/**
 * This component is used to handle situation when row height changed since last render
 * (eg action card's expand state was changed)
 * @param props
 */
const RemeasureHandler = ({ style, measureHandler, children, itemSpacing }: Props) => {

    const outerRef = React.useRef<HTMLDivElement>(),
        innerRef = React.useRef<HTMLDivElement>();

    // same as componentDidMount
    React.useEffect(() => {
        if (outerRef.current.offsetHeight !== innerRef.current.offsetHeight) {
            measureHandler();
        }
    }, []);

    return (
        <div style={style} ref={outerRef}>
            <div ref={innerRef} style={{paddingTop: itemSpacing / 2, paddingBottom: itemSpacing / 2}}>
                { children }
            </div>
        </div>
    )
}

export default RemeasureHandler;
