/**
 * Created by krehl on 17.09.2016.
 */


var repository = (function ($) {

    $('.process-delete').addEventListener('click',function (event) {
        event.preventDefault();
        if (window.confirm("Are you sure that you want to delete the diagram?")) {
            $.ajax({
                url: jsRoutes.controllers.BPMNDiagramController.delete(event.target.getAttributeValue('data-diagram-id')),
                method: 'DELETE',
                success: function () {
                    console.log('deleted');
                }
            })
        }
    });
})(window.jQuery);