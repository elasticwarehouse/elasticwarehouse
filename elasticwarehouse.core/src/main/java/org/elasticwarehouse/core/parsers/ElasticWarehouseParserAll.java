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
import java.io.InputStream;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class ElasticWarehouseParserAll extends AbstractParser {
	   private int att = 0;
	   private Parser parser_ = null;
	   private ElasticWarehouseFile parentfile_ = null;
	private ElasticWarehouseConf conf_;
	   private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseParserAll.class.getName());
	   
	   ElasticWarehouseParserAll(Parser parser, ElasticWarehouseFile file, ElasticWarehouseConf conf)
	   {
		   parser_ = parser;
		   parentfile_ = file;
		   conf_ = conf;
	   }
	   
	   public Set<MediaType> getSupportedTypes(ParseContext context) {
	     // Everything AutoDetect parser does
	     return parser_.getSupportedTypes(context);
	   }
	   public void parse(
	        InputStream stream, ContentHandler handler,
	        Metadata metadata, ParseContext context)
	        throws IOException, SAXException, TikaException 
	        {
			      // Stream to a new file
		   			String filename = metadata.get(Metadata.RESOURCE_NAME_KEY);
		   			LOGGER.info(parentfile_.getOrginalFname() + " Found embedded item: "+filename);
		   			//dumpMetadata(metadata);
		   			
		   			
		   			ElasticWarehouseFile file = new ElasticWarehouseFile(stream, filename, parentfile_.targetfolder_, conf_);
		   			file.fill(metadata, "");
		   			parentfile_.addEmbeddedFile(file);
			      //File f = new File("out-" + (++att) + ".bin");
			      //FileOutputStream fout = new FileOutputStream(f);
			      //IOUtils.copy(stream, fout);
			      //fout.close();
	   }

	private void dumpMetadata(Metadata metadata) {
		for (String name : metadata.names())
        {
            String value = metadata.get(name);
            if (value != null)
            { 
                //System.out.println("Metadata Name:  " + name);
                //System.out.println("Metadata Value: " + value);
            	System.out.println("Meta: " + name + " => " + value);
            }
        }
		
	}
	}