
var sigOptions = { lineMargin: 20, 
	           lineTop: 150,  
		   lineWidth: 4,
		   defaultAction: 'drawIt',
		   drawOnly: true, 
		   clear: null,
		   bgColour: '#eeeeee',
	           validateFields: false
	           //errorMessageDraw: $('.sigError').text()
};


var initSig = function () {
	var readOnly = $('.sigPad').data('read-only');
	if (readOnly) {
		sigOptions.displayOnly = true;	
	}

	var sigcontainer = $('.sigPad');
        if (sigcontainer.length > 0) {	 // Are there any signature fields on the screen
		var sp = sigcontainer.signaturePad(sigOptions);
		/*$('.clearButton').button();
		$('.clearButton').click(       // Register handler for the field button
			function(e) {
				sp.clearCanvas();}); */
		$('form').filter(
			function(e) {
				return $('.sigPad', this).length == 1}).submit( // this finds the form that contains the sigPad and binds handler
			function(e) {
				$('input.output').attr('value', sp.getSignatureImage());  // Assumption: Only one signature field per form
			});

		// Handle loading of an initial signature if present
		var sigImage = $('.sigPad').data('image');
		if (sigImage) {
		   //$('.sigPad').signaturePad(sigOptions);
		   var c = $('canvas')[0];
		   var ctx = c.getContext("2d");
		   var im = new Image();
		   im.onload = function() {
			   ctx.drawImage(im, 0, 0);};
		   im.src = sigImage;
		}
	}
}


$(document).ready(initSig);
