/**
 * bpmn-js-seed
 *
 * This is an example script that loads an embedded diagram file <diagramXML>
 * and opens it using the bpmn-js modeler.
 */


var bpmnModeler = (function (BpmnModeler, $) {

    // create modeler

    if (!$('#canvas')[0]) return;

    console.log("bpmnModeller loading");

    const bpmnModeler = new BpmnModeler({
        container: '#canvas'
    });

    var router = jsRoutes.controllers.BPMNDiagramController.retrieve(window.bpmn_id);
    $.ajax({
        url: router.url,
        type: router.type,
        cache: false,
        success: function (response) {
            console.log(response);
            window.bpmn_id = response.id;

            importXML(response.xml);
        },
        error: function (xhr, ajaxOptions, thrownError) {
            console.log(xhr.status);
            console.log(thrownError);
        }
    });

    // $.ajax({
    //     url: "/bpmn/" + window.bpmn_id,
    //     type: 'GET',
    //     success: function (response) {
    //         window.bpmn_id = response.id;
    //         importXML(response.xml);
    //     },
    //     error: function (xhr, ajaxOptions, thrownError) {
    //         console.log(xhr.status);
    //         console.log(thrownError);
    //     }
    // });


    // $.ajax({
    //     url: "/bpmn/new",
    //     type: 'GET',
    //     success: function (response) {
    //         window.bpmn_id = response.id;
    //         console.log(bpmn_id);
    //         importXML(response.xml);
    //     },
    //     error: function (xhr, ajaxOptions, thrownError) {
    //         console.log(xhr.status);
    //         console.log(thrownError);
    //     }
    // });
    //
    // $("#new-button").click(function () {
    //     window.location.replace("/bpmn/new");

    // $.ajax({
    //     url: "/bpmn/new",
    //     type: 'GET',
    //     success: function (response) {
    //         window.bpmn_id = response.id;
    //         console.log(window.bpmn_id);
    //         importXML(response.xml);
    //     },
    //     error: function (xhr, ajaxOptions, thrownError) {
    //         console.log(xhr.status);
    //         console.log(thrownError);
    //     }
    // });
    // });


    $("#load-form").submit(function (e) {
        e.preventDefault();

        var router = jsRoutes.controllers.BPMNDiagramController.retrieve($('#diagram-id').val());
        $.ajax({
            url: router.url,
            type: router.type,
            cache: false,
            success: function (response) {
                window.bpmn_id = response.id;
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

    var changed = false;

    var eventBus = bpmnModeler.get('eventBus');
    window.eventBus = eventBus;
    eventBus.on('element.changed', function (e) {
        console.log(event, 'on', e.element.id);
        changed = true;
    });

// save diagram on button click
    const saveButton = document.querySelector('#save-button');
    const svgDownload = document.querySelector('#svg-button');
    const xmlDownload = document.querySelector('#xml-button');


    saveButton.addEventListener('click', function () {

        // get the diagram contents
        bpmnModeler.saveXML({format: true}, function (err, xml) {

            if (err) {
                console.error('diagram save failed', err);
            } else {
                var router = jsRoutes.controllers.BPMNDiagramController.update(window.bpmn_id);
                $.ajax({
                    url: router.url,
                    data: xml,
                    type: router.type,
                    cache: false,
                    contentType: "application/json",
                    success: function (response) {
                        console.log(response);
                        changed = false;
                    },
                    error: function (xhr, ajaxOptions, thrownError) {
                        console.log(xhr.status);
                        console.log(thrownError);
                    }
                });
            }
        });
    });

    svgDownload.addEventListener('click', function (event) {
        //event.preventDefault();
        bpmnModeler.saveSVG({},function(err,svg){
            if (err) {
                console.log(err);
                return;
            }
            var link = document.createElement('a');
            link.download = window.bpmn_id + '.svg';
            link.target = '_blank';
            link.href = 'data:application/bpmn20-xml;charset:UFT-8,'+encodeURIComponent(svg)
            link.click();
            console.log(svg);
        });
    });

    xmlDownload.addEventListener('click', function (event) {
        //event.preventDefault();
        bpmnModeler.saveXML({format: true},function(err,xml){
            if (err) {
                console.log(err);
                return;
            }
            var link = document.createElement('a');
            link.download = window.bpmn_id + '.bpmn';
            link.target = '_blank';
            link.href = 'data:application/bpmn20-xml;charset:UFT-8,'+encodeURIComponent(xml)
            link.click();
            console.log(xml);
        });
    });

    const xmlUpload = document.querySelector('#xml-upload');
    const xmlFile = document.querySelector('#xml-file');
    const xmlUploadForm = document.querySelector('#xml-upload-form');

    xmlFile.addEventListener('change',function(event){
        event.preventDefault;
        var file = document.forms['xml-upload-form']['xml-file'].files[0];
        var reader = new FileReader();

        reader.onload = function(event) {

            bpmnModeler.importXML(event.target.result,function (err) {
                if (err) {
                    return console.error('could not import BPMN 2.0 diagram', err);
                }
                var canvas = bpmnModeler.get('canvas');
                // zoom to fit full viewport
                canvas.zoom('fit-viewport');
            });

        };

        reader.readAsText(file);

    });

    xmlUpload.addEventListener('click',function (e) {
        xmlFile.click();
        console.log();
        e.preventDefault();
    })

})(window.BpmnJS, window.jQuery);
