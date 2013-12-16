package org.eclipse.flight.jdt.services;

import org.eclipse.flight.core.IMessagingConnector;
import org.eclipse.flight.core.Repository;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		IMessagingConnector messagingConnector = org.eclipse.flight.core.Activator.getDefault().getMessagingConnector();
		Repository repository = org.eclipse.flight.core.Activator.getDefault().getRepository();
		
		LiveEditUnits liveEditUnits = new LiveEditUnits(messagingConnector, repository);
		new ContentAssistService(messagingConnector, liveEditUnits);
		new NavigationService(messagingConnector, liveEditUnits);
		new RenameService(messagingConnector, liveEditUnits);
		
		if (Boolean.getBoolean("flight-initjdt")) {
			InitializeServiceEnvironment initializer = new InitializeServiceEnvironment(messagingConnector, repository);
			initializer.start();
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
