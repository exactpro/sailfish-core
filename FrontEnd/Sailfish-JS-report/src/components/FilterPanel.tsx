/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
import { StatusType } from '../models/Status';
import '../styles/header.scss';
import { ToggleButton } from './ToggleButton';
import { connect } from 'react-redux';
import AppState from '../state/models/AppState';
import { switchActionsFilter, switchFieldsFilter, switchActionsTransparencyFilter, switchFieldsTransparencyFilter } from '../actions/actionCreators';
import StatusFilterToggler from './StatusFilterToggler';

const ACTIONS_STATUSES = [StatusType.PASSED, StatusType.FAILED, StatusType.CONDITIONALLY_PASSED],
    FIELDS_STATUSES = [StatusType.PASSED, StatusType.FAILED, StatusType.CONDITIONALLY_PASSED, StatusType.NA];

interface StateProps {
    actionsFilters: Set<StatusType>;
    fieldsFilters: Set<StatusType>;
    actionsTransparencyFilters: Set<StatusType>;
    fieldsTransparencyFilters: Set<StatusType>;
}

interface DispatchProps {
    actionFilterHandler: (status: StatusType) => void;
    fieldsFilterHandler: (status: StatusType) => void;
    actionsTransparencyFilterHandler: (status: StatusType) => void;
    fieldsTrancparencyFilterHandler: (status: StatusType) => void;
}

interface Props extends StateProps, DispatchProps {}

const FilterPanelBase = ({ 
    actionFilterHandler, 
    fieldsFilterHandler, 
    actionsTransparencyFilterHandler,
    fieldsTrancparencyFilterHandler,
    actionsFilters, 
    fieldsFilters,
    actionsTransparencyFilters,
    fieldsTransparencyFilters
}: Props) => {
    return (
        <div className="header-filter">
            <div className="header-filter__togglers">
                <div className="header-filter__togglers-title">Actions</div>
                {
                    ACTIONS_STATUSES.map((status, index) => (
                        <StatusFilterToggler
                            key={index}
                            status={status}
                            transparencyFilter={actionsTransparencyFilters.has(status)}
                            visibilityFilter={actionsFilters.has(status)}
                            transparencyFilterHandler={() => actionsTransparencyFilterHandler(status)}
                            visibilityFilterHandler={() => actionFilterHandler(status)}/>
                    ))
                }
            </div>
            <div className="header-filter__togglers">
                <div className="header-filter__togglers-title">Fields</div>
                {
                    FIELDS_STATUSES.map((status, index) => (
                        <StatusFilterToggler
                            key={index}
                            status={status}
                            transparencyFilter={fieldsTransparencyFilters.has(status)}
                            visibilityFilter={fieldsFilters.has(status)}
                            transparencyFilterHandler={() => fieldsTrancparencyFilterHandler(status)}
                            visibilityFilterHandler={() => fieldsFilterHandler(status)}/>
                    ))
                }
            </div>
        </div>
    )
}

const FilterPanel = connect(
    ({ filter: state }: AppState): StateProps => ({
        actionsFilters: state.actionsFilter,
        fieldsFilters: state.fieldsFilter,
        actionsTransparencyFilters: state.actionsTransparencyFilter,
        fieldsTransparencyFilters: state.fieldsTransparencyFilter
    }),
    (dispatch): DispatchProps => ({
        actionFilterHandler: status => dispatch(switchActionsFilter(status)),
        fieldsFilterHandler: status => dispatch(switchFieldsFilter(status)),
        actionsTransparencyFilterHandler: status => dispatch(switchActionsTransparencyFilter(status)),
        fieldsTrancparencyFilterHandler: status => dispatch(switchFieldsTransparencyFilter(status))
    })
)(FilterPanelBase);

export default FilterPanel;
