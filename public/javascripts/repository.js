/**
 * Created by krehl on 17.09.2016.
 */


var repository = (function ($) {

    if(undefined === $('#repository')[0]) return;

    $('.process-delete').on('click',function (event) {
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
    });

    repoVue = new Vue({
        el: '#app-repo',
        data: {
            diagrams: []
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
            gravatar: function (oid) {
                return "https://www.gravatar.com/avatar/"+md5(oid)+"?s=25"
            }
        }
    });


})(window.jQuery);