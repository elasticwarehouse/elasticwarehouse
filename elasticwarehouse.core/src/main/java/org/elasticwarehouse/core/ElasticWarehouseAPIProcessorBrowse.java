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
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MatchQueryBuilder.Type;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFolder;
import org.elasticwarehouse.core.parsers.ParseTools;

public class ElasticWarehouseAPIProcessorBrowse extends ElasticWarehouseAPIProcessor {

	private ElasticWarehouseConf conf_;
	private ElasticWarehouseReqRespHelper responser = new ElasticWarehouseReqRespHelper();
	//private ElasticSearchAccessor elasticSearchAccessor_;
	
	public class ElasticWarehouseAPIProcessorBrowseParams
	{
		public String folder = "";
		public int size = 10;
		public int from = 0;
		public int thumbsize = 180;
		public boolean showrequest = false;
		
		public ElasticWarehouseAPIProcessorBrowseParams()
		{
			thumbsize = conf_.getWarehouseIntValue(ElasticWarehouseConf.THUMBSIZE, thumbsize);
			if( thumbsize != 90 && thumbsize != 180 && thumbsize != 360 && thumbsize != 720)
				thumbsize = 180;
		}

		public void readFrom(RestRequest orgrequest) {
			folder = orgrequest.param("folder");
	    	size = orgrequest.paramAsInt("size", size);
	    	from = orgrequest.paramAsInt("from", from);
	    	showrequest = orgrequest.paramAsBoolean("showrequest", showrequest);
		}
	};
	
	public ElasticWarehouseAPIProcessorBrowse(ElasticWarehouseConf conf, ElasticSearchAccessor elasticSearchAccessor) {
		conf_ = conf;
		//elasticSearchAccessor_ = elasticSearchAccessor;
	}
	
	public boolean processRequest(Client esClient, OutputStream os, HttpServletRequest request) throws IOException
	{
		String reqmethod = request.getMethod();	//GET,POST, etc
		ElasticWarehouseAPIProcessorBrowseParams params = createEmptyParams();
		boolean ret = false;
		
		if( reqmethod.equals("GET") )
		{
			params.folder = request.getParameter("folder");
			params.size = ParseTools.parseIntDirect(request.getParameter("size"), params.size);
			params.from = ParseTools.parseIntDirect(request.getParameter("from"), params.from);
			params.showrequest = ParseTools.parseBooleanDirect(request.getParameter("showrequest"), params.showrequest);
		
			ret = processRequest(esClient, os, params);
			
		}else{
			os.write(responser.errorMessage("_ewbrowse restpoint expects GET requests only. For POST please use _search restpoint", ElasticWarehouseConf.URL_GUIDE_BROWSE));
		}
		
		return ret;
	}

	public boolean processRequest(Client esClient, OutputStream os, ElasticWarehouseAPIProcessorBrowseParams params) throws IOException
	{
		boolean ret = false;
		if( params.folder == null )
		{
			os.write(responser.errorMessage("folder attribute is mandatory.", ElasticWarehouseConf.URL_GUIDE_BROWSE));
		}
		else
		{
			//folder = folder.toLowerCase();
			String folder = ResourceTools.preprocessFolderName(params.folder);
			//http://localhost:10200/_ewbrowse?folder=/&size=10&from=10 
			ElasticWarehouseFolder fldr = new ElasticWarehouseFolder(folder, conf_);
			int level = fldr.getFolderLevel();
			level++;
			//if( level == 0 )
			//	level = 1;
			
			SearchRequestBuilder esreq = esClient.prepareSearch(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) )
		        .setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) )
		        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
		        .setFetchSource(null, new String[] {"filecontent","filetext"});
	        if( folder.equals("/") )
	        	esreq.setQuery( QueryBuilders.termQuery("folderlevel", level) );
        	else
	        	esreq.setQuery(
	        		QueryBuilders.boolQuery()
	        			.must( QueryBuilders.matchQuery("folderna", folder).type(Type.PHRASE_PREFIX) )	//uzylem pola folderna zeby w przypadku dwoch folderow /home/zuko oraz /home/zuko2 i wyszukiwania /home/zuko nie pokzwywal zawartosci /home/zuko2
	        			.must( QueryBuilders.termQuery("folderlevel", level) )
	        			);
		        //else, zakompnetowane gdyz moze byc wiele roznych sciezek o ttym samym levelu, a user chce wylistowac tylko jeden folder, tak wiec "folder" musi byc uzupelniony
		        //	esreq.setQuery(
			    //    			QueryBuilders.termQuery("folderlevel", level)
			    //    			);
	        esreq
		        .addSort(SortBuilders.fieldSort("isfolder").order(SortOrder.DESC).ignoreUnmapped(true))
		        .addSort(SortBuilders.fieldSort("folderna").order(SortOrder.ASC).ignoreUnmapped(true))
		        .addSort(SortBuilders.fieldSort("filenamena").order(SortOrder.ASC).ignoreUnmapped(true))
		        .setVersion(true)
		        .setSize(params.size)
		        .setFrom(params.from); 
			if( params.showrequest )
		    {
		    	System.out.println(esreq.toString());
		    }
			
			SearchResponse response = esreq.execute().actionGet();
			
			os.write(response.toString().getBytes());
		}
		return ret;
	}

	public ElasticWarehouseAPIProcessorBrowseParams createEmptyParams() {
		return new ElasticWarehouseAPIProcessorBrowseParams();
	}

}
