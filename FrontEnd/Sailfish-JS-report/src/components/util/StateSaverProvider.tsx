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

import { h, Component } from 'preact';
import { Provider, StatesMap, StateSaverContext } from './StateSaver';

interface ProviderProps {
    children: JSX.Element | JSX.Element[];
}

interface ProviderState {
    statesMap: StatesMap;
}


/**
 * Context provider for StateSaver
 */
export default class StateSaverProvider extends Component<ProviderProps, ProviderState> {

    constructor(props) {
        super(props);
        
        this.state = {
            statesMap: new Map<string, any>()
        };
    }

    private stateHandler = (stateKey: string, nextState: any) => {
        if (this.state.statesMap[stateKey] !== nextState) {
            this.setState({
                statesMap: {
                    ...this.state.statesMap,
                    [stateKey]: nextState
                }
            });
        }
    }

    render({ children }: ProviderProps, { statesMap }: ProviderState) {
        return (
            <Provider value={{ states: statesMap, saveState: this.stateHandler } as StateSaverContext}>
                {children}
            </Provider>
        )
    }   
}
