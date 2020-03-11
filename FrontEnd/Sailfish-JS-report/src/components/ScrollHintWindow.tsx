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
import { connect } from 'react-redux';
import { createStyleSelector } from '../helpers/styleCreators';
import { selectAction, selectMessage } from '../actions/actionCreators';
import AppState from '../state/models/AppState';
import Action, { isAction, ActionNode } from '../models/Action';
import Message from '../models/Message';
import { ScrollHint } from '../models/util/ScrollHint';
import { getScrollHintItem } from '../selectors/util';
import { raf } from '../helpers/raf';

interface StateProps {
	item: ActionNode | Message | undefined;
}

interface OwnProps {
	scrollHint: ScrollHint;
}

interface Props extends StateProps {
	scrollHint: ScrollHint;
	selectItem: () => void;
}

const ScrollHintWindow = ({ scrollHint, selectItem }: Props) => {
	const [showOnLeftSide, setShowOnLeftSide] = React.useState(false);
	const hintRef = React.useRef<HTMLDivElement>(null);
	React.useEffect(() => {
		raf(() => {
			const bounding = hintRef.current?.getBoundingClientRect();
			if (bounding.right > (window.innerWidth || document.documentElement.clientWidth)) {
				setShowOnLeftSide(true);
			}
			/*
				There are might be situations when list has a lot of items and we need to show hint on last of them
				therefore hint window could be not fully rendered. So we check if its bellow viewport
			*/
			const appPaddingBottom = parseFloat(window.getComputedStyle(document.getElementById('index'))?.paddingBottom); 
			// we need to take app bottom padding into consideration
			if (bounding.bottom > window.innerHeight - appPaddingBottom) {
				const translateY = window.innerHeight - bounding.bottom - bounding.height / 2 - appPaddingBottom;
				hintRef.current.style.transform = `translate3d(0, ${translateY}px, 0)`;
			}
		}, 5);
	}, [])

	const rootClassName = createStyleSelector(
		"scroll-hint",
		showOnLeftSide ? "left" : null
	);

	return (
		<div
			className={rootClassName}
			ref={hintRef}>
			<div className="scroll-hint__title">Go to {scrollHint.type}</div>
			<div className="scroll-hint__id" onClick={selectItem}># {scrollHint.id}</div>
		</div>
	)
}


export default connect(
	(state: AppState, ownProps: OwnProps): StateProps => {
		const selector = getScrollHintItem(ownProps.scrollHint);
		return {
			item: selector(state),
		}
	},
	dispatch => ({
		selectAction: (action: Action) => dispatch(selectAction(action, true)),
		selectMessage: (message: Message) => dispatch(selectMessage(message, null, true)), 
	}),
	(stateProps, dispatchProps, ownProps) => ({
		...stateProps,
		...ownProps,
		selectItem: () => {
			switch(ownProps.scrollHint.type) {
				case 'Message':
					return dispatchProps.selectMessage(stateProps.item as Message);
				case 'Action':
					return isAction(stateProps.item) && dispatchProps.selectAction(stateProps.item);
			}
		}
	})
)(ScrollHintWindow);
