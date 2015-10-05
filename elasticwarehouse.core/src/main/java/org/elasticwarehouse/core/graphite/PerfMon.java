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
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.elasticwarehouse.core.AtrrValue;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.parsers.FileTools;

public abstract class PerfMon {

	protected ElasticWarehouseConf conf_;
	private final static Logger LOGGER = Logger.getLogger(PerfMon.class.getName());
	
	public PerfMon(ElasticWarehouseConf conf)
	{
		conf_ = conf;
	}
	
	public abstract LinkedList<TimeSample> fetchCountersToRender(String target /*i.e. host.cpu.loadavgsec */, String from, String until, 
    		String format, int minSamplesCount, int imaxDataPoints, boolean refreshBeforeFetch) throws IOException, ParseException, DataSourceNotFoundException, DDRFileNotFoundException;
    		
	public String getStorageFolder() {
		String folder = conf_.getWarehouseValue(ElasticWarehouseConf.RRDDBPATH);
    	if( folder == null)
		{
			//throw new ParseException("elasticwarehouse.yml configuration is wrong. Please provide entry: " + ElasticWarehouseConf.RRDDBPATH, 0);
			folder = conf_.getHomePath()+"/data/";
			LOGGER.info("rrd.db.path not provided in configuration, using data folder instead:"+folder);
		}
    	folder = folder+"/perf/"+ conf_.getNodeName()+"/";
    	FileTools.makeFolder(folder);
    	return folder;
	}
	
	protected void saveCounter(RRDManager rrdmanager, String type, LinkedList<AtrrValue> attval)
	{
		long t = System.currentTimeMillis()/1000;
		//attr = ddrManagers_.get(type).transformSourceName(attr);
		try {
			rrdmanager.addValue(t, attval /*attrs, factors*//*,dynamicallyAddSources*/);
			LOGGER.debug("-------- Stored data summary -------------");
			//int pos = 0;
			for(AtrrValue att : attval )
			{
				//Double factor = factors.get(pos);
				LOGGER.debug("Stored Double : ["+type+"][" + att.key_ + "] " + t + " => " + att.value_);
				//pos++;
			}
		} catch (IOException e) {
			LOGGER.error("Cannot store Doubles ["+attval+ "] at " + t +" in RRD database " + type + " : " +e.getMessage());
			EWLogger.logerror(e);
			e.printStackTrace();
		} catch (ParseException e) {
			LOGGER.error("Cannot saveCounter ["+attval+ "] at " + t +" in RRD database " + type + " : " +e.getMessage());
			EWLogger.logerror(e);
			e.printStackTrace();
		}
	}
}
