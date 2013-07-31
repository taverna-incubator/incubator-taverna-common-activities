package net.sf.taverna.t2.activities.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import net.sf.taverna.t2.activities.rest.RESTActivity.DATA_FORMAT;
import net.sf.taverna.t2.activities.rest.RESTActivity.HTTP_METHOD;
import net.sf.taverna.t2.workflowmodel.processor.config.ConfigurationBean;
import net.sf.taverna.t2.workflowmodel.processor.config.ConfigurationProperty;

/**
 * Beans of this class store configuration information for REST activities. Configuration is
 * comprised of the HTTP method to use, URL signature, and MIME types for Accept and Content-Type
 * HTTP request headers. Additional value is used to record the format of outgoing data - binary or
 * string. <br/>
 * <br/>
 *
 * Also, derived attribute "activityInputs" is generated by identifying all "input ports" in the
 * provided URL signature. <br/>
 * <br/>
 *
 * Complete request URL (obtained by substituting values into the placeholders of the URL signature)
 * is not stored, as it represents an instantiation of the activity invocation. The same applies for
 * the input message body sent along with POST / PUT requests.
 *
 * @author Sergejs Aleksejevs
 */
@SuppressWarnings("serial")
@ConfigurationBean(uri = RESTActivity.URI + "#Config")
public class RESTActivityConfigurationBean implements Serializable {
	private static final List<String> knownHeaders = Arrays.asList("Accept", "Content-Type", "Expect");
	private RESTActivity.DATA_FORMAT outgoingDataFormat;

	private boolean showRedirectionOutputPort;
	private boolean showActualUrlPort;
	private boolean showResponseHeadersPort;

	// whether to perform URL escaping of passed parameters, true by default
	private boolean escapeParameters = true;

	// only need to store the configuration of inputs, as all of them are dynamic;
	// only inputs that constitute components of URL signature are to be stored
	// in this map all outputs are currently fixed, so no need to keep configuration of those
	private Map<String, Class<?>> activityInputs;

	private HTTPRequest request;

	/**
	 * @return An instance of the {@link RESTActivityConfigurationBean} pre-configured with default
	 *         settings for all parameters.
	 */
	public static RESTActivityConfigurationBean getDefaultInstance() {
		// TODO - set sensible default values here
		RESTActivityConfigurationBean defaultBean = new RESTActivityConfigurationBean();
		defaultBean.setRequest(new HTTPRequest());
		defaultBean.setHttpMethod(RESTActivity.HTTP_METHOD.GET);
		defaultBean.setAcceptsHeaderValue("application/xml");
		defaultBean.setContentTypeForUpdates("application/xml");
		defaultBean.setUrlSignature("http://www.uniprot.org/uniprot/{id}.xml");
		defaultBean.setOutgoingDataFormat(RESTActivity.DATA_FORMAT.String);
		// not ticked by default to allow to post to Twitter
		defaultBean.setSendHTTPExpectRequestHeader(false);
		// not showing the Redirection output port by default to make processor look simpler
		defaultBean.setShowRedirectionOutputPort(false);
		defaultBean.setShowActualUrlPort(false);
		defaultBean.setShowResponseHeadersPort(false);
		defaultBean.setEscapeParameters(true);
		defaultBean.setOtherHTTPHeaders(new ArrayList<ArrayList<String>>());
		return (defaultBean);
	}

	public RESTActivityConfigurationBean() {

	}

	public RESTActivityConfigurationBean(JsonNode json) {
		JsonNode requestNode = json.get("request");
		HTTPRequest request = new HTTPRequest();
		request.setMethod(HTTP_METHOD.valueOf(requestNode.get("httpMethod").textValue()));
		request.setAbsoluteURITemplate(requestNode.get("absoluteURITemplate").textValue());
		List<HTTPRequestHeader> headers = new ArrayList<>();
		for (JsonNode headerNode : requestNode.get("headers")) {
			HTTPRequestHeader header = new HTTPRequestHeader();
			header.setFieldName(headerNode.get("header").textValue());
			header.setFieldValue(headerNode.get("value").textValue());
			headers.add(header);
		}
		setRequest(request);
		if (json.has("outgoingDataFormat")) {
			setOutgoingDataFormat(DATA_FORMAT.valueOf(json.get("outgoingDataFormat").textValue()));
		} else {
			setOutgoingDataFormat(DATA_FORMAT.String);
		}
		if (json.has("showRedirectionOutputPort")) {
			setShowRedirectionOutputPort(json.get("showRedirectionOutputPort").booleanValue());
		} else {
			setShowRedirectionOutputPort(false);
		}
		if (json.has("showActualURLPort")) {
		    setShowActualUrlPort(json.get("showActualURLPort").booleanValue());
		} else {
			setShowActualUrlPort(false);
		}
		if (json.has("showResponseHeadersPort")) {
            setShowResponseHeadersPort(json.get("showResponseHeadersPort").booleanValue());
        } else {
        	setShowResponseHeadersPort(false);
        }
		if (json.has("escapeParameters")) {
			setEscapeParameters(json.get("escapeParameters").booleanValue());
		} else {
			setEscapeParameters(true);
		}
	}

	/**
	 * Tests validity of the configuration held in this bean.
	 *
	 * <br/>
	 * <br/>
	 * Performed tests are as follows: <br/>
	 * * <code>httpMethod</code> is known to be valid - it's an enum; <br/>
	 * * <code>urlSignature</code> - uses {@link URISignatureHandler#isValid(String)} to test
	 * validity; <br/>
	 * * <code>acceptsHeaderValue</code> and <code>contentTypeForUpdates</code> must not be empty.
	 *
	 * <br/>
	 * <br/>
	 * <code>contentTypeForUpdates</code> is only checked if the <code>httpMethod</code> is such
	 * that it is meant to use the Content-Type header (that is POST / PUT only).
	 *
	 * @return <code>true</code> if the configuration in the bean is valid; <code>false</code>
	 *         otherwise.
	 */
	public boolean isValid() {
		return (getUrlSignature() != null && URISignatureHandler.isValid(getUrlSignature()) && ((RESTActivity
				.hasMessageBodyInputPort(getHttpMethod())
				&& getContentTypeForUpdates() != null
				&& getContentTypeForUpdates().length() > 0 && outgoingDataFormat != null) || !RESTActivity
					.hasMessageBodyInputPort(getHttpMethod())));
	}

	public void setHttpMethod(RESTActivity.HTTP_METHOD httpMethod) {
		request.setMethod(httpMethod);
	}

	public RESTActivity.HTTP_METHOD getHttpMethod() {
		return request.getMethod();
	}

	public String getUrlSignature() {
		return request.getAbsoluteURITemplate();
	}

	public void setUrlSignature(String urlSignature) {
		request.setAbsoluteURITemplate(urlSignature);
	}

	public String getAcceptsHeaderValue() {
		HTTPRequestHeader header = request.getHeader("Accept");
		return header == null ? null : header.getFieldValue();
	}

	public void setAcceptsHeaderValue(String acceptsHeaderValue) {
		request.setHeader("Accept", acceptsHeaderValue);
	}

	public String getContentTypeForUpdates() {
		HTTPRequestHeader header = request.getHeader("Content-Type");
		return header == null ? null : header.getFieldValue();
	}

	public void setContentTypeForUpdates(String contentTypeForUpdates) {
		request.setHeader("Content-Type", contentTypeForUpdates);
	}

	public void setActivityInputs(Map<String, Class<?>> activityInputs) {
		this.activityInputs = activityInputs;
	}

	public Map<String, Class<?>> getActivityInputs() {
		return activityInputs;
	}

	public RESTActivity.DATA_FORMAT getOutgoingDataFormat() {
		return outgoingDataFormat;
	}

	@ConfigurationProperty(name = "outgoingDataFormat", label = "Send data as", required = false)
	public void setOutgoingDataFormat(RESTActivity.DATA_FORMAT outgoingDataFormat) {
		this.outgoingDataFormat = outgoingDataFormat;
	}

	public boolean getSendHTTPExpectRequestHeader() {
		HTTPRequestHeader header = request.getHeader("Expect");
		return header == null ? null : header.isUse100Continue();
	}

	public void setSendHTTPExpectRequestHeader(boolean sendHTTPExpectRequestHeader) {
		request.setHeader("Expect", sendHTTPExpectRequestHeader);
	}

	public boolean getShowRedirectionOutputPort() {
		return showRedirectionOutputPort;
	}

	@ConfigurationProperty(name = "showRedirectionOutputPort", label = "Show 'Redirection' output port", required = false)
	public void setShowRedirectionOutputPort(boolean showRedirectionOutputPort) {
		this.showRedirectionOutputPort = showRedirectionOutputPort;
	}

	@ConfigurationProperty(name = "escapeParameters", label = "Escape URL parameter values", required = false)
	public void setEscapeParameters(boolean escapeParameters) {
		this.escapeParameters = Boolean.valueOf(escapeParameters);
	}

	public boolean getEscapeParameters() {
		return escapeParameters;
	}

	public void setOtherHTTPHeaders(ArrayList<ArrayList<String>> otherHTTPHeaders) {
		for (ArrayList<String> otherHTTPHeader : otherHTTPHeaders) {
			request.setHeader(otherHTTPHeader.get(0), otherHTTPHeader.get(1));
		}
	}

	public ArrayList<ArrayList<String>> getOtherHTTPHeaders() {
		ArrayList<ArrayList<String>> otherHTTPHeaders = new ArrayList<ArrayList<String>>();
		List<HTTPRequestHeader> headers = request.getHeaders();
		for (HTTPRequestHeader header : headers) {
			if (!knownHeaders.contains(header.getFieldName())) {
				ArrayList<String> nameValuePair = new ArrayList<String>();
				nameValuePair.add(header.getFieldName());
				nameValuePair.add(header.getFieldValue());
				otherHTTPHeaders.add(nameValuePair);
			}
		}
		return otherHTTPHeaders;
	}

	/**
		 * @return the showActualUrlPort
		 */
		public boolean getShowActualUrlPort() {
		return showActualUrlPort;
		}

		/**
		 * @param showActualUrlPort the showActualUrlPort to set
		 */
		public void setShowActualUrlPort(boolean showActualUrlPort) {
			this.showActualUrlPort = showActualUrlPort;
		}

		/**
		 * @return the showResponseHeadersPort
		 */
		public boolean getShowResponseHeadersPort() {
			return showResponseHeadersPort;
		}

		/**
		 * @param showResponseHeadersPort the showResponseHeadersPort to set
		 */
		public void setShowResponseHeadersPort(boolean showResponseHeadersPort) {
			this.showResponseHeadersPort = showResponseHeadersPort;
		}

	public HTTPRequest getRequest() {
		return request;
	}

	@ConfigurationProperty(name = "request", label = "HTTP Request")
	public void setRequest(HTTPRequest request) {
		this.request = request;
	}

}
