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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.IndexingResponse;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFileParser;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFileParserFactory;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFolder;
import org.elasticwarehouse.core.parsers.FileDef;
import org.elasticwarehouse.core.parsers.FileTools;
import org.elasticwarehouse.core.parsers.ParseTools;

public class ElasticWarehouseTaskScan extends ElasticWarehouseTask {

	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseTaskScan.class.getName());
	
	Thread th_ = null;
	String path_ = null;
	String targetfolder_ = null;
	private boolean recurrence_;
	ElasticWarehouseConf conf_ = null;
	ArrayList<String> processingErrors_ = new ArrayList<String>();
	int scannedFiles_ = 0;
	
	private boolean keepalive_ = false;
	private Date newerthan_ = null;
	
	public ElasticWarehouseTaskScan(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, String path, String targetfolder/*nullable*/, 
			boolean brecurrence, boolean keepalive, Date newerthan) {
		super(acccessor, conf);
		path_ = path;
		conf_ = conf;
		targetfolder_ = targetfolder;
		recurrence_ = brecurrence;
		keepalive_ = keepalive;
		newerthan_ = newerthan;
	}
	
	public ElasticWarehouseTaskScan(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, Map<String, Object> source)
	{
		super(acccessor, source, conf);
		path_ = source.get("path").toString();
		if(source.get("targetfolder") != null )
			targetfolder_ = source.get("targetfolder").toString();
		recurrence_ = Boolean.parseBoolean(source.get("recurrence").toString());
		scannedFiles_ = Integer.parseInt(source.get("scannedfiles").toString());
		if( source.get("processingerrors") != null )
			processingErrors_ = (ArrayList<String>)source.get("processingerrors");
		keepalive_ = Boolean.parseBoolean(source.get("keepalive").toString());
		if( source.get("newerthan") != null )
			newerthan_ = ParseTools.isDate(source.get("newerthan").toString());
		
		conf_ = conf;
	}

	@Override
	public boolean isAsync()
	{
		return true;
	}
	
	@Override
	public String getActionString() {
		return "scan";
	}

	@Override
	public XContentBuilder vgetJsonSourceBuilder(XContentBuilder builder) throws IOException
	{
		builder.field("path", path_)
		       .field("targetfolder", targetfolder_)
		       .field("recurrence", recurrence_)
		       .field("keepalive", keepalive_)
		       .field("newerthan", (newerthan_==null?newerthan_:df.format(newerthan_)))
		       .field("scannedfiles", scannedFiles_);
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
		    	
		    	ScanResult ret = null;
		    	for(;;)
		    	{
			        LOGGER.info("Starting scan " + path_ + "["+ taskId_ +"] to "+ targetfolder_);
					ret = scanFolder(recurrence_, newerthan_);
					
					if( ret.rc )
					{
						LOGGER.info("OK, scan " + path_ + " finished with code:" + errorcode_ +" ("+comment_+")");
					}else{
						LOGGER.error("ERROR, scan " + path_ + " finished with code:" + errorcode_+" ("+comment_+")");
						
					}
					if( keepalive_ )
					{
						if( ret.lastModifyDate > 0 )
							newerthan_ = new Date(ret.lastModifyDate);	//lastModifyDate returned by scanFolder is a number of milliseconds, so there is no need to multiply it by 1000
						progress_ = 99;
						indexTask();
						try {
							int slp = conf_.getWarehouseIntValue(ElasticWarehouseConf.ES_KEEP_ALIVE_SLEEP, 30000);
							Thread.sleep(slp);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}else{
						break;
					}
		    	}
				setFinished();
				if( ret.rc )
					progress_ = 100;
				else
					errorcode_ = ERROR_TASK_SCAN_OTHER_EXCEPTION;
					
				indexTask();
		    }  
		};
		th_.start();
	}

	class ScanResult
	{
		public boolean rc;
		public long lastModifyDate;
	}
	private ScanResult scanFolder(boolean isrecurrence, Date newerthan)
	{	 
			//ElasticSearchAccessor tmpAccessor = new ElasticSearchAccessor(c);
        	//String path = "/home/streamsadmin/workspaceE/elasticwarehouse.core/src/test/resources/";
        	//LinkedList<String> files = new LinkedList<String>( Arrays.asList(/*"63279218.pdf", 
        	//																"EntitlementsAnalysis_Apr2013.xlsx",
        	//																"RE Embargoed Research Views GoNo Go.msg",
        	//																"Res2.jpg",
        	//																"UsageDataToES.vsd",*/
        	//																"ResearchStatistics.doc"));
			ScanResult scanresult = new ScanResult();
			scanresult.lastModifyDate = 0;
			scanresult.rc = false;
			String processingFilename_ = "";
			try
			{
				if( targetfolder_ != null )
				{
					ElasticWarehouseFolder fldr = new ElasticWarehouseFolder(targetfolder_,conf_);
					acccessor_.indexFolder(fldr);
				}
				
				String extlist = conf_.getWarehouseValue(ElasticWarehouseConf.EXCLUDE_FILES_LIST);
				List<String> excluded_extensions = Arrays.asList( extlist.split(" ") );
				
				LinkedList<FileDef> files = FileTools.scanFolder(path_, excluded_extensions, isrecurrence, newerthan);
				scannedFiles_ += files.size();
				
				for(FileDef file : files)
				{
					if( newerthan != null )
						scanresult.lastModifyDate = Math.max(scanresult.lastModifyDate, file.lastModified_);
					System.out.println(file.folder_ +"   ->   "+ file.fname_);
				}
	        	//Integer atid=1;
	        	int counter = 0;
	        	//int modulofactor = scannedFiles_ / 5;
	        	//if( modulofactor <10 )
	        	//	modulofactor = 10;
	        	
	        	for(FileDef file : files)
	        	{
	        		if( interrupt_ )
	        			return scanresult;//false;
	        		
	        		processingFilename_ = file.fname_;
	        		IndexingResponse ret = null;
	        		if( targetfolder_ != null && targetfolder_.length() > 0 )
	        			ret = acccessor_.uploadFile(path_, file.fname_ , targetfolder_, null, "scan");
	        		else
	        		{
	        			ElasticWarehouseFolder fldr = new ElasticWarehouseFolder(file.folder_, conf_);
						acccessor_.indexFolder(fldr);
						
						ret = acccessor_.uploadFile(file.folder_, file.fname_ , file.folder_, null, "scan");
	        		}
	        		
	        		if( ret == null )
	        		{
	        			processingErrors_.add("Cannot parse "+file.folder_ +"/"+ file.fname_ +" due to unexpected error");
	        			return scanresult;//false;
	        		}
	        		else if( ret.id_ == null )
	        		{
	        			comment_ = ret.error_;
	        			processingErrors_.add(processingFilename_ + " : " + ret.error_);
	        			return scanresult;//false;
	        		}
	        		int ss = files.size();
	        		float pp = (float)counter/ss;
	        		progress_ = (int) ((float) pp*100.0);
	        		counter++;
	        		if( (counter%20) == 0 && counter >0 )
	        			indexTask();
	        	}
	        	scanresult.rc = true;
	        	return scanresult;//true;
			} catch( java.security.AccessControlException e) {
				EWLogger.logerror(e);
				e.printStackTrace();
				processingErrors_.add(processingFilename_ + " : " + e.getMessage() + 
						". To fix it: 1) Please check read access to provided location 2) edit <jre location>/lib/security/java.policy to allow web application access a folder outside its deployment directory by adding line: permission java.io.FilePermission \"/path/-\", \"read\";");
			} catch (IOException e) {
				EWLogger.logerror(e);
				e.printStackTrace();
				processingErrors_.add(processingFilename_ + " : " + e.getMessage());
			}
			
			return scanresult;//false;
	}
	
	
}
