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

interface Props {
	progress: number;
	delay?: number;
}

export function LinearProgressBar({ progress, delay = 150 }: Props){
	if (progress >= 100) return null;

	const [showProgress, setShowProgress] = React.useState(false);

	React.useEffect(() => {
		const timer = setTimeout(() => {
			setShowProgress(true);
		}, delay);
		return () => clearTimeout(timer)
	}, []);

	return showProgress ? (
		<div className="progress-bar">
			<div className="progress-bar__filler" style={{ width: `${progress}%`}}/>
		</div> 
	): null;
}

export default LinearProgressBar;
