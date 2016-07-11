/**
 * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/10/2016
 */
$(window).on("load resize", function() {
    const offsetHeight = document.getElementById('navigation').offsetHeight;
    document.getElementById('content').setAttribute("style", "height:" + (window.innerHeight - offsetHeight) + "px");
});