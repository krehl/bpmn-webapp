/**
 * Created by krehl on 17.09.2016.
 */


var repository = (function ($) {

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
                    console.log('deleted');
                    $this.parent().parent().fadeOut();
                }
            })
        }
    });*/

    var MyComponent = Vue.extend({
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
        template: '<span><a href="{{profileurl}}"><image style="border-radius: 50%;" v-bind:src="imageurl"/> {{name}}</a></span>'
    });

    Vue.component('profile', MyComponent)

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
                    console.log('success', response.diagrams);
                    $this.diagrams = response.diagrams;
                }
            })
        },
        methods: {
            removeprocess: function (index) {
                console.log('delete',index);
                if (window.confirm("Are you sure that you want to delete the diagram?")) {
                    var router = jsRoutes.controllers.BPMNDiagramController.delete($this.diagrams[index].id.$oid);
                    $.ajax({
                        url: router.url,
                        method: 'DELETE',
                        success: function () {
                            console.log('deleted');
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


})(window.jQuery);