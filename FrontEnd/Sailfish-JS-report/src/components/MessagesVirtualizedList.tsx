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

import { h, Component } from 'preact';
import { VirtualizedList } from './VirtualizedList';
import { CellMeasurerCache } from 'react-virtualized';

interface MessagesVirtualizedListProps {
    messagesCount: number;
    messageRenderer: (index: number, isExpanded: boolean, expandHandler: (isExpanded: boolean) => any) => JSX.Element;
}

export class MessagesVirtualizedList extends Component<MessagesVirtualizedListProps> {

    private expanedeStates : boolean[] = [];
    private list : VirtualizedList

    render({messagesCount}: MessagesVirtualizedListProps) {
        return (
            <VirtualizedList
                rowCount={messagesCount}
                elementRenderer={this.rowRenderer}
                ref={ref => this.list = ref}
            />
        )
    }

    private rowRenderer = (index: number): JSX.Element => {
        const isExpanded = this.expanedeStates[index];

        return this.props.messageRenderer(
            index,
            isExpanded,
            (newIsExpanded: boolean) => this.expandHandler(newIsExpanded, index)
        )
    }

    private expandHandler(isExpanded: boolean, elementIndex: number) {
        this.expanedeStates[elementIndex] = isExpanded;
        this.list.measurerCache.clear(elementIndex);
        this.list.forceUpdateList();
    }
}

