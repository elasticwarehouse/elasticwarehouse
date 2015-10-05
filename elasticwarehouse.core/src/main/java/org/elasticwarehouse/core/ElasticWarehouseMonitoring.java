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
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

import org.apache.log4j.Logger;
import org.elasticwarehouse.core.graphite.DDRFileNotFoundException;
import org.elasticwarehouse.core.graphite.ElasticSearchMonitor;
import org.elasticwarehouse.core.graphite.MonitoringManager;
import org.elasticwarehouse.core.graphite.PerformanceMonitor;




//import org.rrd4j.code.*;
//import static org.rrd4j.DsType.*;
//import static org.rrd4j.ConsolFun.*;


public class ElasticWarehouseMonitoring
{
	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseMonitoring.class.getName());
	private ElasticWarehouseServerMonitoringNotifier monitoringNotifier_; 
	
	
	public void startMonitor(ElasticWarehouseConf conf, ElasticSearchAccessor elasticSearchAccessor, ElasticWarehouseServerMonitoringNotifier monitoringNotifier)
	{
		monitoringNotifier_ = monitoringNotifier;
		PerformanceMonitor monitor = null;
		ElasticSearchMonitor esmonitor = null;
		try {
			monitor = new PerformanceMonitor(conf, false);
			esmonitor = MonitoringManager.createElasticSearchMonitor(conf, false, elasticSearchAccessor);
			monitoringNotifier_.notifyListeners(ElasticWarehouseServerMonitoringNotifier.API_MONITORING_INITIALIZED);
		} catch (MalformedObjectNameException e1) {
			EWLogger.logerror(e1);
			e1.printStackTrace();
		} catch (IntrospectionException e1) {
			EWLogger.logerror(e1);
			e1.printStackTrace();
		} catch (InstanceNotFoundException e1) {
			EWLogger.logerror(e1);
			e1.printStackTrace();
		} catch (NullPointerException e1) {
			EWLogger.logerror(e1);
			e1.printStackTrace();
		} catch (ReflectionException e1) {
			EWLogger.logerror(e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			EWLogger.logerror(e1);
			e1.printStackTrace();
		} catch (ParseException e1) {
			EWLogger.logerror(e1);
			e1.printStackTrace();
		}
		
		if( monitor == null)
			return;
		
		LinkedList<String> types = monitor.getAvailableTypes();
		for(;;)
		{
			try 
			{
				long t1 = System.currentTimeMillis();
				LOGGER.info("Performance collector started...");
				for(String type : types)
				{
					try 
					{
						LinkedList<String> availableattrs = monitor.getAvailableAttributes(type, false);
						LinkedList<AtrrValue> attval = new LinkedList<AtrrValue>();
						
						//LinkedList<Double> values = new LinkedList<Double>(); 
						for(String attr : availableattrs)
						{
							if( attr.equals("ObjectName") )
							{
								attval.add(new AtrrValue(attr, Double.NaN));
								//values.add(Double.NaN);
							}
							else
							{
//								if( attr.equals("ProcessCpuLoad") )
//								{
//									int aa=1;//breakpoint
//								}
								
								Object factor = monitor.getPerformanceCounter(type, attr);
								if( factor instanceof javax.management.openmbean.CompositeData[] )
								{
									CompositeData[] cds = (CompositeData[]) factor;
									int pos = 0;
									for(CompositeData cd : cds)
									{
										processCompositeData(cd, attval, monitor, attr+"_"+pos);
										pos++;
									}
								}
								else if( factor instanceof javax.management.openmbean.CompositeData )
								{
									/*String ss = type+"."+attr;
									if( ss.equals("operatingsystem.ProcessCpuLoad") || ss.equals("memory.HeapMemoryUsage") )
									{
										int aa =1;
										System.out.println(ss + " : "+factor);
									}*/
									
									CompositeData cd = (CompositeData) factor;
									processCompositeData(cd, attval, monitor, attr);
																		
								}else{
									Double v = monitor.convertPerformanceCounterToDouble(factor, false);
									if( v != Double.NaN )
										attval.add(new AtrrValue(attr, v));
									else
										attval.add(new AtrrValue(attr, Double.NaN));
								}
								
							}
						}
						
						monitor.saveCounter(type, attval);
					} catch (MalformedObjectNameException e) {
						EWLogger.logerror(e);
						e.printStackTrace();
					} catch (IntrospectionException e) {
						EWLogger.logerror(e);
						e.printStackTrace();
					} catch (InstanceNotFoundException e) {
						EWLogger.logerror(e);
						e.printStackTrace();
					} catch (NullPointerException e) {
						EWLogger.logerror(e);
						e.printStackTrace();
					} catch (ReflectionException e) {
						EWLogger.logerror(e);
						e.printStackTrace();
					}
				}
				
				try 
				{
					LinkedList<String> estypes = esmonitor.getAvailableTypes();
					for(String estype: estypes)
					{
						LinkedList<AtrrValue> attval = new LinkedList<AtrrValue>();
						esmonitor.fetchCustomPerformanceCounters(estype, attval);
						if( esmonitor.IsPerformanceCounterExist(estype) )
							esmonitor.saveCustomPerformanceCounter(estype, attval);
						else
							LOGGER.debug("Found file for: " +estype+", but performance counters are not available.");
					}
				} catch (NullPointerException e) {
					LOGGER.error(e.getMessage());
					EWLogger.logerror(e);
					e.printStackTrace();
				} catch (IOException e) {
					LOGGER.error(e.getMessage());
					EWLogger.logerror(e);
					e.printStackTrace();
				} catch (ParseException e) {
					LOGGER.error(e.getMessage());
					EWLogger.logerror(e);
					e.printStackTrace();
				} catch (DDRFileNotFoundException e) {
					LOGGER.error(e.getMessage());
					EWLogger.logerror(e);
					e.printStackTrace();
				}
			
				
				LOGGER.info("Performance collector stopped : " + ((System.currentTimeMillis()-t1)/1000.0) + " s.");
				Thread.sleep(1000*10);	//wait 10 seconds
				monitoringNotifier_.notifyListeners(ElasticWarehouseServerMonitoringNotifier.API_MONITORING_INITIALIZED);
				Thread.sleep(1000*50);	//wait 50 seconds
				//Thread.sleep(5000);
				
			} catch (InterruptedException e) {
				EWLogger.logerror(e);
				e.printStackTrace();
			}
		}
	}


	private void processCompositeData(CompositeData cd,LinkedList<AtrrValue> attval, PerformanceMonitor monitor, String attr)
	{
		Set< String > keys = cd.getCompositeType().keySet();
		for(String key : keys)
		{
			Object subfactor = cd.get(key);
			Double v = monitor.convertPerformanceCounterToDouble(subfactor, false);

			if( v != Double.NaN )
				attval.add(new AtrrValue(attr+"_"+key, v));
			else
				attval.add(new AtrrValue(attr+"_"+key, Double.NaN));
		}		
	}
}
