/**
 *
 */
package net.sf.taverna.t2.activities.interaction.jetty;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import net.sf.taverna.t2.activities.interaction.FeedReader;
import net.sf.taverna.t2.activities.interaction.InteractionUtils;
import net.sf.taverna.t2.activities.interaction.ResponseFeedListener;
import net.sf.taverna.t2.activities.interaction.feed.ShowRequestFeedListener;
import net.sf.taverna.t2.activities.interaction.preference.InteractionPreference;
import net.sf.taverna.t2.security.credentialmanager.CMException;
import net.sf.taverna.t2.security.credentialmanager.CredentialManager;
import net.sf.taverna.t2.security.credentialmanager.UsernamePassword;
//import net.sf.taverna.t2.spi.SPIRegistry;
import net.sf.webdav.WebdavServlet;

import org.apache.abdera.protocol.server.ServiceManager;
import org.apache.abdera.protocol.server.provider.basic.BasicProvider;
import org.apache.abdera.protocol.server.servlet.AbderaServlet;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * @author alanrw
 * 
 */
public class InteractionJetty {

	private static Logger logger = Logger.getLogger(InteractionJetty.class);
	
	private InteractionUtils interactionUtils;
	
	private ShowRequestFeedListener showRequestFeedListener;
	private ResponseFeedListener responseFeedListener;

	private InteractionPreference interactionPreference;

	private static Server server;

	private static String REALM_NAME = "TavernaInteraction";
	
	private static boolean listenersStarted = false;

	public synchronized void startJettyIfNecessary(CredentialManager credentialManager) {
		if (server != null) {
			return;
		}
		
//		final ClassLoader previousContextClassLoader = Thread.currentThread()
//				.getContextClassLoader();
//		Thread.currentThread().setContextClassLoader(
//				InteractionJetty.class.getClassLoader());

		final String port = interactionPreference.getPort();

		server = new Server(Integer.parseInt(port));
		server.setStopAtShutdown(true);

		final WebdavServlet interactionServlet = new WebdavServlet();

		final ServletHolder interactionHolder = new ServletHolder();
		interactionHolder.setServlet(interactionServlet);

		try {

			interactionHolder.setInitParameter("rootpath",
					getInteractionDirectory().getCanonicalPath());
		} catch (final IOException e1) {
			logger.error("Unable to set root of interaction", e1);
		}

		final HandlerList handlers = new HandlerList();
		final Context overallContext = new Context(handlers, "/",
				Context.SESSIONS);
		overallContext.setContextPath("/");
		server.setHandler(overallContext);

		final AbderaServlet abderaServlet = new AbderaServlet();
		final ServletHolder abderaHolder = new ServletHolder(abderaServlet);
		abderaHolder.setInitParameter(ServiceManager.PROVIDER,
				BasicProvider.class.getName());

		overallContext.addServlet(abderaHolder, "/*");
		overallContext.addServlet(interactionHolder, "/interaction/*");

		if (interactionPreference.getUseUsername()) {
			final Constraint constraint = new Constraint();
			constraint.setName(Constraint.__BASIC_AUTH);

			constraint.setRoles(new String[] { "user", "admin", "moderator" });
			constraint.setAuthenticate(true);

			final ConstraintMapping cm = new ConstraintMapping();
			cm.setConstraint(constraint);
			cm.setPathSpec("/*");

			final SecurityHandler sh = new SecurityHandler();
			try {
				final HashUserRealm realm = new HashUserRealm(REALM_NAME);
				final URI serviceURI = createServiceURI(port);
				final UsernamePassword up = credentialManager
						.getUsernameAndPasswordForService(serviceURI, true,
								"Please specify the username and password to secure your interactions");
				if (up != null) {
					final String username = up.getUsername();
					realm.put(username, up.getPasswordAsString());
					realm.addUserToRole(username, "user");
				}
				sh.setUserRealm(realm);
			} catch (final CMException e) {
				logger.error(e);
			} catch (final URISyntaxException e) {
				logger.error(e);
			}
			sh.setConstraintMappings(new ConstraintMapping[] { cm });
			overallContext.addHandler(sh);

		}

		getFeedDirectory();

		try {
			server.start();
			while (!server.isRunning()) {
				Thread.sleep(5000);
			}
		} catch (final Exception e) {
			logger.error("Unable to start Jetty");
		}
//		Thread.currentThread()
//				.setContextClassLoader(previousContextClassLoader);
	}

	public static URI createServiceURI(final String port)
			throws URISyntaxException {
		return new URI("http://localhost:" + port + "/#" + REALM_NAME);
	}

	public File getJettySubdirectory(final String subdirectoryName) {
		final File workingDir = interactionUtils
				.getInteractionServiceDirectory();
		final File subDir = new File(workingDir, "jetty/" + subdirectoryName);
		subDir.mkdirs();
		return subDir;
	}

	public File getFeedDirectory() {
		return getJettySubdirectory("feed");
	}

	public File getInteractionDirectory() {
		return getJettySubdirectory("interaction");
	}
	
	public synchronized void startListenersIfNecessary() {
		if (listenersStarted) {
			return;
		}
		listenersStarted = true;
		startListener(this.responseFeedListener);
		startListener(showRequestFeedListener);

	}

	private void startListener(FeedReader fr) {
		try {
			fr.start();
		}
		catch (Exception e) {
			logger.error("Failed to start " + fr.getClass().getCanonicalName(), e);
		}
	}
	
	public void setInteractionUtils(InteractionUtils interactionUtils) {
		this.interactionUtils = interactionUtils;
	}

	public void setShowRequestFeedListener(
			ShowRequestFeedListener showRequestFeedListener) {
		this.showRequestFeedListener = showRequestFeedListener;
	}

	public void setResponseFeedListener(ResponseFeedListener responseFeedListener) {
		this.responseFeedListener = responseFeedListener;
	}

	public void setInteractionPreference(InteractionPreference interactionPreference) {
		this.interactionPreference = interactionPreference;
	}

}
