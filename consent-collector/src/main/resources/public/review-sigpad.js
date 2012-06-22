
var sigOptions = { lineMargin: 20, 
	           lineTop: 100,  
		   displayOnly: true , 
		   bgColour: '#eeeeee',
		   validateFields : false 
};

    $(document).ready(function() {
      $('.sigPad').signaturePad(sigOptions);
      var sigapi = signaturePad();
    });

