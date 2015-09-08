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
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;

import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseAPIProcessorGetHelper;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.parsers.FileThumb;
import org.elasticwarehouse.core.parsers.FileTools;

public class ElasticWarehouseTaskRethumb extends ElasticWarehouseTask {

	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseTaskScan.class.getName());

	Thread th_ = null;
	ElasticWarehouseConf conf_ = null;
	ArrayList<String> processingErrors_ = new ArrayList<String>();
	
	long totaldocs_ = 0;
	
	public ElasticWarehouseTaskRethumb(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf) {
		super(acccessor, conf);
		conf_ = conf;
	}
	
	public ElasticWarehouseTaskRethumb(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, Map<String, Object> source)
	{
		super(acccessor, source, conf);
		conf_ = conf;
		if( source.get("totaldocs") != null )
			totaldocs_ = Integer.parseInt(source.get("totaldocs").toString());
		if( source.get("processingerrors") != null )
			processingErrors_ = (ArrayList<String>)source.get("processingerrors");
			
	}
	
	@Override
	public boolean isAsync()
	{
		return true;
	}
	
	@Override
	public String getActionString() {
		return "rethumb";
	}
	
	@Override
	public XContentBuilder vgetJsonSourceBuilder(XContentBuilder builder) throws IOException
	{
		builder.field("totaldocs", totaldocs_);
		if( processingErrors_.size() > 0 )
		{
			builder.array("processingerrors", processingErrors_);
		}
		return builder;
	}
	
	@Override
	public void start()
	{
		th_ = new Thread() {
		    public void run() {
		        LOGGER.info("Starting rethumb ["+ taskId_ +"]");
				boolean ret = startRethumbing();
				if( ret )
				{
					LOGGER.info("OK, rethumb finished with code:" + errorcode_ +" ("+comment_+")");
				}else{
					LOGGER.error("ERROR, rethumb finished with code:" + errorcode_+" ("+comment_+")");
					
				}
				
				setFinished();
				if( ret )
					progress_ = 100;
				else
					errorcode_ = 10;
					
				indexTask();
		    }  
		};
		th_.start();
	}
	
	private boolean startRethumbing()
	{
		Client client = acccessor_.getClient();
		SearchResponse scrollResp = client.prepareSearch( conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) )
		    	.setTypes( conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) )
		        .setSearchType(SearchType.SCAN)
		        .setScroll(new TimeValue(60000))
		        .setQuery( QueryBuilders.termQuery(ElasticWarehouseConf.FIELD_THUMB_AVAILABLE, true) )
		        .setSize(10).execute().actionGet(); //10 hits per shard will be returned for each scroll//Scroll until no hits are returned
		int counter = 0;
		totaldocs_ = scrollResp.getHits().getTotalHits();
		
		int thumbsize = 180;
		thumbsize = conf_.getWarehouseIntValue(ElasticWarehouseConf.THUMBSIZE, thumbsize);
		if( thumbsize != 90 && thumbsize != 180 && thumbsize != 360 && thumbsize != 720)
			thumbsize = 180;
		
		while (true)
	    {
			if( interrupt_ )
    			return false;
	    	scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
		    boolean hitsRead = false;
		    for (SearchHit hit : scrollResp.getHits()) 
		    {
		    	if( interrupt_ )
        			return false;
		    	
		        hitsRead = true;
		        
		        String id = hit.getId();
		        ElasticWarehouseAPIProcessorGetHelper pr = new ElasticWarehouseAPIProcessorGetHelper(conf_, client);
		        ElasticWarehouseAPIProcessorGetHelper.GetProcessorFileData filedata = null;
				try
				{
					filedata = pr.getFileBytes(id, null);
					if( filedata.exists_ && filedata.isFolder_ == false && filedata.bytes_ != null )
					{
						FileThumb thumb = FileTools.generateThumb(filedata.bytes_, filedata.imagewidth_, filedata.imageheight_, thumbsize);
						processUpdateRequest(thumb, id);	
					}
				} catch (IOException e) {
					EWLogger.logerror(e);
					e.printStackTrace();
					processingErrors_.add(id+" : "+e.getMessage());
			    } catch (ElasticsearchException e) {
			    	EWLogger.logerror(e);
					e.printStackTrace();
					processingErrors_.add(id+" : "+e.getMessage());
				}
		        
		        counter++;
		        float pp = (float)counter/totaldocs_;
	    		progress_ = (int) ((float) pp*100.0);
	    		
		    }

		    
		    float pp = (float)counter/totaldocs_;
    		progress_ = (int) ((float) pp*100.0);
    		
    		indexTask();
    		
		    //Break condition: No hits are returned
		    if (!hitsRead) {
		        break;
		    }
	    }

		return true;
	}

	private void processUpdateRequest(FileThumb thumb, String id) throws ElasticsearchException, IOException
	{
		Client client = acccessor_.getClient();
		
		XContentBuilder builder = jsonBuilder();
		builder.startObject();
		thumb.processThumb(builder, df);
		builder.endObject();
		
		//String sss = builder.string();
		//System.out.println(id + "| "+sss);
		//Map<String, Object> updateObject = new HashMap<String, Object>();
		//thumb.processThumb(updateObject, df);
        
		//"filethumb"{"available":true,"thumbdate":"2015-08-07 18:03:28","sameasimage":true}
		
		
		UpdateResponse response = client.prepareUpdate(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), 
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE), id)
				.setRefresh(true)
				//.setDoc("{\"filethumb.thumbdate\" : \"2015-08-07 18:03:28\"}")
				.setDoc(builder)
				//.setScript("ctx._source.filethumb.thumbdate=thumbdate", ScriptType.INLINE)
				//.setScript("ctx._source.filethumb.available=available", ScriptType.INLINE)
				//.setScriptParams(updateObject)
				//.addScriptParam("param1", "2015-08-07 18:03:28")
				//.setScript("ctx._source.filethumb.thumbdate=param1", ScriptType.INDEXED)
				.execute().actionGet();
		if( response.isCreated() == true ) //somethign went wrong, shoudltn be created
			throw new ElasticsearchException("Thumb update for id="+id+" caused document creation");
	}
}
