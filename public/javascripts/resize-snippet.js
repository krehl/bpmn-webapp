/**
 * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/10/2016
 */
$(window).on("load resize", function() {
    const offsetHeight = document.getElementById('app').offsetHeight;
    document.getElementById('content').setAttribute("style", "height:" + (window.innerHeight - offsetHeight - 10) + "px");
});