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

import static org.elasticsearch.node.NodeBuilder.*;
//import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.common.Base64;
import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
//import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse; ES 2.x experiment
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.io.stream.OutputStreamStreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.http.HttpInfo;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.MatchQueryBuilder.Type;
import org.elasticsearch.index.search.MultiMatchQuery.QueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticwarehouse.core.graphite.EsIndexInfo;
import org.elasticwarehouse.core.graphite.EsNodeInfo;
import org.elasticwarehouse.core.graphite.EsShardInfo;
import org.elasticwarehouse.core.graphite.NetworkTools;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFile;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFileParser;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFileParserFactory;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFolder;
import org.elasticwarehouse.core.parsers.FileTools;

import static org.elasticsearch.common.xcontent.XContentFactory.*;

public class ElasticSearchAccessor
{
	private Client client_;
	private Node node_ = null;
	private String myNodeName_ = "";
	
	private final static Logger LOGGER = Logger.getLogger(ElasticSearchAccessor.class.getName());
	//private static final int SIZE_NO_LIMIT = 9999999; 
	private static final int SIZE_NO_LIMIT = 10000;	//ES2.x limit
	
	
	private String hostPort_ = "";
	private ElasticWarehouseConf conf_ = null;
	private boolean embedded_ = true;
	
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public ElasticSearchAccessor(ElasticWarehouseConf conf, boolean openConnections)
	{
		conf_  = conf;
		embedded_ = conf_.getWarehouseBoolValue(ElasticWarehouseConf.MODEEMBEDDED, true);
		if( openConnections )
		{
			if( embedded_ )
				client_ = openNodeClient();
			else
				client_ = openTransportClient();
		}
		initWithClient(client_);
		//recreateTemplatesAndIndices();
	}
	
	

	public ElasticSearchAccessor(Client client, ElasticWarehouseConf conf) {
		conf_  = conf;
		initWithClient(client);
	}

	private void initWithClient(Client client) {
		client_ = client;
		myNodeName_ = conf_.getNodeName();// NetworkTools.getHostName().toUpperCase();
		embedded_ = conf_.getWarehouseBoolValue(ElasticWarehouseConf.MODEEMBEDDED, true);
		
		try
		{
			Runtime.getRuntime().addShutdownHook(new Thread() {
			    @Override
			    public void run() {
			    	LOGGER.info("Client is stopping....");
			    	if( client_ != null )
			    		client_.close();
			    	if( node_ != null )
			    	{
							//node_.stop();    //if started then stop() will be called anyway, ES2.x experiment        		
							node_.close();
			    	}
			    	LOGGER.info("Client stopped");
			    }
			});
		}
		catch(java.security.AccessControlException e)
		{
			//this code is running in plugin mode
		}
		if( embedded_ )
			hostPort_ = determineNodeRestPoint();
		
	}
	
	public void close() {
		if( client_ != null )
			client_.close();
		client_ = null;
		if( node_ != null )
		{
			//node_.stop(); //if started then stop() will be called anyway, ES2.x experiment
			node_.close();
		}
	}
	
	public void recreateTemplatesAndIndices(boolean includeGrafana) {
		applyTemplates();
		createIndex(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/);
		createIndex(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_TASKS_NAME) /*ElasticWarehouseConf.defaultTasksIndexName_*/);
		createIndex(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_UPLOADS_NAME) );

		if( includeGrafana && indexExists("grafana-dash") == false )
		{
			createIndex("grafana-dash");
			importGrafanaDashboard();
		}
	}

	private String importGrafanaDashboard() {
		String dashboard = ResourceTools.getTextFileContent("/res/grafana_dashboard.json");
		
		String id = null;
		try {
			
			IndexResponse response = client_.prepareIndex("grafana-dash", 
					"dashboard", "elasticwarehouse")
			        .setSource( dashboard )
			        .execute()
			        .actionGet();
			
			String _index = response.getIndex();
			String _type = response.getType();
			String _id = response.getId();
			long _version = response.getVersion();
			LOGGER.info("Indexed grafana dashboard: " + _index + "/" + _type + "/" + _id + " at version:" + _version);
			id = _id;

		} catch (ElasticsearchException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		}
		return id;
		
	}

	private synchronized String determineNodeRestPoint()
	{
		if( client_ == null )
			return "";
		NodesInfoResponse r = client_.admin().cluster().nodesInfo(new NodesInfoRequest(myNodeName_).all()).actionGet();
		NodeInfo[] nodes = r.getNodes();
		HttpInfo httpinfo = nodes[0].getHttp();
		TransportAddress addr = httpinfo.address().publishAddress();
		String str = addr.toString();
		
		Pattern patternIp = Pattern.compile("([0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}:[0-9]*)");
		Matcher matcher = patternIp.matcher(str);
		if (matcher.find())
			return matcher.group(1).toString();
		else
			return "";
	}

	public synchronized String getHostPort() {
		return hostPort_;
	}
	
	public synchronized Client getClient() {
		return client_;
	}  
	
	private synchronized boolean applyTemplates() {
		boolean ret = false;
		ret = applyTemplate("/res/template_mapping.json", conf_.getWarehouseValue(ElasticWarehouseConf.ES_TEMPLATE_STORAGE_NAME) );
		ret = applyTemplate("/res/template_mapping_tasks.json", conf_.getWarehouseValue(ElasticWarehouseConf.ES_TEMPLATE_TASKS_NAME) );
		ret = applyTemplate("/res/template_mapping_uploads.json", conf_.getWarehouseValue(ElasticWarehouseConf.ES_TEMPLATE_UPLOADS_NAME) );
        return ret;
	}
	
	private synchronized boolean applyTemplate(String templateName, String tName) {
		boolean ret = false;
		String template = ResourceTools.getTextFileContent(templateName);
		template = template.replace("<templatename>", tName);
		PutIndexTemplateRequestBuilder indexTemplateRequestBuilder = client_.admin().indices().preparePutTemplate(tName);
		PutIndexTemplateResponse putIndexTemplateResponse = indexTemplateRequestBuilder.setSource(template).get(TimeValue.timeValueMinutes(1));
        if (putIndexTemplateResponse.isAcknowledged()) {
        	LOGGER.info("Mapping file "+templateName+" successfully imported !");
            ret = true;
        } else {
        	LOGGER.error("Mapping file "+templateName+" was NOT imported !");
        }
        return ret;
	}
	
	
	public boolean indexExists(String indexName)
	{
		boolean hasIndex = client_.admin().indices().exists(new IndicesExistsRequest(indexName)).actionGet().isExists();
		return hasIndex;
	}
	private synchronized boolean createIndex(String indexName) {
		boolean ret = false;
		
		boolean hasIndex = indexExists(indexName);
		if( hasIndex )
		{
			LOGGER.info("index " + indexName + " already exist");
			return false;
		}
		
		CreateIndexResponse createResponse = client_.admin().indices().create(new CreateIndexRequest(indexName)).actionGet();
		if (createResponse.isAcknowledged()) {
			LOGGER.info("Index successfully created !");
            ret = true;
        } else {
        	LOGGER.error("Index was NOT created !");
        }
		
		//index test document to create childs type
		//client_.prepareIndex(defaultIndexName_, defaultChildsTypeName_)
		//		.setSource("{}")
		//       .execute()
		//        .actionGet();
		
		return ret;
	}
	private synchronized boolean prepareMapping(String typeName) {
		
		String mappingJson = ResourceTools.getTextFileContent("/res/mapping.json");
		//System.out.println(mappingJson);
		PutMappingResponse putMappingResponse = client_.admin().indices()
                .preparePutMapping()
                .setType(typeName)
                .setSource( mappingJson )
                .execute().actionGet();
		return putMappingResponse.isAcknowledged();
		
	}
	private synchronized boolean documentExists(String id)
	{		
		GetResponse response = client_.prepareGet(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/, 
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/,  id).setFields(new String[0])
		        .execute()
		        .actionGet();
		if( response.isExists() )
			return true;
		else
			return false;		
	}
	
	private synchronized void prepareSettings()
	{
		try {
			String settingsAsString = "{ \"script\" : { \"disable_dynamic\" : false } }";
			//UpdateSettingsResponse response = client_.admin().indices().prepareUpdateSettings().setSettings(settingsAsString).setIndices("twitter").execute().actionGet();
			UpdateSettingsResponse response = client_.admin().indices().prepareUpdateSettings().setSettings(settingsAsString).setIndices("twitter").execute().actionGet();
			
			boolean isAcknowledged = response.isAcknowledged();//.getIndex();
			System.out.println("isAcknowledged: " + isAcknowledged);
			
		} catch (ElasticsearchException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		}
		
	}
	private synchronized String indexChildFile(ElasticWarehouseFile file)
	{
		String id = null;
		try {
			
			IndexResponse response = client_.prepareIndex(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/, 
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE) /*ElasticWarehouseConf.defaultChildsTypeName_*/)
			        .setSource( file.getJsonSourceBuilder() )
			        .execute()
			        .actionGet();
			
			String _index = response.getIndex();
			String _type = response.getType();
			String _id = response.getId();
			long _version = response.getVersion();
			LOGGER.info("Indexed Child: " + _index + "/" + _type + "/" + _id + " at version:" + _version);
			id = _id;

		} catch (ElasticsearchException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		}
		return id;
	}
	public synchronized List<IndexingResponse> indexFolder(ElasticWarehouseFolder fldr)
	{
		List<IndexingResponse> ret = new LinkedList<IndexingResponse>();
		String[] fldrs = fldr.splitFolders();
		for(String fldrname : fldrs)
		{
			if( folderExists(fldrname, true) )
			{
				LOGGER.info("mkdir: " + fldrname + " already exists, skipping");
			}
			else
			{
				try {
					ret.add( indexFile(new ElasticWarehouseFolder(fldrname, conf_), false) );
					LOGGER.info("mkdir: " + fldrname + " created");
				} catch (IOException e) {
					EWLogger.logerror(e);
					e.printStackTrace();
				}
			}
		}
		return ret;
	}
	public synchronized IndexingResponse indexFile(ElasticWarehouseFile file, boolean replaceupload)
	{
		String id = null;
		String errormsg = "";
		IndexResponse response = null;
		try {
				
			if( file.getId() == null )	//new file
				response = client_.prepareIndex(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/, 
						conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/)
			        .setSource( file.getJsonSourceBuilder() )
			        .execute()
			        .actionGet();
			else	//new version upload
				response = client_.prepareIndex(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/, 
						conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE)/*ElasticWarehouseConf.defaultTypeName_*/, file.getId() )
		        .setSource( file.getJsonSourceBuilder() )
		        .execute()
		        .actionGet();
			
			String _index = response.getIndex();
			String _type = response.getType();
			String _id = response.getId();
			long _version = response.getVersion();
			LOGGER.info("Indexed: " + _index + "/" + _type + "/" + _id + " at version:" + _version);
			id = _id;
			
			//--------- manage children
			deleteChildren(id);
			LinkedList<ElasticWarehouseFile> embeddedfiles = file.getEmbeddedFiles();
			for(ElasticWarehouseFile embfile : embeddedfiles)
			{
				embfile.setParentId(id);
				if( embfile.targetfolder_ == null )
					embfile.setTargetFolder(file.targetfolder_);

				indexChildFile(embfile);
			}
			
			//--------- manage binary upload
			boolean storeContent = conf_.getWarehouseBoolValue(ElasticWarehouseConf.STORECONTENT, true);
			if( storeContent == false )
			{
		     	boolean storeMoveScanned = conf_.getWarehouseBoolValue(ElasticWarehouseConf.STOREMOVESCANNED, true);
		     	String storeFolder = conf_.getWarehouseValue(ElasticWarehouseConf.STOREFOLDER);
		     	String newpathname = storeFolder+"/"+_id;
		     	try
		     	{
			     	if(file.originSource_.equals("scan") && storeMoveScanned )
			     		FileTools.writeBytes(newpathname, file.binaryContent_);
			     	
			     	if(file.originSource_.equals("upload") )
			     		FileTools.writeBytes(newpathname, file.binaryContent_);
		     	}
		     	catch(IOException e)
		     	{
		     		LOGGER.error("Cannot store file ("+newpathname+") on local filesystem." + e.getMessage());
		     		EWLogger.logerror(e);
		     		e.printStackTrace();
		     	}
			}else{
				if( replaceupload )
				{
					//upload binary data to index defined by 'elasticsearch.index.uploads.name'
					IndexResponse uploadresponse = client_.prepareIndex(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_UPLOADS_NAME) , 
							conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_UPLOADS_TYPE), id )
			        .setSource( file.getJsonBinaryDataUploadBuilder() )
			        .execute()
			        .actionGet();
					
					String __index = response.getIndex();
					String __type = response.getType();
					String __id = response.getId();
					long __version = response.getVersion();
					LOGGER.info("Indexed binary content: " + __index + "/" + __type + "/" + __id + " at version:" + __version);
				}else{
					LOGGER.info("Not indexing binary content for : " + id);
				}
			}
			
			refreshIndex();
			
			
		} catch (ElasticsearchException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
			errormsg = e.getDetailedMessage();
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
			errormsg = e.getMessage();
		}
		return new IndexingResponse(id, errormsg, response);
	}
	
	public LinkedList<String> findChildren(String id)
	{
		LinkedList<String> ret = new LinkedList<String>();
		SearchResponse searchResponse = client_.prepareSearch( conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) )
				.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE) )
		        .setQuery(termQuery("parentId", id))
		        .setSize(SIZE_NO_LIMIT)
		        .setSearchType(SearchType.SCAN)
		        .setScroll(new TimeValue(60000))
		        .execute()
		        .actionGet();
		while (true)
	    {
			searchResponse = client_.prepareSearchScroll(searchResponse.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
		    if( searchResponse.getHits().hits().length == 0 )
		    	break;
		    for (SearchHit hit : searchResponse.getHits()) 
		    {
		    	String childid = hit.getId();
		    	ret.add(childid);
			}
	    }
		
		return ret;
	}
	public LinkedList<EwBrowseTuple> findAllSubFolders(String folderPrefix)
	{
		String folder = ResourceTools.preprocessFolderName(folderPrefix);
		LinkedList<EwBrowseTuple> ret = new LinkedList<EwBrowseTuple>();
		SearchResponse searchResponse = client_.prepareSearch( conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) )
				.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE),
						  conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) )
		        .setQuery(
		        		QueryBuilders.boolQuery()
    						.must(QueryBuilders.termQuery("isfolder", true) )
    						.must(prefixQuery("folderna", folder) ) )
		        .setSize(SIZE_NO_LIMIT)
		        .setSearchType(SearchType.SCAN)
		        .setScroll(new TimeValue(60000))
		        .setVersion(true)
		        .execute()
		        .actionGet();

		while (true)
	    {
			searchResponse = client_.prepareSearchScroll(searchResponse.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
		    if( searchResponse.getHits().hits().length == 0 )
		    	break;
		    for (SearchHit hit : searchResponse.getHits()) 
		    {
				EwBrowseTuple tuple = new EwBrowseTuple(hit);
		    	ret.add(tuple);
			}
	    }
		
		return ret;
	}
	
	public LinkedList<String> findAllSubFilesAndFolders(String folderPrefix)
	{
		String folder = ResourceTools.preprocessFolderName(folderPrefix);
		LinkedList<String> ret = new LinkedList<String>();
		SearchResponse searchResponse = client_.prepareSearch( conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/)
				.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE),
						conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) )
		        .setQuery(prefixQuery("folderna", folder) )
		        .setSize(SIZE_NO_LIMIT)
		        .setSearchType(SearchType.SCAN)
		        .setScroll(new TimeValue(60000))
		        .setFetchSource(false)
		        .execute()
		        .actionGet();
		while (true)
	    {
			searchResponse = client_.prepareSearchScroll(searchResponse.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
		    if( searchResponse.getHits().hits().length == 0 )
		    	break;
			for (SearchHit hit : searchResponse.getHits()) 
		    {
		    	String id = hit.getId();
		    	ret.add(id);
			}
	    }
		return ret;
	}
	public synchronized boolean deleteChildren(String id)
	{
		//ES2.x Experiment
		DeleteByQueryAdapter deletebyquery = new DeleteByQueryAdapter(
				termQuery("parentId", id)
    			);
		boolean rc = deletebyquery.execute(client_, 
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME),
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE)
				);
		
		//ES1.x 
		/*DeleteByQueryResponse deleteResponse = client_.prepareDeleteByQuery(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) )
				.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE) )
		        .setQuery(termQuery("parentId", id))
		        .execute()
		        .actionGet();*/
		return rc;
	}
	
	public synchronized void deleteFile(String id)
	{
		DeleteResponse deleteResponse = client_.prepareDelete(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME)/*ElasticWarehouseConf.defaultIndexName_*/, 
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/, id)
		        .execute()
		        .actionGet();
		//deleteResponse.h
	}
	
	public synchronized void Dispose()
	{
		client_.close();
		if( node_ != null )
			node_.close();
	}
	
	
	private Client openTransportClient() {
		LOGGER.info("Starting in distributed mode (using external ElasticSearch cluster)");
		
		String clusterName = conf_.getWarehouseConfiguration().get(ElasticWarehouseConf.ESCLUSTER);
		String hosts = conf_.getWarehouseConfiguration().get(ElasticWarehouseConf.ESTRANSPORTHOSTS);		
		
		TransportClient client = TransportClient.builder().build();
		if( hosts.length() > 0 )
		{
			String[] hostsArray = hosts.split(",");
			for(String host : hostsArray)
			{
				
				if( host.contains(":") )
				{
					String[] parts = host.split(":");
					client.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress(parts[0], Integer.parseInt(parts[1]) )) );
					if( hostPort_.length() == 0 )
						hostPort_ = parts[0]+":9200";
				}else{
					client.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress(host, 9300 )) );
					if( hostPort_.length() == 0 )
						hostPort_ = host+":9200";
				}
			}
		}
		/*if( clusterName.length() > 0 )
		{
			Settings settings = Settings.settingsBuilder()
			        .put("cluster.name", clusterName)
			        .put("client.transport.sniff", true).build();
			client =  new TransportClient(settings);
		}*/

		return client;
	}

	private Client openNodeClient()
	{
		LOGGER.info("Starting in embedded mode");
		
		Map<String, String > c = conf_.getESConfiguration();
		if( c.containsKey("path.data") == false )
			c.put("path.data", conf_.getHomePath()+"/data" );
		if( c.containsKey("path.logs") == false )
			c.put("path.logs", conf_.getHomePath()+"/logs" );
		if( c.containsKey("path.plugins") == false )
			c.put("path.plugins", conf_.getHomePath()+"/plugins" );
		if( c.containsKey("path.conf") == false )
			c.put("path.conf", conf_.getHomePath()+"/conf" );
		if( c.containsKey("path.home") == false )
			c.put("path.home", conf_.getHomePath() );
		//c.put("node.data" , "true");
		//c.put("node.client" , "true");
		//c.put("node.local" , "false");
		c.put("node.master" , "true");

		Settings settings = null;
		//for compatibility with ES 2.0 separate branch is needed
		/*
		Method m_settingsBuilder = null;
		Method m_put = null;
	    try {
	    	m_settingsBuilder = Settings.class.getMethod("settingsBuilder");
	    	m_put = Settings.Builder.class.getMethod("put");
	    } catch (Exception e) {
	      // doesn't matter
	    }	    
	    if( m_put != null && m_settingsBuilder != null)	//underlying ES is >=2.0
	    {
	    	org.elasticsearch.common.settings.Settings.Builder builder;
			try {
				builder = (org.elasticsearch.common.settings.Settings.Builder)m_settingsBuilder.invoke(null);
				builder = (org.elasticsearch.common.settings.Settings.Builder)m_put.invoke(builder, c);
				settings = builder.build();
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				// doesn't matter			
				} 
	    }else{ */
	      	settings = Settings.settingsBuilder()
						.put(c)
						.build();
	    //}		
		
		
		//node_ = nodeBuilder().settings(settings).data(true).client(true).local(false).node();
		//node_ = nodeBuilder().settings(settings).data(true).client(false).local(false).node();
		node_ = nodeBuilder().settings(settings).data(true).client(false).local(false).node();
		//node_ = nodeBuilder().settings(settings).node();
		Client client = node_.client();
		return client;
	}
	
	private void bulkUpdate()
	{
		BulkRequestBuilder bulkRequest = client_.prepareBulk();
		
		try {
			bulkRequest.add(client_.prepareIndex("twitter", "tweet", "1")
			        .setSource(jsonBuilder()
			                    .startObject()
			                        .field("user", "kimchy")
			                        .field("postDate", new Date())
			                        .field("message", "trying out Elasticsearch")
			                    .endObject()
			                  )
			        );
			for(ActionRequest act : bulkRequest.request().requests())
			{
				OutputStream output = new OutputStream()
			    {
			        private StringBuilder string = new StringBuilder();
			        @Override
			        public void write(int b) throws IOException {
			            this.string.append((char) b );
			        }
			
			        //Netbeans IDE automatically overrides this toString()
			        public String toString(){
			            return this.string.toString();
			        }
			    };
			    OutputStreamStreamOutput out = new OutputStreamStreamOutput (output);
				act.writeTo(out);
				System.out.println(output.toString( ) );
			}
			
			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
        	    // process failures by iterating through each bulk response item
        		System.out.println("Some failures...");
        		for(BulkItemResponse item : bulkResponse.getItems() )
        		{
        			System.out.println(item.getIndex()+"/"+item.getType()+"/"+ item.getId() + " ver."+item.getVersion()+ ", fail:" + item.isFailed() + (item.isFailed()?item.getFailureMessage():"") );
        		}
        	}
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		}
		//bulkRequest.request().requests().get(0).
		//String s = bulkRequest.request().toString();
	}
	public byte[] fetchFile(String id)
	{
		GetResponse response = client_.prepareGet(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/, 
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/,  id).setFetchSource(ElasticWarehouseMapping.FILECONTENT, "")
		        .execute()
		        .actionGet();
		byte[] obj = null;
		try {
			Map<String, Object> source = response.getSource();
			if( source.containsKey(ElasticWarehouseMapping.FILECONTENT))
				obj = Base64.decode(source.get(ElasticWarehouseMapping.FILECONTENT).toString());
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		}
		return obj;
	}

	public boolean isFolder(String id) throws Exception
	{
		GetResponse response = client_.prepareGet(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/, 
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/,  id)
				.setFetchSource("isfolder", "")
		        .execute()
		        .actionGet();
		boolean isfolder = false;
		Map<String, Object> source = response.getSource();
		if( source == null )
			throw new Exception("Provided id="+id+" doesn't exist");
		if( source.containsKey("isfolder") )
		{
			isfolder = Boolean.parseBoolean(source.get("isfolder").toString());
		}
		return isfolder;
	}
	
	
	public boolean folderExists(String infolder, boolean exactTerm)
	{
		String folder = ResourceTools.preprocessFolderName(infolder);
		if( folder.length() == 0 )
			return false;
		
		SearchRequestBuilder seaerchreqbuilder = client_.prepareSearch(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/)
				.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/)
		        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		if( exactTerm )
			seaerchreqbuilder.setQuery(
	        		QueryBuilders.boolQuery()
    					.must(QueryBuilders.termQuery("isfolder", true) )
    					.must(QueryBuilders.termQuery("folderna", folder) )
				);
		else
			seaerchreqbuilder.setQuery(
	        		QueryBuilders.boolQuery()
    					.must(QueryBuilders.termQuery("isfolder", true) )
    					.must(QueryBuilders.prefixQuery("folderna", folder) )
				);
		seaerchreqbuilder.setSize(1);
		seaerchreqbuilder.setFrom(0);
		seaerchreqbuilder.addSort(SortBuilders.fieldSort("submitdate").order(SortOrder.DESC).ignoreUnmapped(true));
		//System.out.println(seaerchreqbuilder.toString());
		SearchResponse scrollResp = seaerchreqbuilder.execute().actionGet();
	
		return scrollResp.getHits().getHits().length > 0 ;
	}

	public boolean removeFolder(String infolder)
	{

		try {
			String folder = ResourceTools.preprocessFolderName(infolder);
			if( folder.length() == 0 )
				return false;
			
			ElasticWarehouseFolder fdlr = new ElasticWarehouseFolder(folder, conf_);
			int level = fdlr.getFolderLevel();
			if( level > 0 )	//cannot delete root "/"
			{
				//ES 2.x experiment
				DeleteByQueryAdapter deletebyquery = new DeleteByQueryAdapter( 
						QueryBuilders.boolQuery()
			        			//.must( QueryBuilders.rangeQuery("folderlevel").gte(level) )
			        			//.must( QueryBuilders.termQuery("folderna", folder) )
			        			.must( QueryBuilders.prefixQuery("folderna", folder) )
			        			);
				boolean rc = deletebyquery.execute(client_, 
						conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME),
						conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE) );
				
				if( rc )
					rc = deletebyquery.execute(client_, 
						conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME),
						conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) );
				
				//ES 1.x
				/*DeleteByQueryResponse deleteResponse = client_.prepareDeleteByQuery(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) )
					//.setTypes(ElasticWarehouseConf.defaultTypeName_)
			        .setQuery( 
			        		QueryBuilders.boolQuery()
			        			//.must( QueryBuilders.rangeQuery("folderlevel").gte(level) )
			        			//.must( QueryBuilders.termQuery("folderna", folder) )
			        			.must( QueryBuilders.prefixQuery("folderna", folder) )
			        			)
			        .execute()
			        .actionGet();*/
				
				refreshIndex();
				
				return rc;
			}else{
				return false;
			}
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
			return false;
		}
	}

	public boolean moveTo(String fileid, String targetfolder) throws IOException
	{
		ElasticWarehouseFolder fdlr = new ElasticWarehouseFolder(targetfolder, conf_);
		int level = fdlr.getFolderLevel();
		
		UpdateResponse updateResponse = client_.prepareUpdate(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/, 
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/, fileid)
				.setRefresh(true)
				.setDoc(jsonBuilder()               
	            .startObject()
	                .field("folder", targetfolder)
	                .field("folderna", targetfolder)
	                .field("folderlevel", level+1)
	            .endObject())
	            .execute()
		        .actionGet();
		
		LinkedList<String> children = getChildrenIds(fileid);
		for(String id : children )
		{
			UpdateResponse updateChildResponse = client_.prepareUpdate(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/, 
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE) /*ElasticWarehouseConf.defaultChildsTypeName_*/, id)
					.setRefresh(true)
					.setDoc(jsonBuilder()               
		            .startObject()
		                .field("folder", targetfolder)
		                .field("folderna", targetfolder)
		                .field("folderlevel", level)
		            .endObject())
		            .execute()
			        .actionGet();
		}
		
		refreshIndex();
		
		return true;
	}

	private LinkedList<String> getChildrenIds(String fileid)
	{
		LinkedList<String> ret = new LinkedList<String>();
		SearchRequestBuilder esreq = client_.prepareSearch(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/)
		        .setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE) /*ElasticWarehouseConf.defaultChildsTypeName_*/)
		        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
		        .setFetchSource(new String[] { "parentId" }, null)
		        .setQuery(QueryBuilders.termQuery("parentId", fileid))
		        .setSize(999)
		        .setFrom(0); 
//		if( showrequest )
//	    {
	    	System.out.println(esreq.toString());
//	    }
		
		SearchResponse response = esreq.execute().actionGet();
		LOGGER.info("**** Found " + response.getHits().getHits().length + " children");
	    for (SearchHit hit : response.getHits()) 
	    {
	    	String childid = hit.getId();
	    	ret.add(childid);
		}

		return ret;
	}
	
	//call this method only when source contains file contents ( source=upload or source=scan )
	public IndexingResponse uploadFile(String path, String file, String targetfolder/*nullable*/, String id /*nullable*/, String source)
	{
		IndexingResponse ret = null;
		try {
			long dwStart = System.currentTimeMillis();
			ElasticWarehouseFileParser parser = ElasticWarehouseFileParserFactory.getAutoParser(path+"/"+file, file, targetfolder, conf_);
			if( parser.parse() )
			{
				long tt = System.currentTimeMillis() - dwStart;
				ret = parser.indexParsedFile(this, id/*, atid.toString()*/, tt, source, path, file, true);
				/*byte[] bytes = tmpAccessor.fetchFile(id.toString());
				FileOutputStream fos = new FileOutputStream(path+"/copy"+file);
				fos.write(bytes);
				fos.close();
				atid++;*/
			}else{
				LOGGER.error("**** Cannot parse " + path+"/"+file);
			}
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
			ret = new IndexingResponse(null, file+ " : " + e.getMessage(), null);
		}
		return ret;
	}

	public LinkedList<EsShardInfo> getESAttributesForWarehouseIndices()
	{
		
		final String[] indices = new String[] { conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) };
		
		final ClusterStateRequest clusterStateRequest = new ClusterStateRequest();
        clusterStateRequest.clear().nodes(true).routingTable(true).indices(indices);
        ClusterStateResponse clusterStateResponse = client_.admin().cluster().state(clusterStateRequest).actionGet();
        
        IndicesStatsRequest indicesStatsRequest = new IndicesStatsRequest();
        indicesStatsRequest.all();
        IndicesStatsResponse indicesStatsResponse = client_.admin().indices().stats(indicesStatsRequest).actionGet();
        
        return buildResponse(clusterStateResponse, indicesStatsResponse);
	}

	private LinkedList<EsShardInfo> buildResponse(
			ClusterStateResponse state,
			IndicesStatsResponse stats)
	{
		LinkedList<EsShardInfo> ret = new LinkedList<EsShardInfo>();
		
		for (ShardRouting shard : state.getState().routingTable().allShards())
		{
			CommonStats shardStats = stats.asMap().get(shard);
			
			String nodeIp="";
			String nodeName="";
			if (shard.assignedToNode() && state.getState().nodes() != null )
			{
				if( state.getState().nodes().get(shard.currentNodeId()) != null )
				{
					nodeIp = state.getState().nodes().get(shard.currentNodeId()).getHostAddress();
					nodeName = state.getState().nodes().get(shard.currentNodeId()).name();
				}
                /*StringBuilder name = new StringBuilder();
                name.append(state.getState().nodes().get(shard.currentNodeId()).name());
                if (shard.relocating()) {
                    String reloIp = state.getState().nodes().get(shard.relocatingNodeId()).getHostAddress();
                    String reloNme = state.getState().nodes().get(shard.relocatingNodeId()).name();
                    name.append(" -> ");
                    name.append(reloIp);
                    name.append(" ");
                    name.append(reloNme);
                }*/
            }
			ret.add(new EsShardInfo(shard.id(), 
									shard.primary(),
									shardStats == null ? -1 : shardStats.getDocs().getCount(),
									shardStats == null ? -1 : shardStats.getStore().getSize().getBytes(),
									nodeIp, nodeName));

		}

		return ret;
	}

	public LinkedList<EsNodeInfo> getESNodeInfos(HashSet<String> assignedNodes)
	{
		final ClusterStateRequest clusterStateRequest = new ClusterStateRequest();
        clusterStateRequest.clear().nodes(true);
        ClusterStateResponse clusterStateResponse = client_.admin().cluster().state(clusterStateRequest).actionGet();
        

        //NodesInfoRequest nodesInfoRequest = new NodesInfoRequest();
        //nodesInfoRequest.clear().jvm(true).os(true).process(true);
        //NodesInfoResponse nodesInfoResponse = client_.admin().cluster().nodesInfo(nodesInfoRequest).actionGet();
        
        NodesStatsRequest nodesStatsRequest = new NodesStatsRequest();
        nodesStatsRequest.clear().jvm(true).os(true).fs(true).indices(true);
        NodesStatsResponse nodesStatsResponse = client_.admin().cluster().nodesStats(nodesStatsRequest).actionGet();
        
        return buildResponse(clusterStateResponse/*, nodesInfoResponse*/, nodesStatsResponse, assignedNodes);
	}
	
    private LinkedList<EsNodeInfo> buildResponse(
			ClusterStateResponse state,
			/*NodesInfoResponse nodesInfo,*/
			NodesStatsResponse nodesStats, HashSet<String> assignedNodes) 
	{
    	LinkedList<EsNodeInfo> ret = new LinkedList<EsNodeInfo>();
        DiscoveryNodes nodes = state.getState().nodes();
        String masterId = nodes.masterNodeId();

        for (DiscoveryNode node : nodes)
        {
            //NodeInfo info = nodesInfo.getNodesMap().get(node.id());
            NodeStats stats = nodesStats.getNodesMap().get(node.id());
            
            short heapusedpercent = (stats == null ? -1 : stats.getJvm().getMem().getHeapUsedPercent());
            double loadvg = (stats == null ? -1.0 : stats.getOs() == null ? -1.0 : stats.getOs().getLoadAverage() < 1 ? -1.0 : stats.getOs().getLoadAverage() );
            short memusedpercent = (stats == null ? -1 : stats.getOs().getMem() == null ? -1 : stats.getOs().getMem().getUsedPercent());
            String nodename = node.name();
            
            if( assignedNodes == null || assignedNodes.isEmpty() || assignedNodes.contains(nodename) )
            {
            	ret.add(new EsNodeInfo(heapusedpercent, loadvg, memusedpercent, nodename) );
            }
        }
    
    
        return ret;
	}
	
	
    public LinkedList<EsIndexInfo> getESIndicesInfos(HashSet<String> forindices)
    {
    	 String[] indices = new String[forindices.size()]; 
    	 indices = forindices.toArray(indices);
         final ClusterStateRequest clusterStateRequest = new ClusterStateRequest();
         clusterStateRequest.clear().indices(indices).metaData(true);
         final ClusterStateResponse clusterStateResponse = client_.admin().cluster().state(clusterStateRequest).actionGet();
         
		//ES 2.x experiment
         final String[] concreteIndices = clusterStateResponse.getState().metaData().getConcreteAllIndices();
         final String[] openIndices = clusterStateResponse.getState().metaData().getConcreteAllOpenIndices();

		//ES 1.x
		//final String[] concreteIndices = clusterStateResponse.getState().metaData().concreteIndices(IndicesOptions.fromOptions(false, true, true, true), indices);
        //final String[] openIndices = clusterStateResponse.getState().metaData().concreteIndices(IndicesOptions.lenientExpandOpen(), indices);

         ClusterHealthRequest clusterHealthRequest = Requests.clusterHealthRequest(openIndices);
         final ClusterHealthResponse clusterHealthResponse = client_.admin().cluster().health(clusterHealthRequest).actionGet();
         
         IndicesStatsRequest indicesStatsRequest = new IndicesStatsRequest();
         indicesStatsRequest.all();
         IndicesStatsResponse indicesStatsResponse = client_.admin().indices().stats(indicesStatsRequest).actionGet();
         
         return buildResponse(concreteIndices, clusterHealthResponse, indicesStatsResponse/*, clusterStateResponse.getState().metaData()*/);
    }
	

	private LinkedList<EsIndexInfo> buildResponse(String[] indices, ClusterHealthResponse health, IndicesStatsResponse stats/*, MetaData indexMetaDatas*/)
	{
		//http://www.tachilab.com/p/github.com/elasticsearch/elasticsearch/v1.4.4/src/main/java/org/elasticsearch/rest/action/cat/RestIndicesAction.java.html
		LinkedList<EsIndexInfo> ret = new LinkedList<EsIndexInfo>();
	
		for (String index : indices) {
            ClusterIndexHealth indexHealth = health.getIndices().get(index);
            IndexStats indexStats = stats.getIndices().get(index);
            //IndexMetaData indexMetaData = indexMetaDatas.getIndices().get(index);
            //IndexMetaData.State state = indexMetaData.getState();
            
            ret.add( new EsIndexInfo(
            		indexHealth.getNumberOfShards(),
            		indexHealth.getNumberOfReplicas(),
            		
            		(indexStats == null ? -1 : indexStats.getPrimaries().getDocs().getCount()),
            		
            		(indexStats == null ? -1 : indexStats.getTotal().getStore().size().bytes()),
            		(indexStats == null ? -1 : indexStats.getPrimaries().getStore().size().bytes()),
            		/* query rates */
            		(indexStats == null ? -1 : indexStats.getTotal().getSearch().getTotal().getQueryCurrent()),
            		(indexStats == null ? -1 : indexStats.getTotal().getSearch().getTotal().getQueryTime().getMillis()),
            		(indexStats == null ? -1 : indexStats.getTotal().getSearch().getTotal().getQueryCount()),
            		(indexStats == null ? -1 : indexStats.getTotal().getSearch().getTotal().getFetchTime().getMillis()),
            		(indexStats == null ? -1 : indexStats.getTotal().getSearch().getTotal().getFetchCount()),
            		
            		/*indexing rates */
            		(indexStats == null ? -1 : indexStats.getTotal().getIndexing().getTotal().getIndexCurrent()),
                    (indexStats == null ? -1 : indexStats.getTotal().getIndexing().getTotal().getIndexTime().getMillis()),
                    (indexStats == null ? -1 : indexStats.getTotal().getIndexing().getTotal().getIndexCount())
            		) );
            
            
            
            //(indexStats == null ? null : indexStats.getTotal().getPercolate().getCurrent());
            //(indexStats == null ? null : indexStats.getTotal().getPercolate().getMemorySize());
            //(indexStats == null ? null : indexStats.getTotal().getPercolate().getNumQueries());
            //(indexStats == null ? null : indexStats.getTotal().getPercolate().getTime());
            //(indexStats == null ? null : indexStats.getTotal().getPercolate().getCount());
            

		}
		return ret;
	}



	public EwInfoTuple getFileInfoById(String id, boolean showrequest, boolean includechildren) {
		EwInfoTuple ei = new EwInfoTuple();
		GetRequestBuilder getreqbuilder = client_.prepareGet(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), 
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE), id);
		
		if( showrequest )
			System.out.println(getreqbuilder.toString());
		
		GetResponse response = getreqbuilder.execute().actionGet();
		if( response.isExists() == false && includechildren )
		{
			getreqbuilder = client_.prepareGet(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), 
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE), id);
			if( showrequest )
				System.out.println(getreqbuilder.toString());
			response = getreqbuilder.execute().actionGet();
		}
		ei.isexists = response.isExists();
		ei.source = response.getSourceAsMap();
		ei.id = response.getId();
		ei.version = response.getVersion();
		
		return ei;
	}
	public boolean setFolder(String id, String newFolderPath) {
		return setFileAttribute("folder", id, newFolderPath);
	}

	public boolean setFilename(String id, String newFilename) {	
		return setFileAttribute("filename", id, newFilename);	
	}

	public boolean setFileAttribute(String attribute, String id, String newValue)
	{
		boolean imchild = false;
		GetResponse response = client_.prepareGet(
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), 
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE), id)
				.execute().actionGet();
		if( response.isExists() == false ) //then it means Id is not valid or ID belongs to child
		{
			imchild = true;
			response = client_.prepareGet(
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), 
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE), id)
					.execute().actionGet();
		}
		Map<String, Object> source = response.getSource();
		if( attribute.startsWith("fileaccess") )
		{
			//don't care about user so far
			Map<String, Object> newfileaccess = new HashMap<String, Object>();
			newfileaccess.put("adate", newValue);
			source.put("fileaccess", newfileaccess);
		}
		else
		{
			source.put(attribute, newValue);
		}
		
		//fill other related fields
		if( attribute.equals("filename") )
			source.put("filenamena", newValue);
		if( attribute.equals("folder") )
			source.put("folderna", newValue);
		
		IndexResponse indexResponse = null;
		
		if( imchild == false )
			indexResponse = client_.prepareIndex(
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), 
				conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE), id)
				.setSource(source).setRefresh(true).execute().actionGet();
		else
			indexResponse = client_.prepareIndex(
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), 
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE), id)
					.setSource(source).setRefresh(true).execute().actionGet();
		
		return (indexResponse.getId().length()>0);
	}



	public boolean updateLastAccessTime(String id)
	{
		Date today = Calendar.getInstance().getTime();
		return setFileAttribute("fileaccess.adate", id, df.format(today) );
	}

	public LinkedList<String> findAllFilesInFolder(String folderpath, boolean inchildtype) throws IOException
	{
		LinkedList<String> result = new LinkedList<String>();
		String folder = ResourceTools.preprocessFolderName(folderpath); 
		ElasticWarehouseFolder fldr = new ElasticWarehouseFolder(folder, conf_);
		int level = fldr.getFolderLevel();
		level++;
		
		String type = "";
		if( inchildtype )
			type = conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE);
		else
			type = conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE);
		
		SearchRequestBuilder esreq = client_.prepareSearch(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) )
	        .setTypes( type )
	        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
	        .setFetchSource(null, new String[] {"filecontent","filetext"});
        if( folder.equals("/") )
        	esreq.setQuery( QueryBuilders.termQuery("folderlevel", level) );
    	else
        	esreq.setQuery(
        		QueryBuilders.boolQuery()
        			.must( QueryBuilders.matchQuery("folderna", folder).type(Type.PHRASE_PREFIX) )
        			.must( QueryBuilders.termQuery("folderlevel", level) )
        			);
        esreq
	        .setVersion(true)
	        .setSize(SIZE_NO_LIMIT)
	        .setSearchType(SearchType.SCAN)
		    .setScroll(new TimeValue(60000))
	        .setNoFields();
        
        SearchResponse response = esreq.execute().actionGet();
        while (true)
	    {
        	response = client_.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
		    if( response.getHits().hits().length == 0 )
		    	break;
		    for (SearchHit hit : response.getHits()) 
		    {
		    	result.add(hit.getId());
			}
	    }
        
        
        return result;
	}

	public void setNewFolderForFilesInFolder(String currentFolder, String newFolder,  boolean inchildtype) throws IOException
	{
		BulkRequestBuilder bulkRequest = client_.prepareBulk();
		String type = "";
		if( inchildtype )
			type = conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE);
		else
			type = conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE);
		
		LinkedList<String> ids = findAllFilesInFolder(currentFolder, inchildtype);
		for(String id : ids)
		{
			UpdateRequestBuilder updatereq = client_.prepareUpdate( 
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME), type, id)
				.setRefresh(false)
				.setDoc(jsonBuilder()
	                    .startObject()
	                        .field("folderna", newFolder)
	                        .field("folder", newFolder)
	                    .endObject());
            bulkRequest.add(updatereq);				
		}
		
		if( ids.size() > 0 )
		{
			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				throw new ElasticsearchException("Updating folders for files inside "+currentFolder+" failed. "+bulkResponse.buildFailureMessage() );
			}
		}
	}
	
	public void setNewFolderForFilesInFolder(String currentFolder, String newFolder) throws IOException
	{
		setNewFolderForFilesInFolder(currentFolder, newFolder, false);
		setNewFolderForFilesInFolder(currentFolder, newFolder, true);
	}

	public void refreshIndex() {
		client_.admin().indices().prepareRefresh().execute().actionGet();
	}



	





	
}

