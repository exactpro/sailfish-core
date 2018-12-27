/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
import React, {PropTypes, Component} from 'react';
import ReactDOM from 'react-dom';
import {ButtonGroup, Button, Glyphicon, Form, FormControl} from 'react-bootstrap';

import TheAppState from 'state/TheAppState.js';
import TheChannel from 'actions/TheChannel.js';
import TheDictManager from 'actions/TheDictManager.js';
import TheHelper from 'actions/TheHelper.js';
import TheHistory from 'actions/TheHistory.js';
import TheRender from 'actions/TheRender.js';
import TheScroller from 'actions/TheScroller.js';
import TheServer from 'actions/TheServer.js';
import TheStoreManager from 'actions/TheStoreManager.js';
import {StoreCfg} from 'actions/__models__.js';


import SearchBox from 'view/controls/SearchBox.js';
import {assignNodeKeys} from 'utils.js';
import * as consts from 'consts.js';

export class EditorToolbar extends Component {
    static displayName = 'EditorToolbar';

    static propTypes = {
        activePanelId: PropTypes.string.isRequired
    };

    constructor(props, context) {
        super(props, context);

        this.handleSave = this.handleSave.bind(this);
        this.handleUndo = this.handleUndo.bind(this);
        this.handleRedo = this.handleRedo.bind(this);
        this.handleContIfFailed = this.handleContIfFailed.bind(this);
        this.handleAutoStart = this.handleAutoStart.bind(this);
        this.handleIgnoreAskForContinue = this.handleIgnoreAskForContinue.bind(this);
        this.handleAllMessages = this.handleAllMessages.bind(this);
        this.handleRun = this.handleRun.bind(this);
        this.handleEncoding = this.handleEncoding.bind(this);
        this.handleEnvironment = this.handleEnvironment.bind(this);
        this.handleHistoryBlock = this.handleHistoryBlock.bind(this);
        this.handleHideScriptResults = this.handleHideScriptResults.bind(this);
        this.handleReloadServerData = this.handleReloadServerData.bind(this);

        this.state = this.__getState(props);
        this.__loadState();
    }

    __getState(props) {
        return {
            'blockUndo': false,
            'blockRedo': false,
            'contIfFailed': false,
            'autoStart': false,
            'ignoreAscForCont': false,
            'allMessages': false
        };
    }

    __loadState() {
        TheServer.configuration((err) => {
            TheHelper.error("Can not load state from server", err.rootCause);
        }, (res) => {
            // TheAppState.select('editor', 'encoding').set(res.encoding);
            // TheAppState.select('editor', 'environment').set(res.environment);
            this.setState({
                'blockUndo': this.state.blockUndo,
                'blockRedo': this.state.blockRedo,
                'contIfFailed': res.continueIfFailed,
                'autoStart': res.autoStart,
                'ignoreAscForCont': res.ignoreAskForContinue,
                'allMessages': res.showAllMessages
            });
        })
    }

    handleSave() {
        TheServer.save(
            TheHelper.addError.bind(TheHelper),
            TheHelper.success.bind(TheHelper, "Saved", ""));
    }

    handleUndo() {
        TheHistory.undo().then(() => {
            TheScroller.updateByStoreId(this.context.storeId);
        }).catch(TheHelper.addError.bind(TheHelper));
    }

    handleRedo() {
        TheHistory.redo().then(() => {
            TheScroller.updateByStoreId(this.context.storeId); // force scroll to update visible nodes
        }).catch(TheHelper.addError.bind(TheHelper));
    }

    handleContIfFailed() {
        this.setState({
            'contIfFailed': !this.state.contIfFailed
        }, this.__sendConfigToServer.bind(this));
    }

    handleAutoStart() {
        this.setState({
            'autoStart': !this.state.autoStart
        }, this.__sendConfigToServer.bind(this));
    }

    handleIgnoreAskForContinue() {
        this.setState({
            'ignoreAskForContinue': !this.state.ignoreAskForContinue
        }, this.__sendConfigToServer.bind(this));
    }

    handleAllMessages() {
        this.setState({
            'allMessages': !this.state.allMessages
        }, () => {
            this.__sendConfigToServer();
            this.loadUnexpected(true)
        });
    }

    handleRun() {
        const me = this;

        TheServer.save(
            TheHelper.addError.bind(TheHelper),
            () => {
                TheServer.execute({
                    'range': ReactDOM.findDOMNode(this.refs.range).value,
                    'continueOnFailed': this.state.contIfFailed,
                    'autoStart': this.state.autoStart,
                    'autoRun': true,
                    'ignoreAskForContinue': this.state.ignoreAskForContinue,
                    'environment': ReactDOM.findDOMNode(this.refs.environment).value,
                    'encoding': ReactDOM.findDOMNode(this.refs.encoding).value
                }, (resp) => {
                    return TheHelper.error('Failed to execute matrix', resp);
                }, (resp) => {
                    var execId = resp;
                    if (execId === -1) {
                        return TheHelper.error('Matrix execution failed', '');
                    }

                    TheAppState.select('execution', 'id').set(execId);

                    TheChannel.setListenExecutionId(execId);
                    TheChannel.setListener((resp) => {
                        if (resp.status !== 'EXECUTED') {
                            return;
                        }
                        me.loadUnexpected(false);
                    }); // setListener
                }); // execute
            });
    }

    render() {
        const environments = TheRender.getEnvironments();
        const environmentPairs = Object.keys(environments).sort().map(x => [x, environments[x]]);
        const hasScriptResults = TheAppState.select('execution', 'id').get() !== -1;

        return (<Form inline className='exa-toolbar'>
            <ButtonGroup>
                <Button onClick={this.handleSave} >
                    <Glyphicon glyph='save' /> Save
                </Button>
            </ButtonGroup>
            <ButtonGroup>
                <Button onClick={this.handleUndo} disabled={this.props.blockHistory}>
                    <Glyphicon glyph='step-backward' /> undo
                </Button>
                <Button onClick={this.handleRedo} disabled={this.props.blockHistory}>
                    <Glyphicon glyph='step-forward' /> redo
                </Button>
            </ButtonGroup>
            <ButtonGroup>
                <Button onClick={this.handleContIfFailed} active={this.state.contIfFailed} title='Continue if failed'>
                    <Glyphicon glyph='hand-right' />
                </Button>
                <Button onClick={this.handleAutoStart} active={this.state.autoStart} title='Auto start' >
                    <Glyphicon glyph='play-circle' />
                </Button>
                <Button onClick={this.handleIgnoreAskForContinue} active={this.state.ignoreAskForContinue} title='Ignore ask for continue' >
                    <Glyphicon glyph='pause' />
                </Button>
                <Button onClick={this.handleAllMessages} active={this.state.allMessages} title='Show all/unexpected messages' >
                    <Glyphicon glyph='transfer' />
                </Button>
                <Button onClick={this.handleRun}>
                    <Glyphicon glyph='play' />
                     Run
                </Button>
            </ButtonGroup>
            {hasScriptResults ?(
                <ButtonGroup>
                    <Button id='hide-script-result' title='Hide script result' onClick={this.handleHideScriptResults}>
                        <Glyphicon glyph='remove' /> Hide script result
                    </Button>
                </ButtonGroup>
                ) : null
            }
            <ButtonGroup>
                <Button onClick={this.handleReloadServerData} title="Refresh server data" >
                    <Glyphicon glyph='refresh' />
                </Button>
            </ButtonGroup>

            <FormControl ref='range' type='text' placeholder='Range...' title='Range' className='form-inline exa-form-inline'/>

            <FormControl ref='encoding' componentClass="select" label='Encoding: ' value={TheAppState.select('editor', 'encoding').get()} onChange={this.handleEncoding} className='form-inline exa-form-inline'>
                {TheRender.getEncodings().map((value) =>
                    <option value={value} key={value}>
                        {value}
                    </option>
                )}
            </FormControl>

            <FormControl ref='environment' componentClass="select" label='Environment: ' value={TheAppState.select('editor', 'environment').get()} onChange={this.handleEnvironment} className='form-inline exa-form-inline'>
                {environmentPairs.map(x =>
                    <option value={x[0]} key={x[0]}>
                        {x[1]}
                    </option>
                )}
            </FormControl>

            <SearchBox activePanelId={this.props.activePanelId}/>
        </Form>);
    }

    handleEncoding() {
        this.__sendConfigToServer();
        TheAppState.select('editor', 'encoding').set(
            ReactDOM.findDOMNode(this.refs.encoding).value
        );
    }

    handleEnvironment() {
        let env = ReactDOM.findDOMNode(this.refs.environment).value;
        this.__sendConfigToServer();
        TheAppState.select('editor', 'environment').set(env);
        TheDictManager.loadServicesInfo(env);
    }

    __sendConfigToServer() {
        TheServer.configure({
            encoding : ReactDOM.findDOMNode(this.refs.encoding).value,
            environment :  ReactDOM.findDOMNode(this.refs.environment).value,
            continueIfFailed: this.state.contIfFailed,
            autoStart: this.state.autoStart,
            ignoreAskForContinue: this.state.ignoreAskForContinue,
            showAllMessages: this.state.allMessages
        });
    }

    handleHistoryBlock(e) {
        let data = e.target.get();
        this.setState({
            'blockHistory': data.blocked
        });
    }

    handleHideScriptResults() {
        // from main.js
        window.toggleDetails('scriptRunResult');
    }

    handleReloadServerData() {
        let env = ReactDOM.findDOMNode(this.refs.environment).value;
        TheDictManager.loadServicesInfo(env, (err, servicesList) => {
            if (!err)
                TheDictManager.loadLanguage((err, language) => {
                if (!err) {
                    TheDictManager.reloadActionsDefinitions();
                    TheDictManager.loadDictionariesList((err, list) => {
                        TheDictManager.getDictNames().forEach (dictname => {
                            TheDictManager.load(dictname, true);
                        });
                    });
                }
                });
        });
    }

    componentWillMount() {
        TheAppState.select('history').on('update', this.handleHistoryBlock);
        TheAppState.select('execution').on('update', this.forceUpdate.bind(this, undefined /* callback */));
        TheAppState.select('editor').on('update', this.forceUpdate.bind(this, undefined /* callback */));
    }

    componentWillUnmount() {
        TheAppState.select('history').off('update', this.handleHistoryBlock);
        TheAppState.select('execution').off('update', this.forceUpdate.bind(this, undefined /* callback */));
        TheAppState.select('editor').off('update', this.forceUpdate.bind(this, undefined /* callback */));
    }

    componentDidMount() {
        this.loadUnexpected(true);
    }

    loadUnexpected(silent) {
        var me = this;
        // load unexpected
        TheServer.getExecutionResult(
            !this.state.allMessages,
            (resp) => {
                if (silent) {
                    return;
                }
                return TheHelper.error('Failed to get matrix execution status', resp);
            }, (resp) => {
                if (!resp.messageContainer) {
                    return; // No execution... Nothing to load
                }

                const data = TheHelper.removeEnvironmentFromService(assignNodeKeys(resp.messageContainer.data));

                let cfg = new StoreCfg({
                    storeId: consts.SCRIPT_RUN_STORE_NAME,
                    filename: '[N/A]',
                    title: 'Script execution #' + resp.id,
                    readonly: true,
                    date: undefined,
                    amlVersion: 3,
                    loaded: true,
                    bookmarks: []
                });

                TheStoreManager.addStore(cfg, data);
            }
        ); // getExecutionResult
    }
}

export default EditorToolbar;
