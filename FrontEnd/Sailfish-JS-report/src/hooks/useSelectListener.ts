/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
 *  limitations under the License.
 ******************************************************************************/

import * as React from 'react';

export default function useSelectListener(ref: React.MutableRefObject<HTMLElement>): [number | null, number | null] {
    const [startOffset, setStartOffset] = React.useState<number | null>(null);
    const [endOffset, setEndOffset] = React.useState<number | null>(null);

    const onSelectionChanged = () => {
        const selection = window.getSelection();

        const refIsAnchorNode = ref.current.contains(selection.anchorNode),
            refIsFocusNode = ref.current.contains(selection.focusNode);

        if (selection.rangeCount == 0 || (!refIsAnchorNode && !refIsFocusNode)) {
            setStartOffset(null);
            setEndOffset(null);
            return;
        }

        const range = selection.getRangeAt(0);

        if (selection.focusNode == selection.anchorNode) {
            setStartOffset(range.startOffset);
            setEndOffset(range.endOffset);
            return;
        }

        if (refIsAnchorNode || refIsFocusNode) {
            setStartOffset(range.startOffset);
            setEndOffset(ref.current.textContent.length);
            return;
        }

        setStartOffset(null);
        setEndOffset(null);
    };

    React.useEffect(() => {
        document.addEventListener("selectionchange", onSelectionChanged);

        return () => {
            document.removeEventListener("selectionchange", onSelectionChanged);
        }
    }, []);

    return [startOffset, endOffset];
}
