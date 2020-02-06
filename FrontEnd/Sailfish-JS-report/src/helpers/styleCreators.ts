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

/**
 * This function can be used to create css selector with the list of modifiers.
 * @param className Name of css selector
 * @param modifiers List of css modifiers
 */
export function createStyleSelector(className: string, ...modifiers: string[]): string {
    // we filter out empty modifiers to create selector without insignificant spaces

    return className + joinModifiers(modifiers);
}

export function createTriStateControlClassName(baseName: string, isActive: boolean, isEnabled?: boolean) {
    return createStyleSelector(baseName, (isEnabled != null && !isEnabled) ? "disabled" : ( isActive ? "active" : "inactive" ))
}

export function joinModifiers(modifiers: string[]): string {
    return modifiers
        .filter(Boolean)
        .map(modifier => ' ' + modifier.toLowerCase())
        .join('');
}

export function createBemBlock(blockName: string, ...modifiers: string[]): string {
    return blockName + joinModifiers(modifiers);
} 

export function createBemElement(blockName: string, elementName: string, ...modifiers: string[]): string {
    return `${blockName}__${elementName}${joinModifiers(modifiers)}`;
}
