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

/**
 * Props for splitter component
 */
export interface SplitViewProps {
    /**
     * Min width for both panels 
     */
    minPanelWidth: number;
    
    /**
     * (optional) Resize event handler, recieves left and right widths in px.
     */
    resizeHandler?: (leftPanelWidth: number, rightPanelWidth: number) => any; 

    /**
     * Panel for compoentns : first child - for left panel, second child - for right panel, other childs will be ignored
     */
    children: React.ReactNodeArray;
}

interface SplitState {
    leftPanelWidth: number;
    isDragging: boolean;
}

export class SplitView extends React.Component<SplitViewProps, SplitState> {

    private rightPanel  = React.createRef<HTMLDivElement>();
    private leftPanel  = React.createRef<HTMLDivElement>();
    private root = React.createRef<HTMLDivElement>();
    private splitter = React.createRef<HTMLDivElement>();
    private lastPosition : number = 0;

    constructor(props) {
        super(props);
        this.state = {
            leftPanelWidth: 0,
            isDragging: false
        };
    }

    componentDidMount() {
        if (this.root.current) {
            this.setState({
                ...this.state,
                leftPanelWidth: this.root.current.offsetWidth / 2
            });
        }
    }

    componentDidUpdate(prevProps: SplitViewProps, prevState: SplitState) {
        if (this.state.leftPanelWidth != prevState.leftPanelWidth && this.props.resizeHandler) {
            this.props.resizeHandler(this.leftPanel.current.offsetWidth, this.rightPanel.current.offsetWidth);
        }
    }
    
    splitterMouseDown(e: React.MouseEvent) {
        this.root.current.addEventListener("mousemove", this.onMouseMove);
        this.root.current.addEventListener("mouseleave", this.onMouseUpOrLeave)
        this.root.current.addEventListener("mouseup", this.onMouseUpOrLeave)
        this.lastPosition = e.clientX;
        this.splitter.current.style.left = this.leftPanel.current.scrollWidth.toString() + 'px';

        this.setState({
            ...this.state, 
            isDragging: true
        }) 
    }

    onMouseUpOrLeave = (e: MouseEvent) => {
        this.root.current.removeEventListener("mouseleave", this.onMouseUpOrLeave);
        this.root.current.removeEventListener("mouseup", this.onMouseUpOrLeave);
        this.root.current.removeEventListener("mousemove", this.onMouseMove);

        this.stopDragging();
    }
    
    onMouseMove = (e: MouseEvent) => {
        // here we catching situation when the mouse is outside the browser document 
        if (e.clientX > document.documentElement.scrollWidth) {
            this.stopDragging();
        } else {
            this.resetPosition(e.clientX);
        }
    }
    
    resetPosition(newPosition: number) {
        const diff = newPosition - this.lastPosition;
        this.splitter.current.style.left = (this.getElementStylePropAsNumber(this.splitter.current, 'left') + diff).toString() + 'px';
        this.lastPosition = newPosition;
    }

    stopDragging() {
        this.setState({
            ...this.state,
            isDragging: false,
            leftPanelWidth: this.getElementStylePropAsNumber(this.splitter.current, 'left')
        });

        this.splitter.current.style.left = null;
    }

    render() {
        const { children, minPanelWidth } = this.props,
            { leftPanelWidth, isDragging } = this.state;

        let rootStyle: React.CSSProperties = null;

        // during first render we can't calculate panel's widths
        if (this.root.current) {
            let splitterWidth = this.splitter.current.offsetWidth,
                rightWidth = (this.root.current.offsetWidth - leftPanelWidth) - splitterWidth / 2,
                leftWidth = leftPanelWidth -  splitterWidth / 2;

            if (rightWidth < minPanelWidth) {
                rightWidth = minPanelWidth;
                leftWidth = this.root.current.offsetWidth - rightWidth - splitterWidth;
            } else if (leftWidth < minPanelWidth) {
                leftWidth = minPanelWidth;
                rightWidth = this.root.current.offsetWidth - leftWidth - splitterWidth;
            }

            rootStyle = { gridTemplateColumns: `${leftWidth}px ${splitterWidth}px ${rightWidth}px` };
        }

        const leftClassName = createStyleSelector("splitter-pane-left", isDragging ? "dragging" : null),
              rightClassName = createStyleSelector("splitter-pane-right", isDragging ? "dragging" : null),
              splitterClassName = createStyleSelector("splitter-bar", isDragging ? "dragging" : null),
              rootClassName = createStyleSelector("splitter", isDragging ? "dragging" : null);

        return (
            <div className={rootClassName} ref={this.root}
                style={rootStyle}>
                <div className={leftClassName} 
                    ref={this.leftPanel}>
                    {children[0]}
                </div>
                <div className={rightClassName} ref={this.rightPanel}>
                    {children[1]}
                </div>
                <div className={splitterClassName} onMouseDown={(e) => this.splitterMouseDown(e)}
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

        const strProp : string = element.style[stylePropName].toString();
        // removing px
        return Number(strProp.substring(0, strProp.length - 2));
    }
} 
