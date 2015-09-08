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
package org.elasticwarehouse.tasks;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;

public class ElasticWarehouseTaskDelete extends ElasticWarehouseTask {

	private ElasticWarehouseConf conf_ = null;
	private String fileid_ = null;
	
	public ElasticWarehouseTaskDelete(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, String id) {
		super(acccessor, conf);
		conf_ = conf;
		fileid_ = id;
	}
	
	public ElasticWarehouseTaskDelete(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, Map<String, Object> source)
	{
		super(acccessor, source, conf);
		fileid_ = source.get("fileid").toString();
		conf_ = conf;
	}

	@Override
	public String getActionString() {
		return "delete";
	}

	@Override
	public XContentBuilder vgetJsonSourceBuilder(XContentBuilder builder) throws IOException
	{
		builder.field("fileid",fileid_);
		return builder;
	}
	
	@Override
	public boolean isAsync()
	{
		return false;
	}
	
	@Override
	public void start()
	{
		progress_ = 100;
		setFinished();
		
		FolderTools toolset;
		try {
			toolset = new FolderTools(acccessor_, conf_);
			FolderToolsResult result = toolset.delete(fileid_);

			comment_ = result.comment_;
			errorcode_ = result.errorCode_;
			
		} catch (Exception e) {
			EWLogger.logerror(e);
			e.printStackTrace();
			errorcode_ = 50;
			comment_ = "Error:" + e.getMessage();
		}
		
		indexTask();
	}
}
