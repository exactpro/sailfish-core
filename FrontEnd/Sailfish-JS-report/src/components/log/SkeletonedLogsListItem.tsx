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

import React from 'react';
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import LogCardSkeleton from './LogCardSkeleton';
import LogCard from './LogCard';
import Log from '../../models/Log';

interface OwnProps {
	index: number;
}

interface MappedProps {
	logs: Log[];
}

export interface SkeletonedLogsListItemProps extends OwnProps, MappedProps { }

function SkeletonedLogsListItem({ logs, index }: SkeletonedLogsListItemProps ){
	const log = logs[index];
	if (!log) {
		return <LogCardSkeleton />
	}

	return <LogCard log={log} index={index} />
}

export default connect(
	(state: AppState, ownProps: OwnProps): MappedProps => ({
		logs: state.selected.testCase.logs
	})
)(SkeletonedLogsListItem);
