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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticwarehouse.core.ElasticWarehouseAPIProcessorSummary.ElasticWarehouseAPIProcessorSummaryParams;


public class ElasticWarehouseSummaryRequest extends ElasticWarehouseReqRespHelper{

	private ElasticWarehouseConf conf_;
	private String field_ = "";
	private int min_doc_count_ = 0;
	private int size_ = 0;
	private Boolean showrequest_ = false;
	private Boolean scanembedded_ = false;
	private String type_ = "terms";
	private int interval_ = 0;
	
	
	public final static String[] available_aggregations = { "terms", "histogram" };
	
	private ElasticWarehouseSummaryRequest(String type, String field, int min_doc_count, int interval, int size, 
			Boolean showrequest, Boolean scanembedded, ElasticWarehouseConf conf)
	{
		conf_ = conf;
		
		type_ = type;
		field_ = field;
		min_doc_count_ = min_doc_count;
		interval_  = interval;
		size_ = size;
		showrequest_ = showrequest;
		scanembedded_ = scanembedded;
	}
	public static ElasticWarehouseSummaryRequest parseSummaryRequest(ElasticWarehouseAPIProcessorSummaryParams params, OutputStream os, ElasticWarehouseConf conf) throws IOException
	{
		//Reader reader = request.getReader();
	    //String postData = IO.toString(reader);
		
		/*String field = request.getParameter("field");
		String size = request.getParameter("size");
		
		String type = request.getParameter("type");
		String min_doc_count = request.getParameter("min_doc_count");
		String interval = request.getParameter("interval");
		
		
		
		String showrequest = request.getParameter("showrequest");
		String scanembedded = request.getParameter("scanembedded");
		*/
		
	    ElasticWarehouseSummaryRequest ret = new ElasticWarehouseSummaryRequest(params.type, params.field, 
	    		params.min_doc_count,
	    		params.interval,
	    		params.size,
	    		params.showrequest, 
	    		params.scanembedded, 
	    		conf);
		return ret;
	}
	
	
	public SearchResponse process(Client esClient, OutputStream os) throws IOException
	{	
		SearchRequestBuilder request = esClient.prepareSearch(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/);
		if( scanembedded_ )
			request.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/, 
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE) /*ElasticWarehouseConf.defaultChildsTypeName_*/);
		else
			request.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/);
		request.setSearchType(SearchType.COUNT);
		
		//exclude text & file content
		//request.setFetchSource(null, new String[] {"filecontent","filetext"} );
	    
		//request.setQuery(QueryBuilders.matchAllQuery());
		if( type_.equals("terms") )
		{
			TermsBuilder tb = AggregationBuilders.terms("summary").field(field_);
			if( min_doc_count_> 0 ) tb.minDocCount(min_doc_count_);
			if( size_> 0 ) tb.size(size_);
			request.addAggregation(tb);
		}
		if( type_.equals("histogram") )
		{
			HistogramBuilder tb = AggregationBuilders.histogram("summary").field(field_);
			if( min_doc_count_> 0 ) tb.minDocCount(min_doc_count_);
			if( interval_> 0 ) tb.interval(interval_);
			request.addAggregation(tb);
		}
		
	    if( size_ > 0 )
	    	request.setSize(size_);
	    
	    if( showrequest_ )
	    {
	    	System.out.println(request.toString());
	    }
		SearchResponse response = request.execute().actionGet();
		
		return response;
	}
}
