/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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

// TODO Hack for #43631. Primefaces 5.0 does not support ajax event 'sort' in TreeTable. 
// We can simply remove this hack after changing version to modern: 
// https://github.com/primefaces/primefaces/issues/1199
document.addEventListener('click', async function () {
	if ($('#results-table_data').length > 0) {
		await sleep(100);
		drawPassedFailedBars();
	}
}); 

function sleep(ms) {
	  return new Promise(resolve => setTimeout(resolve, ms));
}

function drawPassedFailedBars() {
	
	colWidth = $('#results-table_data > tr > td:eq(3)').width();
	
	curGlobalTotal = 0;
	
	$('#results-table_data tr').each(function(i,elem) {
		
		if ($(this).hasClass('ui-treetable-empty-message')) {
			return;
		}
		
		level = $(this).attr('id').match(/_/g).length - 1; //Level = count of '_' symbol in id - 1
		
		// Get td elements
		totalTd = $(this).find('td:eq(2)');
		passedTd = $(this).find('td:eq(3)');
		failedTd = $(this).find('td:eq(4)');
		
		total = +totalTd.text();
		passed = +passedTd.text();
		failed = +failedTd.text();
		
		if (level == 1) {
			curGlobalTotal = total;
		}
		
		passedTd.empty();
		failedTd.empty();
		
		if (passed >= 0) {
			passedWidth = (passed / curGlobalTotal) * colWidth;
			passedTd.append('<div class="colPassed" style="width:' + passedWidth + 'px"></div>');
			passedTd.css('position', 'relative');
			passedTd.append('<div class="numPassed"><div style="position: relative; left: -50%">' + passed + '</div></div>');
		}
		
		if (failed >= 0) {
			failedWidth = (failed / curGlobalTotal) * colWidth;
			failedTd.append('<div class="colFailed" style="width:' + failedWidth + 'px"></div>');
			failedTd.css('position', 'relative');
			failedTd.append('<div class="numFailed"><div style="position: relative; left: -50%">' + failed + '</div></div>');
		}
	});
}