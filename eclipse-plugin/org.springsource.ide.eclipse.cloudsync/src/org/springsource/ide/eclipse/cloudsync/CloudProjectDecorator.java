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
package org.springsource.ide.eclipse.cloudsync;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.ui.IDecoratorManager;

public class CloudProjectDecorator extends LabelProvider implements ILightweightLabelDecorator {

	public static final String ID = "org.springsource.ide.eclipse.cloudsync.projectdecorator";

	public static CloudProjectDecorator getInstance() {
		IDecoratorManager decoratorManager = Activator.getDefault().getWorkbench().getDecoratorManager();
		if (decoratorManager.getEnabled(ID)) {
			return (CloudProjectDecorator) decoratorManager.getBaseLabelProvider(ID);
		}
		return null;
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof IProject && Activator.getDefault().getController().isConnected((IProject) element)) {
			decoration.addSuffix(" [flight627 connected]");
		}
	}

	@Override
	public void fireLabelProviderChanged(LabelProviderChangedEvent event) {
		super.fireLabelProviderChanged(event);
	}

}
