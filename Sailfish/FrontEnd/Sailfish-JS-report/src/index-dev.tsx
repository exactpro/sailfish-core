import { h, render } from "preact";
import { App } from "./components/App-dev";
// enable react-devtools compatibility, APP WORKNIG SLOW WITH THIS
//import 'preact/devtools';

render(<App/>, document.getElementById("index"));