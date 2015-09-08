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

import org.elasticsearch.monitor.jvm.JvmInfo;

public class Version {
	public static void main( String[] args )
    {
		System.out.println("Version: " + Build.CURRENT.version() + ", ElasticSearch: " + Build.CURRENT.esversion() + ", Grafana: " + Build.CURRENT.grafanaversion() + ", Build timestamp: " + Build.CURRENT.timestamp() + ", JVM: " + JvmInfo.jvmInfo().version());
    }
}
