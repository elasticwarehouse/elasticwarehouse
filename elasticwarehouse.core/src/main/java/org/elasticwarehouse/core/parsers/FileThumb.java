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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticWarehouseConf;

public class FileThumb {

	public boolean thumbavailable_ = false;
	public boolean sameasimage = true;
	public Date thumbdate_ = null;
	public BufferedImage thumb_ = null;
	
	public int orginalw = 0;
	public int orginalh = 0;
	public String error_ = "";
	
	private byte[] imageToBytes(BufferedImage image)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] imageInByte = null;
		try {
			ImageIO.write( image, "jpg", baos );
			baos.flush();
			imageInByte = baos.toByteArray();
			baos.close();
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		}
		return imageInByte;
	}
	
	public void processThumb(XContentBuilder builder, DateFormat df) throws IOException
	{
		
		builder.field(ElasticWarehouseConf.FIELD_THUMB_AVAILABLE, thumbavailable_);
		if( thumbdate_ != null )
		{
			String s = df.format(thumbdate_);
			builder.field(ElasticWarehouseConf.FIELD_THUMB_THUMBDATE, s );
		}
		if( sameasimage )
		{
			builder.field(ElasticWarehouseConf.FIELD_THUMB_SAMEASIMAGE, true );
		}else{
			builder.field(ElasticWarehouseConf.FIELD_THUMB_SAMEASIMAGE, false );
			byte[] buffer = null;
			buffer = imageToBytes(thumb_);
			if( buffer != null )
				builder.field(ElasticWarehouseConf.FIELD_THUMB_THUMB, buffer );
		}
		//builder.endObject();
	}

	/*
	public void processThumb(Map<String, Object> updateObject, DateFormat df) {
		updateObject.put("available", thumbavailable_);
		if( thumbdate_ != null )
		{
			String s = df.format(thumbdate_);
			updateObject.put("thumbdate", s );
		}
	}*/

}
