/*******************************************************************************
 * @license
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * Copyright (c) 2012 VMware, Inc.
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors: 
 *     IBM Corporation - initial API and implementation
 *     Andrew Eisenberg - rename jsContentAssist to jsTemplateContentAssist
 *     Martin Lippert - flight prototype work
 *******************************************************************************/
/*global examples orion:true window define*/
/*jslint browser:true devel:true*/

define([
	"require", 
	"orion/editor/textView",
	"orion/keyBinding",
	"editor/textview/textStyler",
	"orion/editor/textMateStyler",
	"orion/editor/htmlGrammar",
	"orion/editor/editor",
	"orion/editor/editorFeatures",
	"orion/editor/contentAssist",
	"editor/javaContentAssist",
	"orion/editor/linkedMode",
	"editor/sha1",
	"editor/socket.io"],

function(require, mTextView, mKeyBinding, mTextStyler, mTextMateStyler, mHtmlGrammar, mEditor, mEditorFeatures, mContentAssist, mJavaContentAssist, mLinkedMode){
	var editorDomNode = document.getElementById("editor");
	
	var textViewFactory = function() {
		return new mTextView.TextView({
			parent: editorDomNode,
			tabSize: 4
		});
	};

	var contentAssist;
	var contentAssistFactory = {
		createContentAssistMode: function(editor) {
			contentAssist = new mContentAssist.ContentAssist(editor.getTextView());
			var contentAssistWidget = new mContentAssist.ContentAssistWidget(contentAssist);
			var result = new mContentAssist.ContentAssistMode(contentAssist, contentAssistWidget);
			contentAssist.setMode(result);
			return result;
		}
	};
	
	var socket = io.connect();
	var javaContentAssistProvider = new mJavaContentAssist.JavaContentAssistProvider(socket);
	javaContentAssistProvider.setSocket(socket);
	
	// Canned highlighters for js, java, and css. Grammar-based highlighter for html
	var syntaxHighlighter = {
		styler: null, 
		
		highlight: function(fileName, editor) {
			if (this.styler) {
				this.styler.destroy();
				this.styler = null;
			}
			if (fileName) {
				var splits = fileName.split(".");
				var extension = splits.pop().toLowerCase();
				var textView = editor.getTextView();
				var annotationModel = editor.getAnnotationModel();
				if (splits.length > 0) {
					switch(extension) {
						case "js":
						case "java":
						case "class":
						case "css":
							this.styler = new mTextStyler.TextStyler(textView, extension, annotationModel);
							break;
						case "html":
							this.styler = new mTextMateStyler.TextMateStyler(textView, new mHtmlGrammar.HtmlGrammar());
							break;
					}
				}
			}
		}
	};
	
	var annotationFactory = new mEditorFeatures.AnnotationFactory();
	
	var linkedMode;
	
	var keyBindingFactory = function(editor, keyModeStack, undoStack, contentAssist) {
		
		// Create keybindings for generic editing
		var genericBindings = new mEditorFeatures.TextActions(editor, undoStack);
		keyModeStack.push(genericBindings);
		
		// Linked Mode
		linkedMode = new mLinkedMode.LinkedMode(editor, undoStack, contentAssist);
		keyModeStack.push(linkedMode);

		// create keybindings for source editing
		var codeBindings = new mEditorFeatures.SourceCodeActions(editor, undoStack, contentAssist, linkedMode);
		keyModeStack.push(codeBindings);
		
		// save binding
		editor.getTextView().setKeyBinding(new mKeyBinding.KeyBinding("s", true), "save");
		editor.getTextView().setAction("save", function(){
				save(editor);
				return true;
		});
		
		editor.getTextView().setKeyBinding(new mKeyBinding.KeyBinding(114), "navigate");
		editor.getTextView().setAction("navigate", function(){
				navigate(editor);
				return true;
		});

		editor.getTextView().setKeyBinding(new mKeyBinding.KeyBinding(115), "renameinfile");
		editor.getTextView().setAction("renameinfile", function(){
				renameInFile(editor);
				return true;
		});

	};
		
	var dirtyIndicator = "";
	var status = "";
	
	var statusReporter = function(message, isError) {
		/*if (isError) {
			status =  "ERROR: " + message;
		} else {
			status = message;
		}
		document.getElementById("status").innerHTML = dirtyIndicator + status;*/
	};
	
	var editor = new mEditor.Editor({
		textViewFactory: textViewFactory,
		undoStackFactory: new mEditorFeatures.UndoFactory(),
		annotationFactory: annotationFactory,
		lineNumberRulerFactory: new mEditorFeatures.LineNumberRulerFactory(),
		contentAssistFactory: contentAssistFactory,
		keyBindingFactory: keyBindingFactory, 
		statusReporter: statusReporter,
		domNode: editorDomNode
	});
		
	editor.addEventListener("DirtyChanged", function(evt) {
		if (editor.isDirty()) {
			dirtyIndicator = "*";
			window.document.title = "*" + fileShortName;
		} else {
			dirtyIndicator = "";
			window.document.title = fileShortName;
		}
		
		// alert("Dirty changes: " + editor.__javaObject);
		// document.getElementById("status").innerHTML = dirtyIndicator + status;
	});
	
	editor.installTextView();
	
	contentAssist.addEventListener("Activating", function() {
		contentAssist.setProviders([javaContentAssistProvider]);
	});
	
	window.onbeforeunload = function() {
		if (editor.isDirty()) {
			 return "There are unsaved changes.";
		}
	};
	
	window.onhashchange = function() {
		console.log("hash changed: " + window.location.hash);
		start();
	}
	
  	socket.on('metadataChanged', function (data) {
		if (username === data.username && data.project !== undefined && data.resource !== undefined && data.metadata !== undefined && data.type === 'marker'
			&& filePath === data.project + "/" + data.resource) {
			
			var markers = [];
			for(i = 0; i < data.metadata.length; i++) {
				var lineOffset = editor.getModel().getLineStart(data.metadata[i].line - 1);
				
				console.log(lineOffset);
				
				markers[i] = {
					'description' : data.metadata[i].description,
					'line' : data.metadata[i].line,
					'severity' : data.metadata[i].severity,
					'start' : (data.metadata[i].start - lineOffset) + 1,
					'end' : data.metadata[i].end - lineOffset
				};
			}
			
			editor.showProblems(markers);
		}
  	});
	
  	socket.on('livemetadata', function (data) {
		if (username === data.username && data.resource !== undefined && data.problems !== undefined && filePath === data.resource) {
			var markers = [];
			for(i = 0; i < data.problems.length; i++) {
				var lineOffset = editor.getModel().getLineStart(data.problems[i].line - 1);
				
				console.log(lineOffset);
				
				markers[i] = {
					'description' : data.problems[i].description,
					'line' : data.problems[i].line,
					'severity' : data.problems[i].severity,
					'start' : (data.problems[i].start - lineOffset) + 1,
					'end' : data.problems[i].end - lineOffset
				};
			}
			
			editor.showProblems(markers);
		}
    	console.log(data);
  	});
	
  	socket.on('navigationresponse', function (data) {
		if (username === data.username && project === data.project && resource === data.resource && data.navigation !== undefined) {
			var navigationTarget = data.navigation;
			if (navigationTarget.project === project && navigationTarget.resource === resource) {
				var offset = navigationTarget.offset;
				var length = navigationTarget.length;
				
				editor.setSelection(offset, offset + length, true);
			}
			else {
				var baseURL = window.location.origin + window.location.pathname;
				var resourceID = navigationTarget.project + "/" + navigationTarget.resource;
				
				if (navigationTarget.offset !== undefined) {
					resourceID += '#offset=' + navigationTarget.offset;
				}
				
				if (navigationTarget.length !== undefined) {
					resourceID += '#length=' + navigationTarget.length;
				}
				
				window.location.hash = resourceID;
			}
		}
    	console.log(data);
  	});
	
  	socket.on('renameinfileresponse', function (data) {
		if (username === data.username && project === data.project && resource === data.resource && data.references !== undefined) {
			var references = data.references;
			
			var positionGroups = [];
			positionGroups.push({
				'positions' : references
			});

			var linkedModeModel = {
				groups: positionGroups,
				escapePosition: 0
			};
			linkedMode.enterLinkedMode(linkedModeModel);
		}
    	console.log(data);
  	});
	
	var username = "defaultuser";
	
	var filePath = undefined;
	var project = undefined;
	var resource = undefined;
	var fileShortName = undefined;
	
	var jumpTo = undefined;
	
	var lastSavePointContent;
	var lastSavePointHash;
	var lastSavePointTimestamp;
	
	function start() {
		lastSavePointContent = '';
		lastSavePointHash = '';
		lastSavePointTimestamp = 0;

		filePath = window.location.href.split('#')[1];
		
		if (filePath !== undefined) {
			var sections = filePath.split('/');
			var newUsername = sections[0];
			var newProject = sections[1];
			var newResource = filePath.slice(newUsername.length + newProject.length + 2);
			
			if (newUsername != username || newProject !== project || newResource !== resource) {
				username = newUsername;
				project = newProject;
				resource = newResource;
				fileShortName = sections[sections.length - 1];
				jumpTo = extractJumpToInformation(window.location.hash);

				editor.getTextView().removeEventListener("ModelChanged", sendModelChanged);
		
				socket.emit('getResourceRequest', {
					'callback_id' : 0,
					'username' : username,
					'project' : project,
					'resource' : resource
				});
			}
			else {
				jumpTo = extractJumpToInformation(window.location.hash);
				jump(jumpTo);
			}
		}
	}
	
	function extractJumpToInformation(hash) {
		var hashValues = hash.split('#');
		var offset = undefined;
		var length = undefined;
		
		for (i = 0; i < hashValues.length; i++) {
			var param = hashValues[i];
			var pieces = param.split('=');
			if (pieces.length == 2) {
				if (pieces[0] === 'offset' && !isNaN(parseInt(pieces[1]))) {
					offset = parseInt(pieces[1]);
				}
				else if (pieces[0] === 'length' && !isNaN(parseInt(pieces[1]))) {
					length = parseInt(pieces[1]);
				}
			}
		}
		
		if (offset !== undefined) {
			return {
				'offset' : offset,
				'length' : (length !== undefined ? length : 0)
			}
		}
	}
	
	function jump(selection) {
		if (selection !== undefined) {
			editor.setSelection(selection.offset, selection.offset + selection.length, true);
		}
	}
	
	start();
	
	socket.on('getResourceResponse', function(data) {
		if (lastSavePointTimestamp != 0 && lastSavePointHash !== '') {
			return;
		}
		
		if (data.username !== username) {
			return;
		}

		var text = data.content;
		
		editor.setInput(fileShortName, null, text);
		syntaxHighlighter.highlight(fileShortName, editor);
		window.document.title = fileShortName;
		
		if (data.readonly) {
			editor.getTextView().setOptions({'readonly' : true});
		}
		else {
			editor.getTextView().setOptions({'readonly' : false});
		}
		
		javaContentAssistProvider.setResourcePath(filePath);
		javaContentAssistProvider.setUsername(username);
		
		lastSavePointContent = text;
		lastSavePointHash = data.hash;
		lastSavePointTimestamp = data.timestamp;
		
		jump(jumpTo);

		socket.emit('startedediting', {
			'username' : username,
			'resource' : filePath
		})
		
		editor.getTextView().addEventListener("ModelChanged", sendModelChanged);
	});
	
	function sendModelChanged(evt) {
		var changeData = {
						'username' : username,
						'resource' : filePath,
						'offset' : evt.start,
						'removedCharCount' : evt.removedCharCount
					};
						
		if (evt.addedCharCount > 0) {
			var addedText = editor.getModel().getText(evt.start, evt.start + evt.addedCharCount);
			changeData.addedCharacters = addedText;
		}
		else {
			changeData.addedCharacters = "";
		}
		
		socket.emit('modelchanged', changeData);
	}
	
	socket.on('modelchanged', function(data) {
		if (data.username === username && data.resource === filePath) {
			var text = data.addedCharacters !== undefined ? data.addedCharacters : "";
			editor.getTextView().removeEventListener("ModelChanged", sendModelChanged);
			editor.getModel().setText(text, data.offset, data.offset + data.removedCharCount);
			editor.getTextView().addEventListener("ModelChanged", sendModelChanged);
		}
	});
	
	socket.on('getResourceRequest', function(data) {
		if (data.username === username && data.project === project && data.resource === resource && data.callback_id !== undefined) {
			
			if ((data.hash === undefined || data.hash === lastSavePointHash)
				&& data.timestamp === undefined || data.timestamp === lastSavePointTimestamp) {

				socket.emit('getResourceResponse', {
					'callback_id' 		: data.callback_id,
					'requestSenderID' 	: data.requestSenderID,
					'username' 			: data.username,
					'project' 			: project,
					'resource' 			: resource,
					'timestamp' 		: lastSavePointTimestamp,
					'hash' 				: lastSavePointHash,
					'content' 			: lastSavePointContent
				});
			}

		}
	});
	
	function save(editor) {
		setTimeout(function() {
			lastSavePointContent = editor.getText();
			
			var hash = CryptoJS.SHA1(lastSavePointContent);
			lastSavePointHash = hash.toString(CryptoJS.enc.Hex);
			lastSavePointTimestamp = Date.now();
			
			socket.emit('resourceChanged', {
				'username' : username,
				'project' : project,
				'resource' : resource,
				'timestamp' : lastSavePointTimestamp,
				'hash' : lastSavePointHash
			});
			
			// this is potentially dangerous because the editor is set to non-dirty
			// even though we don't know whether anything has been saved by someone else
			editor.setDirty(false);
		}, 0);
	}

	function navigate(editor) {
		setTimeout(function() {
			var selection = editor.getSelection();
			var offset = selection.start;
			var length = selection.end - selection.start;
			
			socket.emit('navigationrequest', {
				'username' : username,
				'project' : project,
				'resource' : resource,
				'offset' : offset,
				'length' : length,
				'callback_id' : 0
			});
		}, 0);
	}

	function renameInFile(editor) {
		setTimeout(function() {
			var selection = editor.getSelection();
			var offset = selection.start;
			var length = selection.end - selection.start;
			
			socket.emit('renameinfilerequest', {
				'username' : username,
				'project' : project,
				'resource' : resource,
				'offset' : offset,
				'length' : length,
				'callback_id' : 0
			});
		}, 0);
	}
});
