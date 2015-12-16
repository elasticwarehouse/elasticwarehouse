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

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Properties;

/**
 */
public class Build {

    public static final Build CURRENT;

    static {
        String version = "NA";
        String timestamp = "NA";
        String grafanaversion = "NA";
        String esversion = "NA";
        
        try {
        	
            Properties props = new Properties();
            props.load(Build.class.getClassLoader().getResourceAsStream("/ew-build.properties"));
            
            version = props.getProperty("version", version);
            grafanaversion = props.getProperty("grafanaversion", grafanaversion);
            timestamp = props.getProperty("timestamp", timestamp);
            esversion = props.getProperty("esversion", esversion);
            
            /*String gitTimestampRaw = props.getProperty("timestamp");
            if (gitTimestampRaw != null) {
                timestamp = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC).print(Long.parseLong(gitTimestampRaw));
            }*/
        } catch (Exception e) {
            // just ignore...
        }

        CURRENT = new Build(version, timestamp, grafanaversion, esversion);
    }

    private String version;
    private String timestamp;
    private String grafanaversion;
    private String esversion;

    Build(String version, String timestamp, String grafanaversion, String esversion) {
        this.version = version;
        this.timestamp = timestamp;
        this.grafanaversion = grafanaversion;
        this.esversion = esversion;
    }

    public String esversion() {
        return esversion;
    }

    public String version() {
        return version;
    }
    
    public String grafanaversion() {
        return grafanaversion;
    }

    public String timestamp() {
        return timestamp;
    }

    public static Build readBuild(StreamInput in) throws IOException {
        String version = in.readString();
        String timestamp = in.readString();
        String grafanaversion = in.readString();
        String esversion = in.readString();
        return new Build(version, timestamp, grafanaversion, esversion);
    }

    public static void writeBuild(Build build, StreamOutput out) throws IOException {
        out.writeString(build.version());
        out.writeString(build.timestamp());
        out.writeString(build.grafanaversion());
        out.writeString(build.esversion());
    }
}
