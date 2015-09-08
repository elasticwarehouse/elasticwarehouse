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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestRequest;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFolder;



public class ElasticWarehouseAPIProcessorUpload {

	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseAPIProcessorUpload.class.getName());
	
	private ElasticWarehouseConf conf_;
	private ElasticWarehouseReqRespHelper responser = new ElasticWarehouseReqRespHelper();
	private ElasticSearchAccessor elasticSearchAccessor_;
	
	public class ElasticWarehouseAPIProcessorUploadParams
	{
		public byte[] bytes = null;
		public String id = "";
		public String folder = "";
		public String filename = "";
		
		public boolean validateInputParams(Client esClient, OutputStream os) throws IOException
		{
			if( folder == null || folder.length() == 0)
			{
				os.write(responser.errorMessage("Provided folder is not valid.", ElasticWarehouseConf.URL_GUIDE_UPLOAD));
				return false;
			}
			if( filename == null || filename.length() == 0)
			{
				os.write(responser.errorMessage("Provided filename is not valid.", ElasticWarehouseConf.URL_GUIDE_UPLOAD));
				return false;
			}
			if( id != null )
			{
				GetResponse response = esClient.prepareGet(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE), id)
						.execute().actionGet();
				if( !response.isExists() ) {
					os.write(responser.errorMessage("Provided id is not valid.", ElasticWarehouseConf.URL_GUIDE_UPLOAD));
					return false;
				}
			}
			if( bytes == null || bytes.length == 0 )
			{
				os.write(responser.errorMessage("Please upload file to standard input.", ElasticWarehouseConf.URL_GUIDE_UPLOAD));
				return false;
			}
			return true;
		}

		public void readFrom(RestRequest orgrequest) {
			BytesReference reader = orgrequest.content();
			bytes = reader.toBytes();
					
			LOGGER.debug("Uploading "+bytes.length + " bytes");
			id = orgrequest.param("id");
			folder = orgrequest.param("folder", folder);
			filename = orgrequest.param("filename", filename);
		}
	};
	
	public ElasticWarehouseAPIProcessorUpload(ElasticWarehouseConf conf, ElasticSearchAccessor elasticSearchAccessor) {
		conf_ = conf;
		elasticSearchAccessor_ = elasticSearchAccessor;
	}
	
	
	
	public boolean processRequest(Client esClient, OutputStream os, HttpServletRequest request) throws IOException
	{
		String reqmethod = request.getMethod();	//GET,POST, etc
		ElasticWarehouseAPIProcessorUploadParams params = createEmptyParams();
		
		boolean ret = false;
		if( reqmethod.equals("POST") )
		{
			InputStream is = request.getInputStream();
			params.bytes = IOUtils.toByteArray(is);
			
			params.id = request.getParameter("id");	//if Id provided, then UPDATE, otherwise INSERT
			params.folder = request.getParameter("folder");
			params.filename = request.getParameter("filename");
			
			ret = processRequest(esClient, os, params);
			
		}else{
			os.write(responser.errorMessage("Upload request must be send by POST.", ElasticWarehouseConf.URL_GUIDE_UPLOAD));
		}

		return ret;
	}

	public boolean processRequest(Client esClient, OutputStream os, ElasticWarehouseAPIProcessorUploadParams params) throws FileNotFoundException, IOException 
	{
		boolean ret = false;
		if( params.validateInputParams(esClient, os) )
		{
			String tempfolder = conf_.getWarehouseValue(ElasticWarehouseConf.TMPPATH);
			
			String tmpfilename = params.filename;
			String tmpfilepath = tempfolder;
			if( !tmpfilepath.endsWith("/") )
				tmpfilepath+="/";
			LOGGER.debug("Uploading file to: "+tmpfilepath+tmpfilename);
			FileOutputStream fos = new FileOutputStream(tmpfilepath+tmpfilename);
			fos.write(params.bytes);
			fos.close();
			
			ElasticWarehouseFolder fldr = new ElasticWarehouseFolder(params.folder,conf_);
			elasticSearchAccessor_.indexFolder(fldr);
			
			IndexingResponse indexingreponse = elasticSearchAccessor_.uploadFile(tmpfilepath, tmpfilename , params.folder, params.id, "upload");
			if( indexingreponse != null )
			{
				if( indexingreponse.error_ == null && indexingreponse.error_.length() > 0 )
				{
					os.write(responser.errorMessage("Indexing error: "+indexingreponse.error_, ElasticWarehouseConf.URL_GUIDE_UPLOAD));
				}else{
					IndexResponse ir =indexingreponse.response_;
					
					XContentBuilder builder = jsonBuilder()
					         .startObject()
					         	 .field("id", ir.getId())
					             .field("version", ir.getVersion())
					             .field("created", ir.isCreated())
					             ;
					String s = builder.endObject().string();
					os.write(s.getBytes());
					ret = true;
				}
			}
			else
			{
				os.write(responser.errorMessage("Someting went wrong. Please upload file once again.", ElasticWarehouseConf.URL_GUIDE_UPLOAD));
			}
			Files.deleteIfExists(new File(tmpfilepath+tmpfilename).toPath());
		}
		return ret;
	}



	public ElasticWarehouseAPIProcessorUploadParams createEmptyParams() {
		return new ElasticWarehouseAPIProcessorUploadParams();
	}
	
}
