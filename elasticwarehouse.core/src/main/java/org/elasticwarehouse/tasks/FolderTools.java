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
import java.util.LinkedList;

import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.EwInfoTuple;
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
			ret.errorCode_ = ElasticWarehouseTask.ERROR_TASK_WRONG_PARAMETERS;
		}
		else if( acccessor_.folderExists(folder, true) )
		{
			ret.comment_ = "Folder: "+folder+" already exists";
			ret.errorCode_ = ElasticWarehouseTask.ERROR_TASK_CREATE_FOLDER_ALREADY_EXIST;
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
		if( !acccessor_.folderExists(folder, false) )
		{
			ret.comment_ = "Folder "+folder+" doesn't exist";
			ret.errorCode_ = ElasticWarehouseTask.ERROR_TASK_RMDIR_FOLDER_DOESNT_EXIST;
		}
		else
		{
			if( !acccessor_.removeFolder(folder) )
			{
				ret.comment_ = "Folder "+folder+" cannot be removed";
				ret.errorCode_ = ElasticWarehouseTask.ERROR_TASK_RMDIR_OTHER_EXCEPTION;
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
				ret.errorCode_ = ElasticWarehouseTask.ERROR_TASK_WRONG_PARAMETERS;
			}
			else if( acccessor_.isFolder(fileid ) )
			{
				ret.comment_ = "Folders cannot be moved";
				ret.errorCode_ = ElasticWarehouseTask.ERROR_TASK_MOVE_FOLDERS_CANNOT_BE_MOVED;
			}
			else if( !acccessor_.moveTo(fileid, folder) )
			{
				ret.comment_ = "File "+fileid+" cannot be moved to: "+folder;
				ret.errorCode_ = ElasticWarehouseTask.ERROR_TASK_MOVE_CANNOT_PROCESS;
			}
		}
		catch(Exception e)
		{
			EWLogger.logerror(e);
			ret.comment_ = e.getMessage();
			ret.errorCode_ = ElasticWarehouseTask.ERROR_TASK_MOVE_OTHER_EXCEPTION;
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
				ret.errorCode_ = ElasticWarehouseTask.ERROR_TASK_WRONG_PARAMETERS;
			}else{
				acccessor_.deleteChildren(fileid);
				acccessor_.deleteFile(fileid);
				acccessor_.getClient().admin().indices().prepareRefresh().execute().actionGet();
			}
		}
		catch(Exception e)
		{
			EWLogger.logerror(e);
			ret.comment_ = e.getMessage();
			ret.errorCode_ = ElasticWarehouseTask.ERROR_TASK_DELETE_OTHER_EXCEPTION;
		}
		return ret;
	}



	
}
