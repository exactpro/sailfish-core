function toggleTcRows(matrixRowTogler) {
	
	$togler = $(matrixRowTogler);
	$matrixRow = $togler.closest('tr');
	
	$matrixRow.nextUntil('.eps-matrix-info-row').toggle();
	$togler.toggleClass('ui-icon-circle-triangle-e');
	$togler.toggleClass('ui-icon-circle-triangle-s');
	
};


// FIXME remove this hack after migration to primefaces >= 5.1 and set rowSelectMode="checkbox" attr for dataTable
PrimeFaces.widget.DataTable.prototype.selectRow = function(r, silent) {
     var row = this.findRow(r);
     if(row.hasClass('ui-chkbox-box')) {
        // call the generic implementation
        PrimeFaces.widget.DataTable.prototype.selectRow.call(this);
     }
};