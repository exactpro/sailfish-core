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

import * as React from 'react';

export function LogCardSkeleton(){
	return (
		<div className="lc-skeleton">
			<div className="lc-skeleton__header">
				<div className="lc-skeleton__level"/>
				<div className="lc-skeleton__timestamp"/>
				<div className="lc-skeleton__thread grouped">
					<span className="lc-skeleton__thread-label"/>
					<span className="lc-skeleton__thread-value"/>
				</div>
				<div className="lc-skeleton__class grouped">
					<span className="lc-skeleton__class-label"/>
					<span className="lc-skeleton__class-value"/>
				</div>
				<div className="lc-skeleton__exception grouped">
					<span className="lc-skeleton__exception-label"/>
					<span className="lc-skeleton__exception-value"/>
				</div>
			</div>
			<div className="lc-skeleton__delimiter"/>
			<div className="lc-skeleton__body">
				<div className="lc-skeleton__body-message1"/>
				<div className="lc-skeleton__body-message2"/>
			</div>
		</div>
	)
}

export default LogCardSkeleton;
