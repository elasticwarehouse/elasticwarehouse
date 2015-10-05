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

public class ElasticWarehouseTaskMove extends ElasticWarehouseTask {

	ElasticWarehouseConf conf_ = null;
	String fileid_ = null;
	String targetfolder_ = null;
	
	public ElasticWarehouseTaskMove(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, String id, String targetfolder) {
		super(acccessor, conf);
		conf_ = conf;
		fileid_ = id;
		targetfolder_ = targetfolder;
	}
	
	public ElasticWarehouseTaskMove(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, Map<String, Object> source)
	{
		super(acccessor, source, conf);
		fileid_ = source.get("fileid").toString();
		targetfolder_ = source.get("targetfolder").toString();
		conf_ = conf;
	}

	@Override
	public String getActionString() {
		return "move";
	}

	@Override
	public XContentBuilder vgetJsonSourceBuilder(XContentBuilder builder) throws IOException
	{
		builder.field("targetfolder",targetfolder_);
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
			
			if( acccessor_.folderExists(targetfolder_, false) == false )
			{
				FolderToolsResult mkresult = toolset.createFolder(targetfolder_);
				if( mkresult.errorCode_ != 0 )
				{
					comment_ = mkresult.comment_;
					errorcode_ = mkresult.errorCode_;
				}
			}
			
			if(errorcode_ == 0 )
			{
				FolderToolsResult result = toolset.moveTo(fileid_, targetfolder_);
			
				comment_ = result.comment_;
				errorcode_ = result.errorCode_;
			}
			
		} catch (Exception e) {
			EWLogger.logerror(e);
			e.printStackTrace();
			errorcode_ = ERROR_TASK_MOVE_OTHER_EXCEPTION;
			comment_ = "Error:" + e.getMessage();
		}
		
		indexTask();
	}
	
}
