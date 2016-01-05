var utils = require('utils');
var testlib = require('./testLib');

casper.test.begin('Test Display', function suite(test) {
    casper.start();
    casper.thenOpen(testlib.server + '/app/home', function(){
    });
    testlib.login(test,testlib.user,testlib.pass);

    casper.then(function() {
        test.assertExists('#Scramble-standings');
    });

    casper.run(function(){
        test.done();
    });
});
