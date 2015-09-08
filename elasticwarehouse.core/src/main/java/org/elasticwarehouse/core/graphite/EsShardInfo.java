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

public class EsShardInfo {

	public EsShardInfo(int id, boolean primary, long docsCnt,  long storeSize, String nodeIp, String nodeName) {
		isPrimary_ = primary;
		id_ = id;
		docsCnt_ = docsCnt;
		storeSize_ = storeSize;
		nodeIp_ = nodeIp;
		nodeName_  = nodeName;
	}
	public boolean isPrimary_ = true;
	public int id_ = -1;
	public Long storeSize_ = -1L;
	public Long docsCnt_ = -1L;
	public String nodeIp_ = "";
	public String nodeName_ = "";
	
	@Override
	public String toString() {
		return (isPrimary_?"p":"r") + " " + id_ + " " + docsCnt_ + " " + storeSize_ + " ("+nodeIp_ +"->"+nodeName_+")";
	}
}
