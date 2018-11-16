import { h, render } from "preact";
import { App } from "./components/App-dev";
import 'preact/devtools';

render(<App/>, document.getElementById("index"));