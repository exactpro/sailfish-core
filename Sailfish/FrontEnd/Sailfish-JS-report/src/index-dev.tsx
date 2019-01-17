import { h, render } from "preact";
import { App } from "./components/App-dev";
import { Provider } from 'preact-redux';
import { createAppStore } from './store/store';
import { testReport } from './test/testReport';
// enable react-devtools compatibility, APP WORKNIG SLOW WITH THIS
//import 'preact/devtools';

render(
    <Provider store={createAppStore(testReport)}>
        <App/>
    </Provider>, 
    document.getElementById("index"));