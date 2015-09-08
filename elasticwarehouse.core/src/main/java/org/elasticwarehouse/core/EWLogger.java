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

import java.io.IOException;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;

public class EWLogger {

	private final static Logger LOGGER = Logger.getLogger(EWLogger.class.getName());
	
	public static void logerror(ElasticsearchException e) {
		LOGGER.error(e.getMessage());
		for(StackTraceElement elem : e.getStackTrace())
			LOGGER.error(elem.toString());
	}

	public static void logerror(IOException e) {
		LOGGER.error(e.getMessage());
		for(StackTraceElement elem : e.getStackTrace())
			LOGGER.error(elem.toString());
		
	}

	public static void logerror(InterruptedException e) {
		LOGGER.error(e.getMessage());
		for(StackTraceElement elem : e.getStackTrace())
			LOGGER.error(elem.toString());
		
	}

	public static void logerror(Exception e) {
		LOGGER.error(e.getMessage());
		for(StackTraceElement elem : e.getStackTrace())
			LOGGER.error(elem.toString());
		
	}

}
