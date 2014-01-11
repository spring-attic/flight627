/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.flight.jdt.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.flight.Ids;
import org.eclipse.flight.objects.FlightObject;
import org.eclipse.flight.objects.services.ContentAssist;
import org.eclipse.flight.objects.services.ContentAssist.Proposal.Description;
import org.eclipse.flight.vertx.Responder;
import org.eclipse.flight.vertx.VertxManager;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

/**
 * @author Martin Lippert
 */
public class ContentAssistService {

	private LiveEditUnits liveEditUnits;

	public ContentAssistService(LiveEditUnits liveEditUnits) {
		this.liveEditUnits = liveEditUnits;

		VertxManager.get().register(new Responder(Ids.CONTENT_ASSSIST_SERVICE, Ids.CONTENT_ASSIST_REQUEST) {
			@Override
			public FlightObject respond(FlightObject request) {
				handleContentAssistRequest((ContentAssist) request);
				return request;
			}
		});
//		IMessageHandler contentAssistRequestHandler = new AbstractMessageHandler("contentassistrequest") {
//			@Override
//			public void handleMessage(String messageType, JSONObject message) {
//				handleContentAssistRequest(message);
//			}
//		};
//		messagingConnector.addMessageHandler(contentAssistRequestHandler);
	}

	protected void handleContentAssistRequest(ContentAssist request) {
		final List<CompletionProposal> proposals = new ArrayList<CompletionProposal>();

		try {
			ICompilationUnit liveEditUnit = liveEditUnits.getLiveEditUnit(request.getUserName(), request.getFullPath());
			if (liveEditUnit != null) {
				liveEditUnit.codeComplete(request.getOffset(), new CompletionRequestor() {
					@Override
					public void accept(CompletionProposal proposal) {
						proposals.add(proposal);
					}
				});
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		Collections.sort(proposals, new Comparator<CompletionProposal>() {
			@Override
			public int compare(CompletionProposal o1, CompletionProposal o2) {
				return o2.getRelevance() - o1.getRelevance();
			}
		});

		for (CompletionProposal eclipseProposal : proposals) {
			ContentAssist.Proposal.Description description = computeDescription(request, eclipseProposal);
			String completion = getCompletion(eclipseProposal, request.getPrefix());

			if (description != null) {
				ContentAssist.Proposal proposal = new ContentAssist.Proposal();
				request.getProposals().add(proposal);
				proposal.setProposal(completion);
				proposal.setStyle("attributedString");
				proposal.setReplace(true);
				computePositions(request, eclipseProposal, proposal);
				proposal.setDescription(description);
			}
		}
	}

	private void computePositions(ContentAssist request, CompletionProposal eclipseProposal,
			ContentAssist.Proposal proposal) {
		if (eclipseProposal.getKind() == CompletionProposal.METHOD_REF) {
			String completion = new String(eclipseProposal.getCompletion());
			if (completion.startsWith(request.getPrefix())) {
				completion = completion.substring(request.getPrefix().length());
			}

			char[][] parameterNames = eclipseProposal.findParameterNames(null);
			if (parameterNames != null && parameterNames.length > 0 && completion.endsWith(")")) {
				int offset = request.getOffset();
				offset += completion.length() - 1;

				for (int i = 0; i < parameterNames.length; i++) {
					ContentAssist.Proposal.Position position = new ContentAssist.Proposal.Position();
					proposal.getPositions().add(position);
					position.setOffset(offset);
					position.setLength(parameterNames[i].length);
					offset += parameterNames[i].length;
					offset += ", ".length();
				}
			}
		}
	}

	private String getCompletion(CompletionProposal proposal, String prefix) {
		String completion = new String(proposal.getCompletion());
		if (completion.startsWith(prefix)) {
			completion = completion.substring(prefix.length());
		}

		if (proposal.getKind() == CompletionProposal.METHOD_REF) {
			char[][] parameterNames = proposal.findParameterNames(null);
			if (parameterNames != null && parameterNames.length > 0 && completion.endsWith(")")) {
				completion = completion.substring(0, completion.length() - 1);
				for (int i = 0; i < parameterNames.length; i++) {
					if (i > 0) {
						completion += ", ";
					}
					completion += new String(parameterNames[i]);
				}
				completion += ")";
			}
		}

		return completion;
	}

	protected ContentAssist.Proposal.Description computeDescription(ContentAssist request, CompletionProposal eclipseProposal) {
		Description description = new Description();
		if (eclipseProposal.getKind() == CompletionProposal.METHOD_REF) {
			ContentAssist.Proposal.Description.Icon icon = new ContentAssist.Proposal.Description.Icon();
			icon.setSrc("../js/editor/textview/methpub_obj.gif");

			char[][] parameterNames = eclipseProposal.findParameterNames(null);
			String[] parameters = new String[parameterNames.length];
			for (int i = 0; i < parameterNames.length; i++) {
				parameters[i] = new String(parameterNames[i]);
			}

			ContentAssist.Proposal.Description.Segment segment1 = new ContentAssist.Proposal.Description.Segment();
			description.getSegments().add(segment1);
			String sig = Signature.toString(new String(eclipseProposal.getSignature()), new String(eclipseProposal.getName()),
					parameters, false, false);
			String result = sig
					+ " : "
					+ Signature.getSimpleName(Signature.toString(Signature.getReturnType(new String(
							eclipseProposal.getSignature()))));
			segment1.setValue(result);
			
			ContentAssist.Proposal.Description.Segment segment2 = new ContentAssist.Proposal.Description.Segment();
			description.getSegments().add(segment2);
			String appendix = " - " + Signature.getSignatureSimpleName(new String(eclipseProposal.getDeclarationSignature()));
			segment2.setValue(appendix);
			ContentAssist.Proposal.Description.Segment.Style style = new ContentAssist.Proposal.Description.Segment.Style();
			style.setColor("#AAAAAA");
			segment2.setStyle(style);
			
		} else if (eclipseProposal.getKind() == CompletionProposal.FIELD_REF) {
			ContentAssist.Proposal.Description.Icon icon = new ContentAssist.Proposal.Description.Icon();
			icon.setSrc("../js/editor/textview/field_public_obj.gif");
			
			ContentAssist.Proposal.Description.Segment segment1 = new ContentAssist.Proposal.Description.Segment();
			description.getSegments().add(segment1);
			String result = new String(eclipseProposal.getCompletion())
					+ " : "
					+ (eclipseProposal.getSignature() != null ? Signature.getSignatureSimpleName(new String(
							eclipseProposal.getSignature())) : "<unknown>");
			segment1.setValue(result);
			
			ContentAssist.Proposal.Description.Segment segment2 = new ContentAssist.Proposal.Description.Segment();
			description.getSegments().add(segment2);
			String appendix = " - "
					+ (eclipseProposal.getDeclarationSignature() != null ? Signature.getSignatureSimpleName(new String(
							eclipseProposal.getDeclarationSignature())) : "<unknown>");
			segment1.setValue(appendix);
			ContentAssist.Proposal.Description.Segment.Style style = new ContentAssist.Proposal.Description.Segment.Style();
			style.setColor("#AAAAAA");
			segment2.setStyle(style);
		} else if (eclipseProposal.getKind() == CompletionProposal.TYPE_REF) {
			if (eclipseProposal.getAccessibility() == IAccessRule.K_NON_ACCESSIBLE) {
				return null;
			}
			ContentAssist.Proposal.Description.Icon icon = new ContentAssist.Proposal.Description.Icon();
			icon.setSrc("../js/editor/textview/class_obj.gif");
			
			ContentAssist.Proposal.Description.Segment segment1 = new ContentAssist.Proposal.Description.Segment();
			description.getSegments().add(segment1);
			String result = Signature.getSignatureSimpleName(new String(eclipseProposal.getSignature()));
			segment1.setValue(result);
			
			ContentAssist.Proposal.Description.Segment segment2 = new ContentAssist.Proposal.Description.Segment();
			description.getSegments().add(segment2);
			String appendix = " - " + new String(eclipseProposal.getDeclarationSignature());
			segment1.setValue(appendix);
			ContentAssist.Proposal.Description.Segment.Style style = new ContentAssist.Proposal.Description.Segment.Style();
			style.setColor("#AAAAAA");
			segment2.setStyle(style);
		} else {
			return null;
		}
		return description;
	}

}
