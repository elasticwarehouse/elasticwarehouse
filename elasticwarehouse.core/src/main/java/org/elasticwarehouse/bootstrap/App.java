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
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.util.Constants;
import org.elasticsearch.monitor.jvm.JvmInfo;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticWarehouseConf;


//TODO JVMCheck.check();
//TODO [2015-12-16 22:45:06,515]  WARN [elasticsearch[ptapdev][[ttl_expire]]] (IndicesTTLService.java:143) - [ptapdev] failed to execute ttl purge
//java.lang.NullPointerException
//at org.elasticsearch.indices.ttl.IndicesTTLService.purgeShards(IndicesTTLService.java:199)
//at org.elasticsearch.indices.ttl.IndicesTTLService.access$000(IndicesTTLService.java:67)
//at org.elasticsearch.indices.ttl.IndicesTTLService$PurgerThread.run(IndicesTTLService.java:140)
//[2015-12-16 22:45:36,669]  INFO [monitoring] (ElasticWarehouseMonitoring.java:94) - Performance collector sta

//TODO future: option to backup deleted files to different location
//TODO future: ewinfo to return two versions: upload version + metadata version
//TODO future: private files (per user)
//TODO future: folder/files locking feature. Use it for folders renaming (i.e. lock every file/folder, then perform folder rename operation) 
//TODO future: log details about file changes (when version increases) + copy file in case of new upload
//TODO future: moving folders (_ewtask?action=move)
//TODO future: resume tasks interrupted due to EW crash etc.
//TODO future: notify graphite RRD files reader (PerformanceMonitor instance) about new RRD file created in result of expandRRDFile() method call 
//TODO future community: ewshell for Windows console//TODO future community: ewshell show MB bytes not bytes only (human readable switch)
//TODO future community: ewshell show stats (query, tt, parsing)

//import static org.elasticsearch.common.jna.Kernel32Library.ConsoleCtrlHandler;
//import static org.elasticsearch.common.settings.ImmutableSettings.Builder.EMPTY_SETTINGS;

public class App 
{
	private final static Logger LOGGER = Logger.getLogger(App.class.getName());
	
	private static Elasticwarehouse warehouse_ = null;
	
	static final String JVM_BYPASS = "es.bypass.vm.check";
	
	/**
     * URL with latest JVM recommendations
     */
    static final String JVM_RECOMMENDATIONS = "http://www.elastic.co/guide/en/elasticsearch/reference/current/_installation.html";
    
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
                    fPidFile.getParentFile().mkdirs();
                }
                FileOutputStream outputStream = new FileOutputStream(fPidFile);
                outputStream.write(Long.toString(JvmInfo.jvmInfo().pid()).getBytes(StandardCharsets.UTF_8));
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
        
     // fail if using broken version
        JVMCheck();
        
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
    /**
     * Metadata and messaging for hotspot bugs.
     */
    static final class HotspotBug {
        
        /** OpenJDK bug URL */
        final String bugUrl;
        
        /** Compiler workaround flag (null if there is no workaround) */
        final String workAround;
        
        HotspotBug(String bugUrl, String workAround) {
            this.bugUrl = bugUrl;
            this.workAround = workAround;
        }
        
        /** Returns an error message to the user for a broken version */
        String getErrorMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append("Java version: ").append(fullVersion());
            sb.append(" suffers from critical bug ").append(bugUrl);
            sb.append(" which can cause data corruption.");
            sb.append(System.lineSeparator());
            sb.append("Please upgrade the JVM, see ").append(JVM_RECOMMENDATIONS);
            sb.append(" for current recommendations.");
            if (workAround != null) {
                sb.append(System.lineSeparator());
                sb.append("If you absolutely cannot upgrade, please add ").append(workAround);
                sb.append(" to the JAVA_OPTS environment variable.");
                sb.append(System.lineSeparator());
                sb.append("Upgrading is preferred, this workaround will result in degraded performance.");
            }
            return sb.toString();
        }
        
        /** Warns the user when a workaround is being used to dodge the bug */
        String getWarningMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append("Workaround flag ").append(workAround);
            sb.append(" for bug ").append(bugUrl);
            sb.append(" found. ");
            sb.append(System.lineSeparator());
            sb.append("This will result in degraded performance!");
            sb.append(System.lineSeparator());
            sb.append("Upgrading is preferred, see ").append(JVM_RECOMMENDATIONS);
            sb.append(" for current recommendations.");
            return sb.toString();
        }
    }
    
    /** mapping of hotspot version to hotspot bug information for the most serious bugs */
    static final Map<String,HotspotBug> JVM_BROKEN_HOTSPOT_VERSIONS;
    
    static {
        Map<String,HotspotBug> bugs = new HashMap<String, HotspotBug>();
        
        // 1.7.0: loop optimizer bug
        bugs.put("21.0-b17",  new HotspotBug("https://bugs.openjdk.java.net/browse/JDK-7070134", "-XX:-UseLoopPredicate"));
        // register allocation issues (technically only x86/amd64). This impacted update 40, 45, and 51
        bugs.put("24.0-b56",  new HotspotBug("https://bugs.openjdk.java.net/browse/JDK-8024830", "-XX:-UseSuperWord"));
        bugs.put("24.45-b08", new HotspotBug("https://bugs.openjdk.java.net/browse/JDK-8024830", "-XX:-UseSuperWord"));
        bugs.put("24.51-b03", new HotspotBug("https://bugs.openjdk.java.net/browse/JDK-8024830", "-XX:-UseSuperWord"));
        
        JVM_BROKEN_HOTSPOT_VERSIONS = Collections.unmodifiableMap(bugs);
    }
    
	private static void JVMCheck() {
	        if (Boolean.parseBoolean(System.getProperty(JVM_BYPASS))) {
	        	LOGGER.warn("bypassing jvm version check for version ["+fullVersion()+"], this can result in data corruption!");
	        } else if ("Oracle Corporation".equals(Constants.JVM_VENDOR)) {
	            HotspotBug bug = JVM_BROKEN_HOTSPOT_VERSIONS.get(Constants.JVM_VERSION);
	            if (bug != null) {
	                if (bug.workAround != null && ManagementFactory.getRuntimeMXBean().getInputArguments().contains(bug.workAround)) {
	                	LOGGER.warn(bug.getWarningMessage());
	                } else {
	                    throw new RuntimeException(bug.getErrorMessage());
	                }
	            }
	        } else if ("IBM Corporation".equals(Constants.JVM_VENDOR)) {
	            // currently some old JVM versions from IBM will easily result in index corruption.
	            // 2.8+ seems ok for ES from testing.
	            float version = Float.POSITIVE_INFINITY;
	            try {
	                version = Float.parseFloat(Constants.JVM_VERSION);
	            } catch (NumberFormatException ignored) {
	                // this is just a simple best-effort to detect old runtimes,
	                // if we cannot parse it, we don't fail.
	            }
	            if (version < 2.8f) {
	                StringBuilder sb = new StringBuilder();
	                sb.append("IBM J9 runtimes < 2.8 suffer from several bugs which can cause data corruption.");
	                sb.append(System.lineSeparator());
	                sb.append("Your version: " + fullVersion());
	                sb.append(System.lineSeparator());
	                sb.append("Please upgrade the JVM to a recent IBM JDK");
	                throw new RuntimeException(sb.toString());
	            }
	        }
		
	}
	
	/** 
     * Returns java + jvm version, looks like this:
     * {@code Oracle Corporation 1.8.0_45 [Java HotSpot(TM) 64-Bit Server VM 25.45-b02]}
     */
    static String fullVersion() {
        StringBuilder sb = new StringBuilder();
        sb.append(Constants.JAVA_VENDOR);
        sb.append(" ");
        sb.append(Constants.JAVA_VERSION);
        sb.append(" [");
        sb.append(Constants.JVM_NAME);
        sb.append(" ");
        sb.append(Constants.JVM_VERSION);
        sb.append("]");
        return sb.toString();
    }

	
}
