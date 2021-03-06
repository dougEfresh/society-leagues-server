var utils = require('utils');
var testlib = require('./testLib');
var stats = [];
var u = require('./user.json');
var seasons = require('./seasons.json');

function getStats() {
    var rows = document.querySelectorAll('#table-leaders > tbody > tr');
    var stats = [];

    for (var i = 0, row; row = rows[i]; i++) {
        var a = row.cells[1].querySelector('a[href*="app"]');
        //var l = row.cells[2].querySelector('span');
        var stat = {};
        stat['rank'] = row.cells[0].textContent;
        stat['userId'] = a.id.replace('leader-user-link-','');
        stats.push(stat);
    }
    return stats;
}

function processStats(season,test) {
    for(var i = 0; i< stats.length; i++) {
        verifyStat(stats[i],(i+1),test);
    }
}

function verifyStat(stat,rank,test) {
    casper.then(function() {
        var r = stat.rank;
        test.assert(r == rank + "", "Should be rank " + rank);
    });

    casper.then(function() {
        var r = stat.userId;
        test.assert(r != undefined, "Should have link");
        this.click('#leader-user-link-' + r);
    });

    casper.then(function() {
        test.assertExists('#user-results-' + stat.userId);
    });

    casper.then(function() {
        this.back();
    });

    casper.then(function() {
        test.assertExists('#leader-user-link-' + stat.userId);
    });
}


function processSeason(season,test) {
    if (season.challenge) {
        return processTopgun(season,test);

    }
    casper.thenOpen(testlib.server + '/app/leaders/' + season.id, function () {
    });


    casper.then(function() {
        test.assertExists('#leaders-' + season.id);
        //this.click('#' + season + '-leaders-all');
    });

    casper.then(function() {
        test.assertExists('#table-leaders','table-leaders');
    });

    casper.then(function () {
        stats  = this.evaluate(getStats);
        test.assert(stats != null && stats.length > 0, 'Found leaders');
    });

    casper.then(function () {
        processStats(season,test);
    });
}

function processTopgun(season,test) {
    casper.thenOpen(testlib.server + '/app/display/' + season.id, function () {
    });

    casper.then(function() {
        test.assertExists('#display-topgun','display-topgun');
        test.assertExists('#table-team-standings','table-team-standings');
    });

}

casper.test.begin('Test Home Page', function suite(test) {
    casper.start();
    casper.thenOpen(testlib.server + '/app/login', function(){
    });
    testlib.login(test,testlib.user,testlib.pass);

    seasons.forEach(function(s) {
        processSeason(s,test);
    });

    casper.run(function(){
        test.done();
    });
});
