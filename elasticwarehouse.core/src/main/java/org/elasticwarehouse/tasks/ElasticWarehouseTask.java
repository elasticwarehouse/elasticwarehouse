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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Date;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.graphite.NetworkTools;
import org.elasticwarehouse.core.parsers.ParseTools;

public abstract class ElasticWarehouseTask {

	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseTask.class.getName());
	
	protected DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static final int ERROR_TASK_WRONG_PARAMETERS = 10;
	
	public static final int ERROR_TASK_SCAN_OTHER_EXCEPTION = 20;
	
	public static final int ERROR_TASK_RETHUMB_OTHER_EXCEPTION = 30;
	
	public static final int ERROR_TASK_RENAME_OTHER_EXCEPTION = 40;
	public static final int ERROR_TASK_RENAME_ROOT_CANNOT_BE_RENAMED = 41;
	
	public static final int ERROR_TASK_MOVE_OTHER_EXCEPTION = 50;
	public static final int ERROR_TASK_MOVE_FOLDERS_CANNOT_BE_MOVED = 51;
	public static final int ERROR_TASK_MOVE_CANNOT_PROCESS = 52;

	public static final int ERROR_TASK_DELETE_OTHER_EXCEPTION = 60;

	public static final int ERROR_TASK_RMDIR_OTHER_EXCEPTION = 70;
	public static final int ERROR_TASK_RMDIR_FOLDER_DOESNT_EXIST = 71;

	public static final int ERROR_TASK_CREATE_OTHER_EXCEPTION = 80;
	public static final int ERROR_TASK_CREATE_FOLDER_ALREADY_EXIST = 81;
	
	
	protected int progress_ = 0;
	protected int errorcode_ = 0;
	protected Date submitdate_ = null;
	protected Date enddate_ = null;
	protected String comment_ = "";
	private String host_ = "";
	private String nodename_ = "";
	private boolean finished_ = false;
	protected boolean cancelled_ = false;
	
	protected String taskId_ = "";
	
	protected ElasticSearchAccessor acccessor_ = null;

	protected boolean interrupt_ = false;

	private ElasticWarehouseConf conf_;

	//private String hostname_;
	
	public ElasticWarehouseTask(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf)
	{
		acccessor_ = acccessor;
		host_ = NetworkTools.getHostName2();
		nodename_ = conf.getNodeName();
		conf_ = conf;
		//hostname_ = NetworkTools.getHostName();
	}
	public ElasticWarehouseTask(ElasticSearchAccessor acccessor, Map<String, Object> source, ElasticWarehouseConf conf)
	{
		acccessor_ = acccessor;
		conf_ = conf;
		
		progress_ = Integer.parseInt(source.get("progress").toString() );
		errorcode_ = Integer.parseInt(source.get("errorcode").toString() );
		submitdate_ = ParseTools.isDate(source.get("submitdate").toString());
		if( source.get("enddate") != null )
			enddate_ = ParseTools.isDate(source.get("enddate").toString());
		if( source.get("comment") != null )
			comment_  = source.get("comment").toString();
        //hostname_  = source.get("hostname").toString();
        finished_ = Boolean.parseBoolean(source.get("finished").toString() );
        cancelled_ = Boolean.parseBoolean(source.get("cancelled").toString() );
        
        host_ = source.get("host").toString();
        nodename_ = source.get("node").toString();
        taskId_ = source.get("taskid").toString();
	}
	
	public void setFinished()
	{
		finished_ = true;
		enddate_ = Calendar.getInstance().getTime();
	}
	public boolean finished()
	{
		return finished_;
	}
	public void interrupt()
	{
		cancelled_ = true;
		interrupt_  = true;
	}
	abstract public String getActionString();

	public XContentBuilder getJsonSourceBuilder() throws IOException
	{
		XContentBuilder builder = jsonBuilder();
		return getJsonSourceBuilder(builder, true);
	}
	public XContentBuilder getJsonSourceBuilder(XContentBuilder builder, boolean startNewObject) throws IOException
	{
		if( submitdate_ == null )
			submitdate_ = Calendar.getInstance().getTime();
		if( startNewObject )
			builder.startObject();
		builder
					 .field("action", getActionString() )
		             .field("submitdate", df.format(submitdate_) )
		             .field("progress", progress_)
		             .field("comment", comment_)
		             .field("finished", finished_)
		             .field("cancelled", cancelled_)
		             .field("enddate", (enddate_==null?enddate_:df.format(enddate_)) )
		             .field("errorcode", errorcode_)
		             .field("taskid", taskId_)
		             .field("host", host_)
					 .field("node", nodename_);
		builder = vgetJsonSourceBuilder(builder);
		if( startNewObject )
			builder.endObject();
		return builder;
	}
	
	abstract public XContentBuilder vgetJsonSourceBuilder(XContentBuilder builder) throws IOException;
	
	abstract public boolean isAsync();
	
	abstract public void start();
	
	protected String indexTask()
	{
		String id = null;
		try {
			
			IndexResponse response = null;
			
			if( taskId_.length() > 0 )
				response = acccessor_.getClient().prepareIndex(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_NAME) /*ElasticWarehouseConf.defaultTasksIndexName_*/, 
						conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_TYPE) /*ElasticWarehouseConf.defaultTasksTypeName_*/, taskId_)
		        .setSource( getJsonSourceBuilder() )
		        .setRefresh(true)
		        .execute()
		        .actionGet();
			else
				response = acccessor_.getClient().prepareIndex(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_NAME) /*ElasticWarehouseConf.defaultTasksIndexName_*/, 
						conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_TYPE) /*ElasticWarehouseConf.defaultTasksTypeName_*/ )
			        .setSource( getJsonSourceBuilder() )
			        .setRefresh(true)
			        .execute()
			        .actionGet();
			
			
			String _id = response.getId();
			
			
			id = _id;
			if( taskId_.length() == 0 )
			{
				taskId_ = id;
				response = acccessor_.getClient().prepareIndex(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_NAME) /*ElasticWarehouseConf.defaultTasksIndexName_*/, 
						conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_TYPE) /*ElasticWarehouseConf.defaultTasksTypeName_*/, id)
				        .setSource( getJsonSourceBuilder() )
				        .setRefresh(true)
				        .execute()
				        .actionGet();
			}
			
			String _index = response.getIndex();
			String _type = response.getType();
			long _version = response.getVersion();
			LOGGER.info("Task Indexed: " + _index + "/" + _type + "/" + _id + " at version:" + _version);
			
			taskId_ = id;
			
			//replaced by .setRefresh(true) acccessor_.getClient().admin().indices().prepareRefresh().execute().actionGet();
			
		} catch (ElasticsearchException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		}
		return id;
	}
}
