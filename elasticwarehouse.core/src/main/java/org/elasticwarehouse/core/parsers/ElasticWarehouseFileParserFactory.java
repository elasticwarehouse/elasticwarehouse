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

import org.elasticwarehouse.core.ElasticWarehouseConf;

public class ElasticWarehouseFileParserFactory {
	public static ElasticWarehouseFileParser getParser(String uploadedfilename, String fname, String targetfolder, ElasticWarehouseConf conf) throws IOException
	{
		if( fname.toLowerCase().endsWith(".pdf") )
			return new ElasticWarehouseParserPDF(uploadedfilename, fname, targetfolder, conf);
		else
			return null;
	}
	public static ElasticWarehouseFileParser getAutoParser(String uploadedfilename, String fname, String targetfolder, ElasticWarehouseConf conf) throws IOException
	{
		return new ElasticWarehouseFileParserAuto(uploadedfilename, fname, targetfolder, conf);
	}
}
