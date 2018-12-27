/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactpro.sf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.exceptions.APICallException;
import com.exactpro.sf.exceptions.APIResponseException;
import com.exactpro.sf.testwebgui.restapi.xml.MatrixList;
import com.exactpro.sf.testwebgui.restapi.xml.XmlMatrixLinkUploadResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlMatrixUploadResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlRunReference;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestScriptShortReport;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestscriptActionResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestscriptRunDescription;


public class SFAPIClient implements AutoCloseable {
	
	private static final Logger logger = LoggerFactory.getLogger(SFAPIClient.class);
	private static final String SFAPI = "sfapi/";
	private static final String SERVICES = "services?environment=!env";
	private static final String SERVICE_DELETE = "services/delete?environment=!env&service=!svc";
	private static final String SERVICE_DELETE_ALL = "services/delete?environment=!env";
	private static final String SERVICE_DELETE_CUSTOM = "services/delete?";
	private static final String SERVICE_STOP = "services/!svc?action=stop&environment=!env";
	private static final String SERVICE_START = "services/!svc?action=start&environment=!env";
	private static final String SERVICES_IMPORT = "services/import?environment=!env&replaceexisting=!re&skipexisting=!se";
	
	private static final String ENVIRONMENTS = "environment/";
	private static final String ENVIRONMENT_CREATE = "environment/add?name=!name";
	private static final String ENVIRONMENT_DELETE = "environment/delete?name=!name";
	private static final String ENVIRONMENT_RENAME = "environment/rename?oldname=!old&newname=!new";
	
	private static final String MATRICES = "scripts/";
	private static final String RUN_REFERENCE = "scripts/reference/run";
	private static final String MATRIX_UPLOAD = "scripts/upload";
	private static final String MATRIX_LINK_UPLOAD = "scripts/uploadLink?link=!link";
	private static final String MATRIX_LINK_UPLOAD_BY_PROVIDER = "scripts/uploadLink?link=!link&provider_uri=!provider_uri";
	private static final String MATRIX_DOWNLOAD = "scripts/download?id=!id";
	private static final String MATRIX_RUN = "scripts/";
	private static final String MATRIX_DELETE_BY_ID = "scripts/delete/!id";
	private static final String MATRIX_DELETE_BY_NAME = "scripts/delete/name_!name";
	private static final String MATRIX_DELETE_ALL = "scripts/delete";

    private static final String MATRIX_CONVERT = "scripts/convert?matrix_id=!id&converter_uri=!converter_uri";
    private static final String MATRIX_CONVERT_ENVIRONMENT = "scripts/convert?matrix_id=!id&environment=!environment&converter_uri=!converter_uri";

	private static final String TEST_LIBRARY_UPLOAD = "testlibraries/upload";
	
	private static final String TEST_SCRIPT_RUNS = "testscriptruns/";
	private static final String TEST_SCRIPT_RUN_INFO = "testscriptruns/!id";
	private static final String TEST_SCRIPT_RUN_COMPILE = "testscriptruns/!id?action=compileScript";
	private static final String TEST_SCRIPT_RUN_RUN_COMPILED_SCRIPT = "testscriptruns/!id?action=runCompileScript";
	private static final String TEST_SCRIPT_RUN_REPORT = "testscriptruns/!id?action=report";
	private static final String TEST_SCRIPT_RUN_SHORTREPORT = "testscriptruns/!id?action=shortreport";
	private static final String TEST_SCRIPT_RUN_REPORT_ZIP = "testscriptruns/!id?action=reportzip";
	private static final String TEST_SCRIPT_RUN_AGGREGATE = "testscriptruns/aggregate";
	private static final String TEST_SCRIPT_RUN_DELETE = "testscriptruns/delete/!id?deleteOnDisk=true";
	private static final String TEST_SCRIPT_RUN_DELETE_ALL = "testscriptruns/delete?deleteOnDisk=true";
	private static final String TEST_SCRIPT_RUN_STOP = "testscriptruns/!id?action=stop";
    private static final String TEST_SCRIPT_RUN_UPDATE = "testscriptruns/update/!id?sfCurrentID=!sfCurrentID";
	
	private static final String STATS_REGISTER_TAG = "statistics/register_tag?name=!name";
	private static final String STATS_REGISTER_GROUP = "statistics/register_group?name=!name";
	private static final String STATS_REGISTER_TAG_IN_GROUP = "statistics/register_tag?name=!tag&group=!group";

    private static final String RESOURCES_CLEAN_OLDERTHAN = "resources/clean?olderthan=!olderthan";
    private static final String RESOURCES_CLEAN_TARGETS = "resources/clean?targets=!targets";
    private static final String RESOURCES_CLEAN_OLDERTHAN_TARGETS = "resources/clean?olderthan=!olderthan&targets=!targets";

    private final CloseableHttpClient http;
	private final String rootUrl;
	private final DocumentBuilder docBuilder;
    private Map<Class<?>, Unmarshaller> unmarshallers = new HashMap<Class<?>, Unmarshaller>();

    private String defaultServiceHandlerClassName = "com.exactpro.sf.services.CollectorServiceHandler";
	
	/**
	 * Creates an instance of SFAPI client
	 * @param rootUrl Root url of SFAPI, e.g. "http://localhost/sfgui/sfapi/"
	 */
	public SFAPIClient(String rootUrl) throws ParserConfigurationException {
	    if (rootUrl == null || rootUrl.length() < SFAPI.length()) {
            throw new IllegalArgumentException("Sailfish URL [" + rootUrl + "] is incorrect");
	    }
	    
	    StringBuilder builder = new StringBuilder(rootUrl);
	    if (builder.charAt(builder.length() - 1) != '/') {
	        builder.append('/');
	    }
	    if (!SFAPI.equals(builder.substring(builder.length() - SFAPI.length()))) {
	        builder.append(SFAPI);
	    }
	    
		this.rootUrl = builder.toString();
		this.http = HttpClients.createDefault();
		this.docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}
	
	// Services
	
	/**
	 * Loads service map for given environment.
	 * Please note that current implementation doesn't set <code>settingsTag</code> field properly; you'll need to set it manually, probably using <code>ServiceDescriptors</code> class
	 * @param envName Environment name
	 * @return Service map
	 * @throws IOException
	 * @throws SAXException
	 */
	public Map<String, Service> getServices(String envName) throws APICallException, APIResponseException {
		return getServices(envName, false);
	}
	
	public Map<String, Service> getServices(String envName, boolean setSettingsTag) throws APICallException, APIResponseException{
		if (setSettingsTag && (ServiceDescriptors.getInstance() == null)) {
			try {
				ServiceDescriptors.init(docBuilder);
			} catch (IOException e) {
				throw new APICallException(e);
			} catch (SAXException e) {
				throw new APICallException(e);
			}
		}
		
		String url =SERVICES
				.replaceFirst("!env", envName);
		Document doc = getDocument(url);
		Map<String, Service> map = new HashMap<String, Service>();
		
		NodeList nodes = doc.getDocumentElement().getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Service svc = Service.fromXml(nodes.item(i), defaultServiceHandlerClassName);
			if (setSettingsTag) {
				svc.setSettingsTag(ServiceDescriptors.forName(svc.getType()).getSettingsTagName());
			}
			map.put(svc.getName(), svc);
		}
		
		return map;
	}
	
	public XmlResponse deleteService(String envName, String svcName) throws APICallException, APIResponseException {
		String url = SERVICE_DELETE
				.replace("!env", envName)
				.replace("!svc", svcName);
		return getXmlResponse(url);
	}
	
	public XmlResponse deleteService(String envName, Service svc) throws APICallException, APIResponseException {
		return deleteService(envName, svc.getName());
	}
	
	public XmlResponse deleteService(String args) throws APICallException, APIResponseException {
		String url=SERVICE_DELETE_CUSTOM + args;
		return getXmlResponse(url);
	}
	
	public XmlResponse deleteAllServices(String envName) throws APICallException, APIResponseException {
		String url =SERVICE_DELETE_ALL
				.replace("!env", envName);
		return getXmlResponse(url);
	}
	
	public void startService(String envName, String svcName) throws APICallException, APIResponseException {
		String url = rootUrl + SERVICE_START
				.replace("!env", envName)
				.replace("!svc", svcName);
		try {
			
			
			logger.debug("Request: {}", url);
			
			HttpGet req = new HttpGet(url);
			CloseableHttpResponse res = http.execute(req);
			checkHttpResponse(res);	
			res.close();
			return;
		} catch (APIResponseException e) {
			throw new APIResponseException("URL: "+url,e); 
		} catch (Exception e) {
			throw new APICallException(e);
		}		
	}
	
	public void startService(String envName, Service svc) throws APICallException, APIResponseException {
		startService(envName, svc.getName());
		return;
	}
	
	public void stopService(String envName, String svcName) throws APICallException{
		 try {
				String url = rootUrl + SERVICE_STOP
						.replace("!env", envName)
						.replace("!svc", svcName);
				
				logger.debug("Request: {}", url);
				
				HttpGet req = new HttpGet(url);
				CloseableHttpResponse res = http.execute(req);
				res.close();
				return;
			} catch (Exception e) {
				throw new APICallException(e);
			}
	}
	
	public void stopService(String envName, Service svc) throws APICallException, APIResponseException {
		stopService(envName, svc.getName());
		return;
	}
	
	public List<ServiceImportResult> importServices(String fileName, String envName, InputStream zipStream, boolean replaceExisting, boolean skipExisting) throws APICallException, APIResponseException {
		String url = rootUrl + SERVICES_IMPORT
				.replace("!env", envName)
				.replace("!re", String.valueOf(replaceExisting))
				.replace("!se", String.valueOf(skipExisting));
		try {
			MultipartEntityBuilder mpb = MultipartEntityBuilder.create();
			mpb.addBinaryBody("file", zipStream, ContentType.APPLICATION_OCTET_STREAM, fileName);
			
			
			logger.debug("Root url: {}", url);
			
			HttpPost req = new HttpPost(url);
			req.setEntity(mpb.build());
			CloseableHttpResponse res = http.execute(req);
			checkHttpResponse(res);
			
			Document doc = docBuilder.parse(res.getEntity().getContent());
			Element root = doc.getDocumentElement();

			List<ServiceImportResult> svcs = new ArrayList<ServiceImportResult>();
			NodeList children = root.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				svcs.add(ServiceImportResult.fromXml(children.item(i)));
			}

			res.close();
			return svcs;
		} catch (APIResponseException e) {
			throw new APIResponseException("URL: "+url,e);
		} catch (Exception e) {
			throw new APICallException(e);
		}
	}
	
	// Matrices
	
	public MatrixList getMatrixList() throws APICallException, APIResponseException, ParseException {
		String url = MATRICES;
		try {
			return getResponse(url, MatrixList.class); 
		} catch (APIResponseException e) {
			throw new APIResponseException("URL: "+url,e);
		} catch (Exception e) {
			throw new APICallException(e);
		}
	}
	
	public XmlResponse uploadTestLibrary(InputStream stream, String filename) throws APICallException, APIResponseException {
		String url = rootUrl + TEST_LIBRARY_UPLOAD;
		try {
			MultipartEntityBuilder mpb = MultipartEntityBuilder.create();
			mpb.addBinaryBody("file", stream, ContentType.APPLICATION_OCTET_STREAM, filename);

			HttpPost req = new HttpPost(url);
			req.setEntity(mpb.build());
			CloseableHttpResponse res = http.execute(req);
			checkHttpResponse(res);
			XmlResponse xmlResponse=null;
			try {
				xmlResponse = unmarshall(XmlResponse.class, res);
				res.close();
			} catch (Exception e) {
				throw new APICallException(e);
			}
			res.close();
			return xmlResponse;
		} catch (APIResponseException e) {
			throw new APIResponseException("URL: "+url,e);
		} catch (Exception e) {
			throw new APICallException(e);
		}
	}
	
	public XmlMatrixUploadResponse uploadMatrix(InputStream stream, String filename) throws APICallException, APIResponseException {
		String url = rootUrl + MATRIX_UPLOAD;
		try {
			MultipartEntityBuilder mpb = MultipartEntityBuilder.create();
			mpb.addBinaryBody("file", stream, ContentType.APPLICATION_OCTET_STREAM, filename);

			HttpPost req = new HttpPost(url);
			req.setEntity(mpb.build());
			CloseableHttpResponse res = http.execute(req);
			checkHttpResponse(res);
			XmlMatrixUploadResponse xmlResponse=null;
			try {
				xmlResponse = unmarshall(XmlMatrixUploadResponse.class, res);
				res.close();
			} catch (Exception e) {
				throw new APICallException(e);
			}
			res.close();
			return xmlResponse;		
		} catch (APIResponseException e) {
			throw new APIResponseException("URL: "+url,e);
		} catch (Exception e) {
			throw new APICallException(e);
		}
	}

    public XmlMatrixLinkUploadResponse uploadMatrixLink(String link) throws APIResponseException, APICallException {
        String url = rootUrl + MATRIX_LINK_UPLOAD.replace("!link", link);

        return getMatrixLinkUploadResponse(url);
    }

    public XmlMatrixLinkUploadResponse uploadMatrixLink(String link, SailfishURI providerURI)
            throws APICallException, APIResponseException {
        String url = rootUrl + MATRIX_LINK_UPLOAD_BY_PROVIDER.replace("!link", link)
                                                             .replace("!provider_uri", providerURI.toString());
        return getMatrixLinkUploadResponse(url);
    }

    private XmlMatrixLinkUploadResponse getMatrixLinkUploadResponse(String url)
            throws APIResponseException, APICallException {
        try {
            HttpPost req = new HttpPost(url);
            CloseableHttpResponse res = http.execute(req);
            checkHttpResponse(res);
            XmlMatrixLinkUploadResponse xmlResponse;
            try {
                xmlResponse = unmarshall(XmlMatrixLinkUploadResponse.class, res);
                res.close();
            } catch (Exception e) {
                throw new APICallException(e);
            }
            res.close();
            return xmlResponse;
        } catch (APIResponseException e) {
            throw new APIResponseException("URL: " + url, e);
        } catch (Exception e) {
            throw new APICallException(e);
        }
    }
	
	public InputStream downloadMatrix(int id) throws APICallException, APIResponseException {
		String url = rootUrl + MATRIX_DOWNLOAD.replace("!id", String.valueOf(id));
		try {
			HttpGet req = new HttpGet(url);
			CloseableHttpResponse res = http.execute(req);
			checkHttpResponse(res);
			return res.getEntity().getContent();
            //return new FileDownloadWrapper(res.getHeaders("content-disposition")[0].getElements()[0].getParameterByName("filename").getValue(), res);
		} catch (APIResponseException e) {
			throw new APIResponseException("URL: "+url,e);
		} catch (Exception e) {
			throw new APICallException(e);
		}
	}
	
	private XmlTestscriptActionResponse performMatrixActionInt(String matrix,
			 String actionName,
			 String rangeParam,
			 String environmentParam,
			 String fileEncodingParam,
			 int amlParam,
			 boolean continueOnFailed,
			 boolean autoStart,
			 boolean autoRun,
			 boolean ignoreAskForContinue,
			 List<String> tags,
			 String staticVariables,
			 String subFolder) throws APICallException, APIResponseException {
		return performMatrixActionInt(matrix,actionName,rangeParam,environmentParam, fileEncodingParam,
				amlParam,continueOnFailed,autoStart, autoRun,ignoreAskForContinue, true, false, tags,staticVariables,subFolder, null);
	}
	
	private XmlTestscriptActionResponse performMatrixActionInt(String matrix,
																 String actionName,
																 String rangeParam,
																 String environmentParam,
																 String fileEncodingParam,
																 int amlParam,
																 boolean continueOnFailed,
																 boolean autoStart,
																 boolean autoRun,
																 boolean ignoreAskForContinue,
																 boolean runNetDumper,
																 boolean skipOptional,
																 List<String> tags,
																 String staticVariables,
																 String subFolder,
																 String language) throws APICallException, APIResponseException {

		StringBuilder paramsBuilder = new StringBuilder(matrix);

		paramsBuilder.append("?");
		paramsBuilder.append("action=");
		paramsBuilder.append(actionName);

		if(rangeParam != null && !rangeParam.equals("")) {
			paramsBuilder.append("&");
			paramsBuilder.append("range=");
			paramsBuilder.append(rangeParam);
		}

		paramsBuilder.append("&");
		paramsBuilder.append("environment=");

		if(environmentParam == null || environmentParam.equals("")) {
			paramsBuilder.append("default");
		} else {
			paramsBuilder.append(environmentParam);
		}

		paramsBuilder.append("&");
		paramsBuilder.append("encoding=");

		if(fileEncodingParam == null || fileEncodingParam.equals("")) {
			paramsBuilder.append("ISO-8859-1");
		} else {
			paramsBuilder.append(fileEncodingParam);
		}

		paramsBuilder.append("&");
		paramsBuilder.append("aml=");
		paramsBuilder.append(amlParam);

		paramsBuilder.append("&");
		paramsBuilder.append("continueonfailed=");
		paramsBuilder.append(String.valueOf(continueOnFailed));

		paramsBuilder.append("&");
		paramsBuilder.append("autostart=");
		paramsBuilder.append(String.valueOf(autoStart));

		paramsBuilder.append("&");
		paramsBuilder.append("autorun=");
		paramsBuilder.append(String.valueOf(autoRun));

		paramsBuilder.append("&");
		paramsBuilder.append("ignoreaskforcontinue=");
		paramsBuilder.append(String.valueOf(ignoreAskForContinue));

		paramsBuilder.append("&");
		paramsBuilder.append("runnetdumper=");
		paramsBuilder.append(String.valueOf(runNetDumper));

		paramsBuilder.append("&");
		paramsBuilder.append("skipoptional=");
		paramsBuilder.append(String.valueOf(skipOptional));

		if(tags != null) {
			for(String tag : tags) {
				paramsBuilder.append("&");
				paramsBuilder.append("tag=");
				paramsBuilder.append(tag);
			}
		}

		if(staticVariables != null && !staticVariables.equals("")) {
			paramsBuilder.append("&");
			paramsBuilder.append("staticvariables=");
			paramsBuilder.append(staticVariables);
		}
		if(subFolder != null && !subFolder.equals("")) {
			paramsBuilder.append("&");
			paramsBuilder.append("subfolder=");
			paramsBuilder.append(subFolder);
		}
		
		if(language != null && !language.equals("")) {
			paramsBuilder.append("&");
			paramsBuilder.append("language=");
			paramsBuilder.append(language);
		}

		String params = paramsBuilder.toString();

		String url = MATRIX_RUN + params;		
		logger.debug(url);
		
		if(!"stop".equals(actionName)){
			XmlTestscriptActionResponse res=getResponse(url, XmlTestscriptActionResponse.class);
			return res!=null? res: new XmlTestscriptActionResponse();
		}
		
		try{
			CloseableHttpResponse response=getHttpResponse(url);
			response.close();
			return new XmlTestscriptActionResponse();
		}catch(APIResponseException e){
			throw e;
		}catch(Exception e){
			throw new APICallException(e);
		}
	}
	
	public XmlTestscriptActionResponse performMatrixAction(Matrix mat,
															 String actionName,
															 String rangeParam,
															 String environmentParam,
															 String fileEncodingParam,
															 int amlParam,
															 boolean continueOnFailed,
															 boolean autoStart,
															 boolean autoRun,
															 boolean ignoreAskForContinue,
															 List<String> tags,
															 String staticVariables,
															 String subFolder
															 ) throws APICallException, APIResponseException {
		return performMatrixActionInt(String.valueOf(mat.getId()), actionName, rangeParam, environmentParam,
				                       fileEncodingParam, amlParam, continueOnFailed, autoStart,
		                               autoRun, ignoreAskForContinue, tags, staticVariables, subFolder);
	}
	
	public XmlTestscriptActionResponse performMatrixAction(int id,
															 String actionName,
															 String rangeParam,
															 String environmentParam,
															 String fileEncodingParam,
															 int amlParam,
															 boolean continueOnFailed,
															 boolean autoStart,
															 boolean autoRun,
															 boolean ignoreAskForContinue,
															 List<String> tags,
															 String staticVariables,
															 String subFolder) throws APICallException, APIResponseException {
		return performMatrixActionInt(String.valueOf(id), actionName, rangeParam, environmentParam,
				fileEncodingParam, amlParam, continueOnFailed, autoStart,
				autoRun, ignoreAskForContinue, tags, staticVariables,subFolder);
	}
	
	public XmlTestscriptActionResponse performMatrixAction(int id,
			 String actionName,
			 String rangeParam,
			 String environmentParam,
			 String fileEncodingParam,
			 int amlParam,
			 boolean continueOnFailed,
			 boolean autoStart,
			 boolean autoRun,
			 boolean ignoreAskForContinue,
			 List<String> tags,
			 String staticVariables,
			 String subFolder,
			 String language) throws APICallException, APIResponseException {
		return performMatrixActionInt(String.valueOf(id), actionName, rangeParam, environmentParam,
				fileEncodingParam, amlParam, continueOnFailed, autoStart,
				autoRun, ignoreAskForContinue, true, false, tags, staticVariables,subFolder,language);
}

    public XmlTestscriptActionResponse performMatrixAction(int id,
            String actionName,
            String rangeParam,
            String environmentParam,
            String fileEncodingParam,
            int amlParam,
            boolean continueOnFailed,
            boolean autoStart,
            boolean autoRun,
            boolean ignoreAskForContinue,
            boolean runNetDumper,
            boolean skipOptional,
            List<String> tags,
            String staticVariables,
            String subFolder,
            String language) throws APICallException, APIResponseException {
        return performMatrixActionInt(String.valueOf(id), actionName, rangeParam, environmentParam,
                fileEncodingParam, amlParam, continueOnFailed, autoStart,
                autoRun, ignoreAskForContinue, runNetDumper, skipOptional, tags, staticVariables,subFolder,language);
    }

	public XmlTestscriptActionResponse performMatrixAction(String name,
															 String actionName,
															 String rangeParam,
															 String environmentParam,
															 String fileEncodingParam,
															 int amlParam,
															 boolean continueOnFailed,
															 boolean autoStart,
															 boolean autoRun,
															 boolean ignoreAskForContinue,
															 List<String> tags,
															 String staticVariables,
															 String subFolder) throws APICallException, APIResponseException {
		return performMatrixActionInt("name_" + name, actionName, rangeParam, environmentParam,
				fileEncodingParam, amlParam, continueOnFailed, autoStart,
				autoRun, ignoreAskForContinue, tags, staticVariables,subFolder);
	}
	
    public XmlRunReference getRunReference() throws APIResponseException, APICallException {
        String url = RUN_REFERENCE;
        try {
            return getResponse(url, XmlRunReference.class);
        } catch (APIResponseException e) {
            throw new APIResponseException("URL: " + url, e);
        } catch (Exception e) {
            throw new APICallException(e);
        }
    }
	
	public XmlTestscriptActionResponse runMatrix(Matrix mat) throws APICallException, APIResponseException {
		return performMatrixActionInt(String.valueOf(mat.getId()), "start", null, "default",
		        "ISO-8859-1", 2, false, false, true, true, null, null, null);
	}
	
	public XmlTestscriptActionResponse runMatrix(int id) throws APICallException, APIResponseException {
		return performMatrixActionInt(String.valueOf(id), "start", null, "default",
				"ISO-8859-1", 2, false, false, true, true, null, null, null);
	}
	
	public XmlTestscriptActionResponse runMatrix(String name) throws APICallException, APIResponseException {
		return performMatrixActionInt("name_" + name, "start", null, "default",
				"ISO-8859-1", 2, false, false, true, true, null, null, null);
	}
	
	public XmlTestscriptActionResponse stopMatrix(Matrix mat) throws APICallException, APIResponseException {
		return performMatrixActionInt(String.valueOf(mat.getId()), "stop", null, "default",
				"ISO-8859-1", 2, false, false, true, true, null, null, null);
	}
	
	public XmlTestscriptActionResponse stopMatrix(int id) throws APICallException, APIResponseException {
		return performMatrixActionInt(String.valueOf(id), "stop", null, "default",
				"ISO-8859-1", 2, false, false, true, true, null, null, null);
	}
	
	public XmlTestscriptActionResponse stopMatrix(String name) throws APICallException, APIResponseException {
		return performMatrixActionInt("name_" + name, "stop", null, "default",
				"ISO-8859-1", 2, false, false, true, true, null, null, null);
	}
	
	public List<TestScriptRun> runAllMatrices() throws APICallException, APIResponseException {
		Document doc = getDocument(MATRIX_RUN + "all?action=start");
		Element root = doc.getDocumentElement();
		NodeList children = root.getChildNodes();
		
		List<TestScriptRun> list = new ArrayList<TestScriptRun>();
		for (int i = 0; i < children.getLength(); i++) {
			list.add(TestScriptRun.fromXml(children.item(i)));
		}
		
		return list;
	}
	
	public XmlResponse deleteMatrix(int id) throws APICallException, APIResponseException {
		String url = MATRIX_DELETE_BY_ID
				.replace("!id", String.valueOf(id));
		return getXmlResponse(url);
	}
	
	public XmlResponse deleteMatrix(Matrix mat) throws APICallException, APIResponseException {
		return deleteMatrix(mat.getId());
	}
	
	public XmlResponse deleteMatrix(String name) throws APICallException, APIResponseException {
		String url = MATRIX_DELETE_BY_NAME
				.replace("!name", name);
		return getXmlResponse(url);
	}
	
	public XmlResponse deleteAllMatrices() throws APICallException, APIResponseException {
		String url = MATRIX_DELETE_ALL;
		return getXmlResponse(url);
	}

    public XmlMatrixUploadResponse convertMatrix(int id, String environment, SailfishURI converterUri)
            throws APICallException, APIResponseException {
        String url = MATRIX_CONVERT_ENVIRONMENT.replace("!id", String.valueOf(id))
                                               .replace("!environment", environment)
                                               .replace("!converter_uri", converterUri.toString());
        XmlMatrixUploadResponse res = getResponse(url, XmlMatrixUploadResponse.class);
        return res != null ? res : new XmlMatrixUploadResponse();
    }

    public XmlMatrixUploadResponse convertMatrix(Matrix mat, String environment, SailfishURI converterUri)
            throws APICallException, APIResponseException {
        return convertMatrix(mat.getId(), environment, converterUri);
    }

    public XmlMatrixUploadResponse convertMatrix(int id, SailfishURI converterUri)
            throws APICallException, APIResponseException {
        String url = MATRIX_CONVERT.replace("!id", String.valueOf(id))
                                   .replace("!converter_uri", converterUri.toString());
        XmlMatrixUploadResponse res = getResponse(url, XmlMatrixUploadResponse.class);
        return res != null ? res : new XmlMatrixUploadResponse();
    }

    public XmlMatrixUploadResponse convertMatrix(Matrix mat, SailfishURI converterUri)
            throws APICallException, APIResponseException {
        return convertMatrix(mat.getId(), converterUri);
    }
	
	// Test script runs
	
	public List<TestScriptRun> getTestScriptRunList() throws APICallException, APIResponseException {
		Document doc = getDocument(TEST_SCRIPT_RUNS);
		List<TestScriptRun> list = new ArrayList<TestScriptRun>();
		
		NodeList nodes = doc.getDocumentElement().getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			list.add(TestScriptRun.fromXml(nodes.item(i)));
		}
		
		return list;
	}
	
	public XmlTestscriptRunDescription getTestScriptRunInfo(int id) throws APICallException, APIResponseException {
		String url=TEST_SCRIPT_RUN_INFO
				.replace("!id", String.valueOf(id));
		XmlTestscriptRunDescription res= getResponse(url, XmlTestscriptRunDescription.class);
		return res!=null? res: new XmlTestscriptRunDescription();		
	}
	
    public XmlTestScriptShortReport getTestScriptRunShortReport(int id) throws APICallException, APIResponseException {
        String url = TEST_SCRIPT_RUN_SHORTREPORT.replace("!id", String.valueOf(id));
        XmlTestScriptShortReport res = getResponse(url, XmlTestScriptShortReport.class);
        return res != null ? res : new XmlTestScriptShortReport();
    }

    public InputStream getTestScriptRunReport(int id) throws APICallException, APIResponseException {
		String url = rootUrl + TEST_SCRIPT_RUN_REPORT
				.replace("!id", String.valueOf(id));
		try {
			HttpGet req = new HttpGet(url);
			CloseableHttpResponse res = http.execute(req);
			checkHttpResponse(res);
			return res.getEntity().getContent();
		} catch (APIResponseException e) {
			throw new APIResponseException("URL: "+url,e);
		} catch (Exception e) {
			throw new APICallException(e);
		}
	}

	public FileDownloadWrapper getTestScriptRunReportZip(int id) throws APICallException, APIResponseException {
		String url = rootUrl + TEST_SCRIPT_RUN_REPORT_ZIP
				.replace("!id", String.valueOf(id));
		try {
			HttpGet req = new HttpGet(url);
			CloseableHttpResponse res = http.execute(req);
			checkHttpResponse(res);
            return new FileDownloadWrapper(res.getHeaders("content-disposition")[0].getElements()[0].getParameterByName("filename").getValue(), res);
		} catch (APIResponseException e) {
			throw new APIResponseException("URL: "+url,e);
		} catch (Exception e) {
			throw new APICallException(e);
		}
	}
	
	public InputStream getTestScriptRunAggregateReport(String args) throws APICallException, APIResponseException {
		String url = rootUrl + TEST_SCRIPT_RUN_AGGREGATE + (args.isEmpty() ? "" : '?') + args;
		try {
			HttpGet req = new HttpGet(url);
			CloseableHttpResponse res = http.execute(req);
			checkHttpResponse(res);
			return res.getEntity().getContent();
		} catch (APIResponseException e) {
			throw new APIResponseException("URL: "+url,e);
		} catch (Exception e) {
			throw new APICallException(e);
		}
	}
	
	public XmlResponse deleteTestScriptRun(int id) throws APICallException, APIResponseException {
		String url = TEST_SCRIPT_RUN_DELETE
				.replace("!id", String.valueOf(id));
		return getXmlResponse(url);
	}

    public XmlResponse setSfCurrentID(int id, long sfCurrentID) throws APICallException, APIResponseException {
        String url = TEST_SCRIPT_RUN_UPDATE
                .replace("!id", String.valueOf(id))
                .replace("!sfCurrentID", String.valueOf(sfCurrentID));
        return getXmlResponse(url);
    }


    public XmlResponse compileTestScriptRun(int id) throws APICallException, APIResponseException {
		String url = TEST_SCRIPT_RUN_COMPILE
				.replace("!id", String.valueOf(id));
		return getXmlResponse(url);
	}
	public XmlResponse runCompiledTestScript(int id) throws APICallException, APIResponseException {
		String url = TEST_SCRIPT_RUN_RUN_COMPILED_SCRIPT
				.replace("!id", String.valueOf(id));
		return getXmlResponse(url);
	}
	
	
	public XmlResponse deleteTestScriptRun(TestScriptRun run) throws APICallException, APIResponseException {
		return deleteTestScriptRun(run.getId());
	}
	
	public XmlResponse deleteAllTestScriptRun() throws APICallException, APIResponseException {
		String url = TEST_SCRIPT_RUN_DELETE_ALL;		
		return getXmlResponse(url);
	}
	
	public XmlResponse stopTestScriptRun(int id) throws APICallException, APIResponseException {
		String url = TEST_SCRIPT_RUN_STOP
				.replace("!id", String.valueOf(id));
		return getXmlResponse(url);
	}
	
	public XmlResponse stopTestScriptRun(TestScriptRun run) throws APICallException, APIResponseException {
		return stopTestScriptRun(run.getId());
	}
	
	// Environments
	
	public List<String> getEnvironmentList() throws APICallException, APIResponseException {
		Document doc = getDocument(ENVIRONMENTS);
		List<String> list = new ArrayList<String>();
		
		NodeList nodes = doc.getDocumentElement().getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			list.add(n.getTextContent());
		}
		
		return list;
	}

	public XmlResponse createEnvironment(String name) throws APICallException, APIResponseException {
		String url = ENVIRONMENT_CREATE
				.replace("!name", name);
		return getXmlResponse(url);
	}
	
	public XmlResponse deleteEnvironment(String name) throws APICallException, APIResponseException {
		String url = ENVIRONMENT_DELETE
				.replace("!name", name);
		return getXmlResponse(url);
	}
	
	public XmlResponse renameEnvironment(String oldName, String newName) throws APICallException, APIResponseException {
		String url = ENVIRONMENT_RENAME
				.replace("!old", oldName)
				.replace("!new", newName);
		return getXmlResponse(url);
	}
	
	// Statistics
	
	public XmlResponse registerTag(String name) throws APICallException, APIResponseException {
		String url = STATS_REGISTER_TAG
				.replace("!name", name);
		return getXmlResponse(url);
	}

	public XmlResponse registerTagGroup(String name) throws APICallException, APIResponseException {
		String url = STATS_REGISTER_GROUP
				.replace("!name", name);
		return getXmlResponse(url);
	}
	
	public XmlResponse registerTagInGroup(String tag, String group) throws APICallException, APIResponseException {
		String url = STATS_REGISTER_TAG_IN_GROUP
				.replace("!tag", tag)
				.replace("!group", group);
		return getXmlResponse(url);
	}
	
	public XmlResponse setStatisticsDBSettings(String xmlConfig) throws APICallException, APIResponseException {
        String url = rootUrl + "statistics/set_db_settings";
        XmlResponse xmlResponse;
        try {
            HttpPost req = new HttpPost(url);
            req.setHeader("Content-Type", "application/xml");
            req.setEntity(new StringEntity(xmlConfig));
            CloseableHttpResponse res = http.execute(req);
            checkHttpResponse(res);
            xmlResponse = unmarshall(XmlResponse.class, res);
            res.close();
        } catch (Exception e) {
            throw new APICallException(e);
        }
        return xmlResponse;
    }

    // Resources

    public XmlResponse cleanResources(Instant olderthan, String... targets)
            throws APICallException, APIResponseException {
        String url = rootUrl + RESOURCES_CLEAN_OLDERTHAN_TARGETS.replace("!olderthan", olderthan.toString())
                                                                .replace("!targets", String.join(",", targets));

        XmlResponse xmlResponse;
        try {
            HttpDelete req = new HttpDelete(url);

            CloseableHttpResponse res = http.execute(req);
            checkHttpResponse(res);
            xmlResponse = unmarshall(XmlResponse.class, res);
            res.close();
        } catch (Exception e) {
            throw new APICallException(e);
        }
        return xmlResponse;
    }

    public XmlResponse cleanResources(String... targets) throws APICallException, APIResponseException {
        String url = rootUrl + RESOURCES_CLEAN_TARGETS.replace("!targets", String.join(",", targets));
        XmlResponse xmlResponse;
        try {
            HttpDelete req = new HttpDelete(url);

            CloseableHttpResponse res = http.execute(req);
            checkHttpResponse(res);
            xmlResponse = unmarshall(XmlResponse.class, res);
            res.close();
        } catch (Exception e) {
            throw new APICallException(e);
        }
        return xmlResponse;
    }

    public XmlResponse cleanResources(Instant olderthan) throws APICallException, APIResponseException {
        String url = rootUrl + RESOURCES_CLEAN_OLDERTHAN.replace("!olderthan", olderthan.toString());
        XmlResponse xmlResponse;
        try {
            HttpDelete req = new HttpDelete(url);

            CloseableHttpResponse res = http.execute(req);
            checkHttpResponse(res);
            xmlResponse = unmarshall(XmlResponse.class, res);
            res.close();
        } catch (Exception e) {
            throw new APICallException(e);
        }
        return xmlResponse;
    }


	// Utility methods
	public DocumentBuilder getDocumentBuilder() {
		return docBuilder;
	}
	
	@Override
	public void close() throws IOException {
		http.close();
	}
	

	private CloseableHttpResponse getHttpResponse(String relativeUrl) throws APICallException, APIResponseException{
		String urlString = rootUrl + relativeUrl;
		try {
			logger.debug("Request: {}", urlString);
			
			// encode parameters with URI
			URL url = new URL(urlString);
	        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
			
			HttpGet req = new HttpGet(uri);
			CloseableHttpResponse res = http.execute(req);
			checkHttpResponse(res);
			return res;
		} catch (APIResponseException e) {

			throw new APIResponseException("URL: " + urlString + " Message: " + e.getMessage(), e);

		} catch (Exception e) {
			throw new APICallException(e);
		}
	}
	
	private void checkHttpResponse(CloseableHttpResponse res) throws IOException, JAXBException, APIResponseException{
		if(res.getStatusLine().getStatusCode()!=200){
			XmlResponse xmlResponse =null;
			
			try{
				xmlResponse =unmarshall(XmlResponse.class, res);
			}catch(UnmarshalException e){
				xmlResponse = new XmlResponse();
			}
			
			throw new APIResponseException("Message: "+xmlResponse.getMessage()+ "; Cause: "+ xmlResponse.getRootCause()+
					"; Http status: "+res.getStatusLine().getStatusCode());
		}	
	}
	
	private <T extends Object> T getResponse (String url, Class<T> clazz) throws APICallException, APIResponseException {
		CloseableHttpResponse response=getHttpResponse(url);
		T xmlResponse;
		try {
			xmlResponse = unmarshall(clazz, response);
			response.close();
		} catch (Exception e) {
			throw new APICallException(e);
		}
		return xmlResponse;
	}
	private XmlResponse getXmlResponse(String url) throws APICallException, APIResponseException {
		XmlResponse xmlResponse= getResponse(url, XmlResponse.class);
		if(xmlResponse==null){
			xmlResponse=new XmlResponse();
			xmlResponse.setMessage("No data received");
		}
		return xmlResponse;
	}
	
	private Unmarshaller getUnmarshaller(Class<?> clazz) throws JAXBException {
		Unmarshaller unmarshaller;
		if ((unmarshaller = unmarshallers.get(clazz)) != null) {
			return unmarshaller;
		} else {
			JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
			unmarshaller = jaxbContext.createUnmarshaller();
		    unmarshallers.put(clazz, unmarshaller);
		    return unmarshaller;
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends Object> T unmarshall(Class<T> clazz, HttpResponse response) throws JAXBException, IOException {
		T entity = null;
		if (response != null) {
			Unmarshaller unmarshaller = getUnmarshaller(clazz);
			entity = (T) unmarshaller.unmarshal(response.getEntity().getContent());			
		}
		return entity;
	}
	
	private Document getDocument(String relativeUrl) throws APICallException, APIResponseException{
		String url = rootUrl + relativeUrl;
		try {
			logger.debug("Request: {}", url);
			
			HttpGet req = new HttpGet(url);
			CloseableHttpResponse res = http.execute(req);
			checkHttpResponse(res);
			InputStream content = res.getEntity() != null ? res.getEntity().getContent() : null;
			Document result = content != null ? docBuilder.parse(content) : null;
			res.close();
			return result;
		} catch (APIResponseException e) {
			throw new APIResponseException("URL: "+url,e);
		} catch (Exception e) {
			throw new APICallException(e);
		}
	}	
	
}
