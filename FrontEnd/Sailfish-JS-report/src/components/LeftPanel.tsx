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
import { connect } from 'react-redux';
import { ThunkDispatch } from 'redux-thunk';
import '../styles/layout.scss';
import Panel from '../util/Panel';
import { ToggleButton } from './ToggleButton';
import { ActionsList, ActionsListBase } from './action/ActionsList';
import AppState from '../state/models/AppState';
import { setLeftPane } from '../actions/actionCreators';
import { StatusPanel } from './StatusPanel';
import { createStyleSelector } from '../helpers/styleCreators';
import ActionPanelControl from "./action/ActionPanelControls";

type LeftPanelType = Panel.ACTIONS | Panel.STATUS;

interface StateProps {
    panel: Panel;
    statusEnabled: boolean;
    actionsEnabled: boolean;
}

interface DispatchProps {
    panelSelectHandler: (panel: LeftPanelType) => void;
}

interface Props extends StateProps, DispatchProps {
}

class LeftPanelBase extends React.Component<Props> {

    private actionPanel = React.createRef<ActionsListBase>();

    scrollPanelToTop(panel: Panel) {
        switch (panel) {
            case Panel.ACTIONS: {
                this.actionPanel.current?.scrollToTop();
                break;
            }
            default: {
                return;
            }
        }
    }

    render() {
        const { panel, statusEnabled, actionsEnabled } = this.props;

        const actionRootClass = createStyleSelector(
                "layout-panel__content-wrapper",
                panel == Panel.ACTIONS && actionsEnabled ? null : "disabled"
            ),
            statusRootClass = createStyleSelector(
                "layout-panel__content-wrapper",
                panel == Panel.STATUS && statusEnabled ? null : "disabled"
            );

        return (
            <div className="layout-panel">
                <div className="layout-panel__controls">
                    <div className="layout-panel__tabs">
                        <ToggleButton
                            isToggled={panel == Panel.ACTIONS}
                            isDisabled={!actionsEnabled}
                            onClick={() => actionsEnabled && this.selectPanel(Panel.ACTIONS)}>
                            Actions
                        </ToggleButton>
                        <ToggleButton
                            isToggled={panel == Panel.STATUS}
                            isDisabled={!statusEnabled}
                            onClick={() => statusEnabled && this.selectPanel(Panel.STATUS)}>
                            Status
                        </ToggleButton>
                    </div>
                    {this.getCurrentPanelControls()}
                </div>
                <div className="layout-panel__content">
                    <div className={actionRootClass}>
                        <ActionsList
                            ref={this.actionPanel}/>
                    </div>
                    <div className={statusRootClass}>
                        <StatusPanel/>
                    </div>
                </div>
            </div>
        )
    }

    private getCurrentPanelControls = () => {
        switch (this.props.panel) {
            case Panel.ACTIONS: {
                return <ActionPanelControl/>
            }

            default: {
                return null;
            }
        }
    };

    private selectPanel(panel: LeftPanelType) {
        if (panel == this.props.panel) {
            this.scrollPanelToTop(panel);
        } else {
            this.props.panelSelectHandler(panel);
        }
    }
}

export const LeftPanel = connect(
    (state: AppState): StateProps => ({
        statusEnabled: state.selected.testCase.status.cause != null,
        actionsEnabled: state.selected.testCase.files.action.count > 0,
        panel: state.view.leftPanel
    }),
    (dispatch): DispatchProps => ({
        panelSelectHandler: (panel: LeftPanelType) => dispatch(setLeftPane(panel))
    })
)(LeftPanelBase);
