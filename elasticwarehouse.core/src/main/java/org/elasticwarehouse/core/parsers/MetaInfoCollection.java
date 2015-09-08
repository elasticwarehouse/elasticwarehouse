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
package org.elasticwarehouse.core.parsers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;

public class MetaInfoCollection {
	protected HashMap<String, MetaInfo> metadata_ = new HashMap<String, MetaInfo>();
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public void addMetaString(String key, String value) {
		metadata_.put(key,new MetaInfo(key,value));
	}
	
	public void addMetaLong(String key, long value) {
		metadata_.put(key, new MetaInfo(key,value));
	}
	
	public void addMetaDate(String key, Date value) {
		metadata_.put(key,new MetaInfo(key,value));
	}

	

	public MetaInfoCollection getCopy() {
		MetaInfoCollection ret = new MetaInfoCollection();
		Iterator<?> it = metadata_.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        MetaInfo minfo = (MetaInfo) pairs.getValue();
	        ret.add(new String(pairs.getKey().toString()), minfo.getCopy());
	    }
		return ret;
	}

	private void add(String key, MetaInfo mi)
	{
		metadata_.put(key,mi);
	}

	public String getStringValueFor(LinkedList<String> type, boolean removeUsedOne)
	{
		String ret = "";
		for(String key : type)
		{
			if( metadata_.containsKey(key) )
			{
				ret = metadata_.get(key).valueString_;
				if( removeUsedOne )
					metadata_.remove(key);
				break;
			}
		}
		return ret;
	}
	
	public String getDateValueFor(LinkedList<String> type, boolean removeUsedOne) {
		
		
		String ret = "";
		for(String key : type)
		{
			if( metadata_.containsKey(key) && metadata_.get(key).valueDate_ != null )
			{
				ret = df.format(metadata_.get(key).valueDate_);
				if( removeUsedOne )
					metadata_.remove(key);
				break;
			}
		}
		return ret;
	}

	public void getMetaArray(XContentBuilder builder) throws IOException
	{
		Iterator<?> it = metadata_.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        XContentBuilder subobject = builder.startObject().field("metakey", pairs.getKey());
	        //str.append("{");
	        //str.append("\"metakey\" : \""+pairs.getKey()+"\",");
	        MetaInfo minfo = (MetaInfo) pairs.getValue();
	        if( minfo.valueDate_ != null )
	        	subobject.field("metavaluedate" , df.format(minfo.valueDate_));
	        else if( minfo.valueLong_ != null )
	        	subobject.field("metavaluelong" , minfo.valueLong_);
	        else
	        	subobject.field("metavaluetext" , minfo.valueString_);
	        //str.append("}");
	        subobject.endObject();
	        //System.out.println(pairs.getKey() + " = " + pairs.getValue());
	    }
		
	}

	
}
