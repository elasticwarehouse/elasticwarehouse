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

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFile;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFileParser;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFileParserFactory;
import org.elasticwarehouse.core.parsers.FileTools;
import org.elasticwarehouse.core.parsers.ParseTools;

public class ElasticWarehouseTaskRmdir extends ElasticWarehouseTask {

	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseTaskScan.class.getName());
	
	//Thread th_ = null;
	String targetfolder_ = null;
	ElasticWarehouseConf conf_ = null;
	
	public ElasticWarehouseTaskRmdir(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, String folder) {
		super(acccessor, conf);
		conf_ = conf;
		targetfolder_ = folder;
	}
	
	public ElasticWarehouseTaskRmdir(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, Map<String, Object> source)
	{
		super(acccessor, source, conf);
		targetfolder_ = source.get("targetfolder").toString();
		conf_ = conf;
	}

	@Override
	public String getActionString() {
		return "rmdir";
	}

	@Override
	public XContentBuilder vgetJsonSourceBuilder(XContentBuilder builder) throws IOException
	{
		builder.field("targetfolder", targetfolder_);
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
		
		FolderTools toolset = new FolderTools(acccessor_, conf_);
		FolderToolsResult result = toolset.removeFolder(targetfolder_);
		
		comment_ = result.comment_;
		errorcode_ = result.errorCode_;
		
		indexTask();
	}

}
