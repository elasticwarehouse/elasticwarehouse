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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.elasticwarehouse.core.EWLogger;


public class FileTools {

	public static LinkedList<FileDef> scanFolder(String path, List<String> excluded_extenstions, boolean isrecurrence)
	{
		if( isrecurrence )
		{
			File folder = new File(path);
			String[] directories = folder.list(new FilenameFilter() {
			  public boolean accept(File current, String name) {
				  File f = new File(current, name);
			    return f.isDirectory() && name.startsWith(".") == false;
			  }
			});
			
			if( directories.length == 0 )
			{
				return scanFolder(path, excluded_extenstions);
			}
			else
			{
				LinkedList<FileDef> fulllist =  new LinkedList<FileDef>();
				for(int i=0;i<directories.length;i++)
				{
					LinkedList<FileDef> ret = scanFolder(path+"/"+directories[i], excluded_extenstions, isrecurrence);
					fulllist.addAll(ret);
				}
				LinkedList<FileDef> ret2 = scanFolder(path, excluded_extenstions);
				fulllist.addAll(ret2);
				return fulllist;
			}
		}else{
			return scanFolder(path, excluded_extenstions);
		}
	}
	
	public static LinkedList<FileDef> scanFolder(String path, List<String> excluded_extenstions)
	{
		LinkedList<FileDef> ret = new LinkedList<FileDef>();
		File folder = new File(path);
		
		File[] listOfFiles = folder.listFiles();
		if( listOfFiles == null )
			return ret;
		//return listOfFiles;
		for(File file : listOfFiles)
		{
			if (file.isFile() )
			{
				String fname = file.getName();
				boolean exclude = false;
				for(String excludeext : excluded_extenstions)
				{
					if( fname.endsWith("."+excludeext) )
					{
						exclude = true;
						break;
					}
				}
				if( !exclude )
					ret.add(new FileDef(file.getName(), file.getParent() ) );
			}
		}
		
		return ret;
	}
	
	public static boolean checkFileCanRead(String pathname){
		File file = new File(pathname);
	    if (!file.exists()) 
	        return false;
	    if (!file.canRead())
	        return false;
	    return true;
	}

	public static boolean folderWritable(String folderpath) {
		File file = new File(folderpath);
		if (!file.exists()) 
	        return false;
		if (!file.canWrite()) 
	        return false;
		return true;
	}

	public static void writeBytes(String pathname, byte[] binaryContent) throws IOException {
		FileOutputStream fos = new FileOutputStream(pathname);
		fos.write(binaryContent);
		fos.close();
		
	}

	public static FileThumb generateThumb(byte[] binaryContent,int metaimagewidth, int metaimageheight, int thumbsize) {
		FileThumb ret = new FileThumb();
		
        if( binaryContent != null )
        {
        	//String ftype = metadata_.getStringValueFor(ElasticWarehouseTikaMapper.FILETYPE, false);
    		
        	try {
				BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(binaryContent) );
				if( originalImage != null )
				{
					int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
					
					//int metaimagewidth = ParseTools.parseInt(metadata_.getStringValueFor(ElasticWarehouseTikaMapper.IMAGEWIDTH, false));
					//int metaimageheight = ParseTools.parseInt(metadata_.getStringValueFor(ElasticWarehouseTikaMapper.IMAGEHEIGHT, false));
					
					ret.orginalw = originalImage.getWidth();
					ret.orginalh = originalImage.getHeight();
					
					//int thumbsize = 180;
					//thumbsize = conf_.getWarehouseIntValue(ElasticWarehouseConf.THUMBSIZE, thumbsize);
					//if( thumbsize != 90 && thumbsize != 180 && thumbsize != 360 && thumbsize != 720)
					//	thumbsize = 180;
					
					
					
					if( ret.orginalw <= thumbsize )
					{
						ret.sameasimage = true;
					}else{
						ret.sameasimage = false;
						ret.thumb_ = resizeImageWithHint(originalImage, type, thumbsize, ret.orginalh * thumbsize / ret.orginalw);
					}
					ret.thumbavailable_ = true;
					ret.thumbdate_  = Calendar.getInstance().getTime();
					/*thumbs_.put(90, resizeImageWithHint(originalImage, type, 90, h*90/w));
					if( w>=180 )
						thumbs_.put(180, resizeImageWithHint(originalImage, type, 180, h*180/w));
					if( w>=360 )
						thumbs_.put(360, resizeImageWithHint(originalImage, type, 360, h*360/w));
					if( w>=720 )
						thumbs_.put(720, resizeImageWithHint(originalImage, type, 720, h*720/w));
						*/
				}
			} catch (IOException e) {
				EWLogger.logerror(e);
				e.printStackTrace();
				ret.error_ = e.getMessage();
			} catch( RuntimeException e) {	//i.e. when New BMP version not implemented yet. exception occurs
				EWLogger.logerror(e);
				ret.error_ = e.getMessage();
			}
        }
        return ret;
	}
	
	@SuppressWarnings("unused")
	private static BufferedImage resizeImage(BufferedImage originalImage, int type, int IMG_WIDTH, int IMG_HEIGHT){
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
		g.dispose();
	 
		return resizedImage;
    }
	 
    private static BufferedImage resizeImageWithHint(BufferedImage originalImage, int type, int IMG_WIDTH, int IMG_HEIGHT){
	 
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
		g.dispose();	
		g.setComposite(AlphaComposite.Src);
	 
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,	RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,		RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,		RenderingHints.VALUE_ANTIALIAS_ON);
	 
		return resizedImage;
    }

	public static boolean makeFolder(String folder)
	{
		boolean result = false;
		File theDir = new File(folder);
		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    //System.out.println("creating directory: " + folder);
		    try{
		        theDir.mkdirs();
		        result = true;
		    } 
		    catch(SecurityException se){
		    	EWLogger.logerror(se);
		    }        
		}
		return result;
	}	
	
	public static boolean copy(String sourcefile, String targetFile)
	{
		boolean ret = false;
		try {
			FileUtils.copyFile(new File(sourcefile), new File(targetFile));
			ret = true;
		} catch (IOException e1) {
			EWLogger.logerror(e1);
			e1.printStackTrace();
		}
		
		return ret;
		
		/*
		boolean ret = false;
	    	InputStream inStream = null;
		OutputStream outStream = null;
			
	    	try{
	    		
	    	    File afile =new File(sourcefile);
	    	    File bfile =new File(targetFile);
	    		
	    	    inStream = new FileInputStream(afile);
	    	    outStream = new FileOutputStream(bfile);
	        	
	    	    byte[] buffer = new byte[1024];
	    		
	    	    int length;
	    	    //copy the file content in bytes 
	    	    while ((length = inStream.read(buffer)) > 0){
	    	  
	    	    	outStream.write(buffer, 0, length);
	    	 
	    	    }
	    	 
	    	    inStream.close();
	    	    outStream.close();
	    	    
	    	    ret = true;
	    	    
	    	}catch(IOException e){
	    		e.printStackTrace();
	    	}
	    	
	    	return ret;*/
	}

	public static boolean delete(String filename) {
		File f = new File(filename);
		return f.delete();
	}

	public static String generateNewFilename(String filename) {
		String ret = null;
		File f = new File(filename);
		String basefilename = f.getName();
		String basefilenameWithoutExt = FilenameUtils.removeExtension(basefilename);
		
		//remove follwoign numbers if any
		//basefilename = basefilename.replaceAll(".[0-9]*$", "");
		
		int cnt = 1;
		while(true)
		{
			String path = f.getParent();
			String ftestname = path +"/"+basefilenameWithoutExt+"."+cnt+".rrd";
			File ftest = new File(ftestname);
			if( ftest.exists() == false )
			{
				//if( cnt > 1 )
				//{
				ret = ftestname;
				//}
				break;
			}
			cnt++;
		}
		return ret;
	}

	public static String getLatestFilenameCopy(String filename) {
		String ret = null;
		File f = new File(filename);
		String basefilename = f.getName();
		String basefilenameWithoutExt = FilenameUtils.removeExtension(basefilename);
		int cnt = 1;
		while(true)
		{
			String path = f.getParent();
			String ftestname = path +"/"+basefilenameWithoutExt+"."+cnt+".rrd";
			File ftest = new File(ftestname);
			if( ftest.exists() == false )
			{
				if( cnt > 1)
				{
					int cntprev = cnt -1;
					ret = path +"/"+basefilenameWithoutExt+"."+cntprev+".rrd";
				}
				break;
			}
			cnt++;
		}
		return ret;
	}
}
