import { h, Component } from "preact";
import "../styles/styles.scss";
import { testAction }  from "../test/testAction"
import ActionCard from "./ActionCard";

export class App extends Component<{}, {}> {
    render(props: {}, state: {}) {
        return(
            <div class="root">
                <ActionCard action={testAction}/>
            </div>
        );
    };
}