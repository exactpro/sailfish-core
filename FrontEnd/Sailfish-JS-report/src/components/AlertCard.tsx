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

import React from 'react'

import Alert from '../models/Alert';
import {createBemBlock, createBemElement} from '../helpers/styleCreators';
import '../styles/alert.scss';

interface AlertProps extends Alert {}

export const AlertCard = ({
	column,
	lines,
	message,
	type
}: AlertProps) => {
	const rootClassName = createBemBlock(
		"alert",
		type,
	),	iconClassName = createBemElement(
		'alert',
		'icon',
		type
	);

	return (
		<div className={rootClassName}>
			<div className="alert__row">
				<div className="alert__message">
					{message}
				</div>
				<div className={iconClassName}/>
			</div>
			<div className="alert__row">
				<div className="alert__column">
					<span className="alert__column-title">column:</span>
					<span className="alert__column-value">{column}</span>
				</div>
				<div className="alert__lines">
					<span className="alert__column-title">lines:</span>
					<span className="alert__lines-value">{lines}</span>
				</div>
			</div>
		</div>
	)
}

export default AlertCard;