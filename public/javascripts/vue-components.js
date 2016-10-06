/**
 * Created by krehl on 18.09.2016.
 */

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
    template: '<span><a v-bind:href="profileurl"><image style="border-radius: 50%;" v-bind:src="imageurl"/> {{name}}</a></span>'
});

Vue.component('profile', profileComponent);