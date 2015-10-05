/****************************************************************
 * ElasticWarehouse - File storage based on ElasticSearch
 * ==============================================================
 * Copyright (C) 2015 by EffiSoft (http://www.effisoft.pl)
 ****************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless  required by applicable  law or agreed  to  in  writing, 
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the  License for the  specific language
 * governing permissions and limitations under the License.
 *
 ****************************************************************/
package org.elasticwarehouse.core;

//import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

//import org.eclipse.jetty.rewrite.handler.HeaderPatternRule;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.elasticsearch.client.Client;
import org.elasticwarehouse.core.graphite.DDRFileNotFoundException;
import org.elasticwarehouse.tasks.ElasticWarehouseTasksManager;


public class ElasticWarehouseRequestHandlerAPI  extends AbstractHandler{

	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseRequestHandlerAPI.class.getName());
	
	private ElasticWarehouseReqRespHelper responser = new ElasticWarehouseReqRespHelper();
	Client esClient_;
	private ElasticWarehouseConf conf_;
	ElasticWarehouseAPIProcessorSearch searchProcessor_;
	ElasticWarehouseAPIProcessorSummary summaryProcessor_;
	ElasticWarehouseAPIProcessorInfo infoProcessor_;
	ElasticWarehouseAPIProcessorTask taskProcessor_;
	ElasticWarehouseAPIProcessorGraphite graphiteProcessor_;
	ElasticWarehouseAPIProcessorBrowse browseProcessor_;
	ElasticWarehouseAPIProcessorGet getProcessor_;
	ElasticWarehouseAPIProcessorUpload uploadProcessor_;

	private ElasticSearchAccessor accessor_;
	
	public ElasticWarehouseRequestHandlerAPI(ElasticSearchAccessor accessor, ElasticWarehouseConf conf, ElasticWarehouseTasksManager tasksManager, ElasticWarehouseServerMonitoringNotifier monitoringNotifier) throws MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, NullPointerException, ReflectionException, IOException, ParseException, InterruptedException {
		esClient_ = accessor.getClient();
		accessor_ = accessor;
		conf_ = conf;
		searchProcessor_ = new ElasticWarehouseAPIProcessorSearch(conf_);
		summaryProcessor_ = new ElasticWarehouseAPIProcessorSummary(conf_, accessor, tasksManager);
		taskProcessor_ = new ElasticWarehouseAPIProcessorTask(tasksManager, conf_);
		graphiteProcessor_ = new ElasticWarehouseAPIProcessorGraphite(conf_, accessor, monitoringNotifier);
		browseProcessor_ = new ElasticWarehouseAPIProcessorBrowse(conf_, accessor);
		infoProcessor_ = new ElasticWarehouseAPIProcessorInfo(conf_, accessor, tasksManager);
		getProcessor_ = new ElasticWarehouseAPIProcessorGet(conf_, accessor);
		uploadProcessor_ = new ElasticWarehouseAPIProcessorUpload(conf_, accessor);
	}
	
	
//	protected void doResponseHeaders(HttpServletResponse response,
//            Resource resource,
//            String mimeType)
//	{
//		System.out.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEe");
//	}
	
	public void handle(String target,
            Request baseRequest,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
		
		
		
	    //InputStream in=request.getInputStream();
	    //byte[] b=("read=" + in.read() + "\n").getBytes(StringUtil.__UTF8);
	    //response.setContentLength(b.length);
	    //response.getOutputStream().write(b);
	    //response.flushBuffer();
	      
	    
	    
		//Map<String, String[]> parms = request.getParameterMap();
		//String uri = request.getRequestURI();
		//String query = request.getQueryString();
		//String s1= request.getParameter("username"); 
		//String s2= request.getParameter("pretty"); 
		
		LOGGER.info("*** API requesting " + request.getPathInfo() + "?" + request.getQueryString());
    	
			//String uri = request.getPathInfo();
			
		
		String pathinfo = request.getPathInfo();
		OutputStream os = response.getOutputStream();
		//String customContentType = "";
		//String customContentDisposition = "";
		boolean result = false;
		boolean contenttypeset = false;
		try
		{
			if(pathinfo.equals("/") )
			{
					result = summaryProcessor_.processRequest(esClient_, os, request);
			}
			else if(pathinfo.equals("/_ewsearch") )
			{
					result = searchProcessor_.processRequest(esClient_, os, request, false);
			}
			else if(pathinfo.equals("/_ewsearchall") )
			{
				result = searchProcessor_.processRequest(esClient_, os, request, true);
			}
			else if(pathinfo.equals("/_ewsummary") )
			{
				result = summaryProcessor_.processRequest(esClient_, os, request);
			}
			else if(pathinfo.equals("/_ewtask") )
			{
				result = taskProcessor_.processRequest(esClient_, os, request);
			}
			else if(pathinfo.equals("/_ewbrowse") )
			{
				result = browseProcessor_.processRequest(esClient_, os, request);
			}
			else if(pathinfo.equals("/_ewinfo") )
			{
				result = infoProcessor_.processRequest(esClient_, os, request);
			}
			else if(pathinfo.equals("/_ewupload") )
			{
				result = uploadProcessor_.processRequest(esClient_, os, request);
			}
			else if(pathinfo.equals("/_ewget") )
			{
				//StringBuilder contenttype = new StringBuilder ();
				//StringBuilder customfilename = new StringBuilder ();
				result = getProcessor_.processRequest(esClient_, os, request, response /* pass reposne to modify content type and headers*/);
				contenttypeset = result;	//getProcessor returns true when attachement has been sent to the output
				//customContentType = contenttype.toString();
				//if( customfilename.length() > 0 )
				//{
				//	customContentDisposition = "attachment; filename=\""+customfilename+"\"";
				//}
			}
			
			else if(pathinfo.equals("/_ewgraphite") || pathinfo.startsWith("/metrics/find") || pathinfo.startsWith("/render"))
			{
				try {
					
					response.addHeader("Access-Control-Allow-Origin", "*");
					response.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS, POST");
					response.addHeader("Access-Control-Allow-Headers", "origin, authorization, accept");
					response.addHeader("Access-Control-Allow-Credentials", "true");
					
					result = graphiteProcessor_.processRequest(esClient_, os, request);
					
				} catch (MalformedObjectNameException e) {
					EWLogger.logerror(e);
					e.printStackTrace();
				} catch (IntrospectionException e) {
					EWLogger.logerror(e);
					e.printStackTrace();
				} catch (InstanceNotFoundException e) {
					EWLogger.logerror(e);
					e.printStackTrace();
				} catch (NullPointerException e) {
					EWLogger.logerror(e);
					e.printStackTrace();
				} catch (ReflectionException e) {
					EWLogger.logerror(e);
					e.printStackTrace();
				} catch (ParseException e) {
					EWLogger.logerror(e);
					e.printStackTrace();
				} catch (DDRFileNotFoundException e) {
					EWLogger.logerror(e);
					e.printStackTrace();
				}
			}
			else
			{
				os.write(responser.errorMessage(pathinfo + " rest point is not supported", ElasticWarehouseConf.URL_GUIDE));
			}

		} catch (org.elasticsearch.indices.IndexMissingException e) {
			EWLogger.logerror(e);
			accessor_.recreateTemplatesAndIndices(true);
			os.write(responser.errorMessage("One of indices was missing. Indices have been recreated - send your request once again.", ElasticWarehouseConf.URL_GUIDE));
		}

			/*HeaderPatternRule rule = new HeaderPatternRule();
	        rule.setPattern("*");
	        rule.setAdd(true);
	        rule.setName("CUSTOM_HEADER");
	        rule.setValue("zukovalue");
	        rule.apply(null, request, response);*/
	        
		if( result )
		{
					
			if( !contenttypeset )
				response.setContentType("text/html;charset=utf-8");

			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
	        //response.getWriter().println("Hello!");
	        //OutputStream os= response.getOutputStream();
	        //os.write("Hello!");
	        //os.flush();
		}else{
			response.setContentType("text/html;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
		}
        os.close();
        
    }
	
		
	

}
