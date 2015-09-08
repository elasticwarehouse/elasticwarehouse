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
package org.elasticwarehouse.tasks;

import java.io.IOException;

import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.ResourceTools;
import org.elasticwarehouse.core.parsers.ElasticWarehouseFolder;

public class FolderTools {

	ElasticSearchAccessor acccessor_ = null;
	ElasticWarehouseConf conf_ =  null;
	
	public FolderTools(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf) {
		acccessor_ = acccessor;
		conf_ = conf;
	}

	public FolderToolsResult createFolder(String infolder) throws IOException
	{
		String folder = ResourceTools.preprocessFolderName(infolder);
		FolderToolsResult ret = new FolderToolsResult();
		if( !folder.startsWith("/") || folder.length() == 0)
		{
			ret.comment_ = "Path to be created must starts with /";
			ret.errorCode_ = 50;
		}
		else if( acccessor_.folderExists(folder) )
		{
			ret.comment_ = "Folder: "+folder+" already exists";
			ret.errorCode_ = 40;
		}
		else
		{
			ElasticWarehouseFolder fldr = new ElasticWarehouseFolder(folder,conf_);
			acccessor_.indexFolder(fldr);
		}
		return ret;
	}

	public FolderToolsResult removeFolder(String folder)
	{
		FolderToolsResult ret = new FolderToolsResult();
		if( !acccessor_.folderExists(folder) )
		{
			ret.comment_ = "Folder "+folder+" doesn't exist";
			ret.errorCode_ = 40;
		}
		else
		{
			if( !acccessor_.removeFolder(folder) )
			{
				ret.comment_ = "Folder "+folder+" cannot be removed";
				ret.errorCode_ = 60;
			}
		}
		return ret;
	}

	public FolderToolsResult moveTo(String fileid, String infolder) throws IOException
	{
		String folder = ResourceTools.preprocessFolderName(infolder);
		FolderToolsResult ret = new FolderToolsResult();
		try
		{
			if( !folder.startsWith("/") )
			{
				ret.comment_ = "Path to be created must starts with /";
				ret.errorCode_ = 50;
			}
			else if( acccessor_.isFolder(fileid ) )
			{
				ret.comment_ = "Folders cannot be moved";
				ret.errorCode_ = 51;
			}
			else if( !acccessor_.moveTo(fileid, folder) )
			{
				ret.comment_ = "File "+fileid+" cannot be moved to: "+folder;
				ret.errorCode_ = 60;
			}
		}
		catch(Exception e)
		{
			EWLogger.logerror(e);
			ret.comment_ = e.getMessage();
			ret.errorCode_ = 70;
		}
		return ret;
	}

	public FolderToolsResult delete(String fileid)
	{
		FolderToolsResult ret = new FolderToolsResult();
		try
		{
			if( acccessor_.isFolder(fileid ) )
			{
				ret.comment_ = "To remove folder please use 'rmdir' action";
				ret.errorCode_ = 52;
			}else{
				acccessor_.deleteChildren(fileid);
				acccessor_.deleteFile(fileid);
			}
		}
		catch(Exception e)
		{
			EWLogger.logerror(e);
			ret.comment_ = e.getMessage();
			ret.errorCode_ = 70;
		}
		return ret;
	}

	
}
