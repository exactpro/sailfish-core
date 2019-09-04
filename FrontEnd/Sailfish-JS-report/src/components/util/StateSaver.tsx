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

export interface RecoverableElementProps {
    stateKey: string;
}

export interface StateSaverContext {
    states: Map<string, any>;
    saveState: (stateId: string, nextState: any) => any;
}

export const { Provider, Consumer } = React.createContext({});

export interface StateSaverProps<S> extends RecoverableElementProps {
    children: (state: S, stateHandler: (nextState: S) => any) => React.ReactNode;
    getDefaultState?: () => S;
}

/**
 * This wrapper saves component's state after unmount and recover it by key.
 * @param props renderChild - child render function that recieves recovered state, stateKey - key for recovering state from store 
 */
const StateSaver = <S extends {}>({ children, stateKey, getDefaultState }: StateSaverProps<S>) => (
    <Consumer>
        {
            ({ states, saveState }: StateSaverContext) => {
                const saveNextState = nextState => saveState(stateKey, nextState);

                if (states.has(stateKey) || !getDefaultState) {
                    return children(states.get(stateKey), saveNextState)
                } else {
                    const defaultState = getDefaultState();

                    return (
                        <DefaultStateSaver
                            saveState={saveNextState}
                            defaultState={defaultState}>
                            {children(defaultState, saveNextState)}
                        </DefaultStateSaver>
                    )
                }
            }
        }
    </Consumer>
)

interface DefaultStateSaverProps<S> {
    saveState: (nextState: S) => any;
    defaultState: S;
    children: React.ReactNode;
}

const DefaultStateSaver = <S extends {}>({ saveState, defaultState, children }: DefaultStateSaverProps<S>) => {
    React.useEffect(() => {
        saveState(defaultState);
    }, []);

    return <React.Fragment>{children}</React.Fragment>;
}

export default StateSaver;
