/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import * as React from 'react';
import '../styles/splitter.scss';
import {createStyleSelector} from '../helpers/styleCreators';
import ResizeObserver from "resize-observer-polyfill";
import PanelArea from "../util/PanelArea";

/**
 * Props for splitter component
 */
export interface Props {
    /**
     * Panel for components : first child - for left panel, second child - for right panel, other children will be ignored
     */
    children: [React.ReactNode, React.ReactNode];
    panelArea: PanelArea;
    onPanelAreaChange: (panelArea: PanelArea) => void;
    leftPanelMinWidth?: number;
    rightPanelMinWidth?: number;
}

interface State {
    splitterLeftOffset: number;
    isDragging: boolean;
    steps: number[];
    previewPanelArea: PanelArea;
}

export class SplitView extends React.Component<Props, State> {

    private leftPanel = React.createRef<HTMLDivElement>();
    private root = React.createRef<HTMLDivElement>();
    private splitter = React.createRef<HTMLDivElement>();
    private lastPosition: number = 0;
    private resizeObserver = new ResizeObserver(this.onResize.bind(this));
    private rootWidth: number = 0;

    static defaultProps: Partial<Props> = {
        leftPanelMinWidth: 0,
        rightPanelMinWidth: 0
    };

    constructor(props: Props) {
        super(props);
        this.state = {
            splitterLeftOffset: 0,
            isDragging: false,
            steps: [],
            previewPanelArea: props.panelArea
        };
    }

    componentDidMount() {
        this.resizeObserver.observe(this.root.current)
    }

    componentWillUnmount() {
        this.resizeObserver.unobserve(this.root.current)
    }

    componentDidUpdate(prevProps: Readonly<Props>) {
        if (this.props.leftPanelMinWidth != prevProps.leftPanelMinWidth ||
            this.props.rightPanelMinWidth != prevProps.rightPanelMinWidth) {
            this.setState({
                steps: this.calculateSteps()
            })
        }
    }

    get panelsAvailableWidth() {
        return this.root.current?.offsetWidth - this.splitter.current?.offsetWidth;
    }

    private calculateSteps(): State['steps'] {
        const rootWidth = this.panelsAvailableWidth,
            stepWidth = rootWidth / 4;

        const w25 = stepWidth > this.props.leftPanelMinWidth ?
            stepWidth :
            this.props.leftPanelMinWidth;

        const w50 = rootWidth / 2;

        const w75 = (rootWidth - stepWidth * 3) > this.props.rightPanelMinWidth ?
            stepWidth * 3 :
            (rootWidth - this.props.rightPanelMinWidth);

        return [0, w25, w50, w75, rootWidth];
    }

    private splitterMouseDown = (e: React.MouseEvent) => {
        this.root.current.addEventListener("mousemove", this.onMouseMove);
        this.root.current.addEventListener("mouseleave", this.onMouseUpOrLeave);
        this.root.current.addEventListener("mouseup", this.onMouseUpOrLeave);
        this.lastPosition = e.clientX;

        this.setState({
            splitterLeftOffset: this.leftPanel.current.clientWidth,
            isDragging: true
        })
    };

    private onMouseUpOrLeave = () => {
        this.root.current.removeEventListener("mouseleave", this.onMouseUpOrLeave);
        this.root.current.removeEventListener("mouseup", this.onMouseUpOrLeave);
        this.root.current.removeEventListener("mousemove", this.onMouseMove);

        this.stopDragging();
    };

    private onMouseMove = (e: MouseEvent) => {
        // here we catching situation when the mouse is outside the browser document 
        if (e.clientX > document.documentElement.scrollWidth) {
            this.stopDragging();
        } else {
            this.resetPosition(e.clientX);
        }
    };

    private onResize(elements: ResizeObserverEntry[]) {
        const nextRootWidth = elements[0]?.contentRect.width;

        if (nextRootWidth != this.rootWidth) {
            this.setState({
                steps: this.calculateSteps()
            });
        }

        this.rootWidth = nextRootWidth
    };

    resetPosition(newPosition: number) {
        const diff = newPosition - this.lastPosition;

        this.lastPosition = newPosition;

        const [w0, w25, w50, w75, w100] = this.state.steps;

        const closestStep = this.state.steps
            .map(step => ({
                step,
                diff: Math.abs(step - newPosition)
            }))
            .sort((a, b) => a.diff - b.diff)
            [0]
            .step;

        let previewArea: PanelArea = null;

        switch (closestStep) {
            case w0:
                previewArea = PanelArea.P0;
                break;
            case w25:
                previewArea = PanelArea.P25;
                break;
            case w50:
                previewArea = PanelArea.P50;
                break;
            case w75:
                previewArea = PanelArea.P75;
                break;
            case w100:
                previewArea = PanelArea.P100;
                break;
        }

        this.setState({
            previewPanelArea: previewArea,
            splitterLeftOffset: parseInt(this.splitter.current.style.left) + diff
        });
    }

    private stopDragging() {
        this.props.onPanelAreaChange(this.state.previewPanelArea);

        this.setState({
            isDragging: false,
            splitterLeftOffset: 0
        });
    }

    private getPanelsWidthByArea(area: PanelArea): [number, number] {
        const fullWidth = this.panelsAvailableWidth;

        switch (area) {
            case PanelArea.P0:
                return [0, fullWidth];
            case PanelArea.P25:
                return [this.state.steps[1], fullWidth - this.state.steps[1]];
            case PanelArea.P50:
                return [this.state.steps[2], this.state.steps[2]];
            case PanelArea.P75:
                return [this.state.steps[3], fullWidth - this.state.steps[3]];
            case PanelArea.P100:
                return [this.state.steps[4], this.state.steps[0]];
        }
    }

    render() {
        const { children, panelArea } = this.props,
            { isDragging, splitterLeftOffset, previewPanelArea } = this.state;

        let rootStyle: React.CSSProperties = null,
            previewStyle: React.CSSProperties = null;

        // during first render we can't calculate panel's widths
        if (this.root.current) {
            const [leftWidth, rightWidth] = this.getPanelsWidthByArea(panelArea);
            rootStyle = { gridTemplateColumns: `${leftWidth}px auto ${rightWidth}px` };

            const [leftPreviewWidth, rightPreviewWidth] = this.getPanelsWidthByArea(previewPanelArea);
            previewStyle = { gridTemplateColumns: `${leftPreviewWidth}px auto ${rightPreviewWidth}px` };
        }

        const leftClassName = createStyleSelector(
                "splitter-pane-left",
                isDragging ? "dragging" : null,
                panelArea == PanelArea.P0 ? "hidden" : null
            ),
            rightClassName = createStyleSelector(
                "splitter-pane-right",
                isDragging ? "dragging" : null,
                panelArea == PanelArea.P100 ? "hidden" : null
            ),
            splitterClassName = createStyleSelector(
                "splitter-bar",
                isDragging ? "dragging" : null
            ),
            rootClassName = createStyleSelector(
                "splitter",
                isDragging ? "dragging" : null
            );

        return (
            <div className={rootClassName} ref={this.root}
                 style={rootStyle}>
                {
                    isDragging ? (
                        <div className='splitter-preview'
                            style={previewStyle}>
                            <div className='splitter-preview-left'/>
                            <div className='splitter-preview-right'/>
                        </div>
                    ) : null
                }
                <div className={leftClassName}
                     ref={this.leftPanel}>
                    {children[0]}
                </div>
                <div className={rightClassName}>
                    {children[1]}
                </div>
                <div className={splitterClassName}
                     style={{
                         left: isDragging ? splitterLeftOffset : null
                     }}
                     onMouseDown={this.splitterMouseDown}
                     ref={this.splitter}>
                    <div className="splitter-bar-icon"/>
                </div>
            </div>
        )
    }
} 
