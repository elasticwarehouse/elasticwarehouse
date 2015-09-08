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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class ResourceTools {

	private final static Logger LOGGER = Logger.getLogger(ResourceTools.class.getName()); 
	
	public static Collection<String> getResources(
	        final Pattern pattern){
	        final ArrayList<String> retval = new ArrayList<String>();
	        final String classPath = System.getProperty("java.class.path", ".");
	        final String[] classPathElements = classPath.split(":");
	        for(final String element : classPathElements){
	            retval.addAll(getResources(element, pattern));
	        }
	        return retval;
	    }

	    private static Collection<String> getResources(
	        final String element,
	        final Pattern pattern){
	        final ArrayList<String> retval = new ArrayList<String>();
	        final File file = new File(element);
	        if(file.isDirectory()){
	            retval.addAll(getResourcesFromDirectory(file, pattern));
	        } else{
	            retval.addAll(getResourcesFromJarFile(file, pattern));
	        }
	        return retval;
	    }
	    
	    private static Collection<String> getResourcesFromJarFile(
	            final File file,
	            final Pattern pattern){
	            final ArrayList<String> retval = new ArrayList<String>();
	            ZipFile zf;
	            try{
	                zf = new ZipFile(file);
	            } catch(final ZipException e){
	                throw new Error(e);
	            } catch(final IOException e){
	                throw new Error(e);
	            }
	            final Enumeration e = zf.entries();
	            while(e.hasMoreElements()){
	                final ZipEntry ze = (ZipEntry) e.nextElement();
	                final String fileName = ze.getName();
	                final boolean accept = pattern.matcher(fileName).matches();
	                if(accept){
	                    retval.add(fileName);
	                }
	            }
	            try{
	                zf.close();
	            } catch(final IOException e1){
	                throw new Error(e1);
	            }
	            return retval;
	        }

	        private static Collection<String> getResourcesFromDirectory(
	            final File directory,
	            final Pattern pattern){
	            final ArrayList<String> retval = new ArrayList<String>();
	            final File[] fileList = directory.listFiles();
	            for(final File file : fileList){
	                if(file.isDirectory()){
	                    retval.addAll(getResourcesFromDirectory(file, pattern));
	                } else{
	                    try{
	                        final String fileName = file.getCanonicalPath();
	                        final boolean accept = pattern.matcher(fileName).matches();
	                        if(accept){
	                            retval.add(fileName);
	                        }
	                    } catch(final IOException e){
	                        throw new Error(e);
	                    }
	                }
	            }
	            return retval;
	        }
	public static String getTextFileContent(String resourceFile)
	{
		java.net.URL resourceId = ResourceTools.class.getResource(resourceFile);

        if( resourceId == null )
        	LOGGER.info(resourceFile + " fileResource is null");
        else
        	LOGGER.debug(resourceFile + " reading...");

        //java.nio.file.Path resPath;
        String fileContent = null;
		try {
			//System.out.println(resourceId.toURI());
			InputStream in = ResourceTools.class.getClass().getResourceAsStream(resourceFile);
			byte[] data = IOUtils.toByteArray(in);
			//resPath = java.nio.file.Paths.get(resourceId.toURI());
			fileContent = new String(data, "UTF-8");//new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
			//System.out.println(fileContent);
		/*} catch (URISyntaxException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			EWLogger.logerror(e);
			e.printStackTrace();*/
		} catch (IOException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
		} 
		return fileContent;
	}
	
	public static String preprocessFolderName(String infolder)
	{
		String folder = infolder.toLowerCase();
		folder = folder.replaceAll("[\\\\]+", "/");
		folder = folder.replaceAll("[/]+", "/");
		folder = folder.replaceAll("^([a-z]+):", "/$1");	//for Windows C:/, D:/ etc.
		//while( folder.endsWith("/") )
		//	folder = folder.substring(0,folder.length()-1);
		if( !folder.startsWith("/") )
			folder = "/"+folder;
		if( folder.endsWith("/") == false && folder.endsWith("*")==false )
			folder = folder+"/";
		
		return folder;
	}
}
