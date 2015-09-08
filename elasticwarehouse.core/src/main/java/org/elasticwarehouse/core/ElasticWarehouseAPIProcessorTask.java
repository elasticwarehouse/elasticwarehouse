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

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestRequest;
import org.elasticwarehouse.core.graphite.NetworkTools;
import org.elasticwarehouse.core.parsers.ParseTools;
import org.elasticwarehouse.tasks.ElasticWarehouseTask;
import org.elasticwarehouse.tasks.ElasticWarehouseTasksManager;

public class ElasticWarehouseAPIProcessorTask
{
	private ElasticWarehouseReqRespHelper responser = new ElasticWarehouseReqRespHelper();
	private ElasticWarehouseTasksManager tasksManager_ = null;
	private ElasticWarehouseConf conf_ = null;
	
	public class ElasticWarehouseAPIProcessorTaskParams
	{
		public String action = "";
		public String status = "";
		public String list = "";
		public String cancel = "";
		
		//for scan
		public String path = "";
		public String targetfolder = "";
		public boolean recurrence = false;
		
		public String folder = "";
		
		public String id = "";
		
		int size = 10;
		int from = 0;
		
		public void readFrom(RestRequest orgrequest)
		{
			action = orgrequest.param("action");
			status = orgrequest.param("status");
			list = orgrequest.param("list");
			cancel = orgrequest.param("cancel");
			
			path = orgrequest.param("path", path);
			targetfolder = orgrequest.param("targetfolder",targetfolder);
			recurrence = orgrequest.paramAsBoolean("recurrence", recurrence);
			
			folder = orgrequest.param("folder", folder);
			id = orgrequest.param("id", id);
	    	
			size = orgrequest.paramAsInt("size", size);
			from = orgrequest.paramAsInt("from", from);
		}
	};
	
	public ElasticWarehouseAPIProcessorTask(ElasticWarehouseTasksManager tasksManager, ElasticWarehouseConf conf)
	{
		tasksManager_ = tasksManager;
		conf_  = conf;
	}
	public boolean processRequest(Client esClient, OutputStream os,	HttpServletRequest request) throws IOException
	{
		ElasticWarehouseAPIProcessorTaskParams params = createEmptyParams();
		//UUID taskUUID = UUID.randomUUID();
		//String reqmethod = request.getMethod();	//GET,POST, etc
		params.action = request.getParameter("action");
		params.status = request.getParameter("status");
		params.list = request.getParameter("list");
		params.cancel = request.getParameter("cancel");
		
		params.path = request.getParameter("path");
		params.targetfolder = request.getParameter("targetfolder");
		String srecurrence = request.getParameter("recurrence");
		params.recurrence = false;
		//if( targetfolder == null )
		//	targetfolder = ResourceTools.preprocessFolderName(path);
		if( srecurrence != null )
			params.recurrence = Boolean.parseBoolean(srecurrence);
		
		params.folder = request.getParameter("folder");
		
		params.id = request.getParameter("id");
		
		params.size = ParseTools.parseIntDirect(request.getParameter("size"), ElasticWarehouseConf.TASKLISTSIZE);
		params.from = ParseTools.parseIntDirect(request.getParameter("from"), 0);
		
		boolean ret = processRequest(esClient, os, params);

		return ret;
	}
	public boolean processRequest(Client esClient, OutputStream os, ElasticWarehouseAPIProcessorTaskParams params) throws IOException 
	{
		if( params.action == null && params.status == null && params.list == null && params.cancel == null)
		{
			os.write(responser.errorMessage("'action', 'status', 'cancel' or 'list' are expected.", ElasticWarehouseConf.URL_GUIDE_TASK));
		}
		else if( params.action != null )
		{
			params.action = params.action.toLowerCase();
			if( params.action.equals("scan") )
			{
				/*String path = request.getParameter("path");
				String targetfolder = request.getParameter("targetfolder");
				String recurrence = request.getParameter("recurrence");
				boolean brecurrence = false;
				//if( targetfolder == null )
				//	targetfolder = ResourceTools.preprocessFolderName(path);
				if( recurrence != null )
					brecurrence = Boolean.parseBoolean(recurrence);*/
				
				if( params.path == null )
				{
					os.write(responser.errorMessage("path is needed for scan action.", ElasticWarehouseConf.URL_GUIDE_TASK));
				}else{
					ElasticWarehouseTask taskUUID = tasksManager_.launchScan(params.path, params.targetfolder, params.recurrence);
					os.write(responser.taskAcceptedMessage("Scanning "+params.path, 0, taskUUID));
				}
			}
			else if( params.action.equals("rethumb") )
			{
				ElasticWarehouseTask taskUUID = tasksManager_.launchRethumb();
				os.write(responser.taskAcceptedMessage("Started thumbnails regeneration", 0, taskUUID));
			}
			else if(params.action.equals("mkdir"))
			{
				//String folder = request.getParameter("folder");
				if( params.folder == null )
				{
					os.write(responser.errorMessage("folder is needed for mkdir action.", ElasticWarehouseConf.URL_GUIDE_TASK));
				}else{
					ElasticWarehouseTask taskUUID = tasksManager_.createFolder(params.folder);
					os.write(responser.taskAcceptedMessage("mkdir "+params.folder, 0, taskUUID));
				}
			}
			else if(params.action.equals("rmdir"))
			{
				//String folder = request.getParameter("folder");
				if( params.folder == null )
				{
					os.write(responser.errorMessage("folder is needed for rmdir action.", ElasticWarehouseConf.URL_GUIDE_TASK));
				}else{
					ElasticWarehouseTask taskUUID = tasksManager_.removeFolder(params.folder);
					os.write(responser.taskAcceptedMessage("rmdir "+params.folder, 0, taskUUID));
				}
			}
			else if(params.action.equals("move"))
			{
				//String folder = request.getParameter("folder");
				//String id = request.getParameter("id");
				if( params.id == null)
				{
					os.write(responser.errorMessage("Please provide id of file to be moved.", ElasticWarehouseConf.URL_GUIDE_TASK));
				}
				else if( params.folder == null )
				{
					os.write(responser.errorMessage("folder is needed for move action.", ElasticWarehouseConf.URL_GUIDE_TASK));
				}
				else
				{
					ElasticWarehouseTask taskUUID = tasksManager_.moveTo(params.id, params.folder);
					os.write(responser.taskAcceptedMessage("move id="+params.id + " to "+params.folder, 0, taskUUID));
				}
			}
			else if(params.action.equals("delete"))
			{
				//String id = request.getParameter("id");
				if( params.id == null)
				{
					os.write(responser.errorMessage("Please provide id of file to be deleted.", ElasticWarehouseConf.URL_GUIDE_TASK));
				}
				else
				{
					ElasticWarehouseTask taskUUID = tasksManager_.delete(params.id);
					os.write(responser.taskAcceptedMessage("delete id="+params.id, 0, taskUUID));
				}
			}
			else
			{
				os.write(responser.errorMessage("Unknown task action.", ElasticWarehouseConf.URL_GUIDE_TASK));
			}
		}
		else if( params.status != null )
		{
			ElasticWarehouseTask task = tasksManager_.getTask(params.status);
			if( task != null)
			{
				os.write(task.getJsonSourceBuilder().string().getBytes());
			}
			else
			{
				os.write(responser.errorMessage("Unknown task Id " + params.status , ElasticWarehouseConf.URL_GUIDE_TASK));
			}
		}
		else if( params.cancel != null )
		{
			ElasticWarehouseTask task = tasksManager_.getTask(params.cancel);
			if( task != null)
			{
				if( task.finished() )
				{
					os.write(responser.errorMessage("Task Id " + params.cancel +" is finished and cannot be cancelled.", ElasticWarehouseConf.URL_GUIDE_TASK));
				}else{
					task = tasksManager_.cancelTask(params.cancel);
					if( task == null )
						os.write(responser.errorMessage("Task Id: "+params.cancel+" is no longer running and cannot be cancelled.", ElasticWarehouseConf.URL_GUIDE_TASK));
					else
						os.write(task.getJsonSourceBuilder().string().getBytes());
				}
			}
			else
			{
				os.write(responser.errorMessage("Unknown task Id " + params.status , ElasticWarehouseConf.URL_GUIDE_TASK));
			}
		}
		else if( params.list != null )
		{
			params.list = params.list.toLowerCase();
			
			//int size = ParseTools.parseIntDirect(request.getParameter("size"), ElasticWarehouseConf.TASKLISTSIZE);
			//int from = ParseTools.parseIntDirect(request.getParameter("from"), 0);
			
			LinkedList<String> tasks = null;
			if( params.list.equals("active") )
				tasks = tasksManager_.getTasks(false, conf_.getNodeName()/* NetworkTools.getHostName()*/, params.size, params.from );
			else
				tasks = tasksManager_.getTasks(null, conf_.getNodeName() /*NetworkTools.getHostName()*/, params.size, params.from);
			
			XContentBuilder builder = jsonBuilder().startArray();
			for(String taskid : tasks)
			{
				ElasticWarehouseTask task = tasksManager_.getTask(taskid);
				if( task == null )
				{
					throw new IOException("Cannot fetch taskId: " + taskid);
				}else{
					builder = task.getJsonSourceBuilder(builder, true);
				}
			}
			builder.endArray();
			os.write(builder.string().getBytes());
		}
		return true;
	}
	public org.elasticwarehouse.core.ElasticWarehouseAPIProcessorTask.ElasticWarehouseAPIProcessorTaskParams createEmptyParams() {
		return new ElasticWarehouseAPIProcessorTaskParams();
	}

}
