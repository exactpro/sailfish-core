/*******************************************************************************
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
 *  limitations under the License.
 ******************************************************************************/

import * as React from 'react';
import { connect } from "react-redux";
import AppState from "../../state/models/AppState";
import { setSearchLeftPanelEnabled, setSearchRightPanelEnabled } from "../../actions/actionCreators";
import Panel from "../../util/Panel";
import Checkbox from "../util/Checkbox";
import {getIsLeftPanelClosed, getIsRightPanelClosed} from "../../selectors/view";

interface StateProps {
    leftPanelEnabled: boolean;
    leftPanel: Panel;
    rightPanelEnabled: boolean;
    rightPanel: Panel;
    isLeftPanelClosed: boolean;
    isRightPanelClosed: boolean;
}

interface DispatchProps {
    onLeftPanelEnable: (isEnabled: boolean) => void;
    onRightPanelEnable: (isEnabled: boolean) => void;
}

interface Props extends StateProps, DispatchProps {}

function SearchPanelControlBase(props: Props) {
    const {
        leftPanelEnabled,
        rightPanelEnabled,
        onRightPanelEnable,
        onLeftPanelEnable,
        leftPanel,
        rightPanel,
        isLeftPanelClosed,
        isRightPanelClosed
    } = props;

    const onLeftPanelSelect = () => {
        if (!isLeftPanelClosed) {
            onLeftPanelEnable(!leftPanelEnabled);
        }
    };

    const onRightPanelSelect = () => {
        if (!isRightPanelClosed) {
            onRightPanelEnable(!rightPanelEnabled);
        }
    };

    return (
        <div className="search-panel-controls">
            <Checkbox
                className='search-panel-controls__checkbox'
                id='left-panel'
                checked={leftPanelEnabled}
                label={leftPanel.toLowerCase().replace('_', ' ')}
                onChange={onLeftPanelSelect}
                isDisabled={isLeftPanelClosed}/>
            <Checkbox
                className='search-panel-controls__checkbox'
                id='right-panel'
                checked={rightPanelEnabled}
                label={rightPanel.toLowerCase().replace('_', ' ')}
                onChange={onRightPanelSelect}
                isDisabled={isRightPanelClosed}/>
        </div>
    )
}

const SearchPanelControl = connect(
    (state: AppState): StateProps => ({
        leftPanelEnabled: state.selected.search.leftPanelEnabled,
        leftPanel: state.view.leftPanel,
        rightPanelEnabled: state.selected.search.rightPanelEnabled,
        rightPanel: state.view.rightPanel,
        isLeftPanelClosed: getIsLeftPanelClosed(state),
        isRightPanelClosed: getIsRightPanelClosed(state)
    }),
    (dispatch): DispatchProps => ({
        onLeftPanelEnable: isEnabled => dispatch(setSearchLeftPanelEnabled(isEnabled)),
        onRightPanelEnable: isEnabled => dispatch(setSearchRightPanelEnabled(isEnabled))
    })
)(SearchPanelControlBase);

export default SearchPanelControl;
