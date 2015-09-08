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

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.ConsolFun.MAX;
import static org.rrd4j.ConsolFun.TOTAL;
import static org.rrd4j.DsType.GAUGE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.elasticwarehouse.core.AtrrValue;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.parsers.FileTools;
import org.elasticwarehouse.core.parsers.ParseTools;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdToolkit;
import org.rrd4j.core.Sample;


public class RRDManager
{
	String rrdPath_= null;
	List<String> sources_ = new LinkedList<String>();
	private final static Logger LOGGER = Logger.getLogger(RRDManager.class.getName());
	public static final int MAXDATASOURCENAMELEN = 20; 
	private RrdDb rrdDb_ = null;
	boolean readOnly_ = false;
	
	private HashMap<String, String> renameDictionary = new HashMap<String, String>();	//sometimes when we cut to 20 chars attributes can have the same names
	private String tmpFolder_ = "/tmp";
	private String rrDbBaseFilename_ = "";
	
	public RRDManager(String rrdPath, LinkedList<String> sources, boolean cutSourcesto20chars, boolean readOnly, boolean waittoOpen, String tmpFolder) throws IOException, ParseException, DDRFileNotFoundException
	{
		tmpFolder_  = tmpFolder;
		initMe(rrdPath, sources, cutSourcesto20chars, readOnly, waittoOpen);
	}
	public RRDManager(String rrdPath, String[] sources, boolean cutSourcesto20chars, boolean readOnly, boolean waittoOpen, String tmpFolder) throws IOException, ParseException, DDRFileNotFoundException
	{
		tmpFolder_ = tmpFolder;
		initMe(rrdPath, Arrays.asList(sources), cutSourcesto20chars, readOnly, waittoOpen);
	}
	
	public boolean doesDbFileExist()
	{
		return FileTools.checkFileCanRead(this.rrdPath_);
	}
	public void initMe(String rrdPath, List<String> sources, boolean cutSourcesto20chars, boolean readOnly, boolean waittoOpen) throws ParseException, IOException, DDRFileNotFoundException
	{
		initDictionary();
		this.rrdPath_ = rrdPath;
		this.readOnly_ = readOnly;
		
		initDataSources(sources, cutSourcesto20chars);
		
		File f = new File(this.rrdPath_);
		boolean opened = false;
		int attempt=0;
		int attempt_max=5;
		while(opened==false && waittoOpen)
		{
			if(!f.exists() && !f.isDirectory())
			{
				if( readOnly )
				{
					if( attempt >= attempt_max )
					{
						LOGGER.info("Giving up after "+attempt+" attempts. Path: " + this.rrdPath_);
						break;
					}
					LOGGER.info("Waiting for RRD database to be created.......: " + this.rrdPath_);
					attempt++;
					try{
						Thread.sleep(1000); 
					} catch(InterruptedException v) {
						EWLogger.logerror(v);
						System.out.println(v);
					}
				}else{
					LOGGER.info("Creating rrd database for performance counters: " + this.rrdPath_);
					opened = createRRDFiles();
				}
			}else{
				opened = true;
			}
		}
		if( opened )
			reopenfile();
	}
	private void initDataSources(List<String> sources, boolean cutSourcesto20chars) throws ParseException
	{
		for(String source : sources)
		{
			if( source.length() >MAXDATASOURCENAMELEN && cutSourcesto20chars == false)
				throw new ParseException("Datasource name ["+source+"] too long ("+source.length()+" chars found, only "+MAXDATASOURCENAMELEN+" allowed", 0);
			
			String transformedsource = transformSourceName(source);
			LOGGER.debug("source=" + source +", transformedsource="+transformedsource);
			this.sources_.add(transformedsource);
		}
	}
	public String transformSourceName(String source)
	{
		String transformedsource = "";
		if( renameDictionary.containsKey(source))
			transformedsource = renameDictionary.get(source);
		else
			transformedsource = source;
		
		if( transformedsource.startsWith("DiagnosticOptions"))
			transformedsource="DiagOpt" + transformedsource.substring("DiagnosticOptions".length());
		if( transformedsource.length() >MAXDATASOURCENAMELEN )
			transformedsource = transformedsource.substring(0,MAXDATASOURCENAMELEN);
		
		return transformedsource;
	}
	public void Dispose() throws IOException
	{
		if( rrdDb_ != null )
			rrdDb_.close();
	}
	private void initDictionary()
	{
		renameDictionary.put("ThreadContentionMonitoringEnabled",  	"ThrdContMonitEnabled");
		renameDictionary.put("ThreadContentionMonitoringSupported",	"ThrdContMonitSupprtd" );
		renameDictionary.put("ThreadAllocatedMemoryEnabled",  		"ThrdAllocMemEnabled" );
		renameDictionary.put("ThreadAllocatedMemorySupported",  	"ThrdAllocMemSupprtd" );
		
		renameDictionary.put("CurrentThreadCpuTimeSupported",	   "CurntThrdCpuTSupprtd" );
		
		renameDictionary.put("CollectionUsage",	   				   "CollectionUsage" );
		renameDictionary.put("CollectionUsageThreshold",	   	   "CollectionUsgThld" );
		renameDictionary.put("CollectionUsageThresholdCount",	   "CollectionUsgThldCnt" );
		renameDictionary.put("CollectionUsageThresholdExceeded",   "CollectionUsgThldExc" );
		renameDictionary.put("CollectionUsageThresholdSupported",  "CollectionUsgThldSup" );
		
		renameDictionary.put("LastGcInfo_memoryUsageAfterGc",      "LstGcInfmemUsAfterGc" );
		renameDictionary.put("LastGcInfo_memoryUsageBeforeGc",     "LstGcInfmemUsBeforGc" );
		
		//renameDictionary.put("DiagnosticOptions_origin",     "LstGcInfmemUsBeforGc" );
		//renameDictionary.put("DiagnosticOptions_value",     "LstGcInfmemUsBeforGc" );
		//renameDictionary.put("DiagnosticOptions_writeable",     "LstGcInfmemUsBeforGc" );
		//DiagnosticOptions_origin, transformedsource=DiagnosticOptions_or
		//DiagnosticOptions_value, transformedsource=DiagnosticOptions_va
		//DiagnosticOptions_writeable, transformedsource=DiagnosticOptions_wr
				
		
		
		renameDictionary.put("TotalPhysicalMemorySize",     "TotalPhysicalMemory" );	//for compatibility with Windows (Window shas TotalPhysicalMemory, linux: TotalPhysicalMemorySize
		
	}
	public boolean expandRRDFile(LinkedList<String> customattributes) throws IOException, ParseException
	{
		//sources_ = customattributes;
		//RrdDef def = rrdDb_.getRrdDef();
		LinkedList<DsDef> newdefs = new LinkedList<DsDef>();
		String[] datasources = rrdDb_.getDsNames();
		initDataSources(customattributes, true);
		for(String sourceName : sources_)
        {
			boolean found = false;
			for(String currentsourceName : datasources)
	        {
				if( currentsourceName.equals(sourceName) )
				{
					found = true;
					break;
				}
	        }
			if( !found )
			{
				LOGGER.info("Adding: "+sourceName+" in " + this.rrdPath_);
				//def.addDatasource(sourceName, GAUGE, 600, 0, Double.NaN);
				newdefs.add(new DsDef(sourceName, GAUGE, 600, 0, Double.NaN));
			}
        }
		if( !newdefs.isEmpty() )
		{
			if( rrdDb_ != null )
			{
				rrdDb_.close();
				rrdDb_ = null;
			}
			
			MonitoringManager.closeFilesInElasticSearchMonitors();
			
			String tmpFilename = tmpFolder_+"/"+FilenameUtils.getBaseName(rrdPath_)+".rrd.tmp";
			String tmpFilenameCopy = tmpFolder_+"/"+FilenameUtils.getBaseName(rrdPath_)+".rrd.tmpcopy";
			
			Files.deleteIfExists(new File(tmpFilename).toPath());
			Files.deleteIfExists(new File(tmpFilenameCopy).toPath());
			FileTools.copy(rrdPath_, tmpFilenameCopy);
			int attemp=0;
			for(;;)
			{
				if( attemp == 5)
					break;
				try{
					attemp++;
					Files.deleteIfExists(new File(rrdPath_).toPath());
					break;
				}catch(java.nio.file.FileSystemException e)
				{
					LOGGER.info("Got java.nio.file.FileSystemException, waiting...." + e.getMessage() );
				}
				try
				{
					Thread.sleep(2300);
				}catch(InterruptedException e)
				{
					EWLogger.logerror(e);
				}
			}
			
			if( attemp < 5) 
			{
				RrdToolkit.addDatasources(tmpFilenameCopy, tmpFilename , newdefs);
				FileTools.copy(tmpFilename, rrdPath_);
				MonitoringManager.reopenFilesInElasticSearchMonitors();
				Files.deleteIfExists(new File(tmpFilename).toPath());
				Files.deleteIfExists(new File(tmpFilenameCopy).toPath());
			}
		}
		return true;
	}
	private boolean createRRDFiles() throws IOException
	{
        // creation
		java.util.Date date= new java.util.Date();
		LOGGER.debug("== Creating RRD file " + rrdPath_);
        RrdDef rrdDef = new RrdDef(rrdPath_, (date.getTime()/1000) - 1, 60 /* 60s step */);
        rrdDef.setVersion(2);
        for(String sourceName : sources_)
        {
        	String nn = new File(this.rrdPath_).getName();
			LOGGER.info("Adding: "+sourceName+" in " + nn);
        	rrdDef.addDatasource(sourceName, GAUGE, 600, 0, Double.NaN);
        }
        rrdDef.addArchive(AVERAGE, 0.5, 1, 288);
        rrdDef.addArchive(AVERAGE, 0.5, 3, 672);
        rrdDef.addArchive(AVERAGE, 0.5, 12, 744);
        rrdDef.addArchive(AVERAGE, 0.5, 72, 1460);
        
        rrdDef.addArchive(TOTAL, 0.5, 1, 288);
        rrdDef.addArchive(TOTAL, 0.5, 3, 672);
        rrdDef.addArchive(TOTAL, 0.5, 12, 744);
        rrdDef.addArchive(TOTAL, 0.5, 72, 1460);
        
        rrdDef.addArchive(MAX, 0.5, 1, 288);
        rrdDef.addArchive(MAX, 0.5, 3, 672);
        rrdDef.addArchive(MAX, 0.5, 12, 744);
        rrdDef.addArchive(MAX, 0.5, 72, 1460);
        
        return createRRDFiles(rrdDef);
	}
	
    private boolean createRRDFiles(RrdDef rrdDef) throws IOException
    {
        LOGGER.debug(rrdDef.dump());
        LOGGER.debug("Estimated file size: " + rrdDef.getEstimatedSize());
        RrdDb rrdDb = new RrdDb(rrdDef);
        LOGGER.debug("== RRD file created.");
        if (rrdDb.getRrdDef().equals(rrdDef)) {
        	LOGGER.debug("Checking RRD file structure... OK");
        } else {
        	LOGGER.debug("Invalid RRD file created. This is a serious bug, bailing out");
            return false;
        }
        rrdDb.close();
        LOGGER.debug("== RRD file closed.");
        return true;
	}
	
	public boolean sourceExists(String datasource) throws IOException
	{
		if( rrdDb_ == null )
			throw new IOException("rrdDb is null"); 
		boolean found = false;
		String[] datasources = rrdDb_.getDsNames();
		for(String dsname : datasources)
		{
			if( dsname.equals(datasource))
			{
				found = true;
				break;
			}
		}
		return found;
	}
	public boolean addValue(long t, LinkedList<AtrrValue> attval /*LinkedList<String> attrs, LinkedList<Double> factors*/) throws IOException
	{			
        Sample sample = rrdDb_.createSample();
        sample.setTime(t);
        //int pos =0;
        for(AtrrValue att : attval )
        {
        	//Double factor = factors.get(pos);
        	String attrkey = transformSourceName(att.key_);
        	if( att.value_ != Double.NaN )
			{
				try{
					sample.setValue(attrkey, att.value_);
				}catch(java.lang.IllegalArgumentException e) {
					EWLogger.logerror(e);
					LOGGER.error("Cannot save performance counter in "+rrDbBaseFilename_+": "+attrkey+" -> "+att.value_+". "+e.getMessage() );
				}
        	}
        }
        
        //logger.debug(sample.dump());
        try
        {
        	sample.update();
        	return true;
        }catch(IllegalArgumentException e)
        {
        	EWLogger.logerror(e);
        	e.printStackTrace();
        	return false;
        }
	}
	
	public boolean addValue(long t, String source, Long value) throws IOException
	{
        Sample sample = rrdDb_.createSample();
        sample.setTime(t);
        sample.setValue(source, value);
        //logger.debug(sample.dump());
        try
        {
        	sample.update();
        	return true;
        }catch(IllegalArgumentException e)
        {
        	EWLogger.logerror(e);
        	e.printStackTrace();
        	return false;
        }
	}
	
	public void reopenfile() throws IOException, DDRFileNotFoundException
	{
		if( this.readOnly_ && FileTools.checkFileCanRead(this.rrdPath_)==false)
			throw new DDRFileNotFoundException("Database file "+this.rrdPath_+" doesn't exist or cannot be read");
		
		if( rrdDb_ != null )
		{
			rrdDb_.close();
			rrdDb_ = null;
		}
		rrdDb_ = new RrdDb(this.rrdPath_, this.readOnly_);
		rrDbBaseFilename_  = FilenameUtils.getBaseName(rrdPath_);
	}
	public void refresh() throws DDRFileNotFoundException
	{
		if( rrdDb_ == null )
			return;
		
		try {
			//RrdDef rrdDef = rrdDb_.getRrdDef();
			rrdDb_.close();
			reopenfile();
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		}
	}
	// target=NYCVFENTITLE1.cpu.loadavgsec&from=-6h&until=now&format=json&maxDataPoints=640 
	public LinkedList< TimeSample > exportGraphite(String metric, String from, String until, String format, int maxDataPoints) throws IOException, DataSourceNotFoundException, DDRFileNotFoundException
	{
		
		LinkedList< TimeSample > ret = new LinkedList<TimeSample>();
		
		long start = ParseTools.convertToTime(from);
		long end = ParseTools.convertToTime(until);
		if( !format.equals("json") )
			return null;
		
		metric = transformSourceName(metric);
		if( metric.equals("select metric") )
			return ret;
		
		if( !sources_.contains(metric) )
		{
			throw new DataSourceNotFoundException("["+rrDbBaseFilename_+"] data source '" + metric + "' is not available. Available sources:" + sources_.toString());
		}
		if( rrdDb_ ==  null )
		{
			reopenfile();
			if( rrdDb_ == null )
				return ret;
		}
        // fetch data
        FetchRequest request = rrdDb_.createFetchRequest(ConsolFun.AVERAGE, start, end);
        FetchData fetchData = request.fetchData();
        double[] values = fetchData.getValues(metric);
        long[] timestamps = fetchData.getTimestamps();
        LOGGER.debug("== Data fetched. " + fetchData.getRowCount() + " points obtained");
        LOGGER.debug(fetchData.toString());
        
        for(int i=0;i<fetchData.getRowCount();i++)
        {
        	TimeSample ts = new TimeSample(timestamps[i] , values[i]);
        	ret.add(ts);
        	//System.out.println(timestamps[i] + " => " + values[i]);
        }
		return ret;
	}
	public LinkedList<String> getSources() throws IOException {
		LinkedList<String>  ret = new LinkedList<String>();
		String[] datasources = rrdDb_.getDsNames();
		for(String dsname : datasources)
			ret.add(dsname);
		return ret;
	}
	
	
}
