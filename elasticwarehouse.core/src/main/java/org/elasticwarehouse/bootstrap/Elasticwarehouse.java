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
package org.elasticwarehouse.bootstrap;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.ElasticWarehouseMonitoring;
import org.elasticwarehouse.core.ElasticWarehouseServerAPI;
import org.elasticwarehouse.core.ElasticWarehouseServerAPINotifier;
import org.elasticwarehouse.core.ElasticWarehouseServerGrafana;

public class Elasticwarehouse {

	ElasticWarehouseServerGrafana elasticWarehouseServerGrafana_ = new ElasticWarehouseServerGrafana();
	ElasticWarehouseServerAPI elasticWarehouseServerAPI_ = new ElasticWarehouseServerAPI();
	ElasticWarehouseMonitoring elasticWarehouseServerMonitoring_ = new ElasticWarehouseMonitoring();
	
	ElasticSearchAccessor elasticSearchAccessor_ = null;
	LinkedList<Thread> threads_ = new LinkedList<Thread>(); 
	
	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseServerAPI.class.getName());
	
	public Elasticwarehouse(final ElasticWarehouseConf c)// throws MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, NullPointerException, ReflectionException, IOException, ParseException
	{
		LOGGER.info("Starting ElasticSearchAccessor");
        elasticSearchAccessor_= new ElasticSearchAccessor(c, true);
        elasticSearchAccessor_.recreateTemplatesAndIndices(true);

        //final PerformanceMonitor monitor = new PerformanceMonitor(c);
		
        final ElasticWarehouseServerAPINotifier apiNotifier = new ElasticWarehouseServerAPINotifier();
        
        final String esHostPort = elasticSearchAccessor_.getHostPort(); 
        
		threads_.add(new Thread() {
		    public void run() {
		    	setName("grafana");
		        try {
		        	LOGGER.info("Starting ElasticWarehouseServer for Grafana");
		            Thread.sleep(100);
		            elasticWarehouseServerGrafana_.startServer(c, esHostPort, apiNotifier);	//blocking method
		            LOGGER.info("Stopping ElasticWarehouseServer for Grafana");
		        } catch(InterruptedException v) {
		        	EWLogger.logerror(v);
		        	LOGGER.error(v);
		        }
		    }  
		});
		
		
		
		threads_.add(new Thread() {
		    public void run() {
		    	setName("api");
		        try {
		        	LOGGER.info("Starting ElasticWarehouseServer for WarehouseAPI");
		            Thread.sleep(100);
		            try {
						elasticWarehouseServerAPI_.startServer(c, elasticSearchAccessor_ , apiNotifier);//blocking method
					} catch (IOException e) {
						EWLogger.logerror(e);
						e.printStackTrace();
					}	
		            LOGGER.info("Stopping ElasticWarehouseServer for WarehouseAPI");
		        } catch(InterruptedException v) {
		        	EWLogger.logerror(v);
		        	LOGGER.error(v);
		        }
		    }

			
		});
		
		boolean monitoringstarted = false;
		if( c.getWarehouseBoolValue(ElasticWarehouseConf.RRDENABLED, true) )
		{
			threads_.add(new Thread() {
			    public void run() {
			    	setName("monitoring");
			        try {
			            LOGGER.info("Starting ElasticWarehouseServer for Monitoring");
			            Thread.sleep(100);
			            elasticWarehouseServerMonitoring_.startMonitor(c,elasticSearchAccessor_);	//blocking method
			            LOGGER.info("Stopping ElasticWarehouseServer for Monitoring");
			        } catch(InterruptedException v) {
			        	EWLogger.logerror(v);
			        	LOGGER.error(v);
			        }
			    }  
			});
			monitoringstarted = true;
		}else{
			LOGGER.info("ElasticWarehouseServer for Monitoring is not starting : "+ ElasticWarehouseConf.RRDENABLED + " is set to false");
		}
		/*threads_.add(new Thread() {
		    public void run() {
		        try {
		            System.out.println("Starting ElasticSearchNode");
		            Thread.sleep(100);
		            elasticSearchAccessor_= new ElasticSearchAccessor(c);	//blocking constructor
		            System.out.println("Stopping ElasticSearchNode");
		        } catch(InterruptedException v) {
		            System.out.println(v);
		        }
		    }  
		});*/
		
		
		threads_.get(0).start();
		threads_.get(1).start();
		if( monitoringstarted )
			threads_.get(2).start();
		
		
	}

	public void close(String[] args) {
		elasticSearchAccessor_.close();
		for(Thread t : threads_)
		{
			t.interrupt();
		}
		for(Thread t : threads_)
		{
			try {
				t.wait();
			} catch (InterruptedException e) {
				LOGGER.error("Wait for interrupt failed:" + e.getMessage());
				EWLogger.logerror(e);
				e.printStackTrace();
			}
		}
	}
}
