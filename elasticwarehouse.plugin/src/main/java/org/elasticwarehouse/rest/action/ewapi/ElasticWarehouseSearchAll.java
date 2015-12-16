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
package org.elasticwarehouse.rest.action.ewapi;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.rest.*;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseAPIProcessorSearch;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.ElasticWarehouseAPIProcessorSearch.ElasticWarehouseAPIProcessorSearchParams;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.elasticsearch.common.unit.TimeValue.parseTimeValue;
import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.BAD_REQUEST;
import static org.elasticsearch.rest.RestStatus.OK;
//import static org.elasticsearch.rest.action.support.RestXContentBuilder.restContentBuilder;
//import static org.elasticsearch.search.suggest.SuggestBuilder.termSuggestion;


public class ElasticWarehouseSearchAll extends ElasticWarehouseRestHandler {

	protected Settings settings_;
	//protected RestController controller_;
	protected Client client_;
	
	private ElasticWarehouseAPIProcessorSearch processor_;
	
    @Inject public ElasticWarehouseSearchAll(Settings settings, Client client, RestController controller) {
        super(settings, controller, client);

		settings_ = settings;
		client_ = client;
		//controller_ = controller;
        
        controller.registerHandler(GET, "/_ewsearchall", this);
        //controller.registerHandler(GET, "/{index}/_ewsearchall", this);
        //controller.registerHandler(GET, "/{index}/{type}/_myrestpoint", this);
        //controller.registerHandler(POST, "/{index}/{type}/_myrestpoint", this);
        
        
        processor_ = new ElasticWarehouseAPIProcessorSearch(conf_);
    }

    public void handleRequest(final RestRequest orgrequest, final RestChannel channel, final Client client)
    {
    	logger.debug("_ewsearchall called");
    	
    	ElasticWarehouseAPIProcessorSearchParams params = processor_.createEmptyParams();
    	params.readFrom(orgrequest);

    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	OutputStream os = new BufferedOutputStream(baos);
		boolean retryRequest = false;
        try {
			processor_.processRequest(client, os, "GET", true, params);
		}
        catch(IndexNotFoundException e){
        	elasticSearchAccessor_.recreateTemplatesAndIndices(false);
        	retryRequest = true;
		}
        catch (IOException e) {
			processErrorMessage(e, channel);
			e.printStackTrace();
			return;
		}
        if( retryRequest )
        {
			try {
				processor_.processRequest(client, os, "GET", true, params);
			} catch (IOException e1) {
				processErrorMessage(e1, channel);
				e1.printStackTrace();
			}
    	}
        writeOut(channel, os, baos);

        return;
    }

	

	

}
