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
import '../styles/statusPanel.scss';
import { connect } from 'preact-redux';
import AppState from '../state/models/AppState';
import KnownBugCategory from '../models/KnownBugCategory';
import KnownBug from '../models/KnownBug';
import { KnownBugSummary } from './KnownBugSummary';

interface KnownBugPanelProps {
    bugs: (KnownBugCategory | KnownBug)[];
}

const KnownBugPanelBase = ({ bugs }: KnownBugPanelProps) => {

    return (
        <div class="known-bugs-panel">
            <KnownBugSummary data={bugs} />
        </div>
    );
}

export const KnownBugPanel = connect(
    (state: AppState) => ({
        bugs: state.selected.testCase.bugs
    })
)(KnownBugPanelBase);