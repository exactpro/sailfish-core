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
import MessageCardSkeleton from './MessageCardSkeleton';
import { isAdmin, isCheckpointMessage } from '../../helpers/message';
import { AdminMessageWrapper } from './AdminMessageWrapper';
import CheckpointMessage from './CheckpointMessage';
import MessageCard from './MessageCard';
import Message from '../../models/Message';
import { getCurrentMessages } from '../../selectors/messages';

interface OwnProps {
	index: number;
}

interface MappedProps {
	message: Message | null;
}

export interface SkeletonedMessageCardListItemProps extends OwnProps, MappedProps { }

function SkeletonedMessageCardListItem({ message }: SkeletonedMessageCardListItemProps ){
	if (!message) {
		return <MessageCardSkeleton />
	}

	if (isCheckpointMessage(message)) {
		return <CheckpointMessage message={message}/>;
	}

	if (isAdmin(message)) {
		return (
			<AdminMessageWrapper
				key={message.id}
				message={message}/>
		)
	}

	return (
		<MessageCard
			key={message.id}
			message={message}/>
	);
}

export default connect(
	(state: AppState, ownProps: OwnProps): MappedProps => ({
		message: getCurrentMessages(state)[ownProps.index] ?? null
	})
)(SkeletonedMessageCardListItem);
