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

export function MessageCardSkeleton(){
	return (
		<div className="mc-skeleton">
			<div className="mc-skeleton__header">
				<div className="mc-skeleton__icons">
					<span className="mc-skeleton__icons-el"/>
					<span className="mc-skeleton__icons-el"/>
					<span className="mc-skeleton__icons-el"/>
					<span className="mc-skeleton__icons-el"/>
				</div>
				<div className="mc-skeleton__timestamp"/>
				<div className="mc-skeleton__name"/>
				<div className="mc-skeleton__session">
					<span className="mc-skeleton__session-el"/>
					<span className="mc-skeleton__session-el"/>
					<span className="mc-skeleton__session-el"/>
				</div>
				<div className="mc-skeleton__submit"/>
			</div>
			<div className="mc-skeleton__delimiter"/>
			<div className="mc-skeleton__body">
				<div className="mc-skeleton__body-text1"/>
				<div className="mc-skeleton__body-text2"/>
				<div className="mc-skeleton__body-type"/>
			</div>
		</div>
	)
}

export default MessageCardSkeleton;
