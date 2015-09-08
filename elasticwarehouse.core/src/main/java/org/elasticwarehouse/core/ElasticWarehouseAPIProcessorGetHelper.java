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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Base64;
import org.elasticwarehouse.core.parsers.FileTools;
import org.elasticwarehouse.core.parsers.ParseTools;

public class ElasticWarehouseAPIProcessorGetHelper
{
	private ElasticWarehouseConf conf_ = null;
	private Client esClient_ = null;
	public ElasticWarehouseAPIProcessorGetHelper(ElasticWarehouseConf conf, Client esClient) {
		conf_ = conf;
		esClient_ = esClient;			
	}

	public class GetProcessorFileData
	{
		public byte[] bytes_ = null;
		public boolean exists_ = false;
		public boolean isFolder_ = false;
		public String filetype = "application/octet-stream";
		public String filename = "noname";
		public String filepath = "";
		public Integer imagewidth_ = 0;
		public Integer imageheight_ = 0;
		public boolean readyToWrite_ = false;
	}
	
	public GetProcessorFileData getFileBytes(String id, String type)throws IOException
	{
		GetProcessorFileData ret = new GetProcessorFileData();
		GetResponse response = esClient_.prepareGet(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE), id)
				.execute().actionGet();
			if( response.isExists() )
			{
				ret.exists_ = true;
				Map<String, Object> source = response.getSource();
				if(source.get("isfolder") != null && Boolean.parseBoolean( source.get("isfolder").toString() ) == true )
				{
					//os.write(responser.errorMessage("Provided id is a folder. Please provide file id.", ElasticWarehouseConf.URL_GUIDE_GET));
					ret.isFolder_ = true;
				}
				else
				{
					if( source.get("filename") != null )
						ret.filename = source.get("filename").toString();
					if( source.get("imagewidth") != null )
						ret.imagewidth_ = ParseTools.parseInt(source.get("imagewidth").toString());
					if( source.get("imageheight") != null )
						ret.imageheight_  = ParseTools.parseInt(source.get("imageheight").toString());
					
					Object filecontent = null;
					
					if( type != null && type.equals("thumb") )
					{
						//Map<String, Object> thumb = null; 
						//if( source.get("filethumb_thumb") != null )
						//	thumb = (Map<String, Object>) source.get("filethumb");
						//if( thumb != null )
						//{
							if( source.get(ElasticWarehouseConf.FIELD_THUMB_SAMEASIMAGE) != null )
							{
								if( Boolean.parseBoolean( source.get(ElasticWarehouseConf.FIELD_THUMB_SAMEASIMAGE).toString() ) == true )
									filecontent = source.get("filecontent");
								else
									filecontent = source.get(ElasticWarehouseConf.FIELD_THUMB_THUMB);
							}
						//}
						
						if( filecontent != null )
						{
							ret.filetype = "image/jpeg";
							ret.bytes_ = Base64.decode(filecontent.toString());
							//httpresponse.addHeader("Content-Disposition", "attachment; filename="+filename);
							//httpresponse.setContentType(filetype);
							//os.write(Base64.decode(filecontent.toString()));
							//ret = true;
						}//else{
						//	os.write(responser.errorMessage("Cannot get thumb for provided id.", ElasticWarehouseConf.URL_GUIDE_GET));
						//}
					}
					else
					{
						if( source.get("filetype") != null )
							ret.filetype = source.get("filetype").toString();
						filecontent = source.get("filecontent");
					
						//String filepath = "";
						if( filecontent == null )
						{
							byte[] data = null;
							
							boolean storeContent = conf_.getWarehouseBoolValue(ElasticWarehouseConf.STORECONTENT, true);
							if( storeContent == false )
							{
						     	//boolean storeMoveScanned = conf_.getWarehouseBoolValue(ElasticWarehouseConf.STOREMOVESCANNED, true);
						     	String storeFolder = conf_.getWarehouseValue(ElasticWarehouseConf.STOREFOLDER);
						     	String newpathname = storeFolder+"/"+id;
						     	if( FileTools.checkFileCanRead(newpathname) )
						     	{
						     		Path path = Paths.get(newpathname);
						     		data = Files.readAllBytes(path);
						     	}
							}
							
							//then index could be configured to be indexer only and not to store file contents
							//try to get file path for "origin" tag
							if( source.get("origin") != null && data == null )
							{
								Map<String, Object> origin = (Map<String, Object>) source.get("origin");
								String originpath = origin.get("path").toString();
								String originfilename = origin.get("filename").toString();
								ret.filepath = originpath+"/"+originfilename;
								if( FileTools.checkFileCanRead(ret.filepath) )
								{
									Path path = Paths.get(ret.filepath);
									data = Files.readAllBytes(path);
								}
							}

							if( data != null )
							{
								//httpresponse.addHeader("Content-Disposition", "attachment; filename="+filename);
								//httpresponse.setContentType(filetype);
								//os.write(data);
								ret.bytes_ = data;
								//ret = true;
							}//else{
							//	os.write(responser.errorMessage("File has been indexed without it's content. Origin file is also not available at path: "+ filepath, ElasticWarehouseConf.URL_GUIDE_GET));								
							//	ret = false;
							//}
						}else{
							//httpresponse.addHeader("Content-Disposition", "attachment; filename="+filename);
							//httpresponse.setContentType(filetype);
							//os.write(Base64.decode(filecontent.toString()));
							//ret = true;
							ret.bytes_ = Base64.decode(filecontent.toString());
						}
					}
				}
			}
			return ret;
	}
}
