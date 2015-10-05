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

import static org.elasticsearch.common.xcontent.XContentFactory.*;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.apache.tika.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.graphite.NetworkTools;

public class ElasticWarehouseFile {

	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseFile.class.getName());

	public static final String STAT_PARSE_TIME = "parseTime";
	private HashMap<String, Long> stats_ = new HashMap<String, Long>();
	
	private ElasticWarehouseConf conf_;

	public ElasticWarehouseFile(String uploadedfilename, String fname, String targetfolder, ElasticWarehouseConf conf) throws IOException
	{
		uploadedfilename_ = uploadedfilename;
		conf_ = conf;
		fname_ = fname;
		targetfolder_ = targetfolder;
		File f1 = new File(uploadedfilename_);
		fsize_ = f1.length();
		if( uploadedfilename.length() > 0 )
			binaryContent_ = Files.readAllBytes(Paths.get(uploadedfilename));
		postProcessTargetFolder();
	}
	
	public ElasticWarehouseFile(InputStream inputstream, String fname, String targetfolder, ElasticWarehouseConf conf) throws IOException
	{
		uploadedfilename_ = "";//uploadedfilename;
		conf_ = conf;
		fname_ = fname;
		targetfolder_ = targetfolder;
		File f1 = new File(uploadedfilename_);
		fsize_ = f1.length();
		if( inputstream != null )
			binaryContent_ = IOUtils.toByteArray(inputstream);
		postProcessTargetFolder();
        
	}

	private void postProcessTargetFolder() {
		if( targetfolder_ == null )
			return;
		
		//folder must ends with / to use prefixquery correctly
		while( targetfolder_.endsWith("/") && targetfolder_.equals("/") == false )
			targetfolder_ = targetfolder_.substring(0, targetfolder_.length()-1);
		if( targetfolder_.equals("/") == false )
			targetfolder_ = targetfolder_+"/";
	}
	
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	protected String uploadedfilename_;
	protected String fname_ ;
	protected String targetfolder_;
	protected boolean isfolder_ = false;
	protected long fsize_ = 0;
	
	public String fileuploaddate_ = null;	//set only for modification (when id is provided)
	
	
	protected MetaInfoCollection metadata_ = new MetaInfoCollection();
	//protected HashMap<Integer, BufferedImage> thumbs_ = new HashMap<Integer, BufferedImage>();
	FileThumb fileThumb_ = null;
	//BufferedImage thumb_ = null;
	//boolean sameasimage = true;
	//boolean thumbavailable_ = false;
	
	public byte[] binaryContent_;
	
	
	protected long parsingTime_ = 0;
	
	private long parsingStartTime_ = 0;
	private long parsingEndTime_ = 0;
	
	private String filetext;
	
	private LinkedList<ElasticWarehouseFile> embeddedFiles_ = new LinkedList<ElasticWarehouseFile>();

	private String parentId_ = null;
	private String Id_ = null;

	public String originSource_ = "";	//can be one of: "scan", "upload"
	private String originPath_ = "";
	private String originFilename_ = "";

	public String customkeywords_ = "";
	public String customcomments_ = "";

	//private Date thumbdate_ = null;
	
	public String getUploadedFilename() {
		return uploadedfilename_;
	}
	public String getOrginalFname() {
		return fname_;
	}
	public int getFolderLevel()
	{
		int fileoffset =1;
		String[] tkns = splitFolders();
		if( tkns.length == 0 )
			return 0+fileoffset;
		
		return tkns.length-1+fileoffset;
	}
	public String[] splitFolders()
	{
		String[] ret = targetfolder_.split("/");
		int i =0;
		for(String fname :  ret)
		{
			if( i == 0 )
			{
				ret[i] = "/" +  fname;
			}
			else
			{
				if( ret[i-1].endsWith("/") )
					ret[i] = ret[i-1] + fname;
				else
					ret[i] = ret[i-1] + "/" +  fname;
			}
			i++;
		}
		for(i=0;i<ret.length;i++)
		{
			if( ret[i].endsWith("/") == false && ret[i].equals("/") == false )
				ret[i]+="/";
		}
		LOGGER.debug("Generated folders: " + Arrays.asList(ret) );
		return ret;
	}
	
	public void addEmbeddedFile(ElasticWarehouseFile file)
	{
		embeddedFiles_.add(file);
	}
	public LinkedList<ElasticWarehouseFile> getEmbeddedFiles()
	{
		return embeddedFiles_;
	}
	
	public void statParse()
	{
		parsingStartTime_ = Calendar.getInstance().getTimeInMillis();
	}
	public void endParse()
	{
		parsingEndTime_ = Calendar.getInstance().getTimeInMillis();
		parsingTime_ = parsingEndTime_ - parsingStartTime_;
	}
	public void addMetaString(String key, String value) {
		metadata_.addMetaString(key,value);
	}
	
	public void addMetaLong(String key, long value) {
		metadata_.addMetaLong(key, value);
	}
	
	public void addMetaDate(String key, Date value) {
		metadata_.addMetaDate(key,value);
	}

	public void setTextContent(String value) {
		filetext = value;
		
	}

	public long getParsingTime() {
		return parsingTime_;
	}

	public XContentBuilder getJsonBinaryDataUploadBuilder() throws IOException {
		XContentBuilder builder = jsonBuilder();
		boolean storeContent = conf_.getWarehouseBoolValue(ElasticWarehouseConf.STORECONTENT, true);
		if( storeContent )
		{
			builder.startObject();
			builder.field("filecontent",binaryContent_);
		}
		builder.endObject();
		return builder;
	}
	
	public XContentBuilder getJsonSourceBuilder() throws IOException {
		Date today = Calendar.getInstance().getTime(); 
		MetaInfoCollection metacopy = metadata_.getCopy();
		XContentBuilder builder = jsonBuilder()
	         .startObject()
	         	 .field("folder", targetfolder_)
	         	 .field("folderna", (targetfolder_==null?targetfolder_:targetfolder_.toLowerCase()) )
	         	 .field("folderlevel", getFolderLevel() )
	         	 .field("isfolder", isfolder_)
	             .field("filename", fname_)
	             .field("filenamena", (fname_==null?fname_:fname_.toLowerCase()) )
	             .field("filesize", fsize_)
				 .field("filetext", filetext)
				 .field("customkeywords", customkeywords_)
				 .field("customcomments", customcomments_);
		
		//starting from version 1.2.1 we don't want to have filecontent in the same index as metadata
		//boolean storeContent = conf_.getWarehouseBoolValue(ElasticWarehouseConf.STORECONTENT, true);
		//if( storeContent )
		//{
		//	builder.field("filecontent",binaryContent_);
		//}
		String value = "";
		
		value = metacopy.getStringValueFor(ElasticWarehouseTikaMapper.FILETITLE, true);
		if( value.length()>0)
			builder.field("filetitle", value );
		value = metacopy.getStringValueFor(ElasticWarehouseTikaMapper.FILETYPE, true);
		if( value.length()>0)
			builder.field("filetype", value );
		value = metacopy.getDateValueFor(ElasticWarehouseTikaMapper.FILECREATIONDATE, true);
		if( value.length()>0)
			builder.field("filecreationdate", value );
		value = metacopy.getDateValueFor(ElasticWarehouseTikaMapper.FILEMODIFICATIONDATE, true);
		if( value.length()>0)
			builder.field("filemodificationdate", value );
	             //.field("filetitle", metadata_.get(Metadata.TITLE).valueString_)

		getMetaArray(builder, metacopy);
		
		if( getId() == null )
			builder.field("fileuploaddate", df.format(today) );
		else
		{
			if( fileuploaddate_ != null )
				builder.field("fileuploaddate", fileuploaddate_ );
			builder.field("filemodifydate", df.format(today) );
		}
		
		if( stats_.size() > 0 )
		{
			builder.startObject("stats");
			Iterator<Entry<String, Long>> it = stats_.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry<String, Long> pair = (Map.Entry<String, Long>)it.next();
		        builder.field(pair.getKey(), pair.getValue() );
		    }
	    	builder.endObject();
		}
		
		builder.startObject("origin");
		builder.field("source", originSource_ );
		builder.field("host", NetworkTools.getHostName2() );
		builder.field("node", conf_.getNodeName() );
		builder.field("path", originPath_ );
		builder.field("filename", originFilename_ );
		builder.endObject();
		
		if(parentId_ != null && parentId_.length()>0)
			builder.field("parentId", parentId_);
		
		
        
        //set image specific data
		String imagewidth = metacopy.getStringValueFor(ElasticWarehouseTikaMapper.IMAGEWIDTH, true);
		String imageheight = metacopy.getStringValueFor(ElasticWarehouseTikaMapper.IMAGEHEIGHT, true);
		String imagexresolution = metacopy.getStringValueFor(ElasticWarehouseTikaMapper.IMAGEXRESOLUTION, true);
		String imageyresolution = metacopy.getStringValueFor(ElasticWarehouseTikaMapper.IMAGEYRESOLUTION, true);
		
		String latitude = metacopy.getStringValueFor(ElasticWarehouseTikaMapper.GEOLAT, true);
		String longitude = metacopy.getStringValueFor(ElasticWarehouseTikaMapper.GEOLON, true);
		

		
		if( imagewidth != null && imagewidth.length()>0)
			builder.field("imagewidth", ParseTools.parseInt(imagewidth) );
		if( imageheight != null && imageheight.length()>0)
			builder.field("imageheight", ParseTools.parseInt(imageheight) );
		if( imagexresolution != null && imagexresolution.length()>0)
			builder.field("imagexresolution", ParseTools.parseInt(imagexresolution) );
		if( imageyresolution != null && imageyresolution.length()>0)
			builder.field("imageyresolution", ParseTools.parseInt(imageyresolution) );
		
		if( latitude != null && latitude.length()>0 &&
			longitude != null && longitude.length()>0 )
		{
			builder.startObject("location");
			builder.field("lat", ParseTools.parseFloat(latitude) );
			builder.field("lon", ParseTools.parseFloat(longitude) );
			builder.endObject();
		}
		
		//add thumbs
		//builder.startObject("filethumb");
		processThumb(builder);//, 90);
		//processThumb(builder, 180);
		//processThumb(builder, 360);
		//processThumb(builder, 720);
		//builder.endObject();
		
	    builder.endObject();
		return builder;
	}
	private void processThumb(XContentBuilder builder/*, int size*/) throws IOException
	{
		/*if( thumbs_.containsKey(size) == false)
			return;
		
		byte[] buffer = null;
		buffer = imageToBytes(thumbs_.get(size));
		if( buffer != null )
			builder.field("thumb"+size, buffer );
		*/
		if( fileThumb_ != null )
		{
			fileThumb_.processThumb(builder, df);
		}
		
	}
	
	private void getMetaArray(XContentBuilder builder, MetaInfoCollection metacopy) throws IOException
	{
		builder.startArray("filemeta");
		metacopy.getMetaArray(builder);
	    builder.endArray();

	}
	public void fill(Metadata metadata, String textContent) {
		//Metadata.CONTENT_TYPE
        for (String name : metadata.names())
        {
            String value = metadata.get(name);
            if (value != null)
            { 
                Date valueDate = ParseTools.isDate(value);
                if( valueDate != null )
                {
                	addMetaDate(name, valueDate);
                }
                else
                {
                	Long valueLong = ParseTools.isLong(value);
                	if( valueLong != null )
                	{
                		addMetaLong(name, valueLong);
                	}else{
                		addMetaString(name, value);
                	}
                }
            
                //System.out.println("Metadata Name:  " + name);
                //System.out.println("Metadata Value: " + value);
                //System.out.println("Meta: " + name + " => " + value);
            }
        }
        
        setTextContent(textContent);
        		
        //genearate thumbs
        if( binaryContent_ != null )
        {
	        int metaimagewidth = 0;
	        int metaimageheight = 0;
	        String v1 = metadata_.getStringValueFor(ElasticWarehouseTikaMapper.IMAGEWIDTH, false);
	        if( v1 != null && v1.length()>0 )
	        {
	        	Integer i1 = ParseTools.parseInt(v1);
	        	if( i1 != null )
	        		metaimagewidth = i1; 
	        }
	        v1 = metadata_.getStringValueFor(ElasticWarehouseTikaMapper.IMAGEHEIGHT, false);
	        if( v1 != null && v1.length()>0)
	        {
	        	Integer i1 = ParseTools.parseInt(v1);
	        	if( i1 != null )
	        		metaimageheight = ParseTools.parseInt(v1);
	        }
	        
	        if( metaimageheight >0 && metaimagewidth > 0 )	//otheriwse it's not an image
	        {
				int thumbsize = 180;
				thumbsize = conf_.getWarehouseIntValue(ElasticWarehouseConf.THUMBSIZE, thumbsize);
				if( thumbsize != 90 && thumbsize != 180 && thumbsize != 360 && thumbsize != 720)
					thumbsize = 180;
				fileThumb_ = FileTools.generateThumb(binaryContent_, metaimagewidth, metaimageheight, thumbsize);
		        
		        //override meta width and height by the real values (exif sometimes may return wrong values)
				if( metaimagewidth != fileThumb_.orginalw )
					metadata_.addMetaString(ElasticWarehouseTikaMapper.TIKAIMAGEWIDTH, new Integer(fileThumb_.orginalw).toString());
				if( metaimageheight != fileThumb_.orginalh )
					metadata_.addMetaString(ElasticWarehouseTikaMapper.TIKAIMAGEHEIGHT, new Integer(fileThumb_.orginalh).toString());
		
				//thumbavailable_ = thumbdata.thumbavailable_;
				//sameasimage = thumbdata.sameasimage;
				//thumbdate_ =  thumbdata.thumbdate_;
				//thumb_ =  thumbdata.thumb_;
	        }
        }
		//public int orginalw =  = thumbdata.sameasimage;
		//public int orginalh =  = thumbdata.sameasimage;
		//public String error_ =  = thumbdata.sameasimage;
		
	}
	
	
	    
	public void setParentId(String id) {
		parentId_  = id;
	}
	
	public String getId() {
		return Id_;
		
	}
	public void setId(String id) {
		Id_ = id;
	}
	public void setTypeFolder(boolean b) {
		isfolder_ = b;
	}

	public void setStat(String key, long factor) {
		stats_.put(key, factor);
	}

	public void originSetSource(String source) {
		originSource_  = source;
	}

	public void originSetPath(String originpath) {
		originPath_ = originpath;
	}

	public void originSetFilename(String originfilename) {
		originFilename_  = originfilename;
	}

	public void setTargetFolder(String currentfolder) {
		targetfolder_ = currentfolder;
		postProcessTargetFolder();
	}

	
}
