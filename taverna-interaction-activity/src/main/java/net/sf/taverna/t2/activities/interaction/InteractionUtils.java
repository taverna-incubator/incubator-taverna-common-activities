/**
 * 
 */
package net.sf.taverna.t2.activities.interaction;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// import net.sf.taverna.raven.appconfig.ApplicationRuntime;
import net.sf.taverna.t2.activities.interaction.preference.InteractionPreference;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.org.taverna.configuration.app.ApplicationConfiguration;

/**
 * @author alanrw
 * 
 */
public class InteractionUtils {

	static final Set<String> publishedUrls = Collections
			.synchronizedSet(new HashSet<String>());
	
	private ApplicationConfiguration appConfig;
	
	private InteractionRecorder interactionRecorder;

	private InteractionPreference interactionPreference;

	private InteractionUtils() {
		super();
	}

	protected void copyFixedFile(final String fixedFileName)
			throws IOException {
		final String targetUrl = interactionPreference
				.getLocationUrl() + "/" + fixedFileName;
		this.publishFile(
				targetUrl,
				InteractionActivity.class.getResourceAsStream("/"
						+ fixedFileName), null, null);
	}

	public void publishFile(final String urlString,
			final String contents, final String runId,
			final String interactionId) throws IOException {
		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				contents.getBytes("UTF-8"));
		this.publishFile(urlString, byteArrayInputStream, runId,
				interactionId);
	}

	void publishFile(final String urlString, final InputStream is,
			final String runId, final String interactionId) throws IOException {
		if (InteractionUtils.publishedUrls.contains(urlString)) {
			return;
		}
		InteractionUtils.publishedUrls.add(urlString);
		if (runId != null) {
			interactionRecorder.addResource(runId, interactionId, urlString);
		}

		final URL url = new URL(urlString);
		final HttpURLConnection httpCon = (HttpURLConnection) url
				.openConnection();
		httpCon.setDoOutput(true);
		httpCon.setRequestMethod("PUT");
		final OutputStream outputStream = httpCon.getOutputStream();
		IOUtils.copy(is, outputStream);
		is.close();
		outputStream.close();
		int code = httpCon.getResponseCode();
		if ((code >= 400) || (code < 0)){
			throw new IOException ("Received code " + code);
		}
	}

	public static String getUsedRunId(final String engineRunId) {
		String runId = engineRunId;
		final String specifiedId = System.getProperty("taverna.runid");
		if (specifiedId != null) {
			runId = specifiedId;
		}
		return runId;
	}

	public File getInteractionServiceDirectory() {
		final File workingDir = appConfig
				.getApplicationHomeDir();
		final File interactionServiceDirectory = new File(workingDir,
				"interactionService");
		interactionServiceDirectory.mkdirs();
		return interactionServiceDirectory;
	}

	public static String objectToJson(final Object o) throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		final StringWriter sw = new StringWriter();
		mapper.writeValue(sw, o);
		final String theString = sw.toString();
		return theString;
	}

	public void setAppConfig(ApplicationConfiguration appConfig) {
		this.appConfig = appConfig;
	}

	public void setInteractionRecorder(InteractionRecorder interactionRecorder) {
		this.interactionRecorder = interactionRecorder;
	}

	public void setInteractionPreference(InteractionPreference interactionPreference) {
		this.interactionPreference = interactionPreference;
	}
}
