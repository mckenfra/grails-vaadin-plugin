package org.grails.plugin.vaadin.ui

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.UriFragmentUtility;

/**
 * A special {@link com.vaadin.ui.UriFragmentUtility} that forces
 * the browser to send the initial fragment only when the application first
 * starts. Thereafter, it behaves exactly like the UriFragmentUtility.
 * <p>
 * Note that UriFragmentUtility will not send a fragment changed
 * event if the server-side fragment is the same as the browser-side fragment.
 * This means that we don't get any fragment information when the app first
 * starts. This class is designed to get around that by forcing the browser
 * to send its fragment when the app first loads.
 * 
 * @author Francis McKenzie
 */
class StartupUriFragmentUtility extends UriFragmentUtility {
	/**
	 * Used to control when to send the garbage fragment to the client to force
	 * the new fragment to be sent.
	 * <p>
	 * Set to true after the first call to {@link #paintContent(PaintTarget)}.
	 */
	boolean firstPaintDone = false
	
	/**
	 * Forces utility to send fragment on next paint.
	 */
	public void restart() {
		firstPaintDone = false
	}
	
	/**
	 * Send a garbage fragment on first 'paint', to force the browser
	 * to send us the true fragment.
	 */
	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target)
		if (!firstPaintDone) {
			target.addVariable(this, "fragment", "_FORCE_SEND_FRAGMENT_");
			firstPaintDone = true
		}
	}
}
