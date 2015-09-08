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

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestRequest;
import org.elasticwarehouse.tasks.ElasticWarehouseTasksManager;

public class ElasticWarehouseAPIProcessorSummary extends ElasticWarehouseAPIProcessor {

	private ElasticWarehouseConf conf_;
	private ElasticSearchAccessor elasticSearchAccessor_;
	private ElasticWarehouseReqRespHelper responser = new ElasticWarehouseReqRespHelper();
	private ElasticWarehouseTasksManager tasksManager_;
	
	public class ElasticWarehouseAPIProcessorSummaryParams
	{
		public String field = "";
		public String type = "";
		public int size = 10;
		public int min_doc_count;
		public int interval;
		public boolean showrequest = false;
		public boolean scanembedded = false;
		
		public void readFrom(RestRequest orgrequest) {
			field = orgrequest.param("field");
			type = orgrequest.param("type", type);
			
			size = orgrequest.paramAsInt("size", size);
			min_doc_count = orgrequest.paramAsInt("min_doc_count", min_doc_count);
			interval = orgrequest.paramAsInt("interval", interval);
			
			showrequest = orgrequest.paramAsBoolean("showrequest", showrequest);
			scanembedded = orgrequest.paramAsBoolean("scanembedded", scanembedded);
		}
	};
	
	public ElasticWarehouseAPIProcessorSummary(ElasticWarehouseConf conf, ElasticSearchAccessor elasticSearchAccessor, ElasticWarehouseTasksManager tasksManager) {
		conf_ = conf;
		elasticSearchAccessor_ = elasticSearchAccessor;
		tasksManager_ = tasksManager;
	}
	public boolean processRequest(Client esClient, OutputStream os, HttpServletRequest request) throws IOException
	{
		//String reqmethod = request.getMethod();	//GET,POST, etc
		String pathinfo = request.getPathInfo();
		ElasticWarehouseAPIProcessorSummaryParams params = createEmptyParams();
		params.field = request.getParameter("field");
		params.type = request.getParameter("type");

		String min_doc_count = request.getParameter("min_doc_count");
		String interval = request.getParameter("interval");
		String size = request.getParameter("size");
		String showrequest = request.getParameter("showrequest");
		String scanembedded = request.getParameter("scanembedded");
		

		params.min_doc_count = (min_doc_count==null?0:Integer.parseInt(min_doc_count));
		params.interval = (interval==null?0:Integer.parseInt(interval));
		params.size = (size==null?0:Integer.parseInt(size));
		params.showrequest = (showrequest==null?false:Boolean.parseBoolean(showrequest));
		params.scanembedded = (scanembedded==null?false:Boolean.parseBoolean(scanembedded));
		
		boolean ret = processRequest(esClient, os, pathinfo, params);
		
		return ret;
	}
	public boolean processRequest(Client esClient, OutputStream os, String pathinfo, ElasticWarehouseAPIProcessorSummaryParams params) throws IOException
	{
		if(pathinfo.equals("/") )
		{
			Map<String, String> conf = conf_.getWarehouseConfiguration();
			XContentBuilder builder;
			try {
				builder = jsonBuilder();
				builder.startObject();
				
				builder.field("started" ,conf_.getStartedDateAsString() );
				builder.field("uptime_sec" ,conf_.getUpTime() );
				builder.field("uptime_min" ,conf_.getUpTime()/60 );
				builder.field("uptime_h" ,conf_.getUpTime()/3600 );
				
				
				builder.startObject("configuration");
				
				Iterator<Entry<String, String>> it = conf.entrySet().iterator();
			    while (it.hasNext()) {
			        Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
			        builder.field(pair.getKey() ,pair.getValue());

			    }
	         	builder.endObject();
			 
	         	String confpath = conf_.getConfigurationPath();
	         	builder.field("elastic_search_config_file" ,confpath+"/elasticsearch.yml");
	         	builder.field("elastic_warehouse_config_file" ,confpath+"/elasticwarehouse.yml");
	         	
	         	boolean storeContent = conf_.getWarehouseBoolValue(ElasticWarehouseConf.STORECONTENT, true);
	         	boolean storeMoveScanned = conf_.getWarehouseBoolValue(ElasticWarehouseConf.STOREMOVESCANNED, true);
	         	String storeFolder = conf_.getWarehouseValue(ElasticWarehouseConf.STOREFOLDER);
	         	builder.startObject("store");
	         	builder.field("content" ,storeContent);
	         	builder.field("folder" ,storeFolder);
	         	builder.field("movescanned" ,storeMoveScanned);
	         	builder.endObject();
	         	
	         	//*** show grafana information
	         	builder.startObject("grafana");
	         	int grafanaPort = conf_.getWarehouseIntValue(ElasticWarehouseConf.GRAFANAPORT, ElasticWarehouseConf.GRAFANARESTPORT);
	    		boolean isEmbeddedMode = conf_.getWarehouseBoolValue(ElasticWarehouseConf.MODEEMBEDDED, true);
	    		int apiPort = conf_.getWarehouseIntValue(ElasticWarehouseConf.ESAPIPORT, ElasticWarehouseConf.APIPORT);
	    		
	         	if( isEmbeddedMode )
	         	{
	         		builder.field("enabled" ,"true");
	         		builder.field("url" ,"http://localhost:"+grafanaPort);
	         	}
	         	else
	         	{
	         		builder.field("enabled" ,"false");
	         	}
	         	builder.endObject();
	         	
	         	//*** show api information
	         	builder.startObject("api");
         		builder.field("enabled" ,"true");
         		builder.field("url" ,"http://localhost:"+apiPort);
	         	builder.endObject();
	         	
	         	//*** show ES information
	         	final String esHostPort = elasticSearchAccessor_.getHostPort(); 
	         	builder.startObject("elasticsearch");
         		builder.field("enabled" ,"true");
         		builder.field("url" ,"http://"+esHostPort);
         		builder.startObject("plugins");
         		builder.field("plugin_head" ,"http://"+esHostPort+"/_plugin/head");
         		builder.field("plugin_kopf" ,"http://"+esHostPort+"/_plugin/kopf");
         		builder.endObject();
	         	builder.endObject();
	         	
	         	//*** show running tasks
	         	LinkedList<String> tasks = tasksManager_.getRunningTasks();
	         	builder.field("runningtasks", tasks);
	         	
	         	//*** serialize
	         	byte[] result = builder.endObject().string().getBytes();
			 	os.write(result);
			} catch (IOException e) {
				EWLogger.logerror(e);
				e.printStackTrace();
			}

		}else{
			boolean responseWritten = false;

			boolean typeok = false;
			for(int i=0;i<ElasticWarehouseSummaryRequest.available_aggregations.length;i++)
			{
				if( ElasticWarehouseSummaryRequest.available_aggregations[i].equals(params.type) )
				{
					typeok = true;
					break;
				}
			}
			
			if( params.field == null || params.type == null )
			{
				os.write(responser.errorMessage("'field' and 'type' attributes are expected.", ElasticWarehouseConf.URL_GUIDE_SUMMARY));
				responseWritten = true;
			}
			else if( typeok == false )
			{
				String msg = "Aggregation type not recognized, use one of: ";
				for(int i=0;i<ElasticWarehouseSummaryRequest.available_aggregations.length;i++)
					msg += ElasticWarehouseSummaryRequest.available_aggregations[i] +" ";
				os.write(responser.errorMessage(msg, ElasticWarehouseConf.URL_GUIDE_SUMMARY));
				responseWritten = true;
			}
			else
			{
				ElasticWarehouseSummaryRequest parsedRequest = ElasticWarehouseSummaryRequest.parseSummaryRequest(params, os, conf_);
				if( parsedRequest != null )
				{
					SearchResponse response = parsedRequest.process(elasticSearchAccessor_.getClient(), os);
					if( response != null )
					{
						os.write(response.toString().getBytes());
						responseWritten = true;
					}
				}
			}
			
			if(!responseWritten)
			{
				os.write(responser.errorMessage("Cannot execute summary request for provided parameters.", ElasticWarehouseConf.URL_GUIDE_SUMMARY));
			}
		}
		return true;
	}
	
	public ElasticWarehouseAPIProcessorSummaryParams createEmptyParams() {
		return new ElasticWarehouseAPIProcessorSummaryParams();
	}

}
