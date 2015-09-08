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

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;


import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticwarehouse.tasks.ElasticWarehouseTask;

public class ElasticWarehouseReqRespHelper
{
	public byte[] errorMessage(String string, String helpURL) throws IOException {
		
		 XContentBuilder builder = jsonBuilder()
		         .startObject()
		         	 .field("status", 400)
		             .field("error", string);
		 if( helpURL != null && helpURL.length()>0)
			 builder.field("url", helpURL);
		 
		 return builder.endObject()
		       .string().getBytes();
		 
		             
	}
	public void errorMessage(XContentBuilder builder, String string, String helpURL) throws IOException {
		
		 builder.startObject()
		         	 .field("status", 400)
		             .field("error", string);
		 if( helpURL != null && helpURL.length()>0)
			 builder.field("url", helpURL);
		 
		 builder.endObject();
	}
	public byte[] taskAcceptedMessage(String message, int progress, ElasticWarehouseTask taskUUID) throws IOException 
	{
		XContentBuilder builder = jsonBuilder()
		         .startObject()
		         	 .field("status", 200)
		             .field("message", message)
					 .field("progress", progress);
		builder = taskUUID.getJsonSourceBuilder(builder, false);
		 
		return builder.endObject()
		       .string().getBytes();
	}

	
}
