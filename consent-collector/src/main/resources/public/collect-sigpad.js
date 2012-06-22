
var sigOptions = { lineMargin: 20, 
	           lineTop: 100,  
		   drawOnly: true , 
		   bgColour: '#eeeeee',
		   validateFields : false 
};

//var sigPad_api = $('sigPad').signaturePad(sigOptions);
//$('.sigPad').signaturePad(sigOptions);} 

    $(document).ready(function() {
      $('.sigPad').signaturePad(sigOptions);
      var sigapi = signaturePad();
    });

