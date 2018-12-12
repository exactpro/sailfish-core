import { h, Component } from 'preact';
import '../styles/splitter.scss';

interface SplitState {
    rightPaneWidth: number;
}

export class SplitView extends Component<{}, SplitState> {

    private rightPane : HTMLElement;
    private leftPane : HTMLElement;
    private root : HTMLElement;
    private lastPosition : number;

    constructor(props) {
        super(props);
        this.state = {
            rightPaneWidth: 0
        };
        this.onMouseMove = this.onMouseMove.bind(this)
    }

    componentDidMount() {
        if (this.root) {
            this.setState({
                rightPaneWidth: this.root.offsetWidth / 2 - 10
            });
        }
    }

    resetPosition(newPosition: number) {
        const diff = newPosition - this.lastPosition;
        this.setState({
            rightPaneWidth: this.state.rightPaneWidth - diff
        });
        this.lastPosition = newPosition;
    }

    splitterMouseDown(e: MouseEvent) {
        window.addEventListener("mousemove", this.onMouseMove);
        this.lastPosition = e.clientX;
    }

    splitterMouseUp(e: MouseEvent) {
        window.removeEventListener("mousemove", this.onMouseMove);
    }

    onMouseMove(e: MouseEvent) {
        this.resetPosition(e.clientX);
    }

    render(props, {rightPaneWidth} : SplitState) {
        const percentRightWidth = (this.root ? (rightPaneWidth - 10) / this.root.offsetWidth * 100 : 50); 
        const percentLeftWidth = (this.root ? (this.root.offsetWidth - rightPaneWidth - 10) / this.root.offsetWidth * 100 : 50); 

        return (
            <div id="splitter-root" ref={ref => this.root = ref}
                style={{gridTemplateColumns: `${percentLeftWidth}% 20px ${percentRightWidth}%`}}>
                <div id="left-pane" 
                    ref={ref => this.leftPane = ref}>
                    {props.children[0]}
                </div>
                <div class="splitter-bar" onMouseDown={(e) => this.splitterMouseDown(e)}
                    onMouseUp={e => this.splitterMouseUp(e)}>
                    <div class="splitter-bar-icon"/>
                </div>
                <div id="right-pane" ref={pane => this.rightPane = pane}
                    >
                    {props.children[1]}
                </div>
            </div>
        )
    }
} 
