package org.springframework.roo.uaa;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataTimingStatistic;
import org.springframework.roo.support.osgi.BundleFindingUtils;

/**
 * Regularly polls {@link MetadataDependencyRegistry#getTimings()} and incorporates all timings into UAA
 * feature use statistics. 
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
@Component(enabled=true)
public class MetadataPollingUaaRegistrationFacility {
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private UaaRegistrationService uaaRegistrationService;
	private BundleContext bundleContext;
	private Timer timer = new Timer();
	private Set<String> previouslyNotifiedBsns = new HashSet<String>();
	
	protected void activate(ComponentContext context) {
		this.bundleContext = context.getBundleContext();
		timer.scheduleAtFixedRate(new MetadataTimerTask(), 0, 5 * 1000);
	}

	protected void deactivate(ComponentContext context) {
		timer.cancel();
	}
	
	private class MetadataTimerTask extends TimerTask {
		@Override
		public void run() {
			// Try..catch used to avoid unexpected problems terminating the timer thread
			try {
				// Deal with modules being used via the add-on infrastructure
				for (MetadataTimingStatistic stat : metadataDependencyRegistry.getTimings()) {
					String typeName = stat.getName();
					String bundleSymbolicName = BundleFindingUtils.findFirstBundleForTypeName(bundleContext, typeName);
					if (bundleSymbolicName == null) {
						continue;
					}
					
					// Only notify the UAA service if we haven't previously told it about this BSN (UAA service handles buffering internally)
					if (!previouslyNotifiedBsns.contains(bundleSymbolicName)) {
						// UaaRegistrationService deals with determining if the BSN is public (non-public BSNs are not registered)
						uaaRegistrationService.registerBundleSymbolicNameUse(bundleSymbolicName, null);
						previouslyNotifiedBsns.add(bundleSymbolicName);
					}
					
				}
			} catch (RuntimeException ignored) {}
		}
	}
}
