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

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticwarehouse.core.graphite.DDRFileNotFoundException;
import org.elasticwarehouse.core.graphite.DataSourceNotFoundException;
import org.elasticwarehouse.core.graphite.ElasticSearchMonitor;
import org.elasticwarehouse.core.graphite.GrapFunction;
import org.elasticwarehouse.core.graphite.MonitoringManager;
import org.elasticwarehouse.core.graphite.NetworkTools;
import org.elasticwarehouse.core.graphite.PerfMon;
import org.elasticwarehouse.core.graphite.PerformanceMonitor;
import org.elasticwarehouse.core.graphite.TimeSample;


public class ElasticWarehouseAPIProcessorGraphite extends ElasticWarehouseAPIProcessor {

	private PerformanceMonitor monitor_ = null;
	private ElasticSearchMonitor esmonitor_ = null;
	private ElasticWarehouseReqRespHelper responser = new ElasticWarehouseReqRespHelper();
	
	//Pattern patternScale = Pattern.compile("(scale)\\((\\w.*),[ ]*([0-9.]+)\\)");
	//Pattern patternAlias = Pattern.compile("(alias)\\((\\w.*),[ ]*([a-zA-Z0-9.-_\\ ]+)\\)");
	Pattern patternFunction = Pattern.compile("([a-zA-Z]+)\\((.*),[ ]*([^)]+)\\)");
	private ElasticWarehouseConf conf_ = null;
	
	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseAPIProcessorGraphite.class.getName()); 
	
	public ElasticWarehouseAPIProcessorGraphite(ElasticWarehouseConf conf, ElasticSearchAccessor elasticSearchAccessor) throws MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, NullPointerException, ReflectionException, IOException, ParseException
	{
		conf_  = conf;
		monitor_ = new PerformanceMonitor(conf, true);
		esmonitor_ = MonitoringManager.createElasticSearchMonitor(conf, true, elasticSearchAccessor);
	}
	public boolean processRequest(Client esClient_, OutputStream os, HttpServletRequest request) throws IOException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, NullPointerException, ReflectionException, ParseException, DDRFileNotFoundException 
	{
		String reqmethod = request.getMethod();	//GET,POST, etc
		if( reqmethod.equals("GET") )
		{
			if( request.getPathInfo().equals("/render") || request.getPathInfo().startsWith("/_ewgraphite") )
			{
				//LinkedList<String> targets = new LinkedList<String>();
				String[] target = null;
				String from = "";
				String until = "";
				String format = "";
				String maxDataPoints = "";
				if( request.getParameter("target") != null )
					target = request.getParameterValues("target");
				if( request.getParameter("from") != null )
					from = request.getParameter("from").toLowerCase();
				if( request.getParameter("until") != null )
					until = request.getParameter("until").toLowerCase();
				if( request.getParameter("format") != null )
					format = request.getParameter("format").toLowerCase();
				if( request.getParameter("maxDataPoints") != null )
					maxDataPoints = request.getParameter("maxDataPoints").toLowerCase();
				
				if( !format.equals("json") )
				{
					os.write(responser.errorMessage("Graphipte API supports JSON format only.", ElasticWarehouseConf.URL_GUIDE_GRAPHITE));
				}
				else
				{
					try
					{
						XContentBuilder builder = jsonBuilder();
						
						LinkedList<TimeSample> samples = null; 
						if( target == null || target.length == 0 /* .equals("")*/ )
						{
							builder.startArray().endArray();							
						}
						else
						{
							builder.startArray();
							for(String tgt : target)
							{
								Stack<GrapFunction> functions = new Stack<GrapFunction>();
								String alias = tgt;
								/*if( tgt.equals("randomWalk('random walk')") ||
									tgt.equals("randomWalk(random walk)") )
								{
									samples = monitor_.fetchRandomSamplesToRender(from, until, format, Integer.parseInt(maxDataPoints));
								}
								else
								{*/
									String pureTgt = analyzeTarget(tgt, functions);
									String[] tokens = pureTgt.split("\\.");
									int imaxDataPoints = Integer.parseInt(maxDataPoints);
									try
									{
										if( tokens.length>=2 && tokens[1].startsWith(ElasticSearchMonitor.ES_FILE_PREFIX) )
										{
											samples = esmonitor_.fetchCountersToRender(pureTgt, from, until, format, -1, imaxDataPoints, true);
											alias = getAlias(tgt, functions);
											applyFunctions(samples, functions, esmonitor_, from, until, format, imaxDataPoints);
										}else{
											samples = monitor_.fetchCountersToRender(pureTgt, from, until, format, -1, imaxDataPoints, true);
											alias = getAlias(tgt, functions);
											applyFunctions(samples, functions, monitor_, from, until, format, imaxDataPoints);
										}
									}
									catch(DataSourceNotFoundException e)
									{
										LOGGER.warn(e.getMessage());
									}
									catch(DDRFileNotFoundException e)
									{
										LOGGER.warn(e.getMessage());
									}
								//}
								
								if(samples != null )
								{
									builder.startObject()
											.field("target", alias)
											.startArray("datapoints");
									for(TimeSample ts : samples)
									{
										if( Double.isNaN(ts.v) )
											builder.startArray().nullValue().value(ts.t).endArray();
										else
											builder.startArray().value(ts.v).value(ts.t).endArray();
									}
									builder.endArray().endObject();
								}
//								else
//								{
//									os.write(builder.string().getBytes() );
//								}
							}
							builder.endArray();
						}
						os.write(builder.string().getBytes() );
						
					}
					catch(IOException e)
					{
						EWLogger.logerror(e);
						os.write(responser.errorMessage("Cannot get datapoints: "+ e.getMessage(), ElasticWarehouseConf.URL_GUIDE_GRAPHITE));
					}
				}
			}
			else
			{
				String query = "";
				if( request.getParameter("query") != null )
					query = request.getParameter("query").toLowerCase();
				LOGGER.debug("Got:" +query);
				
				XContentBuilder builder = jsonBuilder();
				if( query.equals("") )
				{
					builder.startArray().endArray();
				}
				else if( query.equals("*") )
				{
					String hostname = conf_.getNodeName() /*NetworkTools.getHostName()*/ ;
					builder.startArray()
								.startObject()
									.field("leaf", 0)
										.startObject("context")
										.endObject()
									.field("text", hostname)
									.field("expandable", 1)
									.field("id", hostname)
									.field("allowChildren", 1)
								.endObject()
							.endArray();
					/*[
					   {
					      "leaf": 0,
					      "context": {},
					      "text": "NYCVFENTITLE1",
					      "expandable": 1,
					      "id": "NYCVFENTITLE1",
					      "allowChildren": 1
					   },]
					 * */
				}
				else if( query.length() > 1 )
				{
					String[] tokens = query.split("\\.");
					if( tokens.length == 2)
					{
						
						LinkedList<String> perftypes = monitor_.getAvailableTypes();
						LinkedList<String> customftypes = esmonitor_.getAvailableTypes();
						
						if( perftypes == null || customftypes == null)
						{
							responser.errorMessage(builder, "Cannot fetch types for given host:" + tokens[0], ElasticWarehouseConf.URL_GUIDE_GRAPHITE);
						}else{
							builder.startArray();
							LinkedList<String> types = new LinkedList<String>();
							types.addAll(perftypes);
							types.addAll(customftypes);
							
							for(String type : types)
							{
								builder.startObject()
									.field("leaf", 0)
										.startObject("context")
										.endObject()
									.field("text", type)
									.field("expandable", 1)
									.field("id", tokens[0]+"."+type)
									.field("allowChildren", 1)
								.endObject();
							}
							builder.endArray();
						}
						
					}
					if( tokens.length == 3)
					{
						
						LinkedList<String> attributes = null;
						if( tokens[1].startsWith(ElasticSearchMonitor.ES_FILE_PREFIX) )
							attributes = esmonitor_.getAvailableAttributes(tokens[1], true);
						else
							attributes = monitor_.getAvailableAttributes(tokens[1], true);
						if( attributes == null )
						{
							responser.errorMessage(builder, "Cannot fetch attributes for given type: " + tokens[1], ElasticWarehouseConf.URL_GUIDE_GRAPHITE);
						}else{
							builder.startArray();
							for(String attr : attributes)
							{
								builder.startObject()
									.field("leaf", 1)
										.startObject("context")
										.endObject()
									.field("text", attr)
									.field("expandable", 0)
									.field("id", tokens[0]+"."+tokens[1]+"."+attr)
									.field("allowChildren", 0)
								.endObject();
							}
							builder.endArray();
						}
						
					}
				}
			
				os.write(builder.string().getBytes() );
			} 
		}else{
			os.write(responser.errorMessage("Graphipte API expects GET requests only.", ElasticWarehouseConf.URL_GUIDE_GRAPHITE));
		}
		//monitor.
		return true;
	}
	private String getAlias(String tgt, Stack<GrapFunction> functions) {
		for(GrapFunction f : functions)
		{
			if( f.isAliasFunction() )
				return f.fparameter.replace("'", "");
		}
		return tgt;
	}
	private void applyFunctions(LinkedList<TimeSample> samples,
			Stack<GrapFunction> functions, PerfMon monitor, String from, String until, String format, int imaxDataPoints) throws IOException, ParseException, DataSourceNotFoundException, DDRFileNotFoundException
	{
		while(!functions.empty())
		{
			GrapFunction f = functions.pop();
			LinkedList<TimeSample> refsamples = null;
			//System.out.println("Apply: "+ f);
			if( f.isReferenceSamplesNeeded() )
			{
				int minSamplesCount = samples.size();
				refsamples = monitor.fetchCountersToRender(f.fparameter, from, until, format, minSamplesCount, imaxDataPoints, true);
			}
			
			for(int i=0;i<samples.size();i++)
			{
				TimeSample refsample = null;
				if( refsamples != null && refsamples.size()>i)
					refsample = (refsamples==null?null:refsamples.get(i));
				f.apply(samples.get(i), refsample );					
			}
		}
	}
	private String analyzeTarget(String target, Stack<GrapFunction> functions)
	{
		Pattern[] patterns = new Pattern[] { patternFunction };
		String pureTarget = processPattern(patterns, target, functions);
		LOGGER.debug("Working on target: "+ pureTarget);
		return pureTarget;
	}
	private String processPattern(Pattern[] patterns, String target, Stack<GrapFunction> functions)
	{
		String pureTarget = null;
		for(int i=0;i<patterns.length;i++)
		{
			Matcher matcher = patterns[i].matcher(target);
			pureTarget = target;
			while(matcher.find())
			{
				int cnt = matcher.groupCount();
				if( cnt >= 2 )
				{
					//functions.add(e)
					//System.out.println(matcher.groupCount());
					String fname = matcher.group(1);
					String fparameter = "";
					if( cnt == 3 )
						fparameter = matcher.group(3);
					String innerString = matcher.group(2);
					functions.push( new GrapFunction(fname, fparameter) );
					
					//System.out.println(fname + ":"+ fparameter + " => " + innerString);
					matcher = patterns[i].matcher(innerString);
					pureTarget = innerString;
				}
			}
		}
		return pureTarget;
	}

}
