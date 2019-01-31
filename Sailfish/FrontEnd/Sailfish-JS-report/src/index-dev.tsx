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

import { h, render } from "preact";
import { App } from "./components/App-dev";
import { Provider } from 'preact-redux';
import { createAppStore } from './store/store';
import { testReport } from './test/testReport';
// enable react-devtools compatibility, APP WORKNIG SLOW WITH THIS
//import 'preact/devtools';

render(
    <Provider store={createAppStore(testReport)}>
        <App/>
    </Provider>, 
    document.getElementById("index"));