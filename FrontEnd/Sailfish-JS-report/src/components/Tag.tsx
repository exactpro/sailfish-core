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
import { getHashCode } from '../helpers/stringHash'
import '../styles/tag.scss';

const SATURATION = 90;
const LIGHTNESS = 40;

interface TagProps {
	tag: string;
};

function Tag({ tag }: TagProps){
	const hue = getHashCode(tag) % 360;
	const hsl = `hsl(${hue}, ${SATURATION}%, ${LIGHTNESS}%)`;
	return (
		<div className="tag" style={{ backgroundColor: hsl }}>
			{tag}
		</div>
	)
};
 
export default Tag;
