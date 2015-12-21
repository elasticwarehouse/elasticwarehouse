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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hslf.exceptions.EncryptedPowerPointFileException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.EmbeddedContentHandler;
import org.elasticwarehouse.core.EWLogger;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class ElasticWarehouseFileParserAuto extends ElasticWarehouseFileParser{
	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseFileParserAuto.class.getName());
	
	public ElasticWarehouseFileParserAuto(String uploadedfilename, String fname, String targetfolder, ElasticWarehouseConf conf) throws IOException {
		super(uploadedfilename, fname, targetfolder, conf);
	}
	
	
	
	public boolean parse() throws IOException
	{
		boolean ret = false;
		InputStream is = null;
		file.statParse();
        try {
        	LOGGER.info("Parsing " + file.getOrginalFname() + " ("+file.getUploadedFilename()+")" );
            is = new BufferedInputStream(new FileInputStream(new File(file.getUploadedFilename())));
 
            Parser parser = new AutoDetectParser();
            OutputStream output = new OutputStringStream();
            ContentHandler handler = new BodyContentHandler( output );//System.out);
            //ContentHandler h2 = new EmbeddedContentHandler(handler);
            Metadata metadata = new Metadata();
            //ExtractParser extractParser = new ExtractParser(parser);
            //ParseContext context = new ParseContext();
            //context.set(Parser.class, extractParser);
            //context.set(EmbeddedDocumentExtractor.class, new FileEmbeddedDocumentExtractor());
            ParseContext parseContext = new ParseContext();
            //parseContext.set(Parser.class, new TikaImageExtractingParser());
            parseContext.set(Parser.class, new ElasticWarehouseParserAll(parser, file, conf_));
            
            parser.parse(is, handler/*handler*/, metadata, parseContext);
            
            file.fill(metadata, output.toString());
            ret = true;
            
        } catch (IOException e) {
        	EWLogger.logerror(e);
        	e.printStackTrace();				
        } catch (java.lang.ExceptionInInitializerError e) {
        	EWLogger.logerror(e);
        	e.printStackTrace();	
        } catch (TikaException e) {
        	Exception e2 = (Exception) ExceptionUtils.getRootCause(e);
        	if (e2 instanceof UnsupportedZipFeatureException) {
				//do nothing, format unsupported
        		ret = true;
			}else if( e2 instanceof EncryptedPowerPointFileException){
				//do nothing, format unsupported
				ret = true;
			}else if( e instanceof org.apache.tika.exception.EncryptedDocumentException){
				//do nothing, format unsupported
				ret = true;
			}else if( e2 instanceof org.apache.poi.EncryptedDocumentException){
				//do nothing, format unsupported
				ret = true;
			}else if( e2 instanceof org.apache.tika.exception.EncryptedDocumentException){
				//do nothing, format unsupported
				ret = true;
			}else{
				EWLogger.logerror(e);
				e.printStackTrace();
				
				//i.e. for org.apache.tika.exception.TikaException: cannot parse detached pkcs7 signature (no signed data to parse)
				ret = true;
			}
        } catch (SAXException e) {
        	EWLogger.logerror(e);
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch(IOException e) {
                	EWLogger.logerror(e);
                    e.printStackTrace();
                }
            }
        }
        
        //add metadata supporoit to teh mapping
        file.endParse();
        LOGGER.info("Parsing " + file.getOrginalFname() + " took " + file.getParsingTime() + " ms");
		/*if( file == null )
			return false;
		InputStream is = null;
	    try {
	      is = new FileInputStream(file.getFilename());
	      ContentHandler contenthandler = new BodyContentHandler();
	      Metadata metadata = new Metadata();
	      PDFParser pdfparser = new PDFParser();
	      pdfparser.parse(is, contenthandler, metadata, new ParseContext());
	      System.out.println(contenthandler.toString());
	    }
	    catch (Exception e) {
	      e.printStackTrace();
	    }
	    finally {
	        if (is != null) is.close();
	    }*/
	    return ret;
	}
}
