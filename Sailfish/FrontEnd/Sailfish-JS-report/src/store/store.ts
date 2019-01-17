import { appReducer } from '../reducers/reducers';
import { createStore } from 'redux';
import { devToolsEnhancer } from 'redux-devtools-extension';
import { initialAppState } from '../state/initialStates';
import Report from '../models/Report';

export const createAppStore = (report: Report) => createStore(
    appReducer,
    {
        ...initialAppState,
        report: report
    } as any,
    devToolsEnhancer({name: 'redux'})
)
