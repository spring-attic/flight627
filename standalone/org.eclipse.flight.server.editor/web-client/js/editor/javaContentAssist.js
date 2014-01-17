/*******************************************************************************
 * @license Copyright (c) 2013 Pivotal Software, Inc. and others. All rights
 *          reserved. This program and the accompanying materials are made
 *          available under the terms of the Eclipse Public License v1.0
 *          (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse
 *          Distribution License v1.0
 *          (http://www.eclipse.org/org/documents/edl-v10.html).
 * 
 * Contributors: Pivotal Software, Inc. - initial API and implementation
 ******************************************************************************/
define("editor/javaContentAssist", ['orion/Deferred'], function(Deferred) {
	
	var currentCallbackId = 0;
	var callbacks = {};
		
	function JavaContentAssistProvider() {
	}
	
	// This creates a new callback ID for a request
	function getCallbackId() {
		currentCallbackId += 1;
		if(currentCallbackId > 10000) {
			currentCallbackId = 0;
		}
		return currentCallbackId;
	}

    function sendContentAssistRequest(request, eb) {
    	
		var deferred = new Deferred();
		console.log("Sent assist request...");
		var callbackId = getCallbackId();
		callbacks[callbackId] = {
			time : new Date(),
			cb : deferred
		};
		request.contents.callback = callbackId;
		eb.send('flight.proposalService', request, function(reply) {
			var data = JSON.parse(JSON.stringify(reply)).contents;
			console.log("Recieved reply: ");
			console.log(data);
			if(callbacks.hasOwnProperty(data.callback)) {
				console.log("has!!");
				console.log(callbacks[data.callback]);
				callbacks[data.callback].cb.resolve(data.proposals);
				delete callbacks[data.callback];
			}
		});

		return deferred.promise;
    }
	
	JavaContentAssistProvider.prototype =
	{
		computeProposals: function(buffer, offset, context) {
			var request = {
				kind : 'request',
				action : 'proposal.request',
				senderId : editor_id,
				contents : {
					'class' : 'org.eclipse.flight.objects.services.ContentAssist',
					'username' : this.username,
					'projectName' : this.project,
					'path' : this.resourcePath,
					'offset' : offset,
					'prefix' : context.prefix
				}
			};
			
			var deferred = sendContentAssistRequest (request, this.eb);
			return deferred;
		},
		
		setProject: function(project) {
			this.project = project;
		},
		
		setResourcePath: function(resourcePath) {
			this.resourcePath = resourcePath;
		},
		
		setUsername: function(username) {
			this.username = username;
		},
		
		setEventBus: function(eb) {
			this.eb = eb;
		}
		
	}
	
	return {
		JavaContentAssistProvider: JavaContentAssistProvider
	};
});