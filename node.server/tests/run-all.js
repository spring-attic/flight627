/*******************************************************************************
 * @license
 * Copyright (c) 2013 VMware, Inc. All Rights Reserved.
 * THIS FILE IS PROVIDED UNDER THE TERMS OF THE ECLIPSE PUBLIC LICENSE
 * ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS FILE
 * CONSTITUTES RECIPIENTS ACCEPTANCE OF THE AGREEMENT.
 * You can obtain a current copy of the Eclipse Public License from
 * http://www.opensource.org/licenses/eclipse-1.0.php
 *
 * Contributors:
 *   Kris De Volder
 ******************************************************************************/
var reporter = require('nodeunit').reporters['default'];
var filesystem = require('../plugable-fs/utils/filesystem').withBaseDir(undefined);
var fswalk = require('../plugable-fs/utils/fswalk').configure(filesystem).fswalk;
var endsWith = require('../plugable-fs/utils/strings').endsWith;
var testFiles = [];
var path = require('path');

var exec = require('child_process').exec;

var eachk = require('../plugable-fs/utils/callback').eachk;

var problem = null; //remembers the first error. So if any errors we can
					// fail the build.

fswalk(__dirname,
	function (file) {
		//console.log("Visiting: "+file);
		if (endsWith(file, '-test.js')) {
			//The paths... apparantly must be relative or nodeunit gets confused concatentating
			// the absolute path to the process current working directory.
			var toRun = path.relative(__dirname, file.substring());
			console.log('test file found: '+toRun);
			testFiles.push(toRun);
		}
	},
	function () {
		process.chdir(__dirname);
		reporter.run(testFiles, undefined, function (err) {
			problem = problem || err;
			if (problem) {
				console.error(problem);
			}
			process.exit(problem ? 1 : 0);
		});
	}
);

//var scripted = require('../../server/scripted');
