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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.common.settings.loader.SettingsLoader;
import org.elasticsearch.common.settings.loader.YamlSettingsLoader;
import org.elasticwarehouse.core.graphite.NetworkTools;
import org.elasticwarehouse.core.parsers.FileTools;

public class ElasticWarehouseConf {
	
	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseConf.class.getName()); 
	
	public static int APIPORT = 10200;
	public static int GRAFANARESTPORT = 10500;

	public DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private Date serviceStarted_ = Calendar.getInstance().getTime();
	
	private Map<String, String> esconfiguration = null;
	private Map<String, String> warehouseconfiguration = null;
	
	public static final String MODEEMBEDDED = "mode.embedded"; 
	
	public static final String STORECONTENT = "store.content";
	public static final String STOREMOVESCANNED = "store.movescanned";
	public static final String STOREFOLDER = "store.folder";
	
	public static final String GRAFANAPORT = "grafana.port";
	public static final String ESAPIPORT = "elasticwarehouse.api.port";
	public static final String THUMBSIZE = "thumb.size";
	public static final String EXCLUDE_FILES_LIST = "exclude.files";
	
	public static final String RRDDBPATH = "rrd.db.path";
	public static final String RRDHOSTNAME = "rrd.hostname";
	public static final String RRDENABLED = "rrd.enabled";
	
	public static final String TMPPATH = "path.tmp";
	
	
	public static final String ESTRANSPORTHOSTS = "elasticsearch.hosts";
	public static final String ESCLUSTER = "elasticsearch.cluster";
	
	public static final String ES_INDEX_STORAGE_NAME 		= "elasticsearch.index.storage.name";
	public static final String ES_INDEX_STORAGE_TYPE 		= "elasticsearch.index.storage.type";
	public static final String ES_INDEX_STORAGE_CHILDTYPE 	= "elasticsearch.index.storage.childtype";
	
	public static final String ES_INDEX_UPLOADS_NAME 		= "elasticsearch.index.uploads.name";
	public static final String ES_INDEX_UPLOADS_TYPE 		= "elasticsearch.index.uploads.type";
	
	public static final String ES_INDEX_TASKS_NAME 			= "elasticsearch.index.tasks.name";
	public static final String ES_INDEX_TASKS_TYPE 			= "elasticsearch.index.tasks.type";
	
	public static final String ES_TEMPLATE_STORAGE_NAME 	= "elasticsearch.template.storage.name";
	public static final String ES_TEMPLATE_TASKS_NAME 		= "elasticsearch.template.tasks.name";
	public static final String ES_TEMPLATE_UPLOADS_NAME 	= "elasticsearch.template.uploads.name";
	
	public static final String TASKSMAXNUMBER = "tasks.max.number";
	public static final int TASKSMAXNUMBERDEFAULT = 2;
	
	public static final String ES_KEEP_ALIVE_SLEEP = "tasks.keepalive.sleep";	//in ms
	
	public static final String AUTOSTART_TASKS = "autostart.tasks";
	
	
	/*
	public static final String defaultClusterName_ = "elasticwarehouse";
	public static final String defaultIndexName_ = "elasticwarehousestorage";
	public static final String defaultTasksIndexName_ = "elasticwarehousetasks";
	
	public static final String defaultTemplateName_ = "elasticwarehousestorage";
	public static final String defaultTemplateTasksName_ = "elasticwarehousetasks";
	public static final String defaultTypeName_ = "files";
	public static final String defaultTasksTypeName_ = "tasks";
	public static final String defaultChildsTypeName_ = "childfiles";
	*/
	
	public static final String URL_GUIDE_SEARCH = "http://elasticwarehouse.org/guide-search/";
	public static final String URL_GUIDE_GET = "http://elasticwarehouse.org/guide-get/";
	public static final String URL_GUIDE_INFO = "http://elasticwarehouse.org/guide-info/";
	public static final String URL_GUIDE_SEARCHALL = "http://elasticwarehouse.org/guide-searchall/";
	public static final String URL_GUIDE_SUMMARY = "http://elasticwarehouse.org/guide-summary/";
	public static final String URL_GUIDE_UPLOAD = "http://elasticwarehouse.org/guide-upload/";
	public static final String URL_GUIDE = "http://elasticwarehouse.org/guide-api/";
	public static final String URL_GUIDE_GRAPHITE = "http://elasticwarehouse.org/guide-graphite/";
	public static final String URL_GUIDE_TASK = "http://elasticwarehouse.org/guide-task/";
	public static final String URL_GUIDE_BROWSE = "http://elasticwarehouse.org/guide-browse/";
	
	public static final int TASKLISTSIZE = 100;

	public static final String FIELD_THUMB_SAMEASIMAGE = "filethumb_sameasimage";
	public static final String FIELD_THUMB_AVAILABLE = "filethumb_available";
	public static final String FIELD_THUMB_THUMB = "filethumb_thumb";
	public static final String FIELD_THUMB_THUMBDATE = "filethumb_thumbdate";

	

	
	
	
	public ElasticWarehouseConf()
	{
		//order is important!
		readWarehouseConfiguration();
		readESConfiguration();
	}
	
	public String getStartedDateAsString()
	{
		return df.format(serviceStarted_);
	}
	private void readESConfiguration()
	{
		esconfiguration = readConfigurationFile("elasticsearch.yml");
		/*if( esconfiguration.containsKey("node.name") )
		{
			LOGGER.warn("elasticsearch.yml contains node name attribute. Node name will be replaced by:" + NetworkTools.getHostName().toUpperCase() );
			esconfiguration.remove("node.name");
			
		}*/
		if( !esconfiguration.containsKey("node.name") )
		{
			esconfiguration.remove("node.name");
			if( warehouseconfiguration.containsKey(RRDHOSTNAME))
				LOGGER.warn("elasticsearch.yml doesn't contain node name attribute. Node name will be replaced by:" + warehouseconfiguration.get(RRDHOSTNAME) );
			else
				LOGGER.warn("elasticsearch.yml doesn't contain node name attribute. Node name will be replaced by:" + NetworkTools.getHostName2().toLowerCase() );
			
			
		}
		
		String keys[] = {"node.data", "node.master", "http.enabled" };
		for(String key : keys)
		{
			if( esconfiguration.containsKey(key) )
			{
				if( Boolean.parseBoolean( esconfiguration.get(key) ) != true )
					LOGGER.warn("elasticsearch.yml contains "+key+" attribute. "+key+" will be set to TRUE");
				esconfiguration.remove(key);
			}			
		}
		
		if( warehouseconfiguration.containsKey(RRDHOSTNAME))
			esconfiguration.put("node.name", warehouseconfiguration.get(RRDHOSTNAME) );
		else
			esconfiguration.put("node.name", NetworkTools.getHostName2().toLowerCase());
		esconfiguration.put("node.data", "true");
		esconfiguration.put("node.master", "true");
		esconfiguration.put("http.enabled", "true");
		
		if( esconfiguration.containsKey("cluster.name") == false )
			esconfiguration.put("cluster.name", warehouseconfiguration.get(ESCLUSTER) /*ElasticWarehouseConf.defaultClusterName_*/);
		
		/*	
		.put("node.data" , "true")
		//.put("node.client" , "true")
		//.put("node.local" , "false")
		.put("node.master" , "true")
		.put("name" , myNodeName_)
		//.put("http.port" , "9200")
		.put("http.enabled" , "true")
		//.put("path.plugins" , "/home/streamsadmin/workspaceE/elasticwarehouse.core/plugins")
		.put("path.logs" , "/home/streamsadmin/workspaceE/elasticwarehouse.core/logs")
		*/
		
	}
	private void readWarehouseConfiguration() {
		warehouseconfiguration = readConfigurationFile("elasticwarehouse.yml");
		
	}
	public String getConfigurationPath()
	{
		String path = getHomePath()+"/config/";
		return path;
	}
	private Map<String, String> readConfigurationFile(String filename)
	{
		Map<String, String> result = null;
        //ClassLoader classLoader = Classes.getDefaultClassLoader();
        //InputStream is = classLoader.getResourceAsStream("elasticsearch.yml");
        //java.nio.file.Path resPath;
        String fileContent = null;
        
        String filePath = getFullPathForConfigurationFile(filename);
        
        
		try {
			fileContent = readFile(filePath, Charset.defaultCharset());
			LOGGER.info("Configuration read");
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
			LOGGER.error("Configuration read error:" + e.getMessage());
		}
			
		SettingsLoader loader = new YamlSettingsLoader();
		try {
			result = loader.load(fileContent);
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		}
		return result;
	}
	
	private String getFullPathForConfigurationFile(String filename)
	{
		String resultPath = null;
		
		//--------
		resultPath = checkPossibleConfigurationFilepath(getConfigurationPath()+"/"+filename);
		if( resultPath != null ) return resultPath;
		
		//--------
		if( esconfiguration != null && esconfiguration.containsKey("path.plugins"))
		{
			resultPath = checkPossibleConfigurationFilepath(esconfiguration.get("path.plugins")+"/config/"+filename);
			if( resultPath != null ) return resultPath;
			resultPath = checkPossibleConfigurationFilepath(esconfiguration.get("path.plugins")+"/elasticwarehouse/config/"+filename);
			if( resultPath != null ) return resultPath;
		}
		
		//--------
		if( esconfiguration != null && esconfiguration.containsKey("path.home"))
		{
			resultPath = checkPossibleConfigurationFilepath(esconfiguration.get("path.home")+"/config/"+filename);
			if( resultPath != null ) return resultPath;
			resultPath = checkPossibleConfigurationFilepath(esconfiguration.get("path.home")+"/config/elasticwarehouse/"+filename);
			if( resultPath != null ) return resultPath;
			resultPath = checkPossibleConfigurationFilepath(esconfiguration.get("path.home")+"/config/elasticwarehouseplugin/"+filename);
			if( resultPath != null ) return resultPath;
			resultPath = checkPossibleConfigurationFilepath(esconfiguration.get("path.home")+"/plugins/config/"+filename);
			if( resultPath != null ) return resultPath;
			resultPath = checkPossibleConfigurationFilepath(esconfiguration.get("path.home")+"/plugins/elasticwarehouse/config/"+filename);
			if( resultPath != null ) return resultPath;
			resultPath = checkPossibleConfigurationFilepath(esconfiguration.get("path.home")+"/plugins/elasticwarehouseplugin/config/"+filename);
			if( resultPath != null ) return resultPath;
			
		}
		
		//--------
		resultPath = checkPossibleConfigurationFilepath(getHomePath()+"/config/"+filename);
		if( resultPath != null ) return resultPath;
		resultPath = checkPossibleConfigurationFilepath(getHomePath()+"/config/elasticwarehouse/"+filename);
		if( resultPath != null ) return resultPath;
		resultPath = checkPossibleConfigurationFilepath(getHomePath()+"/config/elasticwarehouseplugin/"+filename);
		if( resultPath != null ) return resultPath;
		resultPath = checkPossibleConfigurationFilepath(getHomePath()+"/plugins/config/"+filename);
		if( resultPath != null ) return resultPath;
		resultPath = checkPossibleConfigurationFilepath(getHomePath()+"/plugins/elasticwarehouse/config/"+filename);
		if( resultPath != null ) return resultPath;
		resultPath = checkPossibleConfigurationFilepath(getHomePath()+"/plugins/elasticwarehouseplugin/config/"+filename);
		if( resultPath != null ) return resultPath;
		
		resultPath = checkPossibleConfigurationFilepath(getElasticHomePath()+"/config/"+filename);
		if( resultPath != null ) return resultPath;
		resultPath = checkPossibleConfigurationFilepath(getElasticHomePath()+"/config/elasticwarehouse/"+filename);
		if( resultPath != null ) return resultPath;
		resultPath = checkPossibleConfigurationFilepath(getElasticHomePath()+"/config/elasticwarehouseplugin/"+filename);
		if( resultPath != null ) return resultPath;
		resultPath = checkPossibleConfigurationFilepath(getElasticHomePath()+"/plugins/config/"+filename);
		if( resultPath != null ) return resultPath;
		resultPath = checkPossibleConfigurationFilepath(getElasticHomePath()+"/plugins/elasticwarehouse/config/"+filename);
		if( resultPath != null ) return resultPath;
		resultPath = checkPossibleConfigurationFilepath(getElasticHomePath()+"/plugins/elasticwarehouseplugin/config/"+filename);
		if( resultPath != null ) return resultPath;
        
        return filename;
	}

	private String checkPossibleConfigurationFilepath(String filepath) {
		if( FileTools.checkFileCanRead(filepath) )
        {
        	LOGGER.info("Reading configuration file : " + filepath);
        	return filepath;
        }
		return null;
	}

	public String getESValue(String key)
	{
		return esconfiguration.get(key);
	}
	public Map<String, String> getESConfiguration()
	{
		return esconfiguration;
	}
	public String getWarehouseValue(String key)
	{
		return warehouseconfiguration.get(key);
	}
	public boolean getWarehouseBoolValue(String key, boolean defaultValue)
	{
		Boolean ret = Boolean.parseBoolean(warehouseconfiguration.get(key));
		if( ret == null )
			return defaultValue;
		else
			return ret;
	}
	public int getWarehouseIntValue(String key, int defaultValue)
	{
		Integer ret = Integer.parseInt(warehouseconfiguration.get(key));
		if( ret == null )
			return defaultValue;
		else
			return ret;
	}
	private String readFile(String path, Charset encoding) 
			  throws IOException 
	{
	  byte[] encoded = Files.readAllBytes(Paths.get(path));
	  return new String(encoded, encoding);
	}
	
	public String getElasticHomePath()
	{
		String path = System.getProperty("es.path.home", System.getProperty("user.dir")); 
		return path;
	}
	
	public String getHomePath()
	{
		String path = System.getProperty("ew.path.home", System.getProperty("user.dir")); 
		//Path homeFile = Paths.get(System.getProperty("user.dir"));
		//Path configFile = homeFile.resolve("config");
		//return configFile.toString();
		return path;
		/*
		//return new File(ElasticWarehouseConf.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		String path = ElasticWarehouseConf.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String decodedPath = null;
		try {
			decodedPath = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		}
		return decodedPath;
		*/
	}
	public Map<String, String> getWarehouseConfiguration() {
		return warehouseconfiguration;
	}
	public boolean validate() {
		
		boolean storeContent = getWarehouseBoolValue(ElasticWarehouseConf.STORECONTENT, true);
     	//boolean storeMoveScanned = getWarehouseBoolValue(ElasticWarehouseConf.STOREMOVESCANNED, true);
     	String storeFolder = getWarehouseValue(ElasticWarehouseConf.STOREFOLDER);
     	
     	if(storeContent == false && (storeFolder == null || storeFolder.length()==0 || FileTools.folderWritable(storeFolder) == false) )
     	{
     		LOGGER.error("When you conigured storeContent=false, then you must provide writable folder path in "+ElasticWarehouseConf.STOREFOLDER+" attribute" );
     		return false;     		
     	}
		return true;
	}

	public Long getUpTime() {
		Long seconds = (Calendar.getInstance().getTime().getTime() - serviceStarted_.getTime())/1000;
		return seconds;
	}

	public String getNodeName()
	{
		if( warehouseconfiguration.containsKey(RRDHOSTNAME))
			return warehouseconfiguration.get(RRDHOSTNAME);
		if( esconfiguration.containsKey("node.name") )
			return esconfiguration.get("node.name");
		return NetworkTools.getHostName2().toLowerCase();
	}

	public String getTempFolder() {
		String tmp = getWarehouseValue(TMPPATH);
		if( tmp.length() == 0 )
			tmp ="/tmp";
		return tmp;
	}

	public void setWarehouseValue(String key, Integer value) {
		warehouseconfiguration.put(key, value.toString() );
	}
	public void setWarehouseValue(String key, Boolean value) {
		warehouseconfiguration.put(key, value.toString() );
	}
}
