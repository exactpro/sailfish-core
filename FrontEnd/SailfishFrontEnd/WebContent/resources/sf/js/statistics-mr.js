function toggleTcRows(matrixRowTogler) {
	
	$togler = $(matrixRowTogler);
	$matrixRow = $togler.closest('tr');
	
	$matrixRow.nextUntil('.eps-matrix-info-row').toggle();
	$togler.toggleClass('ui-icon-circle-triangle-e');
	$togler.toggleClass('ui-icon-circle-triangle-s');
	
};