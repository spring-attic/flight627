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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class RenameService {

	private String resourcePath;
	private ICompilationUnit unit;

	public RenameService(String resourcePath, ICompilationUnit unit) {
		this.resourcePath = resourcePath;
		this.unit = unit;
	}

	public JSONArray compute(int offset, int length) {
		try {
			final ASTParser parser = ASTParser.newParser(AST.JLS4);

		    // Parse the class as a compilation unit.
		    parser.setKind(ASTParser.K_COMPILATION_UNIT);
		    parser.setSource(unit);
		    parser.setResolveBindings(true);

		    // Return the compiled class as a compilation unit
		    final ASTNode compilationUnit = parser.createAST(null);
			final ASTNode nameNode= NodeFinder.perform(compilationUnit, offset, length);
			
			final List<ASTNode> nodes = new ArrayList<ASTNode>();
			
			if (nameNode instanceof SimpleName) {
				compilationUnit.accept(new ASTVisitor() {
					@Override
					public boolean visit(SimpleName node) {
						if (node.getIdentifier().equals(((SimpleName) nameNode).getIdentifier())) {
							nodes.add(node);
						}
						return super.visit(node);
					}
				});
			}
			
			JSONArray references = new JSONArray();
			for (ASTNode astNode : nodes) {
				JSONObject nodeObject = new JSONObject();
				nodeObject.put("offset", astNode.getStartPosition());
				nodeObject.put("length", astNode.getLength());
				
				references.put(nodeObject);
			}
			
			return references;

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

}
