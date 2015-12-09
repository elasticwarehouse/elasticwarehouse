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
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticwarehouse.tasks.ElasticWarehouseTasksManager;


public class ElasticWarehouseAPIProcessorInfo extends ElasticWarehouseAPIProcessor {
	private ElasticWarehouseConf conf_;
	private ElasticWarehouseReqRespHelper responser = new ElasticWarehouseReqRespHelper();
	private ElasticSearchAccessor elasticSearchAccessor_;
	private ElasticWarehouseTasksManager tasksManager_ = null;
	
	public class ElasticWarehouseAPIProcessorInfoParams
	{
		public String id = "";
		public String folder = "";
		public String filename = "";
		
		public boolean showrequest = false;
		
		//for setting purposes
		public boolean set = false;
		public String attribute = "";
		public String value = "";

		public void readFrom(RestRequest orgrequest) {
			id = orgrequest.param("id");
			folder = orgrequest.param("folder");
			filename = orgrequest.param("filename");
			showrequest = orgrequest.paramAsBoolean("showrequest", showrequest);
			
			set = orgrequest.paramAsBoolean("set", set);
			attribute = orgrequest.param("attribute");
			value = orgrequest.param("value");
		}
	};
	
	public ElasticWarehouseAPIProcessorInfo(ElasticWarehouseConf conf, ElasticSearchAccessor elasticSearchAccessor, ElasticWarehouseTasksManager tasksManager) {
		conf_ = conf;
		tasksManager_  = tasksManager;
		elasticSearchAccessor_ = elasticSearchAccessor;
	}
	
	public boolean processRequest(Client esClient, OutputStream os, HttpServletRequest request) throws IOException
	{
		String reqmethod = request.getMethod();	//GET,POST, etc
		ElasticWarehouseAPIProcessorInfoParams params = createEmptyParams();
		boolean ret = false;
		if( reqmethod.equals("GET") )
		{
			params.id = request.getParameter("id");
			params.folder = request.getParameter("folder");
			params.filename = request.getParameter("filename");
			
			if( params.folder != null )
				params.folder = params.folder.trim();
			if( params.filename != null )
				params.filename = params.filename.trim().toLowerCase();
			
			String sshowrequest = request.getParameter("showrequest");
			if( sshowrequest != null )
				params.showrequest = Boolean.parseBoolean(sshowrequest);
			
			String sset = request.getParameter("set");
			if( sset != null )
				params.set = Boolean.parseBoolean(sset);
			params.attribute = request.getParameter("attribute");
			params.value = request.getParameter("value");
			
			ret = processRequest(esClient, os, params);
			
		}else{
			os.write(responser.errorMessage("_ewbrowse restpoint expects GET requests only. For POST please use _search restpoint", ElasticWarehouseConf.URL_GUIDE_INFO));
		}
		
		return ret;
	}

	public boolean processRequest(Client esClient, OutputStream os, ElasticWarehouseAPIProcessorInfoParams params) throws IOException
	{
		boolean ret = false;
	    if (params.id == null)
	    {
	      os.write(this.responser.errorMessage("id attribute is mandatory.", "http://elasticwarehouse.org/guide-info/"));
	    }
	    else
	    {
	      GetResponse response = (GetResponse)esClient.prepareGet(this.conf_.getWarehouseValue("elasticsearch.index.storage.name"), this.conf_.getWarehouseValue("elasticsearch.index.storage.type"), params.id).execute().actionGet();
	      if (response.isExists())
	      {
	        Map<String, Object> source = response.getSourceAsMap();
	        source.put("version", Long.valueOf(response.getVersion()));
	        source.remove("filethumb_thumb");
	        source.remove("filecontent");
	        source.remove("filetext");
	        source.remove("filenamena");
	        source.remove("folderna");
	        
	        LinkedList<String> childfiles = this.elasticSearchAccessor_.findChildren(response.getId());
	        source.put("children", childfiles);
	        
	        XContentBuilder builder = XContentFactory.jsonBuilder();
	        builder.map(source);
	        os.write(builder.string().getBytes());
	        ret = true;
	      }
	      else
	      {
	        os.write(this.responser.errorMessage("provided id doesn't exist. Please provide correct file Id.", "http://elasticwarehouse.org/guide-info/"));
	      }
	    }
	    return ret;
	}

	public ElasticWarehouseAPIProcessorInfoParams createEmptyParams() {
		return new ElasticWarehouseAPIProcessorInfoParams();
	}
}
