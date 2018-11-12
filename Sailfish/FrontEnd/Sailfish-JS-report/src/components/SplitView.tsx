import { h, Component, Ref } from 'preact';
import '../styles/splitter.scss';

interface SplitState {
    rightPaneWidth: number;
}

export class SplitView extends Component<{}, SplitState> {

    private rightPane : any;
    private lastPosition : number;

    constructor(props) {
        super(props);
        this.state = {
            rightPaneWidth: 0
        };
        this.onMouseMove = this.onMouseMove.bind(this)
    }

    componentWillMount() {
        this.setState({
            rightPaneWidth: window.innerWidth / 2
        });
    }

    resetPosition(newPosition: number) {
        this.setState({
            rightPaneWidth: this.state.rightPaneWidth + this.lastPosition - newPosition
        });
        this.lastPosition = newPosition;
    }

    splitterMouseDown(e: MouseEvent) {
        console.log("down")
        window.addEventListener("mousemove", this.onMouseMove);
        this.lastPosition = e.clientX
    }

    splitterMouseUp(e: MouseEvent) {
        console.log("up")
        window.removeEventListener("mousemove", this.onMouseMove);
    }

    onMouseMove(e: MouseEvent) {
        this.resetPosition(e.clientX);
    }

    render(props, {rightPaneWidth} : SplitState) {
        return (
            <div id="splitter-root">
                <div id="left-pane">
                    {props.children[0]}
                </div>
                <div class="splitter-bar" onMouseDown={(e) => this.splitterMouseDown(e)}
                    onMouseUp={e => this.splitterMouseUp(e)}/>
                <div id="right-pane" ref={pane => this.rightPane = pane}
                    style={{width: rightPaneWidth}}>
                    {props.children[1]}
                </div>
            </div>
        )
    }
} 

export const SplitViewFun = (props) => {
    const initWidth = window.innerWidth / 2;
    console.log(initWidth)
    return (
        <div id="splitter-root">
            <div id="left-pane">
                {props.children[0]}
            </div>
            <div class="splitter-bar" onClick={() => {
                console.log(this.rightPane)
                console.log(this.rightPane.style);
                this.rightPane.style.width = "100px";
            }}/>
            <div id="right-pane" ref={pane => this.rightPane = pane}
                style={{width: initWidth}}>
                {props.children[1]}
            </div>
        </div>
    )
}
