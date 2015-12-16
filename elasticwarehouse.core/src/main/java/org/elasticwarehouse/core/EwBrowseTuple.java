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

import org.elasticsearch.search.SearchHit;

public class EwBrowseTuple {
	public EwBrowseTuple(EwInfoTuple infotuple) {
		id = infotuple.id;
		version = infotuple.version;
		if( infotuple.source != null )
		{
			if( infotuple.source.get("folder") != null )
				folderna = infotuple.source.get("folder").toString();
			if( infotuple.source.get("filename") != null )
				filenamena = infotuple.source.get("filename").toString();
		}
	}
	public EwBrowseTuple(SearchHit hit) {
		id = hit.getId();
		version = hit.getVersion();
		if( hit.getSource() != null )
		{
			if( hit.getSource().get("folderna") != null )
				folderna = hit.getSource().get("folderna").toString();
			if( hit.getSource().get("filenamena") != null )
				filenamena = hit.getSource().get("filenamena").toString();
		}
	}
	public String id = "";
	public long version;
	public String folderna = "";
	public String filenamena = "";
	
	@Override
	public String toString() {
		return id;
	}
}

