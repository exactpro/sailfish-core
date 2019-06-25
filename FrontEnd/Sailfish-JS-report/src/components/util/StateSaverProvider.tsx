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
import { Provider, StateSaverContext } from './StateSaver';

interface State {
    statesMap: Map<string, any>;
}

/**
 * Context provider for StateSaver
 */
export default class StateSaverProvider extends React.Component<{}, State> {

    constructor(props) {
        super(props);
        
        this.state = {
            statesMap: new Map<string, any>()
        };
    }

    private stateHandler = (stateKey: string, nextState: any) => {
        if (this.state.statesMap.get(stateKey) !== nextState) {
            // todo(we realy need to do something with it)
            
            // using 'setState(state)' is an anti-pattern, but we can't create new state on each call, 
            // because in some cases component recievs too many updates and it updates state only for last call.
            this.state.statesMap.set(stateKey, nextState);

            this.setState(this.state);
        }
    }

    render() {
        return (
            <Provider value={{ states: this.state.statesMap, saveState: this.stateHandler } as StateSaverContext}>
                {this.props.children}
            </Provider>
        )
    }   
}
