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

import { h } from 'preact';
import { createContext } from 'preact-context';

export interface RecoverableElementProps {
    stateKey: string;
}

export interface StatesMap {
    [key: string]: any;
}

export interface StateSaverContext {
    states: StatesMap;
    saveState: (stateId: string, nextState: any) => any;
}

export const { Provider, Consumer } = createContext({});

export interface StateSaverProps extends RecoverableElementProps {
    children: (state: any, stateHandler: (nextState: any) => any) => JSX.Element;
}

/**
 * This wrapper saves component's state after unmount and recover it by key.
 * @param props renderChild - child render function that recieves recovered state, stateKey - key for recovering state from store 
 */
const StateSaver = ({ children, stateKey }: StateSaverProps) => (
    <Consumer>
        {
            // at preact children's always is array, so if we need to use render prop as a child, we need to get first child in childrens array
            // https://github.com/developit/preact/issues/45#issuecomment-182326000
            ({ states, saveState }: StateSaverContext) => children[0](
                states[stateKey], 
                (nextState) => saveState(stateKey, nextState)
            )
        }
    </Consumer>
)

export default StateSaver;
