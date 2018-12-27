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
$(document).ready( function() {

	/*	Set active menu item	*/
	var path = document.location.pathname;
	var name = path.substring( path.lastIndexOf('/') + 1, path.lastIndexOf('.') );

	var liItem = document.getElementById(name + 'MenuItem');
	if(liItem  !== null) {
		liItem.className += ' active';
	} else {
	    //statistics page fix
	    const upLevelPath = path.substring(0, path.lastIndexOf('/'));
	    const upDomain = upLevelPath.substring(upLevelPath.lastIndexOf('/') + 1);
	    if(upDomain === "statistics") {
	        document.getElementById('statisticsMenuItem').className += ' active';
	    }
	}

	$(document).keyup(function(e) {
       if (e.keyCode == 27) { // esc code is 27
           closeAllDialog() ;
       }
    });
	
});

function closeAllDialog() {
   for (var propertyName in PrimeFaces.widgets) {
     if (PrimeFaces.widgets[propertyName] instanceof PrimeFaces.widget.Dialog ||
         PrimeFaces.widgets[propertyName] instanceof PrimeFaces.widget.LightBox) {
         PrimeFaces.widgets[propertyName].hide();
     }
   }
}