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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;


public class ElasticWarehouseMapping {

	public static final String FILECONTENT = "filecontent"; 
	
	public static final LinkedList<String> availableFields = new LinkedList<String>(Arrays.asList("folder","folderna",
			"parentId" ,
			"statParseTime" ,
			"filename" ,
			"filesize" ,
			"filetype",
			"filethumb" ,
			"filecontent" ,
			"filetitle"  ,
			"filetext"  ,
			"imagewidth" ,
			"imageheight"  ,
			"imagexresolution" ,
			"imageyresolution" ,
			"filemeta"  ,
			"filecreationdate"  ,
			"filemodificationdate"  ,
			"fileuploaddate"  ,
			"fileaccess"  ,
			"location", 
			"filemodification" ));
	
	public static final LinkedList<String> availableFieldsForModification = new LinkedList<String>(
			Arrays.asList("customkeywords","customcomments"));
	
	public static final String FIELDSCORE = "score";
	public static final String FIELDALL = "all";
	public static final String FIELDFOLDER = "folder";
	public static final String FIELDFOLDERNA = "folderna";

	public static final String FIELDLOCATION = "location";
	
	public static boolean isOneOf(String field)
	{
		return availableFields.contains(field);
	}

	public static boolean isOneOf(HashMap<String, String> queryfields) {
		Iterator<Entry<String, String>> it = queryfields.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, String> pairs = (Map.Entry<String, String>)it.next();
            String fieldname = pairs.getKey();
            if( isOneOf(fieldname) )
            	return true;
        }
		return false;
	}

	public static boolean isIntegerField(String fieldname)
	{
		if( fieldname.equals("filesize") || fieldname.equals("imagewidth") || fieldname.equals("imageheight") 
				|| fieldname.equals("imagexresolution") || fieldname.equals("imageyresolution"))
			return true;
		else
			return false;
	}

	public static boolean isDateField(String fieldname)
	{
		if( fieldname.equals("filecreationdate") || fieldname.equals("filemodificationdate") || fieldname.equals("fileuploaddate") 
				|| fieldname.equals("fileaccess") || fieldname.equals("filemodification"))
			return true;
		else
			return false;
	}

	public static boolean isGeoField(String fieldname) {
		return fieldname.equals("location");
	}
}
