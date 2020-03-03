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
import { createStyleSelector } from '../helpers/styleCreators';
import { isEqual } from '../helpers/object';
import ResizeObserver from "resize-observer-polyfill";
import PanelSide from "../util/PanelSide";

/**
 * Props for splitter component
 */
export interface SplitViewProps {
    /**
     * Panel for components : first child - for left panel, second child - for right panel, other childs will be ignored
     */
    children: [React.ReactNode, React.ReactNode];
    closedPanel: PanelSide | null;
    onClosedPanelChanged: (panel: PanelSide | null) => void;
}

enum PreviewAreaStep {
    p0 = 'w0', p25 = 'w25', p50 = 'w50', p75 = 'w75', p100 = 'w100'
}

class PreviewArea {
    private _leftArea = PreviewAreaStep.p50;
    private _rightArea = PreviewAreaStep.p50;

    get leftArea() {
        return this._leftArea
    }

    get rightArea() {
        return this._rightArea
    }

    set leftArea(step: PreviewAreaStep) {
        this._leftArea = step;

        switch (step) {
            case PreviewAreaStep.p0:
                this._rightArea = PreviewAreaStep.p100;
                break;
            case PreviewAreaStep.p25:
                this._rightArea = PreviewAreaStep.p75;
                break;
            case PreviewAreaStep.p50:
                this._rightArea = PreviewAreaStep.p50;
                break;
            case PreviewAreaStep.p75:
                this._rightArea = PreviewAreaStep.p25;
                break;
            case PreviewAreaStep.p100:
                this._rightArea = PreviewAreaStep.p0;
                break;
        }
    }
}

interface SplitState {
    splitterPosition: number;
    isDragging: boolean;
    closedPanel: PanelSide | null;
    preview: PreviewArea;
    steps: number[];
    closestStep: number;
}

export class SplitView extends React.Component<SplitViewProps, SplitState> {

    private rightPanel = React.createRef<HTMLDivElement>();
    private leftPanel = React.createRef<HTMLDivElement>();
    private leftPreviewArea = React.createRef<HTMLDivElement>();
    private rightPreviewArea = React.createRef<HTMLDivElement>();
    private root = React.createRef<HTMLDivElement>();
    private splitter = React.createRef<HTMLDivElement>();
    private lastPosition: number = 0;
    private resizeObserver: ResizeObserver;
    private rootWidth: number = 0;


    constructor(props: SplitViewProps) {
        super(props);
        this.state = {
            splitterPosition: 0,
            closedPanel: props.closedPanel,
            isDragging: false,
            preview: new PreviewArea(),
            steps: [],
            closestStep: null
        };
    }

    componentDidMount() {
        if (this.root.current) {
            this.resizeObserver = new ResizeObserver(elements => {
                const nextRootWidth = elements[0]?.contentRect.width;

                if (nextRootWidth != this.rootWidth) {
                    this.setState({
                        ...this.state,
                        ...this.calculateSteps(),
                        splitterPosition: this.root.current.offsetWidth / 2,
                    });
                }

                this.rootWidth = nextRootWidth
            })
            this.resizeObserver.observe(this.root.current)
        }
    }

    componentDidUpdate(prevProps: Readonly<SplitViewProps>, prevState: Readonly<SplitState>) {
        if (this.props.closedPanel != prevProps.closedPanel) {
            this.setState({
                closedPanel: this.props.closedPanel
            });

            return;
        }

        if (this.state.closedPanel !== prevState.closedPanel) {
            this.props.onClosedPanelChanged(this.state.closedPanel);
        }
    }

    componentWillUnmount() {
        this.resizeObserver.unobserve(this.root.current)
    }

    private calculateSteps() {
        const w25 = this.root.current.offsetWidth / 4,
            w50 = w25 * 2,
            w75 = w25 * 3,
            w100 = this.root.current.offsetWidth;
        return {
            steps: [0, w25, w50, w75, w100],
            closestStep: w50
        }
    }

    splitterMouseDown = (e: React.MouseEvent) => {
        this.root.current.addEventListener("mousemove", this.onMouseMove);
        this.root.current.addEventListener("mouseleave", this.onMouseUpOrLeave);
        this.root.current.addEventListener("mouseup", this.onMouseUpOrLeave);
        this.lastPosition = e.clientX;
        if (this.state.closedPanel === PanelSide.LEFT) {
            this.splitter.current.style.left = '0px';
        } else {
            this.splitter.current.style.left = this.leftPanel.current.scrollWidth.toString() + 'px';
        }

        this.setState({
            ...this.state,
            isDragging: true
        })
    };

    onMouseUpOrLeave = () => {
        this.root.current.removeEventListener("mouseleave", this.onMouseUpOrLeave);
        this.root.current.removeEventListener("mouseup", this.onMouseUpOrLeave);
        this.root.current.removeEventListener("mousemove", this.onMouseMove);

        this.stopDragging();
    };

    onMouseMove = (e: MouseEvent) => {
        // here we catching situation when the mouse is outside the browser document 
        if (e.clientX > document.documentElement.scrollWidth) {
            this.stopDragging();
        } else {
            this.resetPosition(e.clientX);
        }
    };

    resetPosition(newPosition: number) {
        const diff = newPosition - this.lastPosition;

        this.splitter.current.style.left = (this.getElementStylePropAsNumber(this.splitter.current, 'left') + diff).toString() + 'px';
        this.lastPosition = newPosition;

        const [w0, w25, w50, w75, w100] = this.state.steps,
            preview = new PreviewArea();
        const closestStep = this.state.steps
            .map(step => ({
                step,
                diff: Math.abs(step - newPosition)
            }))
            .sort((a, b) => a.diff - b.diff)[0].step;

        switch (closestStep) {
            case w0:
                preview.leftArea = PreviewAreaStep.p0;
                break;
            case w25:
                preview.leftArea = PreviewAreaStep.p25;
                break;
            case w50:
                preview.leftArea = PreviewAreaStep.p50;
                break;
            case w75:
                preview.leftArea = PreviewAreaStep.p75;
                break;
            case w100:
                preview.leftArea = PreviewAreaStep.p100;
                break;
        }

        if (!isEqual(this.state.preview, preview)) {
            this.setState({
                preview: preview,
                closestStep: closestStep
            })
        }
    }

    stopDragging() {
        const { closestStep, steps } = this.state;

        const leftHideStep = steps[0],
            rightHideStep = steps[steps.length - 1];

        if (closestStep == rightHideStep) {
            this.setState({
                isDragging: false,
                splitterPosition: rightHideStep,
                closedPanel: PanelSide.RIGHT
            });
        } else if (closestStep == leftHideStep) {
            this.setState({
                isDragging: false,
                splitterPosition: leftHideStep,
                closedPanel: PanelSide.LEFT
            });
        } else {
            this.setState({
                isDragging: false,
                splitterPosition: closestStep,
                closedPanel: null
            });
        }

        this.splitter.current.style.left = null;
    }

    render() {
        const { children } = this.props,
            { splitterPosition, isDragging, closedPanel } = this.state;

        let rootStyle: React.CSSProperties = null;

        // during first render we can't calculate panel's widths
        if (this.root.current) {
            let splitterWidth = this.splitter.current.offsetWidth,
                rightWidth = (this.root.current.offsetWidth - splitterPosition) - splitterWidth / 2,
                leftWidth = splitterPosition - splitterWidth / 2;

            if (closedPanel === PanelSide.LEFT) {
                rightWidth += leftWidth;
                leftWidth = 0;
            }

            if (closedPanel === PanelSide.RIGHT) {
                leftWidth += rightWidth;
                rightWidth = 0;
            }

            rootStyle = { gridTemplateColumns: `${leftWidth}px ${splitterWidth}px ${rightWidth}px` };
        }

        const leftClassName = createStyleSelector(
            "splitter-pane-left",
            isDragging ? "dragging" : null,
            closedPanel === PanelSide.LEFT ? "hidden" : null
            ),
            rightClassName = createStyleSelector(
                "splitter-pane-right",
                isDragging ? "dragging" : null,
                closedPanel === PanelSide.RIGHT ? "hidden" : null
            ),
            splitterClassName = createStyleSelector(
                "splitter-bar",
                isDragging ? "dragging" : null
            ),
            rootClassName = createStyleSelector(
                "splitter",
                isDragging ? "dragging" : null
            ),
            leftPreviewAreaClassName = createStyleSelector(
                "splitter-preview-area-left",
                isDragging ? null : "hidden",
                this.state.preview.leftArea
            ),
            rightPreviewAreaClassName = createStyleSelector(
                "splitter-preview-area-right",
                isDragging ? null : "hidden",
                this.state.preview.rightArea
            );

        return (
            <div className={rootClassName} ref={this.root}
                 style={rootStyle}>
                <div className={leftPreviewAreaClassName} ref={this.leftPreviewArea}/>
                <div className={rightPreviewAreaClassName} ref={this.rightPreviewArea}/>
                <div className={leftClassName}
                     ref={this.leftPanel}>
                    {children[0]}
                </div>
                <div className={rightClassName} ref={this.rightPanel}>
                    {children[1]}
                </div>
                <div className={splitterClassName}
                     onMouseDown={this.splitterMouseDown}
                     ref={this.splitter}>
                    <div className="splitter-bar-icon"/>
                </div>
            </div>
        )
    }

    private getElementStylePropAsNumber(element: HTMLElement, stylePropName: string): number {
        if (!element.style[stylePropName]) {
            return 0;
        }

        const strProp: string = element.style[stylePropName].toString();
        // removing px
        return Number(strProp.substring(0, strProp.length - 2));
    }
} 
