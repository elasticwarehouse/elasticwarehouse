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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.elasticwarehouse.core.ElasticWarehouseAPIProcessorTask.ElasticWarehouseAPIProcessorTaskParams;
import org.elasticwarehouse.tasks.ElasticWarehouseTasksManager;


public class ElasticWarehouseServerAPI {

	private ElasticSearchAccessor acccessor_;
	private ElasticWarehouseTasksManager tasksManager_ = null;
	private ElasticWarehouseConf conf_;
	private ElasticWarehouseServerAPINotifier apiNotifier_;
	private ElasticWarehouseServerMonitoringNotifier monitoringNotifier_;
	
	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseServerAPI.class.getName());
	
	public void startServer(ElasticWarehouseConf conf, ElasticSearchAccessor acccessor, ElasticWarehouseServerAPINotifier apiNotifier, ElasticWarehouseServerMonitoringNotifier monitoringNotifier) throws IOException
	{
		acccessor_ = acccessor;
		apiNotifier_ = apiNotifier;
		monitoringNotifier_ = monitoringNotifier;
		conf_ = conf;
		int PORT = conf.getWarehouseIntValue(ElasticWarehouseConf.ESAPIPORT, ElasticWarehouseConf.APIPORT);
		Server server = null;
		
		int maxTries = 3;
		int attempt = 0; 
		while(attempt<maxTries)
		{
			try
			{
				tasksManager_ = new ElasticWarehouseTasksManager(acccessor_, conf, true);
				break;
			}
			catch(org.elasticsearch.action.search.SearchPhaseExecutionException e)
			{
				EWLogger.logerror(e);
				LOGGER.info("Cluster not ready, waiting....");
				//e.printStackTrace();
				attempt++;
				try {
					Thread.sleep(2000*attempt);
				} catch (InterruptedException e1) {
					EWLogger.logerror(e1);
					e1.printStackTrace();
				}
			}
		}
		
		if( tasksManager_ == null )
			throw new IOException("Cannot create tasks manager, cluster not ready");
		
		while (server == null)
		{
			try {
				server = new Server(PORT);
				ElasticWarehouseRequestHandlerAPI apihandler = new ElasticWarehouseRequestHandlerAPI(acccessor_, conf, tasksManager_, monitoringNotifier_);
			
				//HandlerWrapper wrapper = new HandlerWrapper();
				//wrapper.setHandler(apihandler);
				//HandlerCollection handlers=new HandlerCollection();
				
				ContextHandler context1 = new ContextHandler();
				context1.setContextPath( "/" );
				context1.setHandler( apihandler );
	        
				/*ResourceHandler reshandler = new ResourceHandler(){
					@Override protected void doResponseHeaders(HttpServletResponse response,
							 Resource resource,
							 String mimeType)
					{
						System.out.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
					}
				};
				
				ContextHandler context2 = new ContextHandler();
				context2.setContextPath( "/" );
				context2.setHandler( rewrite/*reshandler*/ /*);
				*/
				//HandlerList list=new HandlerList();
				//list.setHandlers(new Handler[]{reshandler, context});
				//handlers.setHandlers(new Handler[]{list});
				//server.setHandler(handlers);
	        
				/*ContextHandlerCollection contexts = new ContextHandlerCollection();
				contexts.setHandlers(new Handler[] { context1, context2 });
				
				server.setHandler(contexts);*/
	        
				server.setHandler(context1);
	        
	        
				//wrapper.setHandler(rewrite);
				LOGGER.info("Starting API handler on port:"+ PORT);
				conf_.setWarehouseValue(ElasticWarehouseConf.ESAPIPORT, PORT);
				notifyAPIinitialized();
				//server.setHandler( reshandler );
	 
				server.start();
				
				String tasks = conf_.getWarehouseValue(ElasticWarehouseConf.AUTOSTART_TASKS);
				if( tasks != null && tasks.length() >0 )
				{
					String[] tasksarray = tasks.split(",");
					LOGGER.info("Processing "+tasksarray.length+" autostart commands....");
					for(String command : tasksarray)
					{
						Long timestamp = System.currentTimeMillis()/1000;
						command = command.replace("{$now}", timestamp.toString());
						
						ElasticWarehouseAPIProcessorTaskParams params = apihandler.taskProcessor_.createEmptyParams();
				    	params.readFromURL(command);
						
				    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
				    	OutputStream os = new BufferedOutputStream(baos);
				    	
				    	boolean rc = apihandler.taskProcessor_.processRequest(acccessor_.getClient(), os, params);
				    	if( !rc )
				    		LOGGER.error("Cannot execute autostart command: " + command);
					}
				}
				server.join();
			} catch (java.net.BindException e ) {
				EWLogger.logerror(e);
				server = null;
				PORT++;
				LOGGER.warn("Port already used ["+e.getMessage()+"], using "+PORT);
			} catch (IOException e) {
				EWLogger.logerror(e);
				e.printStackTrace();
				break;
			} catch (Exception e) {
				EWLogger.logerror(e);
				e.printStackTrace();
				break;
			}
		}
	}

	private void notifyAPIinitialized() {
		apiNotifier_.notifyListeners(ElasticWarehouseServerAPINotifier.API_INITIALIZED);
		
	}
}
