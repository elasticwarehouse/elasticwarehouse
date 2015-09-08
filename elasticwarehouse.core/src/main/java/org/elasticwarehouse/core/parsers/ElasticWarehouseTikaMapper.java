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

import java.util.Arrays;
import java.util.LinkedList;

import org.apache.tika.metadata.Metadata;

public class ElasticWarehouseTikaMapper {

	public static final LinkedList<String> FILETYPE = new LinkedList<String>(Arrays.asList(Metadata.CONTENT_TYPE) );
	public static final LinkedList<String> FILECREATIONDATE = new LinkedList<String>(Arrays.asList("meta:creation-date", "Creation-Date", "date", "Last-Save-Date") );
	public static final LinkedList<String> FILEMODIFICATIONDATE = new LinkedList<String>(Arrays.asList("dcterms:modified", "Last-Modified", "modified") );
	public static final LinkedList<String> FILETITLE = new LinkedList<String>(Arrays.asList("dc:title", "title") );
	public static final LinkedList<String> IMAGEWIDTH = new LinkedList<String>(Arrays.asList("Image Width","Exif Image Width",  "tiff:ImageWidth") );
	public static final LinkedList<String> IMAGEHEIGHT = new LinkedList<String>(Arrays.asList("Image Height", "Exif Image Height",  "tiff:ImageHeight") );
	public static final LinkedList<String> IMAGEXRESOLUTION = new LinkedList<String>(Arrays.asList("X Resolution", "tiff:XResolution") );
	public static final LinkedList<String> IMAGEYRESOLUTION = new LinkedList<String>(Arrays.asList("Y Resolution", "tiff:YResolution") );
	
	public static final LinkedList<String> GEOLAT = new LinkedList<String>(Arrays.asList("geo:lat", "latitude") );
	public static final LinkedList<String> GEOLON = new LinkedList<String>(Arrays.asList("geo:long", "longitude") );
	
	
	//tiff:ResolutionUnit => Inch

	
	//TODO dokonczyc geo search
	//geo:lat
	//geo:long
	
	public static final String TIKAIMAGEWIDTH = "Image Width";
	public static final String TIKAIMAGEHEIGHT = "Image Height";									
}
