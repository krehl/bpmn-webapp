/**
 * modeler.js
 * written by Konstantin Krehl <konstantin.krehl@gmail.com>
 *
 * This script uses bpmn.js to load a diagram from the server into the canvas. Then it bind different data to views using Vue.js.
 * Additional all buttons used in the page are initialised with the correct actions.
 * 
 * Input: BpmnModeler Instance, JQuery Instance
 * 
 */


var bpmnModelerModule = (function (BpmnModeler, $) {

    var debug = true;
    var $debug = function (message) {
        if (debug) console.log(message);
    }
    var initDiagram = true;
    var initHistory = true;
    changed = false;

    if (!$('#canvas')[0]) return; //if the canvas element is not present, something went wrong and we can stop here.

    //to detect unsaved changes we use a global variable and the onbeforeunload event to trigger a prompt by the browser.
    window.onbeforeunload = function () {
        if (changed) {
            return "Are you sure?"; //The string is normally ignored and replaced by a browser specific message.
        }

    };

    //initialize a custom Vue.js component to encapsulate the display of profiles on the page
    var profileComponent = Vue.extend({
        data: function () { //a components data must always be returned by a function
            return {
                imageurl: "",
                name: "",
                profileurl: ""
            }
        },
        //done is the callback function which is injected by the framework
        activate: function (done) { //the activate lifecycle hook is triggered right before the component is rendered, we use it to load data dynamically
            var self = this;
            $.ajax({
                url: jsRoutes.controllers.ProfileController.profile(self.oid).url, //the url to fetch a single users info
                headers: {
                    Accept: "application/json"
                },
                success: function (response) {
                    $debug(response);
                    self.imageurl = "https://www.gravatar.com/avatar/" + md5(response.email) + "?s=20"; //calculate the gravatar url
                    self.name = response.firstName +" "+ response.lastName;
                    self.profileurl = jsRoutes.controllers.ProfileController.profile(self.oid).url;
                    $debug(self);
                    done(); //trigger the rendering of the component
                }

            })
        },
        props: ['oid'], //this data has to be provided by the function using the component
        template: '<span><a v-bind:href=profileurl"><image style="border-radius: 50%;" v-bind:src="imageurl"/></a> <a v-bind:href=profileurl">{{name}}</a></span>' //the actual template code
    });

    Vue.component('profile', profileComponent); //registers the component to be used subsequently

    $debug("bpmnModeller loading"); //debug informatin

    bpmnModeler = new BpmnModeler({
        container: '#canvas'
    });

    var router = jsRoutes.controllers.BPMNDiagramController.retrieve(window.bpmn_id);
    $.ajax({
        url: router.url,
        type: router.type,
        cache: false,
        success: function (response) {
            $debug(response);
            //window.bpmn_id = response.id;
            importXML(response.xmlContent);
            if (initDiagram) {
                $debug("Initiation");
                app = new Vue({
                    el: '#app',
                    data: {
                        process: {
                            name: response.name,
                            description: response.description,
                            xmlContent: response.xmlContent,
                        },
                        old: {
                            name: response.name,
                            description: response.description,
                            xmlContent: response.xmlContent,
                        }
                    },
                    methods: {
                        checkforchanges: function () {
                            if (this.process.name !== this.old.name
                                || this.process.description !== this.old.description ) {
                                changed = true;
                                $debug("Changed");
                            }
                        }
                    }
                });
                $debug(app);
                initDiagram = false;
            } else {
                $debug("Not initiated.");
                app.process.name = response.name;
                app.process.xmlContent = response.xmlContent;
                permissionVue.canEdit = response.canEdit;
                permissionVue.canEdit = response.canView;
            };
            const offsetHeight = document.getElementById('header').offsetHeight;
            document.getElementById('canvas').setAttribute("style", "height:" + (window.innerHeight - offsetHeight - 10) + "px");

        },
        error: function (xhr, ajaxOptions, thrownError) {
            $debug(xhr.status);
            $debug(thrownError);
        }
    });

   /* $('#app h1, #rename-btn').on('click', function (event) {
        var name = prompt('Process Title:', app.name);
        if (!name=="") {
            if (!(name == app.name)) {
                app.name = name;
                changed = true;
            }
        }
    })*/



    permissionVue = new Vue({
        el: '#permissionModal',
        data: {
            user: "",
            remove: [],
            load: {
                canEdit: [],
                canView: []
            }
        },

        created: function () {
            var router = jsRoutes.controllers.BPMNDiagramController.listPermissions(window.bpmn_id.toString());
            $.ajax({
                url: router.url,
                method: 'GET',
                success: function (response) {
                    $debug(response);
                    permissionVue.load.canEdit = response.canEdit;
                    permissionVue.load.canView = response.canView;
                },
                error: function (xhr, ajaxOptions, thrownError) {
                    $('#share-button').hide();
                }
            });
        },

        methods: {
            refresh: function () {
                var router = jsRoutes.controllers.BPMNDiagramController.listPermissions(window.bpmn_id.toString());
                $.ajax({
                    url: router.url,
                    method: 'GET',
                    success: function (response) {
                        $debug(response);
                        permissionVue.remove = [];
                        permissionVue.load.canEdit = response.canEdit;
                        permissionVue.load.canView = response.canView;
                    }
                });
            },
            popEdit: function (index) {
                this.remove.push(this.load.canEdit[index]);
                this.load.canEdit.splice(index,1);
            },
            popView: function (index) {
                this.remove.push(this.load.canView[index]);
                this.load.canView.splice(index,1);
            },
            addViewer: function () {
                if (this.user != "") {
                    this.load.canView.push(this.user);
                    this.user = "";}
            },
            addEditor: function () {
                if (this.user != "") {
                    this.load.canEdit.push(this.user);
                    this.user = "";
                }
            },
            submit: function () {
                var router = jsRoutes.controllers.BPMNDiagramController.addPermissions(window.bpmn_id.toString());
                $.ajax({
                    url: router.url,
                    method : 'PUT',
                    data: JSON.stringify(permissionVue.$data.load),
                    type: router.type,
                    cache: false,
                    contentType: "application/json",
                    success: function (response) {
                        $debug("permissions updated");
                        $debug(response);
                        var options =  {
                            content: "Permissions added", // text of the snackbar
                            style: "toast", // add a custom class to your snackbar
                            timeout: 2000 // time in milliseconds after the snackbar autohides, 0 is disabled
                        }
                        $.snackbar(options);
                    },
                    error: function (xhr, ajaxOptions, thrownError) {
                        $debug(xhr.status);
                        $debug(thrownError);
                    }
                });
                if (this.remove.length > 0) {
                    var remove = jsRoutes.controllers.BPMNDiagramController.removePermissions(window.bpmn_id.toString());
                    $.ajax({
                        url: remove.url,
                        method : 'PUT',
                        data: JSON.stringify(permissionVue.$data.remove),
                        type: router.type,
                        cache: false,
                        contentType: "application/json",
                        success: function (response) {
                            $debug("permissions removed")
                            $debug(response);
                            var options =  {
                                content: "Permissions removed", // text of the snackbar
                                style: "toast", // add a custom class to your snackbar
                                timeout: 2000 // time in milliseconds after the snackbar autohides, 0 is disabled
                            }
                            $.snackbar(options);
                        },
                        error: function (xhr, ajaxOptions, thrownError) {
                            $debug(xhr.status);
                            $debug(thrownError);
                        }
                    });
                }
            }
        }
    });


    $("#load-form").submit(function (e) {
        e.preventDefault();

        var router = jsRoutes.controllers.BPMNDiagramController.retrieve($('#diagram-id').val());
        $.ajax({
            url: router.url,
            type: router.type,
            cache: false,
            success: function (response) {
                window.bpmn_id = response.id;
                importXML(response.xmlContent);
            },
            error: function (xhr, ajaxOptions, thrownError) {
                $debug(xhr.status);
                $debug(thrownError);
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

    changed = false;

    var eventBus = bpmnModeler.get('eventBus');
    window.eventBus = eventBus;
    eventBus.on('element.changed', function (e) {
        //$debug(event, 'on', e.element.id);
        changed = true;
    });

// save diagram on button click
    const saveButton = document.querySelector('#save-button');
    const svgDownload = document.querySelector('#svg-button');
    const xmlDownload = document.querySelector('#xml-button');
    const historyButton = document.querySelector('#history');
    const deleteButton = document.querySelector('#delete-btn');

    deleteButton.addEventListener('click', function (e) {
        e.preventDefault();
        if (window.confirm("Are you sure that you want to delete the diagram?")) {
            var router = jsRoutes.controllers.BPMNDiagramController.delete(window.bpmn_id.toString());
            $.ajax({
                url: router.url,
                method: "DELETE",
                success: function (response) {
                    $debug('deleted');
                    location.href = jsRoutes.controllers.RepositoryController.repository().url;
                },
                error: function (xhr, ajaxOptions, thrownError) {
                    $debug(xhr.status);
                    $debug(thrownError);
                }
            });
        }
    });


    historyButton.addEventListener('click', function () {
       var router = jsRoutes.controllers.BPMNDiagramController.getHistory(window.bpmn_id.toString());
        $debug("History Button clicked.")
        $.ajax({
            url: router.url,
            success: function(response) {
                $debug(response);
                if (initHistory) {
                    historyVue = new Vue({
                        el: '#history-modal',
                        data: {
                            items: response
                        },
                        methods: {
                            fromNow: function(string) {
                                return moment(new Date(string)).fromNow();
                            },
                            loadVersion: function (index) {
                                $debug(this.items[index].xmlContent);
                                importXML(this.items[index].xmlContent);
                                var options =  {
                                    content: "diagram loaded", // text of the snackbar
                                    style: "toast", // add a custom class to your snackbar
                                    timeout: 1000 // time in milliseconds after the snackbar autohides, 0 is disabled
                                }
                                $.snackbar(options);
                            }
                        }
                    });
                    initHistory = false;
                } else {
                    historyVue.items = response;
                }

            },
            error: function (xhr, ajaxOptions, thrownError) {
                $debug(xhr.status);
                $debug(thrownError);
            }
        })
    });


    saveButton.addEventListener('click', function () {
        $debug("Save Button clicked.")
        if (changed) {
            $debug("Changed = true.")
            // get the diagram contents
            bpmnModeler.saveXML({format: true}, function (err, xml) {

                if (err) {
                    console.error('diagram save failed', err);
                } else {
                    app.process.xmlContent = xml;
                    var router = jsRoutes.controllers.BPMNDiagramController.update(window.bpmn_id.toString());
                    $.ajax({
                        url: router.url,
                        data: JSON.stringify(app.$data.process),
                        type: router.type,
                        cache: false,
                        contentType: "application/json",
                        success: function (response) {
                            $debug("update successful")
                            $debug(response);
                            changed = false;
                        },
                        error: function (xhr, ajaxOptions, thrownError) {
                            $debug(xhr.status);
                            $debug(thrownError);
                        }
                    });
                }
            });
        } else {
            $debug('no changes, save unneccessary');
        }

    });

    svgDownload.addEventListener('click', function (event) {
        //event.preventDefault();
        bpmnModeler.saveSVG({},function(err,svg){
            if (err) {
                $debug(err);
                return;
            }
            var link = document.createElement('a');
            link.download = window.bpmn_id + '.svg';
            link.target = '_blank';
            link.href = 'data:application/bpmn20-xml;charset:UFT-8,'+encodeURIComponent(svg)
            link.click();
            $debug(svg);
        });
    });

    xmlDownload.addEventListener('click', function (event) {
        //event.preventDefault();
        bpmnModeler.saveXML({format: true},function(err,xml){
            if (err) {
                $debug(err);
                return;
            }
            var link = document.createElement('a');
            link.download = window.bpmn_id + '.bpmn';
            link.target = '_blank';
            link.href = 'data:application/bpmn20-xml;charset:UFT-8,'+encodeURIComponent(xml)
            link.click();
            $debug(xml);
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
                changed = true;
                var canvas = bpmnModeler.get('canvas');
                // zoom to fit full viewport
                canvas.zoom('fit-viewport');
            });

        };

        reader.readAsText(file);

    });

    xmlUpload.addEventListener('click',function (e) {
        xmlFile.click();
        $debug();
        e.preventDefault();
    });

    $(".modal-draggable .modal-dialog").draggable({
        handle: ".modal-header"
    });

    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    })

})(window.BpmnJS, window.jQuery);
