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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.elasticwarehouse.core.graphite.NetworkTools;

import com.sun.net.httpserver.HttpServer;

public class ElasticWarehouseServerGrafana {

	private final static Logger LOGGER = Logger.getLogger(ElasticSearchAccessor.class.getName());
	private ElasticWarehouseServerAPINotifier apiNotifier_; 
	
	public void startServer(ElasticWarehouseConf conf, String esHostPort, ElasticWarehouseServerAPINotifier apiNotifier)
	{
		apiNotifier_ = apiNotifier;
		int grafanaPort = conf.getWarehouseIntValue(ElasticWarehouseConf.GRAFANAPORT, ElasticWarehouseConf.GRAFANARESTPORT);
		boolean isEmbeddedMode = conf.getWarehouseBoolValue(ElasticWarehouseConf.MODEEMBEDDED, true);
		int apiPort = conf.getWarehouseIntValue(ElasticWarehouseConf.ESAPIPORT, ElasticWarehouseConf.APIPORT);
		//String localIP = NetworkTools.getHostIP();
		
		Server server = null;
		while (server == null)
		{
			try {
				/*
				server = HttpServer.create(new InetSocketAddress(PORT), BACKLOG);
				//server.createContext("/app/grafana", new ElasticWarehouseRequestHandler());
				server.createContext("/", new ElasticWarehouseRequestHandler());
				//LinkedBlockingQueue queue = new LinkedBlockingQueue();
				//ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 10, 1, TimeUnit.MINUTES, queue);
				//server.setExecutor(executor);
				server.setExecutor(null); // creates a default executor
				server.start();*/
				LOGGER.info("Waiting for API to be initialized....");
				apiNotifier_.waitFor(ElasticWarehouseServerAPINotifier.API_INITIALIZED);
				LOGGER.info("API initialized, running grafana....");
				apiPort = conf.getWarehouseIntValue(ElasticWarehouseConf.ESAPIPORT, ElasticWarehouseConf.APIPORT);
				
				server = new Server(grafanaPort);
				//ServerConnector  connector = new ServerConnector(server);
				//connector.setPort(8081);
				//server.addConnector(connector);
		 
				ContextHandler context = new ContextHandler();
				context.setContextPath( "/" );
				context.setHandler( new ElasticWarehouseRequestHandlerGrafana(isEmbeddedMode, esHostPort, /*localIP+":"+*/apiPort) );
				
				server.setHandler( context );
				
				/*ResourceHandler resource_handler = new ResourceHandler();
				resource_handler.setDirectoriesListed(true);
				resource_handler.setWelcomeFiles(new String[]{ "index.html" });
		 
				resource_handler.setResourceBase("resources/res/grafana-1.9.0");
		 
				HandlerList handlers = new HandlerList();
				handlers.setHandlers(new Handler[] { new HelloWorld(), new DefaultHandler() });
				server.setHandler(handlers);*/
		 
				server.start();
				server.join();
			} catch (java.net.BindException e ) {
				server = null;
				grafanaPort++;
				LOGGER.warn("Port already used ["+e.getMessage()+"], using "+grafanaPort);
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

}
