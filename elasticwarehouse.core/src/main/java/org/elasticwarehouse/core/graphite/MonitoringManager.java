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

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;

import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;
//import org.apache.log4j.Logger;

public class MonitoringManager {

	static LinkedList<ElasticSearchMonitor> esmonitors = new LinkedList<ElasticSearchMonitor>();
	//private final static Logger LOGGER = Logger.getLogger(MonitoringManager.class.getName());
	
	public static ElasticSearchMonitor createElasticSearchMonitor(ElasticWarehouseConf conf, boolean b, ElasticSearchAccessor elasticSearchAccessor) throws MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, NullPointerException, ReflectionException, IOException, ParseException
	{
		ElasticSearchMonitor mon = new ElasticSearchMonitor(conf, b, elasticSearchAccessor);
		esmonitors.add(mon);
		return mon;
	}
	
	public static void closeFilesInElasticSearchMonitors() throws IOException
	{
		/*for(ElasticSearchMonitor mon : esmonitors)
		{
			LOGGER.info("Closing RRD....");
			mon.rrdmanager_.Dispose();
			LOGGER.info("Closed RRD");
		}*/
	}
	public static void reopenFilesInElasticSearchMonitors() throws IOException
	{
		/*for(ElasticSearchMonitor mon : esmonitors)
		{
			LOGGER.info("Opening RRD....");
			mon.rrdmanager_.reopenfile();
			LOGGER.info("Opened RRD");
		}*/
	}
}
