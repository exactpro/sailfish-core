/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

import AppState from '../state/models/AppState';
import PanelArea from "../util/PanelArea";

export const getLeftPanelEnabled = (state: AppState) => state.selected.search.leftPanelEnabled;
export const getRightPanelEnabled = (state: AppState) => state.selected.search.rightPanelEnabled;
export const getLeftPanel = (state: AppState) => state.view.leftPanel;
export const getRightPanel = (state: AppState) => state.view.rightPanel;
export const getIsLeftPanelClosed = (state: AppState) => state.view.panelArea == PanelArea.P0;
export const getIsRightPanelClosed = (state: AppState) => state.view.panelArea == PanelArea.P100;

export const getIsConnectionError = (state: AppState) => state.view.isConnectionError;

export const getTestCaseLoadingProgress = (state: AppState) => {
	if (!state.selected.testCase?.files) return 100;
	const total = state.selected.testCase.files.action.count + state.selected.testCase.files.message.count;
	if (total == 0) return 100;
	const loaded = state.selected.testCase.actions.length + state.selected.testCase.messages.length;
	return Math.round(loaded / total * 100 );
  };
  