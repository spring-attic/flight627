/*******************************************************************************
 * @license
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors: 
 *     IBM Corporation - initial API and implementation
 *     Andrew Eisenberg - rename jsContentAssist to jsTemplateContentAssist
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
	"orion/editor/javaContentAssist",
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
	
	var javaContentAssistProvider = new mJavaContentAssist.JavaContentAssistProvider();
	
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

	function save(editor) {
		setTimeout(function() {
			xhr.open("PUT", "/api/" + filePath, true);
			xhr.onreadystatechange = function() {
				if (xhr.readyState == 4) {
			        if (xhr.status==200) {
						var response = xhr.responseText;
			        } else {
						window.alert("Error during save.");
			        }
			    }
			}
			xhr.send(editor.getText());
		}, 0);
	}
	
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
				if( editor.__javaObject ) {
					editor.__javaObject._js_Action("Save");
				}
				return true;
		});
		
		// speaking of save...
		// document.getElementById("save").onclick = function() {save(editor);};

	};
		
	var dirtyIndicator = "";
	var status = "";
	
	var statusReporter = function(message, isError) {
		if( editor.__javaObject ) {
			editor.__javaObject._js_StatusReporter(message, isError);
		}
		
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
		
		if( editor.__javaObject ) {
			editor.__javaObject._js_Event("DirtyChanged", "Event", evt);
		}
		
		// alert("Dirty changes: " + editor.__javaObject);
		// document.getElementById("status").innerHTML = dirtyIndicator + status;
	});
	
	editor.installTextView();
	
	editor.getTextView().addEventListener("Modify", function(evt) {
		if( editor.getTextView().__javaObject ) {
			editor.getTextView().__javaObject._js_Event("Modify","orion.textview.ModifyEvent",evt);
		}
	});
	editor.getTextView().addEventListener("ModelChanged", function(evt) {
		if( editor.getTextView().__javaObject ) {
			editor.getTextView().__javaObject._js_Event("ModelChanged","orion.textview.ModelChangedEvent",evt);
		}
	});
	
	// if there is a mechanism to change which file is being viewed, this code would be run each time it changed.
	var contentName = "sample.java";  // for example, a file name, something the user recognizes as the content.
	var initialContent = "window.alert('this is some javascript code');  // try pasting in some real code";
	editor.setInput(contentName, null, initialContent);
	syntaxHighlighter.highlight(contentName, editor);
	contentAssist.addEventListener("Activating", function() {
		contentAssist.setProviders([javaContentAssistProvider]);
	});
	// end of code to run when content changes.
	
	if( window.javaEditor ) {
	  window.EditorImpl = editor;
	  window.TextViewImpl = editor.getTextView();
	  window.javaEditor.initJava(editor);
	  window.javaContentAssist.initJava(javaContentAssistProvider);
	}
	
	window.onbeforeunload = function() {
		if (editor.isDirty()) {
			 return "There are unsaved changes.";
		}
	};
	
	var xhr = new XMLHttpRequest();

	var filePath = window.location.href.split('#')[1];
	if (filePath !== undefined) {
		xhr.open("GET", "/api/" + filePath, true);
		xhr.onreadystatechange = function() {
			if (xhr.readyState == 4) {
		        if (xhr.status==200) {
					var response = xhr.responseText;
					editor.setInput("HomeController.java", null, response);
		        } else {
					editor.setInput("Error", null, xhr.status);
		        }
		    }
		}
		xhr.send();
	}
	
	var socket = io.connect('http://localhost');
  	socket.on('metadataupdate', function (data) {
    	console.log(data);
		if (data.project !== undefined && data.resource !== undefined && data.type !== undefined
			&& filePath === data.project + "/" + data.resource) {
			
			var markers = [];
			for(i = 0; i < data.type.length; i++) {
				var lineOffset = editor.getModel().getLineStart(data.type[i].line - 1);
				
				console.log(lineOffset);
				
				markers[i] = {
					'description' : data.type[i].description,
					'line' : data.type[i].line,
					'severity' : data.type[i].severity,
					'start' : (data.type[i].start - lineOffset) + 1,
					'end' : data.type[i].end - lineOffset
				};
			}
			
			editor.showProblems(markers);
		}
  	});
	
});
