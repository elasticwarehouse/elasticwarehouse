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

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticsearch.indices.IndexMissingException;

public class ElasticWarehouseTasksManager {

	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseTasksManager.class.getName());
	
	private ElasticSearchAccessor acccessor_ = null;
	private ElasticWarehouseConf conf_ = null;
	private LinkedList<ElasticWarehouseTask> runningTasks_ = new LinkedList<ElasticWarehouseTask>();
	private boolean notFinishedTasksProcessed_ = false;
	private int maxTasks_ = 2;
	
	public ElasticWarehouseTasksManager(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, boolean cancelNotFinishedTasks) 
	{
		acccessor_ = acccessor;
		conf_ = conf;
		
		maxTasks_ = conf_.getWarehouseIntValue(ElasticWarehouseConf.TASKSMAXNUMBER, ElasticWarehouseConf.TASKSMAXNUMBERDEFAULT);
		
		if( cancelNotFinishedTasks )
			processNotFinishedTasks();
	}
	
	private void processNotFinishedTasks()
	{
		try
		{
			LOGGER.info("Marking old tasks as cancelled......");
			boolean ret = markNotFinishedTasksAsCancelled();
			LOGGER.info("Marking old tasks as cancelled: "+ret);
			notFinishedTasksProcessed_ = ret;
		} catch (org.elasticsearch.indices.IndexMissingException e) {
			acccessor_.recreateTemplatesAndIndices(true);
		} finally {
			//index just created, so nothing to mark as cancelled
		}
	}
	
	public LinkedList<String> getTasks(Boolean finished, String nodename, int size, int from, boolean showrequest, boolean allnodes, String correspondingFileId)
	{	
		LinkedList<String> ret = new LinkedList<String>();
		
		//ES 1.x
		/*SearchRequestBuilder seaerchreqbuilder = acccessor_.getClient().prepareSearch(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_NAME) )
				.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_TYPE) )
		        //.setSearchType(SearchType.SCAN)
		        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		        //.setScroll(new TimeValue(60000));*/

		/*ElasticWarehouseConf.defaultTasksIndexName_*/
		String indices = conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_NAME);
		//System.out.println(indices);
		LOGGER.info("Getting tasks from index:" + indices);
		SearchRequestBuilder seaerchreqbuilder = acccessor_.getClient().prepareSearch(indices )
				.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_TYPE) )
		        //.setSearchType(SearchType.SCAN)
		        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		        //.setScroll(new TimeValue(60000));
		
		BoolQueryBuilder bqbuilder = QueryBuilders.boolQuery();
		if( finished != null )
			bqbuilder.must(QueryBuilders.termQuery("finished", finished) );
		
		if( allnodes == false )
			bqbuilder.must(QueryBuilders.termQuery("node", nodename) );

		if( correspondingFileId != null && correspondingFileId.length()>0 )
			bqbuilder.must(QueryBuilders.termQuery("fileid", correspondingFileId));
		
		seaerchreqbuilder.setQuery(bqbuilder);
		seaerchreqbuilder.setSize(size);
		seaerchreqbuilder.setFrom(from);
		seaerchreqbuilder.addSort(SortBuilders.fieldSort("submitdate").order(SortOrder.DESC).ignoreUnmapped(true));
		
		if( showrequest )
			System.out.println(seaerchreqbuilder.toString());
		
		SearchResponse scrollResp = seaerchreqbuilder.execute().actionGet();
		
		while (true) 
		{
		    //scrollResp = acccessor_.getClient().prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
		    
			LOGGER.info("**** Found " + scrollResp.getHits().getHits().length + " task(s)");
		    for (SearchHit hit : scrollResp.getHits()) 
		    {
		    	if (hit.isSourceEmpty())
		    		continue;
		    	
		    	String taskid = hit.getSource().get("taskid").toString();
		    	if( taskid.length() > 0 )
		    		ret.add(taskid);
			}
		    
		    //Break condition: No hits are returned
		    //if (scrollResp.getHits().getHits().length == 0) 
		    //{
		        break;
		    //}
		}
		
		return ret;		
	}
	public LinkedList<String> getTasks(Boolean finished, String nodename, int size, int from, boolean showrequest, boolean allnodes)
	{   
		try{
			return getTasks(finished, nodename, size, from, showrequest, allnodes, null);
		}catch(IndexNotFoundException e){
			LOGGER.error(e.getMessage());
			return new LinkedList<String>();
		}
	}
	
	public boolean markNotFinishedTasksAsCancelled()
	{
		LinkedList<String> tasks = getTasks(false, conf_.getNodeName() /* NetworkTools.getHostName()*/, 1000, 0, false, false);
		for(String taskId : tasks)
		{
			ElasticWarehouseTask task = getTask(taskId);
			task.cancelled_ = true;
			task.setFinished();
			if( task.indexTask() == null )
				return false;
		}
		return true;
	}
	private void checkCurrentlyRunningTasks() throws IOException
	{
		if( !notFinishedTasksProcessed_ )
			processNotFinishedTasks();
		
		LinkedList<String> tasks = getTasks(false, conf_.getNodeName() /*NetworkTools.getHostName()*/, 1000, 0, false, false);
		int cnt = tasks.size();
		for(String tskid : tasks)
		{
			if( !isTaskStillActive(tskid) )
			{
				ElasticWarehouseTask tsk = getTask(tskid);
				if( tsk == null )
					throw new IOException("Cannot close ghost task "+tskid+". Please try again");
				tsk.setFinished();
				tsk.cancelled_ = true;
				tsk.indexTask();
				cnt--;
			}
		}
		
		if( cnt >= maxTasks_)
			throw new IOException("Max tasks limit ("+maxTasks_+") has been reached.");
	}
	public synchronized ElasticWarehouseTask launchScan(String path, String targetfolder /*nullable*/, boolean brecurrence) throws IOException
	{	
		checkCurrentlyRunningTasks();
		
		ElasticWarehouseTaskScan task = new ElasticWarehouseTaskScan(acccessor_, conf_, path, targetfolder, brecurrence); 
		String taskId = task.indexTask();
		runningTasks_.add(task);
		task.start();
		
		return task;
	}
	
	public ElasticWarehouseTask launchRethumb()throws IOException
	{
		checkCurrentlyRunningTasks();
		
		ElasticWarehouseTaskRethumb task = new ElasticWarehouseTaskRethumb(acccessor_, conf_); 
		String taskId = task.indexTask();
		runningTasks_.add(task);
		task.start();
		
		return task;
	}
	
	private boolean isTaskStillActive(String tskid)
	{
		for( ElasticWarehouseTask tsk : runningTasks_)
		{
			if (tsk.taskId_.equals(tskid) )
				return true;
		}
		return false;
	}

	public ElasticWarehouseTask getTask(String taskId)
	{
		if( !notFinishedTasksProcessed_ )
			processNotFinishedTasks();
		
		GetResponse response = acccessor_.getClient().prepareGet(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_NAME) /*ElasticWarehouseConf.defaultTasksIndexName_*/, 
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_TYPE) /*ElasticWarehouseConf.defaultTasksTypeName_*/,  taskId)
		        .execute()
		        .actionGet();
		if( !response.isExists() )
		{
			return null;
		}
		else
		{
			ElasticWarehouseTask task = createTask(response.getSource());
			//task.taskId_ = taskId;
			return task;
		}
	}

	private ElasticWarehouseTask createTask(Map<String, Object> source)
	{
		if( source.get("action").equals("scan") )
			return new ElasticWarehouseTaskScan(acccessor_, conf_, source);
		else if( source.get("action").equals("mkdir") )
			return new ElasticWarehouseTaskMkdir(acccessor_, conf_, source);
		else if( source.get("action").equals("rmdir") )
			return new ElasticWarehouseTaskRmdir(acccessor_, conf_, source);
		else if( source.get("action").equals("rename") )
			return new ElasticWarehouseTaskRename(acccessor_, conf_, source);
		else if( source.get("action").equals("move") )
			return new ElasticWarehouseTaskMove(acccessor_, conf_, source);
		else if( source.get("action").equals("delete") )
			return new ElasticWarehouseTaskDelete(acccessor_, conf_, source);
		else if( source.get("action").equals("rethumb") )
			return new ElasticWarehouseTaskRethumb(acccessor_, conf_, source);
		else
			return null;
	}

	public ElasticWarehouseTask createFolder(String folder)
	{
		if( !notFinishedTasksProcessed_ )
			processNotFinishedTasks();
		
		ElasticWarehouseTaskMkdir task = new ElasticWarehouseTaskMkdir(acccessor_, conf_, folder);
		task.start();		
		String taskId = task.indexTask();
		//runningTasks_.add(task);
		return task;
	}

	public ElasticWarehouseTask removeFolder(String folder)
	{
		if( !notFinishedTasksProcessed_ )
			processNotFinishedTasks();
		
		ElasticWarehouseTaskRmdir task = new ElasticWarehouseTaskRmdir(acccessor_, conf_, folder);
		task.start();		
		String taskId = task.indexTask();
		//runningTasks_.add(task);
		return task;
	}

	public ElasticWarehouseTask moveTo(String id, String folder)
	{
		if( !notFinishedTasksProcessed_ )
			processNotFinishedTasks();
		
		ElasticWarehouseTaskMove task = new ElasticWarehouseTaskMove(acccessor_, conf_, id, folder);
		task.start();		
		String taskId = task.indexTask();
		//runningTasks_.add(task);
		return task;
	}

	public ElasticWarehouseTask delete(String id) 
	{
		if( !notFinishedTasksProcessed_ )
			processNotFinishedTasks();
		
		ElasticWarehouseTaskDelete task = new ElasticWarehouseTaskDelete(acccessor_, conf_, id);
		task.start();		
		String taskId = task.indexTask();
		//runningTasks_.add(task);
		return task;
	}
	public ElasticWarehouseTask rename(String id, String targetname)throws IOException
	{
		checkCurrentlyRunningTasks();
		
		ElasticWarehouseTaskRename task = new ElasticWarehouseTaskRename(acccessor_, conf_, id, targetname);
		task.start();		
		String taskId = task.indexTask();
		runningTasks_.add(task);
		return task;
	}
	
	public ElasticWarehouseTask cancelTask(String id) {
		ElasticWarehouseTask task = null;
		if ( isTaskStillActive(id) )
		{
			for( ElasticWarehouseTask tsk : runningTasks_)
			{
				if (tsk.taskId_.equals(id)) {
					task = tsk;
					task.interrupt();
					String taskId = task.indexTask();
					runningTasks_.remove(tsk);
				}
			}
		}

		return task;
	}

	public LinkedList<String> getRunningTasks()
	{
		if( !notFinishedTasksProcessed_ )
			processNotFinishedTasks();
		
		LinkedList<String> ret = new LinkedList<String>();
		for (ElasticWarehouseTask tsk : runningTasks_) {
			ret.add(tsk.taskId_);
		}
		return ret;
	}

}
