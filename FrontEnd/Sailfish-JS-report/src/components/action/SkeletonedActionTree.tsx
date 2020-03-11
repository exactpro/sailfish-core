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
import ActionCardSkeleton from './ActionCardSkeleton';
import { ActionTree } from './ActionTree';
import { ActionNode } from '../../models/Action';
import { getCurrentActions } from '../../selectors/actions';

interface OwnProps {
	index: number;
}

interface MappedProps {
	action: ActionNode | null;
}

export interface SkeletonedActionTreeProps extends OwnProps, MappedProps { }

function SkeletonedActionTree({ action }: SkeletonedActionTreeProps ){
	if (!action) {
		return <ActionCardSkeleton />
	}

	return <ActionTree action={action} />
}

export default connect(
	(state: AppState, ownProps: OwnProps): MappedProps => ({
		action: getCurrentActions(state)[ownProps.index] ?? null
	})
)(SkeletonedActionTree);
