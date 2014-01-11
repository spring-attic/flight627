/*******************************************************************************
 * @license Copyright (c) 2010, 2011 IBM Corporation and others. Copyright (c)
 *          2012 VMware, Inc. Copyright (c) 2013 Pivotal Software, Inc. All
 *          rights reserved. This program and the accompanying materials are
 *          made available under the terms of the Eclipse Public License v1.0
 *          (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse
 *          Distribution License v1.0
 *          (http://www.eclipse.org/org/documents/edl-v10.html).
 * 
 * Contributors: IBM Corporation - initial API and implementation Andrew
 * Eisenberg - rename jsContentAssist to jsTemplateContentAssist Martin Lippert -
 * flight prototype work
 ******************************************************************************/
/* global examples orion:true window define */
/* jslint browser:true devel:true */

var editor_id = Math.ceil(10000000 * Math.random());

define([ "require", "orion/editor/textView", "orion/keyBinding", "editor/textview/textStyler",
		"orion/editor/textMateStyler", "orion/editor/htmlGrammar", "orion/editor/editor",
		"orion/editor/editorFeatures", "orion/editor/contentAssist", "editor/javaContentAssist",
		"orion/editor/linkedMode", "editor/sha1", "editor/socket.io" ],

function(require, mTextView, mKeyBinding, mTextStyler, mTextMateStyler, mHtmlGrammar, mEditor, mEditorFeatures,
		mContentAssist, mJavaContentAssist, mLinkedMode) {
	var eb = new vertx.EventBus('http://localhost:6271/eventbus');

	var editorDomNode = document.getElementById("editor");

	var textViewFactory = function() {
		return new mTextView.TextView({
			parent : editorDomNode,
			tabSize : 4
		});
	};

	var contentAssist;
	var contentAssistFactory = {
		createContentAssistMode : function(editor) {
			contentAssist = new mContentAssist.ContentAssist(editor.getTextView());
			var contentAssistWidget = new mContentAssist.ContentAssistWidget(contentAssist);
			var result = new mContentAssist.ContentAssistMode(contentAssist, contentAssistWidget);
			contentAssist.setMode(result);
			return result;
		}
	};

	 var javaContentAssistProvider = new mJavaContentAssist.JavaContentAssistProvider();
	//	
	// Canned highlighters for js, java, and css. Grammar-based highlighter
	// for
	// html
	var syntaxHighlighter = {
		styler : null,

		highlight : function(fileName, editor) {
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
					switch (extension) {
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
		editor.getTextView().setAction("save", function() {
			save(editor);
			return true;
		});

		editor.getTextView().setKeyBinding(new mKeyBinding.KeyBinding(114), "navigate");
		editor.getTextView().setAction("navigate", function() {
			navigate(editor);
			return true;
		});

		editor.getTextView().setKeyBinding(new mKeyBinding.KeyBinding(115), "renameinfile");
		editor.getTextView().setAction("renameinfile", function() {
			renameInFile(editor);
			return true;
		});

	};

	var dirtyIndicator = "";
	var status = "";

	var statusReporter = function(message, isError) {
		/*
		 * if (isError) { status = "ERROR: " + message; } else { status =
		 * message; } document.getElementById("status").innerHTML =
		 * dirtyIndicator + status;
		 */
	};

	var editor = new mEditor.Editor({
		textViewFactory : textViewFactory,
		undoStackFactory : new mEditorFeatures.UndoFactory(),
		annotationFactory : annotationFactory,
		lineNumberRulerFactory : new mEditorFeatures.LineNumberRulerFactory(),
		contentAssistFactory : contentAssistFactory,
		keyBindingFactory : keyBindingFactory,
		statusReporter : statusReporter,
		domNode : editorDomNode
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
		// document.getElementById("status").innerHTML = dirtyIndicator +
		// status;
	});

	editor.installTextView();

	contentAssist.addEventListener("Activating", function() {
		contentAssist.setProviders([ javaContentAssistProvider ]);
	});

	window.onbeforeunload = function() {
		if (editor.isDirty()) {
			return "There are unsaved changes.";
		}
	};

	window.onhashchange = function() {
		console.log("hash changed: " + window.location.hash);
		start();
	};

	// socket.on('liveMetadataChanged', function (data) {
	// if (username === data.username && project === data.project && resource
	// ===
	// data.resource && data.problems !== undefined) {
	// var markers = [];
	// for(i = 0; i < data.problems.length; i++) {
	// var lineOffset = editor.getModel().getLineStart(data.problems[i].line -
	// 1);
	//				
	// console.log(lineOffset);
	//				
	// markers[i] = {
	// 'description' : data.problems[i].description,
	// 'line' : data.problems[i].line,
	// 'severity' : data.problems[i].severity,
	// 'start' : (data.problems[i].start - lineOffset) + 1,
	// 'end' : data.problems[i].end - lineOffset
	// };
	// }
	//			
	// editor.showProblems(markers);
	// }
	// console.log(data);
	// });
	//	
	// socket.on('navigationresponse', function (data) {
	// if (username === data.username && project === data.project && resource
	// ===
	// data.resource && data.navigation !== undefined) {
	// var navigationTarget = data.navigation;
	// if (navigationTarget.project === project && navigationTarget.resource ===
	// resource) {
	// var offset = navigationTarget.offset;
	// var length = navigationTarget.length;
	//				
	// editor.setSelection(offset, offset + length, true);
	// }
	// else {
	// var baseURL = window.location.origin + window.location.pathname;
	// var resourceID = data.username + "/" + navigationTarget.project + "/" +
	// navigationTarget.resource;
	//				
	// if (navigationTarget.offset !== undefined) {
	// resourceID += '#offset=' + navigationTarget.offset;
	// }
	//				
	// if (navigationTarget.length !== undefined) {
	// resourceID += '#length=' + navigationTarget.length;
	// }
	//				
	// window.location.hash = resourceID;
	// }
	// }
	// console.log(data);
	// });
	//	
	// socket.on('renameinfileresponse', function (data) {
	// if (username === data.username && project === data.project && resource
	// ===
	// data.resource && data.references !== undefined) {
	// var references = data.references;
	//			
	// var positionGroups = [];
	// positionGroups.push({
	// 'positions' : references
	// });
	//
	// var linkedModeModel = {
	// groups: positionGroups,
	// escapePosition: 0
	// };
	// linkedMode.enterLinkedMode(linkedModeModel);
	// }
	// console.log(data);
	// });

	var username = "defaultuser";

	var filePath = undefined;
	var project = undefined;
	var resource = undefined;
	var fileShortName = undefined;

	var jumpTo = undefined;

	var lastSavePointContent = '';
	var lastSavePointHash = '';
	var lastSavePointTimestamp = 0;

	function start() {
		 javaContentAssistProvider.setEventBus(eb);

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

				lastSavePointContent = '';
				lastSavePointHash = '';
				lastSavePointTimestamp = 0;

				var request = {
					kind : 'request',
					action : 'resource.get',
					senderId : editor_id,
					contents : {
						'class' : 'org.eclipse.flight.objects.Resource',
						'username' : username,
						'projectName' : project,
						'path' : resource,
						'timestamp' : 0,
						'hash' : '123'
					}
				};

				eb.send('flight.resourceProvider', request, function(reply) {
					var data = JSON.parse(JSON.stringify(reply)).contents;

					if (lastSavePointTimestamp != 0 && lastSavePointHash !== '') {
						return;
					}
					if (data.username !== username) {
						return;
					}

					var text = data.data;

					editor.setInput(fileShortName, null, text);
					syntaxHighlighter.highlight(fileShortName, editor);
					window.document.title = fileShortName;

					if (data.readonly) {
						editor.getTextView().setOptions({
							'readonly' : true
						});
					} else {
						editor.getTextView().setOptions({
							'readonly' : false
						});
					}

					 javaContentAssistProvider.setProject(project);
					 javaContentAssistProvider.setResourcePath(resource);
					 javaContentAssistProvider.setUsername(username);

					lastSavePointContent = text;
					lastSavePointHash = data.hash;
					lastSavePointTimestamp = data.timestamp;

					jump(jumpTo);

					var notification = {
						kind : 'notification',
						action : 'live.resource.started',
						senderId : editor_id,
						contents : {
							"class" : 'org.eclipse.flight.objects.Edit',
							'username' : username,
							'projectName' : project,
							'path' : resource,
							'hash' : lastSavePointHash,
							'offset' : -1,
							'removeCount' : 0,
							'editType' : '',
							'savePointTimestamp' : lastSavePointTimestamp,
							'savePointHash' : lastSavePointHash,
							'timestamp' : lastSavePointTimestamp
						}
					};
					eb.publish('flight.editParticipant', notification);

					editor.getTextView().addEventListener("ModelChanged", sendModelChanged);
				});

				eb.registerHandler('flight.editParticipant', function(message, replier) {
					var msg = JSON.parse(JSON.stringify(message));
					var data = msg.contents;
					// console.log(msg.action);
					// console.log(data);
					if (data.username !== username || project !== data.projectName || data.path !== resource
							|| msg.senderId == editor_id) {
						return;
					}
					if (msg.action == "live.resource.startedResponse") {
						if (lastSavePointTimestamp === data.savePointTimestamp
								&& lastSavePointHash === data.savePointHash) {
							var currentEditorContent = editor.getText();
							var currentEditorContentHash = CryptoJS.SHA1(currentEditorContent).toString(
									CryptoJS.enc.Hex);

							if (currentEditorContentHash === data.savePointHash) {
								editor.getTextView().removeEventListener("ModelChanged", sendModelChanged);
								editor.getModel().setText(data.liveContent);
								editor.getTextView().addEventListener("ModelChanged", sendModelChanged);
							}
						}
					} else if (msg.action == "live.resource.started") {
						if ((data.hash === undefined || data.hash === lastSavePointHash)
								&& data.timestamp === undefined || data.timestamp === lastSavePointTimestamp) {

							eb.publish('flight.editParticipant', {
								kind : 'notification',
								action : 'live.resource.started',
								senderId : editor_id,
								contents : {
									'username' : data.username,
									'class' : 'org.eclipse.flight.objects.Edit',
									'projectName' : data.project,
									'path' : data.resource,
									'timestamp' : data.timestamp,
									'offset' : -1,
									'removeCount' : 0,
									'editType' : '',
									'savePointTimestamp' : lastSavePointTimestamp,
									'savePointHash' : lastSavePointHash,
									'data' : editor.getText()
								}
							});
						}
					} else if (msg.action == "live.resource.changed") {
						var text = data.data !== undefined ? data.data : "";
						editor.getTextView().removeEventListener("ModelChanged", sendModelChanged);
						editor.getModel().setText(text, data.offset, data.offset + data.removeCount);
						editor.getTextView().addEventListener("ModelChanged", sendModelChanged);
					} else if (msg.action == "live.metadata.changed") {
						if (data.markers) {
							var markers = [];
							for (i = 0; i < data.markers.length; i++) {
								var lineOffset = editor.getModel().getLineStart(data.markers[i].line - 1);
								markers[i] = {
									'description' : data.markers[i].description,
									'line' : data.markers[i].sourceLine,
									'severity' : data.markers[i].severity,
									'start' : (data.markers[i].start - lineOffset) + 1,
									'end' : data.markers[i].end - lineOffset
								};
							}

							editor.showProblems(markers);
						}
					}
				});
			} else {
				jumpTo = extractJumpToInformation(window.location.hash);
				jump(jumpTo);
			}
		}
	}
	// socket.on('liveMetadataChanged', function (data) {
	// if (username === data.username && project === data.project && resource
	// ===
	// data.resource && data.problems !== undefined) {
	// var markers = [];
	// for(i = 0; i < data.problems.length; i++) {
	// var lineOffset = editor.getModel().getLineStart(data.problems[i].line -
	// 1);
	//				
	// console.log(lineOffset);
	//				
	// markers[i] = {
	// 'description' : data.problems[i].description,
	// 'line' : data.problems[i].line,
	// 'severity' : data.problems[i].severity,
	// 'start' : (data.problems[i].start - lineOffset) + 1,
	// 'end' : data.problems[i].end - lineOffset
	// };
	// }
	//			
	// editor.showProblems(markers);
	// }
	// console.log(data);
	// });
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
				} else if (pieces[0] === 'length' && !isNaN(parseInt(pieces[1]))) {
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
	eb.onopen = function() {
		start();
	};

	function sendModelChanged(evt) {
		var changeData = {
			kind : 'notification',
			action : 'live.resource.changed',
			senderId : editor_id,
			contents : {
				'username' : username,
				'projectName' : project,
				'class' : 'org.eclipse.flight.objects.Resource',
				'path' : resource,
				'offset' : evt.start,
				'timestamp' : lastSavePointTimestamp,
				'type' : 'file',
				'editType' : '',
				'savePointTimestamp' : lastSavePointTimestamp,
				'savePointHash' : lastSavePointHash,
				'removeCount' : evt.removedCharCount
			}
		};

		if (evt.addedCharCount > 0) {
			changeData.contents.data = editor.getModel().getText(evt.start, evt.start + evt.addedCharCount);
		} else {
			changeData.contents.data = "";
		}

		eb.publish('flight.editParticipant', changeData);
	}

	//	
	// socket.on('getResourceRequest', function(data) {
	// if (data.username === username && data.project === project &&
	// data.resource
	// === resource && data.callback_id !== undefined) {
	//			
	// if ((data.hash === undefined || data.hash === lastSavePointHash)
	// && data.timestamp === undefined || data.timestamp ===
	// lastSavePointTimestamp)
	// {
	//
	// socket.emit('getResourceResponse', {
	// 'callback_id' : data.callback_id,
	// 'requestSenderID' : data.requestSenderID,
	// 'username' : data.username,
	// 'projectName' : project,
	// 'path' : resource,
	// 'timestamp' : lastSavePointTimestamp,
	// 'hash' : lastSavePointHash,
	// 'content' : lastSavePointContent
	// });
	// }
	//
	// }
	// });
	//	
	// socket.on('resourceChanged', function(data) {
	// if (data.username === username && data.project === project &&
	// data.resource
	// === resource) {
	//			
	// var currentEditorContent = editor.getText();
	// var currentEditorContentHash =
	// CryptoJS.SHA1(currentEditorContent).toString(CryptoJS.enc.Hex);
	//			
	// if (data.hash === currentEditorContentHash) {
	// lastSavePointContent = currentEditorContent;
	// lastSavePointHash = data.hash;
	// lastSavePointTimestamp = data.timestamp;
	// editor.setDirty(false);
	// }
	// }
	// });

	function save(editor) {
		setTimeout(function() {
			lastSavePointContent = editor.getText();
			lastSavePointHash = CryptoJS.SHA1(lastSavePointContent).toString(CryptoJS.enc.Hex);
			lastSavePointTimestamp = Date.now();

			socket.emit('resourceChanged', {
				'username' : username,
				'projectName' : project,
				'path' : resource,
				'timestamp' : lastSavePointTimestamp,
				'hash' : lastSavePointHash
			});

			// this is potentially dangerous because the editor is set to
			// non-dirty
			// even though we don't know whether anything has been saved by
			// someone else
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
				'projectName' : project,
				'path' : resource,
				'offset' : offset,
				'length' : length
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
				'projectName' : project,
				'path' : resource,
				'offset' : offset,
				'length' : length,
				'callback_id' : 0
			});
		}, 0);
	}
});
