/**
 * 
 */
package net.sf.taverna.t2.activities.interaction.jetty;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.Servlet;

import net.sf.taverna.raven.appconfig.ApplicationRuntime;
import net.sf.taverna.t2.activities.interaction.preference.InteractionPreference;
import net.sf.taverna.t2.security.credentialmanager.CMException;
import net.sf.taverna.t2.security.credentialmanager.CredentialManager;
import net.sf.taverna.t2.security.credentialmanager.UsernamePassword;
import net.sf.webdav.WebdavServlet;

import org.apache.abdera.protocol.server.ServiceManager;
import org.apache.abdera.protocol.server.provider.basic.BasicProvider;
import org.apache.abdera.protocol.server.servlet.AbderaServlet;
import org.apache.log4j.Logger;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.nio.SelectChannelConnector;
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

	private static Server server;
	
    private static InteractionPreference interactionPreference = InteractionPreference.getInstance();
    
	private static String REALM_NAME = "TavernaInteraction";


	public static synchronized void checkJetty() {
		if (server != null) {
			return;
		}

		ClassLoader previousContextClassLoader = Thread.currentThread()
				.getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				InteractionJetty.class.getClassLoader());

		String port = interactionPreference.getPort();

		server = new Server(Integer.parseInt(port));
		server.setStopAtShutdown(true);

		WebdavServlet interactionServlet = new WebdavServlet();
		
		Class current;
		try {
			current = InteractionJetty.class.getClassLoader().loadClass("javax.servlet.Servlet");
			logger.error("InteractionJetty sees " + current.hashCode());
		} catch (ClassNotFoundException e) {
			logger.error(e);
		}
		
		try {
			current = ServletHolder.class.getClassLoader().loadClass("javax.servlet.Servlet");
			logger.error("ServletHolder sees " + current.hashCode());
		} catch (ClassNotFoundException e) {
			logger.error(e);
		}
		
		try {
			current = WebdavServlet.class.getClassLoader().loadClass("javax.servlet.Servlet");
			logger.error("WebdavServlet sees " + current.hashCode());
		} catch (ClassNotFoundException e) {
			logger.error(e);
		}
		
		try {
			current = AbderaServlet.class.getClassLoader().loadClass("javax.servlet.Servlet");
			logger.error("AbderaServlet sees " + current.hashCode());
		} catch (ClassNotFoundException e) {
			logger.error(e);
		}
		

		ServletHolder interactionHolder = new ServletHolder();
		interactionHolder.setServlet(interactionServlet);

 		try {

			interactionHolder.setInitParameter("rootpath",
					getInteractionDirectory().getCanonicalPath());
		} catch (IOException e1) {
			logger.error("Unable to set root of interaction", e1);
		}

 		HandlerList handlers = new HandlerList();
 		Context overallContext = new Context(handlers, "/", Context.SESSIONS);
		overallContext.setContextPath("/");
		server.setHandler(overallContext);

		AbderaServlet abderaServlet = new AbderaServlet();
		ServletHolder abderaHolder = new ServletHolder(abderaServlet);
		abderaHolder.setInitParameter(ServiceManager.PROVIDER,
				BasicProvider.class.getName());


		overallContext.addServlet(abderaHolder, "/*");
		overallContext.addServlet(interactionHolder, "/interaction/*");

		if (interactionPreference.getUseUsername()) {
			Constraint constraint = new Constraint();
			constraint.setName(Constraint.__BASIC_AUTH);
			;
			constraint.setRoles(new String[] { "user", "admin", "moderator" });
			constraint.setAuthenticate(true);

			ConstraintMapping cm = new ConstraintMapping();
			cm.setConstraint(constraint);
			cm.setPathSpec("/*");

			SecurityHandler sh = new SecurityHandler();
			try {
				HashUserRealm realm = new HashUserRealm(REALM_NAME);
				URI serviceURI = createServiceURI(port);
				UsernamePassword up = CredentialManager
						.getInstance()
						.getUsernameAndPasswordForService(
								serviceURI, true,
								"Please specify the username and password to secure your interactions");
				if (up != null) {
					String username = up.getUsername();
					realm.put(username, up.getPasswordAsString());
					realm.addUserToRole(username, "user");
				}
				sh.setUserRealm(realm);
			} catch (CMException e) {
				logger.error(e);
			} catch (URISyntaxException e) {
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
		} catch (Exception e) {
			logger.error("Unable to start Jetty");
		}
		Thread.currentThread()
				.setContextClassLoader(previousContextClassLoader);

	}

	public static URI createServiceURI(String port) throws URISyntaxException {
		return new URI("http://localhost:" + port + "/#"
				+ REALM_NAME);
	}
	
	private static void createDirectory(String dirPath) {
		File dir = new File(dirPath);
		dir.mkdirs();
	}

	public static File getJettySubdirectory(String subdirectoryName) {
		File workingDir = ApplicationRuntime.getInstance().getApplicationHomeDir();
		File subDir = new File(workingDir, "jetty/" + subdirectoryName);
		subDir.mkdirs();
		return subDir;
	}
	
	public static File getFeedDirectory() {
		return getJettySubdirectory("feed");
	}
	
	public static File getInteractionDirectory() {
		return getJettySubdirectory("interaction");
	}

}
