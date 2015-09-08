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
package org.elasticwarehouse.core.parsers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.IndexingResponse;

public abstract class ElasticWarehouseFileParser
{
	protected ElasticWarehouseFile file = null;
	protected ElasticWarehouseConf conf_;
	//public abstract ElasticWarehouseFile getParsedObject();
	public abstract boolean parse() throws IOException;
	
	
	public ElasticWarehouseFileParser(String uploadedfilename, String fname, String targetfolder, ElasticWarehouseConf conf) throws IOException
	{
		conf_ = conf;
		file = new ElasticWarehouseFile(uploadedfilename, fname, targetfolder, conf);
	}
	
	public IndexingResponse indexParsedFile(ElasticSearchAccessor tmpAccessor, String id, long tt, String source, String originpath, String originfilename)
	{
		ElasticWarehouseFile file = getParsedObject();
		if( id != null && id.length()>0)
		{
			file.setId(id);
			//copy some persitant information, like first upload timestamp
			
			GetResponse response = tmpAccessor.getClient().prepareGet(
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), 
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE), id)
					.execute().actionGet();
			String firstUploadTime = response.getSource().get("fileuploaddate").toString();
			file.fileuploaddate_ = firstUploadTime;
		}
		file.setStat(ElasticWarehouseFile.STAT_PARSE_TIME, tt);
		file.originSetSource(source);
		file.originSetPath(originpath);
		file.originSetFilename(originfilename);
		return tmpAccessor.indexFile(file);
	}
	
	
	public ElasticWarehouseFile getParsedObject()
	{
		return file;
	}
}
