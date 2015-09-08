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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
		
		 //************** TESTS ***************************
        //System.out.println("Value: " + c.getValue("cluster.name") );
        
        //************** TESTS ***************************
        //String path = "/home/streamsadmin/workspaceE/elasticwarehouse.core/src/test/resources/";
        //scanFolder(c , path);
       /*
        try {
			runRRDTest();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
        
        //ElasticSearchAccessor tmpAccessor = new ElasticSearchAccessor(c);
        //uploadFile(tmpAccessor, path, "testJPEG_GEO_2.jpg");
        
        /*LinkedList<DsDef> newdefs = new LinkedList<DsDef>();
        newdefs.add(new DsDef("samplefake", GAUGE, 600, 0, Double.NaN));
        try {
			RrdToolkit.addDatasources("/home/user/elasticsearch.rrd", 
					"/home/user/elasticsearch2.rrd", newdefs);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
		/*
	private static void runRRDTest() throws IOException, InterruptedException
	{
		RrdDef rrdDef = new RrdDef("/tmp/test.rrd", 300);
		rrdDef.addArchive(AVERAGE, 0.5, 1, 600); // 1 step, 600 rows
		rrdDef.addArchive(AVERAGE, 0.5, 6, 700); // 6 steps, 700 rows
		rrdDef.addArchive(MAX, 0.5, 1, 600);

		rrdDef.addDatasource("inbytes", GAUGE, 600, 0, Double.NaN);
		rrdDef.addDatasource("outbytes", GAUGE, 600, 0, Double.NaN);
		
		// then, create a RrdDb from the definition and start adding data
		RrdDb rrdDb = new RrdDb(rrdDef);
		Sample sample = rrdDb.createSample();

		    sample.setTime(System.currentTimeMillis());
		    sample.setValue("inbytes", 123);
		    sample.setValue("outbytes", 456);
		    sample.update();
		    Thread.sleep(1300);
		    
		    rrdDef.addDatasource("bytes", GAUGE, 600, 0, Double.NaN);
		    rrdDb = new RrdDb(rrdDef);
		    sample = rrdDb.createSample();
		    
		    sample.setTime(System.currentTimeMillis());
		    sample.setValue("bytes", 123);
		    sample.update();
		    
		rrdDb.close();
		
	}
*/


/*PerformanceMonitor monitor;
		try {
			monitor = new PerformanceMonitor(c);
		} catch (MalformedObjectNameException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IntrospectionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InstanceNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NullPointerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ReflectionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
        /*try {
			monitor.getProcessCpuLoad();
		} catch (MalformedObjectNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstanceNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ReflectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
}
