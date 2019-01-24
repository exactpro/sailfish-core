import { h, Component } from 'preact';
import '../styles/splitter.scss';

const SPLITTER_WIDTH = 25;

interface SplitViewProps {
    minPanelPercentageWidth: number;
    children: JSX.Element[];
}

interface SplitState {
    leftPanelWidth: number;
    isDragging: boolean;
}

export class SplitView extends Component<SplitViewProps, SplitState> {

    private rightPanel : HTMLElement;
    private leftPanel : HTMLElement;
    private root : HTMLElement;
    private splitter : HTMLElement;
    private lastPosition : number;

    constructor(props) {
        super(props);
        this.state = {
            leftPanelWidth: 0,
            isDragging: false
        };
        this.onMouseMove = this.onMouseMove.bind(this)
    }

    componentDidMount() {
        if (this.root) {
            this.setState({
                ...this.state,
                leftPanelWidth: this.root.offsetWidth / 2
            });
        }
    }
    
    splitterMouseDown(e: MouseEvent) {
        document.addEventListener("mousemove", this.onMouseMove);
        this.lastPosition = e.clientX;
        this.splitter.style.left = this.leftPanel.scrollWidth.toString() + 'px';

        this.setState({
            ...this.state, 
            isDragging: true
        }) 
    }

    splitterMouseUp(e: MouseEvent) {
        this.stopDragging();
    }
    
    onMouseMove(e: MouseEvent) {
        // here we catching situation when the mouse is outside the browser window 
        if (e.clientX > document.documentElement.scrollWidth) {
            this.stopDragging();
        } else {
            this.resetPosition(e.clientX);
        }
    }
    
    resetPosition(newPosition: number) {
        const diff = newPosition - this.lastPosition;
        this.splitter.style.left = (this.getElementStylePropAsNumber(this.splitter, 'left') + diff).toString() + 'px';
        this.lastPosition = newPosition;
    }

    stopDragging() {
        document.removeEventListener("mousemove", this.onMouseMove);

        this.setState({
            ...this.state,
            isDragging: false,
            leftPanelWidth: this.getElementStylePropAsNumber(this.splitter, 'left')
        });

        this.splitter.style.left = null;
    }

    render({children, minPanelPercentageWidth}: SplitViewProps, {leftPanelWidth, isDragging} : SplitState) {
        // codition with root - first render
        const splitterPercentageWidth = (this.root ? SPLITTER_WIDTH / this.root.offsetWidth * 100 : 50);

        let percentageRightWidth = (this.root ? 
            (this.root.offsetWidth - leftPanelWidth) / this.root.offsetWidth * 100 -  splitterPercentageWidth / 2 
            : 50); 
        let percentageLeftWidth = (this.root ? 
            leftPanelWidth / this.root.offsetWidth * 100 - splitterPercentageWidth / 2
            : 50); 

        if (percentageRightWidth < minPanelPercentageWidth) {
            percentageRightWidth = minPanelPercentageWidth - splitterPercentageWidth;
            percentageLeftWidth = 100 - minPanelPercentageWidth - splitterPercentageWidth;
        } else if (percentageLeftWidth < minPanelPercentageWidth) {
            percentageLeftWidth = minPanelPercentageWidth - splitterPercentageWidth;
            percentageRightWidth = 100 - minPanelPercentageWidth - splitterPercentageWidth;
        }

        const leftClassName = ["splitter-pane-left", (isDragging ? "dragging" : "")].join(' '),
              rightClassName = ["splitter-pane-right", (isDragging ? "dragging" : "")].join(' '),
              splitterClassName = ["splitter-bar", (isDragging ? "dragging" : "")].join(' '),
              rootClassName = ["splitter", (isDragging ? "dragging" : "")].join(' ');

        return (
            <div class={rootClassName} ref={ref => this.root = ref}
                style={{gridTemplateColumns: `${percentageLeftWidth}% ${SPLITTER_WIDTH}px ${percentageRightWidth}%`}}>
                <div class={leftClassName} 
                    ref={ref => this.leftPanel = ref}>
                    {children[0]}
                </div>
                <div class={rightClassName} ref={pane => this.rightPanel = pane}>
                    {children[1]}
                </div>
                <div class={splitterClassName} onMouseDown={(e) => this.splitterMouseDown(e)}
                    onMouseUp={e => this.splitterMouseUp(e)}
                    ref={ref => this.splitter = ref}>
                    <div class="splitter-bar-icon"/>
                </div>
            </div>
        )
    }

    getElementStylePropAsNumber(element: HTMLElement, stylePropName: string): number {
        if (!element.style[stylePropName]) {
            return 0;
        }

        const strProp : string = element.style[stylePropName].toString();
        // removing px
        return Number(strProp.substring(0, strProp.length - 2));
    }
} 
