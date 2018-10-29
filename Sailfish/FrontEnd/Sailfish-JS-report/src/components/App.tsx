import { h, Component } from "preact";
import "../styles/styles.scss";

export class App extends Component<{}, {}> {
    render(props: {}, state: {}) {
        return(
            <div class="root">
                <p>
                    Sailfish the best!!!
                </p>
            </div>
        );
    };
}