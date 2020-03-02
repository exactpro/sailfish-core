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
import Log from '../../models/Log';
import { createStyleSelector } from '../../helpers/styleCreators';
import SearchableContent from "../search/SearchableContent";
import { keyForLog } from "../../helpers/keys";

interface LogCardProps {
	log: Log;
	index: number;
}

export default function LogCard({ log , index }: LogCardProps){
	const {
		class: classStr,
		exception,
		level,
		message,
		thread,
		timestamp,
	} = log;

	const rootClassName = createStyleSelector(
        "log-card",
        level
	);
	
	return (
		<div className={rootClassName}>
			<div className="log-card__head">
				<div className="log-card__level">{level}</div>
				<div className="log-card__thread">Thread</div>
				<div className="log-card__thread-val">
					<SearchableContent
						contentKey={keyForLog(index, 'thread')}
						content={thread}/>
				</div>
				<div className="log-card__timestamp">{timestamp}</div>
				<div className="log-card__class">Class</div>
				<div className="log-card__class-val">
					<SearchableContent
						contentKey={keyForLog(index, 'class')}
						content={classStr}/>
				</div>
				{
					exception && 
						<>
							<div className="log-card__exception">Exception</div>
							<div className="log-card__exception-val">{exception.class}</div>
						</>
				}
			</div>
			<div className="log-card__delimiter" />
			<pre className="log-card__message">
				<SearchableContent
					contentKey={keyForLog(index, 'message')}
					content={message}/>
			</pre>
		</div>
	)
}
