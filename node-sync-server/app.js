
var express = require('express')
  , app = express()
  , server = app.listen(3000)
  , io = require('socket.io').listen(server);

var ProjectProvider = require('./project-provider').ProjectProvider;
var projectProvider = new ProjectProvider();

var getProjects = function(req, res) {
    projectProvider.getProjects(function(error, result) {
        res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
    });
}

var getProject = function(req, res) {
    projectProvider.getProject(req.params.project, function(error, result) {
		if (error == null) {
        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
		}
		else {
			res.send(error);
		}
    });
}

var createProject = function(req, res) {
    projectProvider.createProject(req.params.project, function(error, result) {
		if (error == null) {
        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
		}
		else {
			res.send(error);
		}
    });
}

var putResource = function(req, res) {
	var body = '';
	req.on('data', function(buffer) {
		console.log("Chunk:", buffer.length );
		body += buffer;
	});
	
	req.on('end', function() {
	    projectProvider.putResource(req.params.project, req.params.resource, body, function(error, result) {
			if (error == null) {
	        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
			}
			else {
				res.send(error);
			}
	    });
	});
}

var postResource = function(req, res) {
	var body = '';
	req.on('data', function(buffer) {
		console.log("Chunk:", buffer.length );
		body += buffer;
	});
	
	req.on('end', function() {
	    projectProvider.updateResource(req.params.project, req.params.resource, body, function(error, result) {
			if (error == null) {
	        	res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
				io.sockets.emit('resourceupdate', { 'project' : req.params.project,
													'resource' : req.params.resource,
													'newversion' : result.newversion,
													'fingerprint' : result.fingerprint});
			}
			else {
				res.send(error);
			}
	    });
	});
	
	req.on('error', function(error) {
		console.log('Error: ' + error);
	});
	
}

var getResource = function(req, res) {
    projectProvider.getResource(req.params.project, req.params.resource, function(error, result) {
		if (error == null) {
        	res.send(result, 200);
		}
		else {
			res.send(error);
		}
    });
}

// app.use(express.methodOverride());

app.get('/', getProjects);

app.get('/:project', getProject);
app.put('/:project', createProject);

app.get('/:project/:resource(*)', getResource);
app.put('/:project/:resource(*)', putResource);
app.post('/:project/:resource(*)', postResource);

// server.listen(3000);
console.log('Express server started on port 3000');

io.set('transports', ['websocket']);

io.sockets.on('connection', function (socket) {
	console.log('client connected for update notifications');

	socket.on('disconnect', function () {
		console.log('client disconnected from update notifications');
	});
});
