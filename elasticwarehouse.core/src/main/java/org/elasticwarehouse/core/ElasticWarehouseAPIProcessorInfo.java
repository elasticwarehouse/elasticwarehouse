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

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestRequest;


public class ElasticWarehouseAPIProcessorInfo extends ElasticWarehouseAPIProcessor {
	private ElasticWarehouseConf conf_;
	private ElasticWarehouseReqRespHelper responser = new ElasticWarehouseReqRespHelper();
	private ElasticSearchAccessor elasticSearchAccessor_;
	
	public class ElasticWarehouseAPIProcessorInfoParams
	{
		public String id = "";

		public void readFrom(RestRequest orgrequest) {
			id = orgrequest.param("id");
		}
	};
	
	public ElasticWarehouseAPIProcessorInfo(ElasticWarehouseConf conf, ElasticSearchAccessor elasticSearchAccessor) {
		conf_ = conf;
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
		if( params.id == null )
		{
			os.write(responser.errorMessage("id attribute is mandatory.", ElasticWarehouseConf.URL_GUIDE_INFO));
		}
		else
		{
			GetResponse response = esClient.prepareGet(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE), params.id)
				.execute().actionGet();
			if( response.isExists() )
			{
				Map<String, Object> source = response.getSourceAsMap();
				source.put("version",  response.getVersion() );
				source.remove("filethumb_thumb");
				source.remove("filecontent");
				source.remove("filetext");
				source.remove("filenamena");
				source.remove("folderna");

				LinkedList<String> childfiles = elasticSearchAccessor_.findChildren(response.getId());
				source.put("children", childfiles);

				XContentBuilder builder = jsonBuilder();
				builder.map(source);
				os.write( builder.string().getBytes() );
				ret = true;
			}else{
				os.write(responser.errorMessage("provided id doesn't exist. Please provide correct file Id.", ElasticWarehouseConf.URL_GUIDE_INFO));
			}
		}
		return ret;
	}

	public ElasticWarehouseAPIProcessorInfoParams createEmptyParams() {
		return new ElasticWarehouseAPIProcessorInfoParams();
	}
}
