function selectDate(e,s) {
    var myselect = document.getElementById("select-date");
    console.log(e);
    console.log(window.location.pathname);
    console.log(myselect.options[myselect.selectedIndex].value);
    window.location = myselect.options[myselect.selectedIndex].value;
}

function searchUser(e) {
    var id = $('#users-search').val();
    window.location = '/admin/user/' + id;
}

function searchUserStats(e) {
    var id = $('#users-stats-search').val();
    window.location = '/admin/stats/' + id;
}


 $(document).ready(function() {
     $.dynatableSetup({
           dataset: {
               perPageDefault: 20,
               perPageOptions: [10,20,50]
           }
     });

     $("#team-members").select2({ width: 600 });
     $("#users-search").select2();
     $("#users-stats-search").select2();
     $('#table-player-results').dynatable();
     $('#table-teams').dynatable();
     $('#table-users').dynatable();
     $('#table-team-members').dynatable(
         {
             features: {
             paginate: false,
             sort: true,
             pushState: true,
             search: false,
             recordCount: false,
             perPageSelect: false
             }
         });
 });

$(function() {
    $('#side-menu').metisMenu();
});

//Loads the correct sidebar on window load,
//collapses the sidebar on window resize.
// Sets the min-height of #page-wrapper to window size
$(function() {
    $(window).bind("load resize", function() {
        topOffset = 50;
        width = (this.window.innerWidth > 0) ? this.window.innerWidth : this.screen.width;
        if (width < 768) {
            $('div.navbar-collapse').addClass('collapse');
            topOffset = 100; // 2-row-menu
        } else {
            $('div.navbar-collapse').removeClass('collapse');
        }

        height = ((this.window.innerHeight > 0) ? this.window.innerHeight : this.screen.height) - 1;
        height = height - topOffset;
        if (height < 1) height = 1;
        if (height > topOffset) {
            $("#page-wrapper").css("min-height", (height) + "px");
        }
    });

    var url = window.location;
    var element = $('ul.nav a').filter(function() {
        return this.href == url || url.href.indexOf(this.href) == 0;
    }).addClass('active').parent().parent().addClass('in').parent();
    if (element.is('li')) {
        element.addClass('active');
    }
});

