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

import java.io.File;
import java.io.FileOutputStream;

import org.elasticsearch.common.base.Charsets;
import org.elasticsearch.common.io.FileSystemUtils;

import org.elasticsearch.monitor.jvm.JvmInfo;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticWarehouseConf;

//TODO future: option to backup deleted files to different location
//TODO future: ewinfo to return two versions: upload version + metadata version
//TODO future: private files (per user)
//TODO future: folder/files locking feature. Use it for folders renaming (i.e. lock every file/folder, then perform folder rename operation) 
//TODO future: log details about file changes (when version increases) + copy file in case of new upload
//TODO future: moving folders (_ewtask?action=move)
//TODO future: resume tasks interrupted due to EW crash etc.
//TODO future: notify graphite RRD files reader (PerformanceMonitor instance) about new RRD file created in result of expandRRDFile() method call 
//TODO future community: ewshell show MB bytes not bytes only (human readable switch)
//TODO future community: ewshell show stats (query, tt, parsing)

//import static org.elasticsearch.common.jna.Kernel32Library.ConsoleCtrlHandler;
//import static org.elasticsearch.common.settings.ImmutableSettings.Builder.EMPTY_SETTINGS;

public class App 
{
	//private final static Logger LOGGER = Logger.getLogger(App.class.getName());
	
	private static Elasticwarehouse warehouse_ = null;
	
	public static void close(String[] args) {
		System.out.println( "Hello! ElasticWarehouse is stopping...." );
        warehouse_.close(args);
    } 
    public static void main( String[] args )
    {
        System.out.println( "Hello! ElasticWarehouse is starting...." );
        final String pidFile = System.getProperty("es.pidfile", System.getProperty("es-pidfile"));

        if (pidFile != null) {
            try {
                File fPidFile = new File(pidFile);
                if (fPidFile.getParentFile() != null) {
                    FileSystemUtils.mkdirs(fPidFile.getParentFile());
                }
                FileOutputStream outputStream = new FileOutputStream(fPidFile);
                outputStream.write(Long.toString(JvmInfo.jvmInfo().pid()).getBytes(Charsets.UTF_8));
                outputStream.close();

                fPidFile.deleteOnExit();
            } catch (Exception e) {
            	EWLogger.logerror(e);
                String errorMessage = e.getMessage();
                System.err.println(errorMessage);
                System.err.flush();
                System.exit(3);
            }
        }
        
        ElasticWarehouseConf c = new ElasticWarehouseConf();
        
        /*Natives.addConsoleCtrlHandler(new ConsoleCtrlHandler() {
            @Override
            public boolean handle(int code) {
                if (CTRL_CLOSE_EVENT == code) {
                    ESLogger logger = Loggers.getLogger(App.class);
                    logger.info("running graceful exit on windows");

                    System.exit(0);
                    return true;
                }
                return false;
            }
        });*/
        
        if( c.validate() )
        {
        	try
        	{
        		warehouse_ = new Elasticwarehouse(c);
        	}catch(org.elasticsearch.ElasticsearchException e)
        	{
        		EWLogger.logerror(e);
        		e.printStackTrace();
        	}
        	System.out.println( "Done!" );
        }else{
        	System.out.println( "Please fix configuration files. Exitting...." );
        }
    }

	
}
