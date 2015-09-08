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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.tika.io.IOUtils;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.tasks.ElasticWarehouseTaskScan;

public class ElasticWarehouseFolder extends ElasticWarehouseFile {

	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseFolder.class.getName());
	
	public ElasticWarehouseFolder(String targetfolder, ElasticWarehouseConf conf) throws IOException
	{
		super("", "", targetfolder, conf);
		this.setTypeFolder(true);
	}
	
	@Override
	public int getFolderLevel()
	{
		String[] tkns = splitFolders();
		if( tkns.length == 0 )
			return 0;
		
		return tkns.length-1;
	}
	
	
}
