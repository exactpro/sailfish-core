import { h, render } from "preact";
import { App } from "./components/App";
import { Provider } from 'preact-redux';
import { createAppStore } from './store/store';

render(
    <Provider store={createAppStore(null)}>
        <App/>
    </Provider>, 
    document.getElementById("index"));