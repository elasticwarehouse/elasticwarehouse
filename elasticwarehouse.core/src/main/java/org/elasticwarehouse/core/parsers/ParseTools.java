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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ParseTools {
	
	public static DateFormat[] dateformats = { new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") /*2009-02-23 18:04:40Z*/,
		 new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") /*2009-02-23T18:04:40Z*/,
		 new SimpleDateFormat("EEE MMM dd kk:mm:ss zzz yyyy"), /*Thu Sep 28 20:29:30 JST 2000*/
		 new SimpleDateFormat("EEE MMM dd kk:mm:ss yyyy"), /*Fri Jul 15 17:50:51 2011*/
		 new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"), /*27-09-1991 20:29:30*/
		 new SimpleDateFormat("dd.MM.yyyy HH:mm:ss") /*27.09.1991 20:29:30*/
	   };

	
	private static Pattern patternPasth = Pattern.compile("-(\\d*)h");
	private static Pattern patternPastd = Pattern.compile("-(\\d*)d");
	private static Pattern patternFuture = Pattern.compile("\\+(\\d*)h");
	
	public static Date isDate(String target)
	{
		if( target == null )
			return null;
		java.util.Date result = null;
        //int pos =0;
        for(DateFormat df : dateformats)
        {
        	df.setLenient(false);
        	try
            {
        		result =  df.parse(target);
        		//System.out.println(result + "[used format:"+pos+"]");
        		break;
            } catch (ParseException pe) {
            	//EWLogger.logerror(pe);
                //pe.printStackTrace();
            }
        	//pos++;
        }
		return result;
	}
	
	public static Long isLong(String value)
	{
		Long longValue = null;
		try
		{
			longValue = Long.parseLong(value);
		}
		catch(NumberFormatException e)
		{
			//EWLogger.logerror(e);
			longValue = null;
			//String target = "27-09-1991 20:29:30";2009-02-23T18:04:40Z
			//DateFormat df = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ssZ");
			//Date result =  df.parse(target);
		}
		return longValue;
	}
	
	public static Integer parseInt(String myString)
	{
		Integer ret = null;
		//String myString = "1280 px";
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(myString); 
        while (m.find()) {
        	//System.out.println("num:"+m.group());
        	ret = Integer.parseInt(m.group());
        }
        return ret;
	}
	public static Float parseFloat(String myString) {
		if( myString == null )
			return 0.0f;
		if( myString.length()==0)
			return 0.0f;
		
		Float ret = 0.0f;
		try
		{
			ret = Float.parseFloat(myString);
		}
		catch(NumberFormatException e)
		{
			//EWLogger.logerror(e);
			ret = 0.0f;
		}
		return ret;
	}
	
	public static int parseIntDirect(String myString, int defaultValue)
	{
		Integer ret = null;
		try
		{
			ret = Integer.parseInt(myString);
		}catch(java.lang.NumberFormatException e)
		{
			//EWLogger.logerror(e);
		}
		if(ret == null)
			return defaultValue;
		else
			return ret;
	}

	public static long convertToTime(String value) 
	{
		Long ret = 0L;
		try
		{
			ret = Long.parseLong(value);
		}
		catch(NumberFormatException e)
		{
			//EWLogger.logerror(e);
			ret = 0L;
		}
		
		if( ret > 0 )
			return ret;
		
		if( value.equals("now") )
			return System.currentTimeMillis()/1000;
		
		
		Matcher matcher = null;
		
		matcher = patternFuture.matcher(value);
		if (matcher.find())
			return (System.currentTimeMillis()/1000) + (Long.parseLong(matcher.group(1).toString())*60*60);
		
		matcher = patternPasth.matcher(value);
		if (matcher.find())
			return (System.currentTimeMillis()/1000) - (Long.parseLong(matcher.group(1).toString())*60*60); 
		
		matcher = patternPastd.matcher(value);
		if (matcher.find())
			return (System.currentTimeMillis()/1000) - (Long.parseLong(matcher.group(1).toString())*60*60*24); 
		
		return -1;
	}

	public static boolean parseBooleanDirect(String myString, boolean defaultValue)
	{
		Boolean ret = null;
		try
		{
			ret = Boolean.parseBoolean(myString);
		}catch(java.lang.NumberFormatException e)
		{
			//EWLogger.logerror(e);
		}
		if(ret == null)
			return defaultValue;
		else
			return ret;
	}


}
