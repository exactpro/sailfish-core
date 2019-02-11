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

import {h} from 'preact';
import Status from '../models/Status';
import ExpandablePanel from './ExpandablePanel';
import Exception from '../models/Exception';
import '../styles/statusPanel.scss';

interface StatusPaneProps {
    status: Status;
}

export const StatusPane = ({status}: StatusPaneProps) => {

    const rootClass = ["status-panel", status.status.toLowerCase()].join(' '),
        headerClass = ["status-panel-header", status.status.toLowerCase()].join(' ');

    return (
        <div class="status">
            <div class="status-controls"/>
            <div class={rootClass}>
                <ExpandablePanel>
                    <div class={headerClass}>   
                        <h3>{status.status.toUpperCase()}</h3>
                        <p>{status.description}</p>
                    </div>
                    {status.cause ? renderCause(status.cause) : null}
                </ExpandablePanel>
            </div>
        </div>
    );
}

const renderCause = (exception: Exception) => (
    <div class="status-panel-cause">
        <ExpandablePanel>
            <div class="status-panel-cause-header">
                <h3>{exception.message}</h3>
            </div>
            {exception.cause ? renderCause(exception.cause) : null}
            <div class="status-panel-cause-stacktrace">
                <pre>{exception.stacktrace}</pre>
            </div>
        </ExpandablePanel>
    </div>
)