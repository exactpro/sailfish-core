/******************************************************************************
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
 * limitations under the License.
 ******************************************************************************/

export function downloadTxtFile(content: BlobPart[], filename: string): void {
	const anchorElement = document.createElement('a');
	const file = new Blob(
		content,
		{type: "text/plain;charset=utf-8"}
	);
	const url = URL.createObjectURL(file)
	anchorElement.href = url;
	anchorElement.download = filename;
	anchorElement.click();
	setTimeout(() => {
		anchorElement.remove();
		URL.revokeObjectURL(url);
	}, 0);
}