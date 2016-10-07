/**
 * Created by krehl on 17.09.2016.
 */


var repository = (function ($) {

    var debug = true;
    var $debug = function (message) {
        if (debug) console.log(message);
    }

    if(undefined === $('#repository')[0]) return;

/*    $('.process-delete').on('click',function (event) {
        event.preventDefault();
        if (window.confirm("Are you sure that you want to delete the diagram?")) {
            var $this = $(this);
            var router = jsRoutes.controllers.BPMNDiagramController.delete($(this).attr('data-diagram-id'));
            $.ajax({
                url: router.url,
                method: 'DELETE',
                success: function () {
                    $debug('deleted');
                    $this.parent().parent().fadeOut();
                }
            })
        }
    });*/

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
                    $debug(response);
                    self.imageurl = "https://www.gravatar.com/avatar/" + md5(response.email) + "?s=20";
                    self.name = response.firstName +" "+ response.lastName;
                    self.profileurl = jsRoutes.controllers.ProfileController.profile(self.oid).url;
                    $debug(self);
                    done();
                }
            })
        },
        props: ['oid'],
        template: '<span><a v-bind:href="profileurl" target="_blank"><image style="border-radius: 50%;width:20px;height:20px;" src="" v-bind:src="imageurl"></image></a> <a v-bind:href="profileurl" target="_blank">{{name}}</a></span>'
    });

    Vue.component('profile', profileComponent);

    repoVue = new Vue({
        el: '#app-repo',
        data: {
            diagrams: [],
        },
        created: function(){
            $this = this;
            var router = jsRoutes.controllers.RepositoryController.repositoryJson();
            $.ajax({
                url: router.url,
                success: function (response) {
                    $debug('success', response.diagrams);
                    $this.diagrams = response.diagrams;
                }
            })
        },
        methods: {
            removeprocess: function (index) {
                $debug('delete',index);
                if (window.confirm("Are you sure that you want to delete the diagram?")) {
                    var router = jsRoutes.controllers.BPMNDiagramController.delete($this.diagrams[index].id.$oid);
                    $.ajax({
                        url: router.url,
                        method: 'DELETE',
                        success: function () {
                            $debug('deleted');
                            $this.diagrams.splice(index,1);
                        }
                    })
                }
            },
            download: function(index) {
                return jsRoutes.controllers.BPMNDiagramController.download($this.diagrams[index].id.$oid).url;
            },
            loadModeller: function (index) {
                return jsRoutes.controllers.BPMNDiagramController.loadModeller($this.diagrams[index].id.$oid).url;
            },
            profile : function (oid) {
                return jsRoutes.controllers.ProfileController.profile(oid).url;
            }
        }
    });

    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    })

})(window.jQuery);