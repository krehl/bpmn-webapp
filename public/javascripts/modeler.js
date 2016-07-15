/**
 * bpmn-js-seed
 *
 * This is an example script that loads an embedded diagram file <diagramXML>
 * and opens it using the bpmn-js modeler.
 */
(function(BpmnModeler, $) {

    // create modeler
    var bpmnModeler = new BpmnModeler({
        container: '#canvas'
    });


    // import function
    function importXML(xml) {

        // import diagram
        bpmnModeler.importXML(xml, function(err) {

            if (err) {
                return console.error('could not import BPMN 2.0 diagram', err);
            }

            var canvas = bpmnModeler.get('canvas');

            // zoom to fit full viewport
            canvas.zoom('fit-viewport');
        });


        // save diagram on button click
        var saveButton = document.querySelector('#save-button');

        saveButton.addEventListener('click', function() {

            // get the diagram contents
            bpmnModeler.saveXML({ format: true }, function(err, xml) {

                if (err) {
                    console.error('diagram save failed', err);
                } else {
                    console.info('diagram saved');
                    $.ajax({
                        url: "/save",
         //               data: xml,
                        type: 'POST',
                       contentType: "text",
                        dataType: "text",
                        success : function (a) {
                            console.log(a)
                        },
                        error : function (xhr, ajaxOptions, thrownError){
                            console.log(xhr.status);
                            console.log(thrownError);
                        }
                    });
                }
            });
        });
    }


    $.ajax({
        url: "/load/" + bpmn_id,
        type: 'GET',
        success : function (a) {
            console.log(a)
            importXML(a)
        },
        error : function (xhr, ajaxOptions, thrownError){
            console.log(xhr.status);
            console.log(thrownError);
        }});

})(window.BpmnJS, window.jQuery);