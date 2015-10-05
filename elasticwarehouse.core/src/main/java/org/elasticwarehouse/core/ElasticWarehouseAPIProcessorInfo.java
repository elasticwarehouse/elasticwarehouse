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
				params.filename = params.filename.trim();
			
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
		//boolean showrequest = ParseTools.parseBooleanDirect(request.getParameter("showrequest"), false);
		boolean infoById = ( params.id!= null && params.id.length() > 0 );
		boolean infoByFilename = ( params.folder!= null && params.folder.length() > 0 && params.filename!= null && params.filename.length() > 0 );
		
		if( params.set && ( (params.attribute==null || params.attribute.length()==0) || (params.value==null || params.value.length()==0) )  )
		{
			os.write(responser.errorMessage("When set=true, parameters 'attribute' and 'value' must be also provided.", ElasticWarehouseConf.URL_GUIDE_INFO));
			return ret;
		}
		
		if( params.attribute != null && ElasticWarehouseMapping.availableFieldsForModification.contains(params.attribute) == false )
		{
			os.write(responser.errorMessage("Field " + params.attribute + " is not valid field name or it's not possible to change it's value via REST.", ElasticWarehouseConf.URL_GUIDE_INFO));
			return ret;
		}
		

		if( infoById == false && infoByFilename == false )
		{
			os.write(responser.errorMessage("Please provide id or folder/filename to get file information.", ElasticWarehouseConf.URL_GUIDE_INFO));
		}
		else
		{
			//Map<String, Object> source = null;
			//boolean isexists = false;
			//String id = "";
			//long version = 0;
			EwInfoTuple infotuple = null;
			if( infoById )
			{
				if( params.set )
					elasticSearchAccessor_.setFileAttribute(params.attribute, params.id, params.value);
				infotuple = elasticSearchAccessor_.getFileInfoById(params.id, params.showrequest, false);
				if( infotuple.isexists )
					infotuple.tasks = tasksManager_.getTasks(false, conf_.getNodeName()/* NetworkTools.getHostName()*/, 999, 0, false, true, infotuple.id );
			}
			else
			{
				SearchRequestBuilder esreq =esClient.prepareSearch(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) )
						.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) )
						.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
						.setFetchSource(null, new String[] {"filecontent","filetext"})
						.setVersion(true)
						.setSize(1);
				String folder = ResourceTools.preprocessFolderName(params.folder.toLowerCase());
				String possiblefolder = ResourceTools.preprocessFolderName(params.folder.toLowerCase()+"/"+params.filename);
				
				esreq.setQuery(
		        		QueryBuilders.boolQuery()
	    					//.must(QueryBuilders.termQuery("isfolder", true) )
	    					//.must(QueryBuilders.termQuery("folder", folder) 
	    					.must(QueryBuilders.prefixQuery("folderna", folder) )
	    					.must(QueryBuilders.termQuery("filenamena", params.filename))
	    					.must(QueryBuilders.termQuery("isfolder", false))
	    					//.should(QueryBuilders.termQuery("filenamena", possiblefolder))
	    					//.minimumNumberShouldMatch(1)
					);
				if( params.showrequest )
					System.out.println(esreq.toString());
				
				SearchResponse response = esreq.execute().actionGet();
				
				infotuple = new EwInfoTuple();
				infotuple.isexists = (response.getHits().getHits().length == 1);
				
				//if file doesn't exist , then maybe ID is a folder?
				if( !infotuple.isexists )
				{
					esreq.setQuery(
			        		QueryBuilders.boolQuery()
		    					.must(QueryBuilders.termQuery("folderna", possiblefolder) )
		    					.must(QueryBuilders.termQuery("isfolder", true))
						);
					if( params.showrequest )
						System.out.println(esreq.toString());
					
					response = esreq.execute().actionGet();
					infotuple.isexists = (response.getHits().getHits().length == 1);
				}
				if(infotuple.isexists )
				{
					infotuple.id = response.getHits().getAt(0).getId();
					infotuple.version = response.getHits().getAt(0).getVersion();
					infotuple.source = response.getHits().getAt(0).sourceAsMap();
					infotuple.tasks = tasksManager_.getTasks(false, conf_.getNodeName()/* NetworkTools.getHostName()*/, 999, 0, false, true, infotuple.id );
					
					if( params.set )
					{
						elasticSearchAccessor_.setFileAttribute(params.attribute, params.id, params.value);
						//and modify response to don't query again
						infotuple.source.put(params.attribute, params.value);
					}
				}
			}
			if( infotuple.isexists )
			{
				infotuple.source.put("version", infotuple.version );
				infotuple.source.put("id", infotuple.id );
				infotuple.source.put("activetasks", infotuple.tasks );
				infotuple.source.remove("filethumb_thumb");
				infotuple.source.remove("filecontent");
				infotuple.source.remove("filetext");
				infotuple.source.remove("filenamena");
				infotuple.source.remove("folderna");

				LinkedList<String> childfiles = elasticSearchAccessor_.findChildren(infotuple.id);
				infotuple.source.put("children", childfiles);

				XContentBuilder builder = jsonBuilder();
				builder.map(infotuple.source);
				os.write( builder.string().getBytes() );
				ret = true;
			}else{
				os.write(responser.errorMessage("provided id or folder/filename doesn't exist. Please provide correct file Id or path folder/filename.", ElasticWarehouseConf.URL_GUIDE_INFO));
			}
		}
		return ret;
	}

	public ElasticWarehouseAPIProcessorInfoParams createEmptyParams() {
		return new ElasticWarehouseAPIProcessorInfoParams();
	}
}
