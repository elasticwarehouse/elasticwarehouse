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
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Base64;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.rest.RestRequest;
import org.elasticwarehouse.core.ElasticWarehouseAPIProcessorGetHelper.GetProcessorFileData;
import org.elasticwarehouse.core.ElasticWarehouseAPIProcessorUpload.ElasticWarehouseAPIProcessorUploadParams;
import org.elasticwarehouse.core.parsers.FileTools;
import org.elasticwarehouse.core.parsers.ParseTools;


public class ElasticWarehouseAPIProcessorGet extends ElasticWarehouseAPIProcessor {

	private ElasticWarehouseConf conf_;
	private ElasticWarehouseReqRespHelper responser = new ElasticWarehouseReqRespHelper();
	//private ElasticSearchAccessor elasticSearchAccessor_;
	
	public class ElasticWarehouseAPIProcessorGetParams
	{
		public String id = "";
		public String type = "";

		public void readFrom(RestRequest orgrequest) {
			id = orgrequest.param("id");
			type = orgrequest.param("type", type);
		}
	};
	
	public ElasticWarehouseAPIProcessorGet(ElasticWarehouseConf conf, ElasticSearchAccessor elasticSearchAccessor) {
		conf_ = conf;
		//elasticSearchAccessor_ = elasticSearchAccessor;
	}

	
	public boolean processRequest(Client esClient, OutputStream os, HttpServletRequest request, HttpServletResponse httpresponse) throws IOException
	{
		String reqmethod = request.getMethod();	//GET,POST, etc
		ElasticWarehouseAPIProcessorGetParams params = createEmptyParams();
		
		boolean ret = false;
		if( reqmethod.equals("GET") )
		{
			params.id = request.getParameter("id");
			params.type = request.getParameter("type");
			
			GetProcessorFileData fd = processRequest(esClient, os, params);
			if( fd != null && fd.readyToWrite_ )
			{
				httpresponse.addHeader("Content-Disposition", "attachment; filename="+fd.filename);
				httpresponse.setContentType(fd.filetype);
				os.write(fd.bytes_);
				ret = true;
			}
			
		}else{
			os.write(responser.errorMessage("_ewbrowse restpoint expects GET requests only. For POST please use _search restpoint", ElasticWarehouseConf.URL_GUIDE_GET));
		}
		
		return ret;
	}
	
	public GetProcessorFileData processRequest(Client esClient, OutputStream os, ElasticWarehouseAPIProcessorGetParams params) throws IOException
	{
		GetProcessorFileData filedata = null;
		if( params.id == null )
		{
			os.write(responser.errorMessage("id attribute is mandatory.", ElasticWarehouseConf.URL_GUIDE_GET));
		}
		else
		{
			ElasticWarehouseAPIProcessorGetHelper pr = new ElasticWarehouseAPIProcessorGetHelper(conf_, esClient);
			filedata = pr.getFileBytes(params.id, params.type);
			
			if( filedata.exists_ )
			{
				if( filedata.isFolder_ )
				{
					os.write(responser.errorMessage("Provided id is a folder. Please provide file id.", ElasticWarehouseConf.URL_GUIDE_GET));
				}
				else
				{
					if( filedata.bytes_ != null )
					{
						filedata.readyToWrite_ = true;
						//httpresponse.addHeader("Content-Disposition", "attachment; filename="+filedata.filename);
						//httpresponse.setContentType(filedata.filetype);
						//os.write(filedata.bytes_);
					}else{
						if( params.type.equals("thumb"))
							os.write(responser.errorMessage("Cannot get thumb for provided id.", ElasticWarehouseConf.URL_GUIDE_GET));
						else
							os.write(responser.errorMessage("File has been indexed without it's content. Origin file is also not available at path: "+ filedata.filepath, ElasticWarehouseConf.URL_GUIDE_GET));								
					}
					
				}
			}else{
				os.write(responser.errorMessage("provided id doesn't exist. Please provide correct file Id.", ElasticWarehouseConf.URL_GUIDE_GET));
			}
		}
		return filedata;
	}


	public ElasticWarehouseAPIProcessorGetParams createEmptyParams() {
		return new ElasticWarehouseAPIProcessorGetParams();
	}
}
