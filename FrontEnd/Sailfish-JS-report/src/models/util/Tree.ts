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

export default interface Tree<T> {
    readonly value: T;
    readonly nodes?: Array<Tree<T>>;
}

export const createNode = <T>(value: T, nodes: Array<Tree<T>> = []): Tree<T> => ({
    value: value,
    nodes: nodes
});

export function mapTree<IN, OUT>(fn: (node: IN) => OUT, tree: Tree<IN>): Tree<OUT> {
    const newValue = fn(tree.value);

    if (tree.nodes.length === 0) {
        return createNode(newValue);
    }

    return createNode(
        newValue,
        tree.nodes.map(node => mapTree(fn, node))
    );
}
