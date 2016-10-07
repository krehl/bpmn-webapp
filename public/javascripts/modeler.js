/**
 * bpmn-js-seed
 *
 * This is an example script that loads an embedded diagram file <diagramXML>
 * and opens it using the bpmn-js modeler.
 */


var bpmnModelerModule = (function (BpmnModeler, $) {

    // create modeler

    var initDiagram = true;
    var initHistory = true;
    changed = false;

    if (!$('#canvas')[0]) return;

    window.onbeforeunload = function () {
        if (changed) {
            return "Are you sure?";
        }

    };

    var profileComponent = Vue.extend({
        data: function () {
            return {
                imageurl: "",
                name: "",
                profileurl: ""
            }
        },
        activate: function (done) {
            var self = this;
            $.ajax({
                url: jsRoutes.controllers.ProfileController.profile(self.oid).url,
                headers: {
                    Accept: "application/json"
                },
                success: function (response) {
                    console.log(response);
                    self.imageurl = "https://www.gravatar.com/avatar/" + md5(response.email) + "?s=20";
                    self.name = response.firstName +" "+ response.lastName;
                    self.profileurl = jsRoutes.controllers.ProfileController.profile(self.oid).url;
                    console.log(self);
                    done();
                }

            })
        },
        props: ['oid'],
        template: '<span><a v-bind:href=profileurl"><image style="border-radius: 50%;" v-bind:src="imageurl"/></a> <a v-bind:href=profileurl">{{name}}</a></span>'
    });

    Vue.component('profile', profileComponent);

    console.log("bpmnModeller loading");

    bpmnModeler = new BpmnModeler({
        container: '#canvas'
    });

    var router = jsRoutes.controllers.BPMNDiagramController.retrieve(window.bpmn_id);
    $.ajax({
        url: router.url,
        type: router.type,
        cache: false,
        success: function (response) {
            console.log(response);
            //window.bpmn_id = response.id;
            importXML(response.xmlContent);
            if (initDiagram) {
                console.log("Initiation");
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
                                console.log("Changed");
                            }
                        }
                    }
                });
                console.log(app);
                initDiagram = false;
            } else {
                console.log("Not initiated.");
                app.process.name = response.name;
                app.process.xmlContent = response.xmlContent;
                permissionVue.canEdit = response.canEdit;
                permissionVue.canEdit = response.canView;
            };
            const offsetHeight = document.getElementById('header').offsetHeight;
            document.getElementById('canvas').setAttribute("style", "height:" + (window.innerHeight - offsetHeight - 10) + "px");

        },
        error: function (xhr, ajaxOptions, thrownError) {
            console.log(xhr.status);
            console.log(thrownError);
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
                    console.log(response);
                    permissionVue.load.canEdit = response.canEdit;
                    permissionVue.load.canView = response.canView;
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
                        console.log(response);
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
                        console.log("permissions updated")
                        console.log(response);
                    },
                    error: function (xhr, ajaxOptions, thrownError) {
                        console.log(xhr.status);
                        console.log(thrownError);
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
                            console.log("permissions removed")
                            console.log(response);
                        },
                        error: function (xhr, ajaxOptions, thrownError) {
                            console.log(xhr.status);
                            console.log(thrownError);
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

    changed = false;

    var eventBus = bpmnModeler.get('eventBus');
    window.eventBus = eventBus;
    eventBus.on('element.changed', function (e) {
        //console.log(event, 'on', e.element.id);
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
                    console.log('deleted');
                    location.href = jsRoutes.controllers.RepositoryController.repository().url;
                },
                error: function (xhr, ajaxOptions, thrownError) {
                    console.log(xhr.status);
                    console.log(thrownError);
                }
            });
        }
    });


    historyButton.addEventListener('click', function () {
       var router = jsRoutes.controllers.BPMNDiagramController.getHistory(window.bpmn_id.toString());
        console.log("History Button clicked.")
        $.ajax({
            url: router.url,
            success: function(response) {
                console.log(response);
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
                                console.log(this.items[index].xmlContent);
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
                console.log(xhr.status);
                console.log(thrownError);
            }
        })
    });


    saveButton.addEventListener('click', function () {
        console.log("Save Button clicked.")
        if (changed) {
            console.log("Changed = true.")
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
                            console.log("update successful")
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
        } else {
            console.log('no changes, save unneccessary');
        }

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
        console.log();
        e.preventDefault();
    });

    $(".modal-draggable .modal-dialog").draggable({
        handle: ".modal-header"
    });

    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    })

})(window.BpmnJS, window.jQuery);
