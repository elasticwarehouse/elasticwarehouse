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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticSearchAccessor;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.EwBrowseTuple;
import org.elasticwarehouse.core.EwInfoTuple;
import org.elasticwarehouse.core.ResourceTools;

public class ElasticWarehouseTaskRename  extends ElasticWarehouseTask {

	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseTaskRename.class.getName());
	
	private ElasticWarehouseConf conf_ = null;
	private String fileid_ = null;
	private String targetname_ = null;
	private int renamedFiles_ = 0;
	ArrayList<String> processingErrors_ = new ArrayList<String>();
	Thread th_ = null;
	
	
	public ElasticWarehouseTaskRename(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, String id, String targetname) {
		super(acccessor, conf);
		conf_ = conf;
		fileid_ = id;
		targetname_ = targetname;
	}
	
	public ElasticWarehouseTaskRename(ElasticSearchAccessor acccessor, ElasticWarehouseConf conf, Map<String, Object> source)
	{
		super(acccessor, source, conf);
		fileid_ = source.get("fileid").toString();
		targetname_ = source.get("targetname").toString();
		renamedFiles_  = (source.get("renamedfiles")==null?0:Integer.parseInt(source.get("renamedfiles").toString()));
		conf_ = conf;
	}

	@Override
	public String getActionString() {
		return "rename";
	}

	@Override
	public XContentBuilder vgetJsonSourceBuilder(XContentBuilder builder) throws IOException
	{
		builder.field("fileid",fileid_);
		builder.field("targetname", targetname_);
		builder.field("renamedfiles", renamedFiles_);
		if( processingErrors_.size() > 0 )
		{
			builder.array("processingerrors", processingErrors_);
		}
		return builder;
	}
	
	@Override
	public boolean isAsync()
	{
		return true;
	}
	
	@Override
	public void start()
	{
		try {			
			if( acccessor_.isFolder(fileid_ ) )
			{
				indexTask();	//index asap to avoid subsequent rename calls to the same folder
				
				//first rename current item without thread, then use thread to rename all subitems
				final EwInfoTuple rootinfotuple = acccessor_.getFileInfoById(fileid_, false, false);
				FolderToolsResult result = renameProcess(rootinfotuple, targetname_, true, true);
				comment_ = result.comment_;
				errorcode_ = result.errorCode_;
				
				indexTask();
				acccessor_.refreshIndex();
				
				th_ = new Thread() {
				    public void run() {
				        LOGGER.info("Starting renaming folder " + fileid_ + " to "+ targetname_);
				        //FolderTools toolset = new FolderTools(acccessor_, conf_);
				        FolderToolsResult result = renameProcess(rootinfotuple, targetname_, true, false);

						comment_ = result.comment_;
						errorcode_ = result.errorCode_;
						
						setFinished();
						progress_ = 100;
						
						indexTask();
						acccessor_.refreshIndex();
				    }  
				};
				th_.start();
			}
			else
			{
				progress_ = 100;
				setFinished();
				//FolderTools toolset = new FolderTools(acccessor_, conf_);
				EwInfoTuple rootinfotuple = acccessor_.getFileInfoById(fileid_, false, false);
				FolderToolsResult result = renameProcess(rootinfotuple, targetname_, false, false);

				comment_ = result.comment_;
				errorcode_ = result.errorCode_;
				
				indexTask();
			}
		} catch (Exception e) {
			EWLogger.logerror(e);
			e.printStackTrace();
			errorcode_ = ERROR_TASK_RENAME_OTHER_EXCEPTION;
			comment_ = "Error:" + e.getMessage();
			indexTask();
		}
	}
	
	private FolderToolsResult renameProcess(EwInfoTuple rootinfotuple, String targetname, boolean itemsIsFolder, boolean skipKids)
	{
		FolderToolsResult ret = new FolderToolsResult();
		try
		{
			if( itemsIsFolder /*acccessor_.isFolder(fileid )*/ )
			{
				//EwInfoTuple rootinfotuple = acccessor_.getFileInfoById(fileid, false, false);
				if( rootinfotuple == null || rootinfotuple.source.get("folder").equals("/") )
				{
					if( rootinfotuple == null )
						comment_ = "Provided Id doesn't exist. Please provide valid Id.";
					else
						comment_ = "Root folder / cannot be renamed.";
					errorcode_ = ERROR_TASK_RENAME_ROOT_CANNOT_BE_RENAMED;
					setFinished();
					indexTask();
					throw new ElasticsearchException(comment_);
				}
				
				String rootFolderPath = rootinfotuple.source.get("folder").toString();
				//int rootFolderLevel = Integer.parseInt( rootinfotuple.source.get("folderlevel").toString() );
				String[] rootfolders = rootFolderPath.split("/");	//size should be rootFolderLevel+2 thanks to leading and following slashes
				LinkedList<EwBrowseTuple> tuples = new LinkedList<EwBrowseTuple>();
				if( skipKids )
				{
					tuples.add(new EwBrowseTuple(rootinfotuple) );
				}else{
					tuples = acccessor_.findAllSubFolders(rootFolderPath);
					tuples.add(new EwBrowseTuple(rootinfotuple) );
				}
				renamedFiles_ = tuples.size(); //TODO compute correct size
				int counter = 0;
				int modulofactor = renamedFiles_ / 5;
	        	if( modulofactor <10 )
	        		modulofactor = 10;
	        	
				for(EwBrowseTuple tuple : tuples)
				{
					//EwInfoTuple infotuple = acccessor_.getFileInfoById(id, false, true);
					//String folderPath = infotuple.source.get("folder").toString();
					//boolean currentisfolder = Boolean.parseBoolean( infotuple.source.get("isfolder").toString() );
					//String currentfilename = "";
					//if( infotuple.source.get("filename") != null )
					//	currentfilename = infotuple.source.get("filename").toString();
					//String filename = infotuple.source.get("filename").toString();
					//int folderLevel = Integer.parseInt( infotuple.source.get("folderlevel").toString() );
					String[] folders = tuple.folderna.split("/");	//size should be folderLevel+2 thanks to leading and following slashes
					
					String newFolderPath = "";
					int i = 0;
					if( rootinfotuple.id.equals(tuple.id) ) //renaming myself
					{
						for(i=0;i<rootfolders.length-1;i++)
							newFolderPath += rootfolders[i]+"/";
						newFolderPath+=targetname+"/";
					}
					else	//renaming others
					{
						for(i=0;i<rootfolders.length - 1;i++)
							newFolderPath += rootfolders[i]+"/";
						newFolderPath+=targetname+"/";
						i++;
						for(;i<folders.length;i++)
							newFolderPath += folders[i]+"/";
					}
					
					String folder = ResourceTools.preprocessFolderName(newFolderPath);
					//all tuplease are folders if( currentisfolder )
					LOGGER.info("Renaming folder:"+tuple.id +" ("+tuple.folderna+") to "+folder);
					//else
					//	LOGGER.info("Renaming file:"+id +" ("+folderPath+"/"+currentfilename+") to "+folder);
					if( tuple.folderna.equals(folder) == false )
						acccessor_.setFolder(tuple.id, folder);
					//LOGGER.info("Renaming file:"+id +" done");
					
					if( !skipKids )
					{
						//now set proper folderpath for all files inside folder using script
						acccessor_.setNewFolderForFilesInFolder(tuple.folderna, folder);
					}
					
	        		float pp = (float)counter/renamedFiles_;
	        		progress_ = (int) ((float) pp*100.0);
	        		counter++;
	        		if( (counter%modulofactor) == 0 && counter>0 )	//when skipKids=true, counter=0, so task won't be indexed 
	        			indexTask();
				}
				
			}else{
				renamedFiles_ = 1;
				acccessor_.setFilename(rootinfotuple.id, targetname);	//renaming file, not folder
			}
		}
		catch(Exception e)
		{
			EWLogger.logerror(e);
			ret.comment_ = e.getMessage();
			ret.errorCode_ = ERROR_TASK_RENAME_OTHER_EXCEPTION;
		}
		return ret;
	}
}
