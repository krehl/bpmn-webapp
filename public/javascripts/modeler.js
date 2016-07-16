/**
 * bpmn-js-seed
 *
 * This is an example script that loads an embedded diagram file <diagramXML>
 * and opens it using the bpmn-js modeler.
 */


(function (BpmnModeler, $) {

    var bpmn_id;

    // create modeler
    const bpmnModeler = new BpmnModeler({
        container: '#canvas'
    });

    $("#new-button").click(function () {

        $.ajax({
            url: "/bpmn/new",
            type: 'GET',
            success: function (response) {
                bpmn_id = response.id;
                console.log(bpmn_id);
                importXML(response.xml);
            },
            error: function (xhr, ajaxOptions, thrownError) {
                console.log(xhr.status);
                console.log(thrownError);
            }
        });
    });


    $("#load-form").submit(function (e) {
        e.preventDefault();

        $.ajax({
            url: "/bpmn/" + $('#diagram-id').val(),
            type: 'GET',
            success: function (response) {
                bpmn_id = response.id;
                importXML(response.xml);
            },
            error: function (xhr, ajaxOptions, thrownError) {
                console.log(xhr.status);
                console.log(thrownError);
            }
        });
    });


// import function
    function importXML(xml) {
        // import diagram
        bpmnModeler.importXML(xml, function (err) {

            if (err) {
                return console.error('could not import BPMN 2.0 diagram', err);
            }

            var canvas = bpmnModeler.get('canvas');

            // zoom to fit full viewport
            canvas.zoom('fit-viewport');
        });
    }

// save diagram on button click
    const saveButton = document.querySelector('#save-button');

    saveButton.addEventListener('click', function () {

        // get the diagram contents
        bpmnModeler.saveXML({format: true}, function (err, xml) {

            if (err) {
                console.error('diagram save failed', err);
            } else {
                console.info('diagram saved');
                $.ajax({
                    url: "/bpmn/" + bpmn_id,
                    data: xml,
                    type: 'PUT',
                    // contentType: "text/plain",
                    contentType: "application/xml",
                    success: function (response) {
                        console.log(response)
                    },
                    error: function (xhr, ajaxOptions, thrownError) {
                        console.log(xhr.status);
                        console.log(thrownError);
                    }
                });
            }
        });
    });


})(window.BpmnJS, window.jQuery);


