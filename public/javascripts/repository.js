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
})(window.jQuery);