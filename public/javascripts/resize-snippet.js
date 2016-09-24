/**
 * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/10/2016
 */

resize_snippet = (function($,selector) {
    if (!$(selector)[0]) return;
    $(window).on("load resize", function() {
        const offsetHeight = document.getElementById('header').offsetHeight;
        document.getElementById('canvas').setAttribute("style", "height:" + (window.innerHeight - offsetHeight - 10) + "px");
    });
})($,"#canvas");

