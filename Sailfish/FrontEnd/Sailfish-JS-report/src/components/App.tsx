import { h, Component } from "preact";
import "../styles/styles.scss";
import { testAction }  from "../test/testAction"
import ActionCard from "./ActionCard";
import BaseLayout from "./BaseLayout";

export class App extends Component<{}, {}> {
    render(props: {}, state: {}) {
        return(
            <div class="root">
                <BaseLayout/>
            </div>
        );
    };
}