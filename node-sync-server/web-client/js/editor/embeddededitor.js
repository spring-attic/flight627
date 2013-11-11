/*******************************************************************************
 * @license
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * Copyright (c) 2012 VMware, Inc.
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors: 
 *     IBM Corporation - initial API and implementation
 *     Andrew Eisenberg - rename jsContentAssist to jsTemplateContentAssist
 *     Martin Lippert - flight627 prototype work
 *******************************************************************************/
/*global examples orion:true window define*/
/*jslint browser:true devel:true*/

define([
	"require", 
	"orion/textview/textView",
	"orion/textview/keyBinding",
	"editor/textview/textStyler",
	"orion/editor/textMateStyler",
	"orion/editor/htmlGrammar",
	"orion/editor/editor",
	"orion/editor/editorFeatures",
	"orion/editor/contentAssist",
	"editor/javaContentAssist",
	"editor/sha1",
	"editor/socket.io"],

function(require, mTextView, mKeyBinding, mTextStyler, mTextMateStyler, mHtmlGrammar, mEditor, mEditorFeatures, mContentAssist, mJavaContentAssist){
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
			return new mContentAssist.ContentAssistMode(contentAssist, contentAssistWidget);
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
	
	var keyBindingFactory = function(editor, keyModeStack, undoStack, contentAssist) {
		
		// Create keybindings for generic editing
		var genericBindings = new mEditorFeatures.TextActions(editor, undoStack);
		keyModeStack.push(genericBindings);
		
		// create keybindings for source editing
		var codeBindings = new mEditorFeatures.SourceCodeActions(editor, undoStack, contentAssist);
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
		} else {
			dirtyIndicator = "";
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
		if (data.project !== undefined && data.resource !== undefined && data.metadata !== undefined && data.type === 'marker'
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
		if (data.resource !== undefined && data.problems !== undefined && filePath === data.resource) {
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
		if (project === data.project && resource === data.resource && data.navigation !== undefined) {
			var navigationTarget = data.navigation;
			if (navigationTarget.project === project && navigationTarget.resource === resource) {
				var offset = navigationTarget.offset;
				var length = navigationTarget.length;
				
				editor.setSelection(offset, offset + length, true);
			}
			else {
				var baseURL = window.location.origin + window.location.pathname;
				var resourceID = navigationTarget.project + "/" + navigationTarget.resource;
				window.location.hash = resourceID;
			}
		}
    	console.log(data);
  	});
	
	var filePath = undefined;
	var project = undefined;
	var resource = undefined;
	var fileShortName = undefined;
	
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
			project = sections[0];
			resource = filePath.slice(project.length + 1);
			fileShortName = sections[sections.length - 1];
			
			editor.getTextView().removeEventListener("ModelChanged", sendModelChanged);
		
			socket.emit('getResourceRequest', {
				'callback_id' : 0,
				'project' : project,
				'resource' : resource
			});
		}
	}
	
	start();
	
	socket.on('getResourceResponse', function(data) {
		if (lastSavePointTimestamp != 0 && lastSavePointHash !== '') {
			return;
		}

		var text = data.content;
		
		editor.setInput(fileShortName, null, text);
		syntaxHighlighter.highlight(fileShortName, editor);
		window.document.title = fileShortName;
		
		javaContentAssistProvider.setResourcePath(filePath);
		
		lastSavePointContent = text;
		lastSavePointHash = data.hash;
		lastSavePointTimestamp = data.timestamp;

		socket.emit('startedediting', {'resource' : filePath})
		
		editor.getTextView().addEventListener("ModelChanged", sendModelChanged);
	});
	
	function sendModelChanged(evt) {
		var changeData = {
						'resource' : filePath,
						'start' : evt.start,
						'addedCharCount' : evt.addedCharCount,
						'addedLineCount' : evt.addedLineCount,
						'removedCharCount' : evt.removedCharCount,
						'removedLineCount' : evt.removedLineCount
						};
						
		if (evt.addedCharCount > 0) {
			var addedText = editor.getModel().getText(evt.start, evt.start + evt.addedCharCount);
			changeData.addedCharacters = addedText;
		}
		
		socket.emit('modelchanged', changeData);
	}
	
	socket.on('getResourceRequest', function(data) {
		if (data.project == project && data.resource == resource && data.callback_id !== undefined) {
			
			if ((data.hash === undefined || data.hash === lastSavePointHash)
				&& data.timestamp === undefined || data.timestamp === lastSavePointTimestamp) {

				socket.emit('getResourceResponse', {
					'callback_id' 		: data.callback_id,
					'requestSenderID' 	: data.requestSenderID,
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
				'project' : project,
				'resource' : resource,
				'timestamp' : lastSavePointTimestamp,
				'hash' : lastSavePointHash
			});
		}, 0);
	}

	function navigate(editor) {
		setTimeout(function() {
			var selection = editor.getSelection();
			var offset = selection.start;
			var length = selection.end - selection.start;
			
			socket.emit('navigationrequest', {
				'project' : project,
				'resource' : resource,
				'offset' : offset,
				'length' : length,
				'callback_id' : 0
			});
		}, 0);
	}

});
