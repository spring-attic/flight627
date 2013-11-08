/*******************************************************************************
 *  Copyright (c) 2013 GoPivotal, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.cloudsync.internal;

import java.util.ArrayList;
import java.util.List;

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

	private String resourcePath;
	private ICompilationUnit unit;

	public ContentAssistService(String resourcePath, ICompilationUnit unit) {
		this.resourcePath = resourcePath;
		this.unit = unit;
	}

	public String compute(int offset) {
		final List<CompletionProposal> proposals = new ArrayList<CompletionProposal>();
		
		try {
			unit.codeComplete(offset, new CompletionRequestor() {
				@Override
				public void accept(CompletionProposal proposal) {
					proposals.add(proposal);
				}
			});
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		StringBuilder result = new StringBuilder();
		boolean flag = false;
		result.append("[");
		for (CompletionProposal proposal : proposals) {
			String description = getDescription(proposal);
			if (description != null) {
				if (flag) {
					result.append(",");
				}
				
				result.append("{");
				result.append("\"proposal\"");
				result.append(":");
				result.append("\"");
				result.append(proposal.getCompletion());
				result.append("\",");
				result.append("\"description\"");
				result.append(":");
				result.append(getDescription(proposal));
				result.append(",");
				result.append("\"style\":\"attributedString\",");
				result.append("\"replace\"");
				result.append(":");
				result.append("true");
				result.append("}");
	
				flag = true;
			}
		}
		result.append("]");
		return result.toString();
	}
	
	protected String getDescription(CompletionProposal proposal) {
		StringBuilder description = new StringBuilder();
		description.append("{");
		
		if( proposal.getKind() == CompletionProposal.METHOD_REF ) {
			description.append("\"icon\":{\"src\":\"../js/editor/textview/methpub_obj.gif\"},");
			description.append("\"segments\": ");
			description.append("[");

			String sig = Signature.toString(
					new String(proposal.getSignature()), 
					new String(proposal.getName()), 
					null,
					false, false);
			
			description.append("{");
			String result = sig + " : " + Signature.getSimpleName(Signature.toString(Signature.getReturnType(new String(proposal.getSignature()))));
			description.append("\"value\":\"" +result +"\"");
			description.append("}");
			
			description.append(",");
			description.append("{");
			String appendix = " - " + Signature.getSignatureSimpleName(new String(proposal.getDeclarationSignature()));
			description.append("\"value\":\"" +appendix +"\",");
			description.append("\"style\":{");
			description.append("\"color\":\"#AAAAAA\"");
			description.append("}");
			description.append("}");

			description.append("]");

		} else if( proposal.getKind() == CompletionProposal.FIELD_REF ) {
			description.append("\"icon\":{\"src\":\"../js/editor/textview/field_public_obj.gif\"},");
			description.append("\"segments\": ");
			description.append("[");
			
			description.append("{");
			String result = new String(proposal.getCompletion()) + " : " + (proposal.getSignature() != null ? Signature.getSignatureSimpleName(new String(proposal.getSignature())) : "<unknown>");
			description.append("\"value\":\"" +result +"\"");
			description.append("}");

			description.append(",");
			description.append("{");
			String appendix = " - " +  (proposal.getDeclarationSignature() != null ? Signature.getSignatureSimpleName(new String(proposal.getDeclarationSignature())) : "<unknown>");
			description.append("\"value\":\"" +appendix +"\",");
			description.append("\"style\":{");
			description.append("\"color\":\"#AAAAAA\"");
			description.append("}");
			description.append("}");

			description.append("]");
			
		} else if( proposal.getKind() == CompletionProposal.TYPE_REF ) {
			if( proposal.getAccessibility() == IAccessRule.K_NON_ACCESSIBLE ) {
				return null;
			}

			description.append("\"icon\":{\"src\":\"../js/editor/textview/class_obj.gif\"},");
			description.append("\"segments\": ");
			description.append("[");
			
			description.append("{");
			String result = Signature.getSignatureSimpleName(new String(proposal.getSignature()));
			description.append("\"value\":\"" +result +"\"");
			description.append("}");

			description.append(",");
			description.append("{");
			String appendix = " - " + new String(proposal.getDeclarationSignature());
			description.append("\"value\":\"" +appendix +"\",");
			description.append("\"style\":{");
			description.append("\"color\":\"#AAAAAA\"");
			description.append("}");
			description.append("}");

			description.append("]");

		} else {
			return null;
		}
		
		description.append("}");
		return description.toString();
	}

}
