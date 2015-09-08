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

import org.elasticsearch.rest.*;

import static org.elasticsearch.rest.RestStatus.BAD_REQUEST;
import static org.elasticsearch.rest.RestStatus.OK;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;

public abstract class ElasticWarehouseRestHandler extends BaseRestHandlerESVer {

	protected ElasticSearchAccessor elasticSearchAccessor_;
	protected ElasticWarehouseConf conf_ = null;
	
	public ElasticWarehouseRestHandler(Settings settings, RestController controller, Client client)
	{
		super(settings, controller, client); 
		
		conf_ = new ElasticWarehouseConf();
        conf_.setWarehouseValue(ElasticWarehouseConf.MODEEMBEDDED, false);
        
        elasticSearchAccessor_= new ElasticSearchAccessor(client, conf_);
	}

	@Override
	protected abstract void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception;
	
	protected void processErrorMessage(IOException e, RestChannel channel)
	{
		processErrorMessage( e.getMessage(), channel );
	}
	protected void processErrorMessage(String msg, RestChannel channel)
	{
    	XContentBuilder builder;
		try 
		{
			builder = channel.newBuilder();
			builder.startObject();
			builder.field("error", msg );
			builder.array("success", false );
			builder.endObject();
			channel.sendResponse(new BytesRestResponse(BAD_REQUEST, builder));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	protected void writeOut(RestChannel channel, OutputStream os, ByteArrayOutputStream baos)
	{
		try {
			os.flush();
		} catch (IOException e) {
			processErrorMessage(e, channel);
			e.printStackTrace();
			return;
		}

		channel.sendResponse(new BytesRestResponse(OK, baos.toString()) );
	}
}
