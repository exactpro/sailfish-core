import { testCaseReducer } from '../reducers/testCaseReducers';
import { createStore } from 'redux';
import { initialTestCaseState } from '../state/initialStates';

export const appStore = createStore(testCaseReducer, initialTestCaseState as any)
