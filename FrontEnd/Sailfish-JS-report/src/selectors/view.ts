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

export const getIsConnectionError = (state: AppState) => state.view.isConnectionError;

export const getTestCaseLoadingProgress = (state: AppState) => {
	return state.selected.testCase?.files ? Math.round(
	  (state.selected.testCase.actions.length + state.selected.testCase.messages.length) /
		(state.selected.testCase.files.action.count + state.selected.testCase.files.message.count) * 100
	) : 0;
  };
  