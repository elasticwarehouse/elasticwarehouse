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
package org.elasticwarehouse.core.graphite;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;

import org.apache.log4j.Logger;
import org.elasticwarehouse.core.AtrrValue;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.parsers.FileDef;
import org.elasticwarehouse.core.parsers.FileTools;


public class ElasticSearchMonitor  extends PerfMon {

	private enum MonitorType
	{
		NODES, SHARDS, INDEX
	};
	HashMap<String, RRDManager> rrdmanagers_ = new HashMap<String, RRDManager>();;
	private ElasticSearchAccessor elasticSearchAccessor_;
	public static final String ES_FILE_PREFIX = "elasticsearch_";
	//public static final String CUSTOM_ES_TYPE = "elasticsearch";
	private final static Logger LOGGER = Logger.getLogger(ElasticSearchMonitor.class.getName());
	//LinkedList<String> customattributes = new LinkedList<String>();
	private String folder_;
	private boolean readOnly_;
	private long last_query_time_ = -1;
	private long last_query_count_ = -1;
	private long lastFetchTime_ = -1;
	private long last_docs_count_ = -1 ;
	//private HashMap<String, Long> lastDocsCount_= new HashMap<String, Long>();
	//private static Boolean reopenViewer_ = false; 
			
	public ElasticSearchMonitor(ElasticWarehouseConf conf, boolean readOnly, ElasticSearchAccessor elasticSearchAccessor)throws MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, NullPointerException, ReflectionException, IOException, ParseException
	{
		super(conf);
    	folder_ = getStorageFolder();
    	readOnly_ = readOnly;
		elasticSearchAccessor_ = elasticSearchAccessor;
		
		//customattributes = readDataSources();
		
		//recreateDbManager();
	}
/*
	private LinkedList<String> readDataSources() throws IOException {
		LinkedList<String> ret = new LinkedList<String>();
		String mypath = folder_+"/"+CUSTOM_ES_TYPE.toLowerCase()+".rrd";
		if(FileTools.checkFileCanRead(mypath))
		{
			RrdDb rrdDb = new RrdDb(mypath, true);
			RrdDef def = rrdDb.getRrdDef();
			DsDef[] dsdefs = def.getDsDefs();
			for(DsDef dsdef: dsdefs)
			{
				ret.add(dsdef.getDsName());
			}
		}else{
			ret.add("fake");	
		}
		return ret;
	}
 */
	private RRDManager createDbManager(String filebasename, MonitorType type) throws IOException, ParseException, DDRFileNotFoundException
	{
		return new RRDManager(folder_+"/"+filebasename+".rrd", getAvailableAttributes(type), true, readOnly_, true, conf_.getTempFolder());
	}
	private void recreateDbManagerIfNeeded(String filebasename) throws IOException, ParseException, DDRFileNotFoundException {
		//if( customattributes.isEmpty() )
		//	return;
		if( rrdmanagers_.get(filebasename) != null )
			return;
		
		/*if( rrdmanager_ != null)
		{
			rrdmanager_.expandRRDFile(customattributes);
			rrdmanager_.Dispose();
			rrdmanager_ = null;
			synchronized (reopenViewer_ ) {
				reopenViewer_ = true;	
			}
		}
		rrdmanager_ = new RRDManager(folder_+"/"+CUSTOM_ES_TYPE.toLowerCase()+".rrd", customattributes, true, readOnly_, conf_.getTempFolder());
		customattributes = readDataSources();
		*/
		MonitorType montype = determineType(filebasename);
		if( montype != null )
			rrdmanagers_.put(filebasename, createDbManager(filebasename, montype) );
		
	}
	public LinkedList<String> getAvailableTypes() throws IOException, ParseException, DDRFileNotFoundException
	{
		//read factors from ES
		HashSet<String> assignedNodes = new HashSet<String>();
		LinkedList<EsShardInfo> infos = elasticSearchAccessor_.getESAttributesForWarehouseIndices();
		for(EsShardInfo ei : infos)
		{
			if( ei.isPrimary_)
				recreateDbManagerIfNeeded(ES_FILE_PREFIX+"shard_"+ei.id_);
			if( !assignedNodes.contains(ei.nodeName_) )
				assignedNodes.add(ei.nodeName_);
		}
		LinkedList<EsNodeInfo> nodeinfos = elasticSearchAccessor_.getESNodeInfos(assignedNodes);
		for(EsNodeInfo ni : nodeinfos)
		{
			String nodenameconvert = ni.nodename_;
			nodenameconvert = adaptNodeName(nodenameconvert);
			recreateDbManagerIfNeeded(ES_FILE_PREFIX+"node_"+nodenameconvert);
		}
		recreateDbManagerIfNeeded(ES_FILE_PREFIX+"node_localhost");
		recreateDbManagerIfNeeded(ES_FILE_PREFIX+"index");
		
		
		//collect files........
		LinkedList<String> ret = new LinkedList<String>();
		LinkedList<FileDef> list = FileTools.scanFolder(folder_, new LinkedList<String>());
		for(FileDef fd : list)
		{
			if(fd.fname_.startsWith(ES_FILE_PREFIX) && fd.fname_.endsWith(".rrd"))
				ret.add(fd.fname_.substring(0,fd.fname_.length()-4));
		}

		return ret;
	}
	public LinkedList<String> getAvailableAttributes(MonitorType type)
	{
		LinkedList<String> ret = null;
		switch(type)
		{
		case NODES:
			ret = new LinkedList(Arrays.asList(new String[] { "heap_used", "memory_used", "avg_load" } ));
			break;
		case SHARDS:
			ret = new LinkedList(Arrays.asList(new String[] { "documents_count", "storage_size" } ) );
			break;
		case INDEX:
			ret = new LinkedList(Arrays.asList(new String[] { "documents_count", "primary_storage_size" , "total_storage_size", 
															  "insert_current", "insert_avgrate", "search_current", "search_avgrate"} ) );
			break;
		}
		return ret;
	}
	public LinkedList<String> getAvailableAttributes(String filebasename, boolean expandCompositeData) throws MalformedObjectNameException, NullPointerException, IntrospectionException, InstanceNotFoundException, ReflectionException, IOException, ParseException, DDRFileNotFoundException
	{
		recreateDbManagerIfNeeded(filebasename);
		return rrdmanagers_.get(filebasename).getSources();
		//checkReopenFlag();
		//return customattributes;
	}
	/*private void checkReopenFlag() throws IOException, ParseException {
		synchronized (reopenViewer_ ) {
			if( readOnly_ )
			{
				rrdmanager_ = new RRDManager(folder_+"/"+CUSTOM_ES_TYPE.toLowerCase()+".rrd", customattributes, true, readOnly_, conf_.getTempFolder());
				customattributes = readDataSources();
				reopenViewer_ = false;	
			}
				
		}
	}*/

	public void saveCustomPerformanceCounter(String filebasename, LinkedList<AtrrValue> attval) throws IOException, ParseException
	{
		RRDManager rrd = rrdmanagers_.get(filebasename);
		if( rrd == null )
			throw new IOException("Cannot get RRD database for type: " + filebasename + ". Available:" + getAvailableRRDManagersAsString());
		
		/*LinkedList<String> sources2add  = new LinkedList<String>();
		for(AtrrValue av : attval)
		{
			if( rrd != null && rrd.sourceExists(av.key_)== false )
			{
				sources2add.add(av.key_);
			}
		}
			
		if( sources2add.isEmpty()== false || rrdmanager_== null )
		{
			customattributes = sources2add;
			recreateDbManager();
		}*/
		
		saveCounter(rrd, filebasename, attval );
	}

	private String getAvailableRRDManagersAsString() {
		StringBuilder sb = new StringBuilder();
		int pos = 0;
		for (Entry<String, RRDManager> entry : rrdmanagers_.entrySet())
		{
			if( pos > 0 )
				sb.append(",");
			sb.append(entry.getKey());
			pos++;
		    //System.out.println(entry.getKey() + "/" + entry.getValue());
		}
		return sb.toString();
	}
	public void fetchCustomPerformanceCounters(String filebasename, LinkedList<AtrrValue> attval)
	{
		MonitorType montype = determineType(filebasename);
		switch(montype)
		{
		case INDEX:
			fetchCustomPerformanceCountersForIndices(attval);
			break;
		case SHARDS:
			Integer shardNb = getShardNbFromType(filebasename);
			if( shardNb!= null )
			{
				LinkedList<EsShardInfo> infos = elasticSearchAccessor_.getESAttributesForWarehouseIndices();
				for(EsShardInfo ei : infos)
				{
					if( ei.isPrimary_  && ei.id_ == shardNb)
					{
						attval.add(new AtrrValue("documents_count", (double) ei.docsCnt_));
						attval.add(new AtrrValue("storage_size", (double) ei.storeSize_));
					}
				}
			}
			break;
		case NODES:
			String nodeName = getNodeNameFromType(filebasename);
			if( nodeName != null )
			{
				HashSet<String> assignedNodes = new HashSet<String>();
				LinkedList<EsShardInfo> infos2 = elasticSearchAccessor_.getESAttributesForWarehouseIndices();
				for(EsShardInfo ei : infos2)
				{
					String nodenameconvert = ei.nodeName_;
					nodenameconvert = adaptNodeName(nodenameconvert);
					if( nodenameconvert.equals(nodeName) && assignedNodes.contains(ei.nodeName_) == false )
						assignedNodes.add(ei.nodeName_);
				}
				if( !assignedNodes.isEmpty() )
				{
					LinkedList<EsNodeInfo> nodeinfos = elasticSearchAccessor_.getESNodeInfos(assignedNodes);
					for(EsNodeInfo ni : nodeinfos)
					{
						
						attval.add(new AtrrValue("heap_used", (double) ni.heapUsedPercent_));
						attval.add(new AtrrValue("memory_used", (double) ni.memusedpercent_));
						attval.add(new AtrrValue("avg_load", (double) ni.loadAvg_));
					}
				}
			}
			break;
		}
		
		lastFetchTime_  = System.currentTimeMillis();

	}
	
	private String getNodeNameFromType(String filebasename) {
		String pfx = "elasticsearch_node_";
		if( !filebasename.startsWith(pfx) )
			return null;
		return filebasename.substring( pfx.length() );
	}
	private Integer getShardNbFromType(String filebasename) {
		Pattern patternShardNb = Pattern.compile("elasticsearch_shard_(\\d*)");
		Matcher matcher = patternShardNb.matcher(filebasename);
		if (matcher.find())
			return Integer.parseInt(matcher.group(1).toString()); 
		
		return null;
	}
	private void fetchCustomPerformanceCountersForIndices(LinkedList<AtrrValue> attval)
	{
		
		HashSet<String> forindices = new HashSet<String>();
		forindices.add(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME));
		LinkedList<EsIndexInfo> indexinfos = elasticSearchAccessor_.getESIndicesInfos(forindices);
		for(EsIndexInfo ii : indexinfos)
		{
			attval.add(new AtrrValue("documents_count", (double) ii.docscount_));
			attval.add(new AtrrValue("primary_storage_size", (double) ii.pristoresize_));
			attval.add(new AtrrValue("total_storage_size", (double) ii.totatlstoresize_));
			
			/************* indexing counters ***************/
			attval.add(new AtrrValue("insert_current", (double) ii.indexcurrent_));
			//if( ii.indextime_ > 0 && ii.indexcount_>=0)
			//{
			//	double d = (double)( ((double)ii.indextime_/1000.0 ) / (double)ii.indexcount_ );
			//	attval.add(new AtrrValue("ewindex_ins_avg", d));
			//	
			//}
			if( last_docs_count_!=-1 )
			{
				double countsdiff = ii.docscount_ - last_docs_count_ ;
				double timediff = (System.currentTimeMillis() - lastFetchTime_)/1000.0;
				double ingestiorate = (double)countsdiff/(double)timediff;
				attval.add(new AtrrValue("insert_avgrate", ingestiorate));
				LOGGER.debug("countsdiff="+countsdiff+" ,timediff="+timediff+" ,ingestionrate="+ingestiorate);
			}
			last_docs_count_ = ii.docscount_;
			
			/********* search **********/
			attval.add(new AtrrValue("search_current", (double) ii.querycurrent_));
			if( /*ii.querytime_ > 0 &&*/ ii.querycount_>=0 && ii.nbofshards_>0)
			{
				//double querytime = (double)ii.querytime_;
				double querycount = (double)ii.querycount_;
				//double fetchtime = (double)ii.fetchtime_;
				//double fetchcount = (double)ii.fetchcount_;

				//double d = 0;
				//if( fetchcount > 0 )
				//	d =((querytime+fetchtime)/1000.0)/fetchcount;
				
				double avg_query_time = 0;
				if( last_query_count_ !=-1 && last_query_time_ !=-1 && ii.querycount_!=last_query_count_)
				{
					//avg_query_time = (querytime - last_query_time_)/(querycount-last_query_count_);
					double timediff = (System.currentTimeMillis() - lastFetchTime_)/1000.0;
					double countsdiff = (querycount-last_query_count_)/ii.nbofshards_;
					avg_query_time = countsdiff/timediff;
					LOGGER.debug("countsdiff="+countsdiff+" ,timediff="+timediff+" ,avg_query_time="+avg_query_time);
				}
				last_query_time_  = ii.querytime_;
				last_query_count_ = ii.querycount_;
				
				attval.add(new AtrrValue("search_avgrate", avg_query_time));
				
				
			}
			
			
		}
	}
	
	private MonitorType determineType(String filebasename) {
		if( filebasename.startsWith(ES_FILE_PREFIX+"node") )
			return MonitorType.NODES;
		if( filebasename.startsWith(ES_FILE_PREFIX+"shard") )
			return MonitorType.SHARDS;
		if( filebasename.startsWith(ES_FILE_PREFIX+"index") )
			return MonitorType.INDEX;
		return null;
	}
	private String adaptNodeName(String nodenameconvert/*, int reserverdChars*/) {
		nodenameconvert = nodenameconvert.replace(" ", "_").toLowerCase();
		//if( nodenameconvert.length() > RRDManager.MAXDATASOURCENAMELEN-reserverdChars)
		//	return nodenameconvert.substring(0,RRDManager.MAXDATASOURCENAMELEN-reserverdChars);
		//else
		return nodenameconvert;
	}

	@Override
	public synchronized LinkedList<TimeSample> fetchCountersToRender(String target /*i.e. host.cpu.loadavgsec */, String from, String until, 
    		String format, int minSamplesCount, int maxDataPoints, boolean refreshBeforeFetch) throws IOException, ParseException, DataSourceNotFoundException, DDRFileNotFoundException
    {
    	String[] tokens = target.split("\\.");
		if( tokens.length != 3)
			throw new IOException("target is wrong, 3 tokens expected, but got:" + target);
		
		String uniqType = tokens[1].toLowerCase();
		String metric = tokens[2];
		
		
		//checkReopenFlag();
		RRDManager rrd = rrdmanagers_.get(uniqType);
		if( rrd == null )
		{
			recreateDbManagerIfNeeded(uniqType);
			rrd = rrdmanagers_.get(uniqType);
			if( rrd == null )
				throw new IOException("database type is wrong, no datasource found, 3 tokens expected, but got:" + uniqType);
		}
		
		if( refreshBeforeFetch )
		{
			rrd.refresh();
		}
		
		return rrd.exportGraphite(metric, from, until, format, maxDataPoints);
    	
    }

}
