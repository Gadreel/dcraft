/*
 * Global script for the entire site
 */
 
function generalLoader(page) {
	var area = page.Name.substring(1);
	var apos = area.indexOf('/');
	
	if (apos != -1)
		area = area.substring(0,apos);
	
	if (!area)
		area = 'Home';
	
	$('#pubHeaderMenu a[Id="mnu' + area + '"]').addClass('selected');
	
	//dc.pui.Apps.activateCms();
}


