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
import java.io.Reader;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.util.IO;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.search.highlight.FastVectorHighlighter;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder.Field;
import org.elasticwarehouse.core.parsers.ParseTools;


public class ElasticWarehouseAPIProcessorSearch extends ElasticWarehouseAPIProcessor
{
	public class ElasticWarehouseAPIProcessorSearchParams
	{
		public String q = "";
		public String qfolder = "";
		public int size = 10;
		public int from = 0;
		public int thumbsize = 180;
		public int fragmentSize = 300;
		public String pre_tags = "<em>";
		public String post_tags = "</em>";
		public boolean showrequest = false;
		public boolean highlight = false;
		public String postData = "";
		
		public ElasticWarehouseAPIProcessorSearchParams(ElasticWarehouseConf conf)
		{
			thumbsize = conf_.getWarehouseIntValue(ElasticWarehouseConf.THUMBSIZE, thumbsize);
			if( thumbsize != 90 && thumbsize != 180 && thumbsize != 360 && thumbsize != 720)
				thumbsize = 180;
		}

		public void readFrom(RestRequest orgrequest)
		{
			q = orgrequest.param("q");
			qfolder = orgrequest.param("folder", qfolder);
	    	size = orgrequest.paramAsInt("size", size);
	    	from = orgrequest.paramAsInt("from", from);
	    	showrequest = orgrequest.paramAsBoolean("showrequest", showrequest);
	    	
			fragmentSize = orgrequest.paramAsInt("fragmentSize", fragmentSize);
			highlight = orgrequest.paramAsBoolean("highlight", highlight);
			
			pre_tags = orgrequest.param("pretag", pre_tags);
			post_tags = orgrequest.param("posttag", post_tags);
		}
	};
	
	private ElasticWarehouseConf conf_;
	private ElasticWarehouseReqRespHelper responser = new ElasticWarehouseReqRespHelper();
	public ElasticWarehouseAPIProcessorSearch(ElasticWarehouseConf conf) {
		conf_ = conf;
	}

	public boolean processRequest(Client esClient, OutputStream os, HttpServletRequest request, boolean searchAll) throws IOException
	{
		String reqmethod = request.getMethod();	//GET,POST, etc
		ElasticWarehouseAPIProcessorSearchParams params = createEmptyParams();
		
		
		
		if( searchAll )
		{
			if( reqmethod.equals("GET") )
			{
				params.q = request.getParameter("q").toLowerCase();
				if( request.getParameter("folder")!=null)
					params.qfolder = request.getParameter("folder").toLowerCase();
				params.size = ParseTools.parseIntDirect(request.getParameter("size"), params.size);
				params.from = ParseTools.parseIntDirect(request.getParameter("from"), params.from);
				params.fragmentSize = ParseTools.parseIntDirect(request.getParameter("fragmentSize"), params.fragmentSize);
				params.showrequest = ParseTools.parseBooleanDirect(request.getParameter("showrequest"), false);
				params.highlight = ParseTools.parseBooleanDirect(request.getParameter("highlight"), false);
				if(request.getParameter("pretag") != null )
					params.pre_tags = request.getParameter("pretag");
				if(request.getParameter("posttag") != null )
					params.post_tags = request.getParameter("posttag");
			}
		}else{
			if( reqmethod.equals("POST") )
			{
				Reader reader = request.getReader();
				params.postData = IO.toString(reader);
			}
		}
		return processRequest(esClient, os, reqmethod, searchAll, params);
	}
	
	public ElasticWarehouseAPIProcessorSearchParams createEmptyParams() {
		return new ElasticWarehouseAPIProcessorSearchParams(conf_);
	}

	public boolean processRequest(Client esClient, OutputStream os,
			String reqmethod, boolean searchAll, ElasticWarehouseAPIProcessorSearchParams params)  throws IOException
	{
		if( searchAll )
		{
			if( reqmethod.equals("GET") )
			{
				if( params.q == null )
				{
					os.write(responser.errorMessage("q attribute is mandatory for _searchall restpoint.", ElasticWarehouseConf.URL_GUIDE_SEARCHALL));
				}else{
					SearchRequestBuilder esreq =esClient.prepareSearch(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/)
					        .setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/)
					        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
					        .setFetchSource(null, new String[] {"filecontent","filetext"});
					        //.setQuery(QueryBuilders.termQuery("_all", q))
					
					BoolQueryBuilder bqbuilder = QueryBuilders.boolQuery();
					if( params.qfolder.length() > 0 )
					{
						String fldr = params.qfolder;
						fldr = ResourceTools.preprocessFolderName(fldr.toLowerCase());
					
						if( params.qfolder.contains("*") )
							bqbuilder.must(QueryBuilders.wildcardQuery(ElasticWarehouseMapping.FIELDFOLDERNA, fldr));
						else
							bqbuilder.must(QueryBuilders.prefixQuery(ElasticWarehouseMapping.FIELDFOLDERNA, fldr) );
					}
							
					if( params.q.contains("*") )
						bqbuilder.must(QueryBuilders.queryStringQuery(params.q)
								.field("filename").field("filetitle").field("filetext").field("filemeta.metavaluetext")
								.field("customkeywords").field("customcomments"));
					else
						bqbuilder.must(QueryBuilders.multiMatchQuery(params.q, "filename","filetitle", "filetext", 
					        											"filemeta.metavaluetext", "customkeywords", "customcomments" /*, "filemeta.metavaluedate", "filemeta.metavaluelong"*/));
					        //.setPostFilter(FilterBuilders.rangeFilter("age").from(12).to(18))   // Filter
					        //.setExplain(true)
					esreq.setQuery(bqbuilder);
					esreq.setVersion(true)
					        .setSize(params.size)
					        .setFrom(params.from);
					if( params.highlight )
					{
						esreq.addHighlightedField(new HighlightBuilder.Field("filename").highlighterType("fvh").fragmentSize(params.fragmentSize).preTags(params.pre_tags).postTags(params.post_tags) );
						esreq.addHighlightedField(new HighlightBuilder.Field("filetitle").highlighterType("fvh").fragmentSize(params.fragmentSize).preTags(params.pre_tags).postTags(params.post_tags) );
						esreq.addHighlightedField(new HighlightBuilder.Field("filetext").highlighterType("fvh").fragmentSize(params.fragmentSize).preTags(params.pre_tags).postTags(params.post_tags) );
						esreq.addHighlightedField(new HighlightBuilder.Field("filemeta.metavaluetext").highlighterType("fvh").fragmentSize(params.fragmentSize).preTags(params.pre_tags).postTags(params.post_tags) );
						
						esreq.addHighlightedField(new HighlightBuilder.Field("customkeywords").highlighterType("fvh").fragmentSize(params.fragmentSize).preTags(params.pre_tags).postTags(params.post_tags) );
						esreq.addHighlightedField(new HighlightBuilder.Field("customcomments").highlighterType("fvh").fragmentSize(params.fragmentSize).preTags(params.pre_tags).postTags(params.post_tags) );
						
					}
					if( params.showrequest )
				    {
				    	System.out.println(esreq.toString());
				    }
					
					SearchResponse response = esreq.execute().actionGet();
					os.write(response.toString().getBytes());
				}
			}else{
				os.write(responser.errorMessage("_searchall restpoint expects GET requests only. For POST please use _search restpoint", ElasticWarehouseConf.URL_GUIDE_SEARCHALL));
			}
		}else{
			if( reqmethod.equals("POST") )
			{
				
				ElasticWarehouseSearchRequest parsedRequest = ElasticWarehouseSearchRequest.parseSearchRequest(params.postData, os, conf_);
				if( parsedRequest != null )
				{
					SearchResponse response = null;
					try {
						response = parsedRequest.process(esClient, os);
					} catch (ElasticWarehouseAPIExecutionException e) {
						os.write(responser.errorMessage(e.getMessage(), ElasticWarehouseConf.URL_GUIDE_SEARCH));
					}
					if( response != null )
						os.write(response.toString().getBytes());
					//else	error will be written to os by the 'process' method
					//	os.write(responser.errorMessage("Cannot process search request", ElasticWarehouseConf.URL_GUIDE_SEARCH));
				}//else{ possible errro will be written to os by the 'parseRequest' method
				//	os.write(responser.errorMessage("Cannot parse search request", ElasticWarehouseConf.URL_GUIDE_SEARCH));	
				//}
				
			}else{
				os.write(responser.errorMessage("_search restpoint expects POST requests only. For GET please use simplified version available via _searchall restpoint", ElasticWarehouseConf.URL_GUIDE_SEARCH));
			}
		}
		return true;
	}

}
