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

import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;

public class DeleteByQueryAdapter {

	private final static Logger LOGGER = Logger.getLogger(DeleteByQueryAdapter.class.getName());
	private BoolQueryBuilder b_builder_ = null;
	private TermQueryBuilder t_builder_ = null;
	
	public DeleteByQueryAdapter(BoolQueryBuilder builder) {
		b_builder_ = builder;
	}

	public DeleteByQueryAdapter(TermQueryBuilder termQuery) {
		t_builder_ = termQuery;
	}

	public boolean execute(Client client, String indexName, String typeName ) {
		SearchRequestBuilder bldr = client.prepareSearch( indexName )
		    	//.setTypes( conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) )
		        .setSearchType(SearchType.SCAN)
		        .setScroll(new TimeValue(60000))
		        .setFetchSource(false)
		        //.setQuery( builder_ )
		        .setSize(20);
		if( typeName.length()>0)
			bldr.setTypes( typeName );
		if( b_builder_ != null )
			bldr.setQuery( b_builder_ );
		if( t_builder_ != null )
			bldr.setQuery( t_builder_ );
		
		SearchResponse scrollResp = bldr.execute().actionGet(); //10 hits per shard will be returned for each scroll//Scroll until no hits are returned
		
		while (true)
	    {
	    	scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
		    if( scrollResp.getHits().hits().length == 0 )
		    	break;
		    BulkRequestBuilder bulkDelete = client.prepareBulk();
		    for (SearchHit hit : scrollResp.getHits()) 
		    {
		    	bulkDelete.add(client.prepareDelete().setIndex(indexName).setType(typeName).setId(hit.getId()));
		    }
		    BulkResponse bulkResponse = bulkDelete.execute().actionGet();
		    if( bulkResponse.hasFailures() ) {
		    	LOGGER.error(bulkResponse.buildFailureMessage());
		    	return false;
		    }
		    
	    }
		return true;
	}

}
