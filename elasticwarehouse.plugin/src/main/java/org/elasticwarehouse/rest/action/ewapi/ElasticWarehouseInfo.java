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

import static org.elasticsearch.rest.RestRequest.Method.GET;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticwarehouse.core.ElasticWarehouseAPIProcessorInfo;
import org.elasticwarehouse.core.ElasticWarehouseAPIProcessorInfo.ElasticWarehouseAPIProcessorInfoParams;
import org.elasticwarehouse.tasks.ElasticWarehouseTasksManager;


public class ElasticWarehouseInfo extends ElasticWarehouseRestHandler {

	protected Settings settings_;
	//protected RestController controller_;
	protected Client client_;
	
	private ElasticWarehouseAPIProcessorInfo processor_;
	private ElasticWarehouseTasksManager tasksManager_;
	
    @Inject public ElasticWarehouseInfo(Settings settings, Client client, RestController controller) {
        super(settings, controller, client);

		settings_ = settings;
		client_ = client;
		//controller_ = controller;
        
        controller.registerHandler(GET, "/_ewinfo", this);
        //controller.registerHandler(GET, "/{index}/_ewsearchall", this);
        //controller.registerHandler(GET, "/{index}/{type}/_myrestpoint", this);
        //controller.registerHandler(POST, "/{index}/{type}/_myrestpoint", this);
        tasksManager_ = new ElasticWarehouseTasksManager(elasticSearchAccessor_, conf_, false);
        
        processor_ = new ElasticWarehouseAPIProcessorInfo(conf_, elasticSearchAccessor_, tasksManager_);
    }

    public void handleRequest(final RestRequest orgrequest, final RestChannel channel, final Client client)
    {
    	logger.debug("_ewinfo called");
    	
    	ElasticWarehouseAPIProcessorInfoParams params = processor_.createEmptyParams();
    	params.readFrom(orgrequest);

    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	OutputStream os = new BufferedOutputStream(baos);
		boolean retryRequest = false;
        try {
			processor_.processRequest(client, os, params);
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
				processor_.processRequest(client, os, params);
			} catch (IOException e1) {
				processErrorMessage(e1, channel);
				e1.printStackTrace();
			}
    	}
        writeOut(channel, os, baos);

        return;
    }

}
