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

function NetworkError(){
	return (
		<div className="tc-error">
			<div className="tc-error__message">
				<span className="tc-error__attention-icon"/>
				<span className="tc-error__message-text">
					Please try to refresh the page
				</span>
				<div className="tc-error__refresh-button" onClick={() => location.reload()}>
					<span className="tc-error__refresh-icon"/>
					<span className="tc-error__refresh-text">
						Refresh
					</span>
				</div>
			</div>
		</div>
	)
}

export default NetworkError;