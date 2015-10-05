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
package org.elasticwarehouse.core.graphite;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

import org.apache.log4j.Logger;
import org.elasticwarehouse.core.AtrrValue;
import org.elasticwarehouse.core.ElasticWarehouseConf;
import org.elasticwarehouse.core.parsers.ParseTools;


public class PerformanceMonitor extends PerfMon
{ 
    //private int  availableProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    //private long lastSystemTime      = 0;
    //private long lastProcessCpuTime  = 0;

    private MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    private HashMap<String, String> availablePerformaceTypes_ = new HashMap<String, String>();	// short type name => long type name
    //private HashMap<String, String> storedPerformaceTypes_ = new HashMap<String, String>();		// short type name => long type name
    
    private HashMap<String, RRDManager> ddrManagers_ = new HashMap<String, RRDManager>();
	//private ElasticSearchAccessor elasticSearchAccessor_;
    private final static Logger LOGGER = Logger.getLogger(PerformanceMonitor.class.getName()); 
    
    public PerformanceMonitor(ElasticWarehouseConf conf, boolean readOnly/*, ElasticSearchAccessor elasticSearchAccessor*/) throws MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, NullPointerException, ReflectionException, IOException, ParseException
    {
    	super(conf);
    	String folder = getStorageFolder();
    			
    	
    	Set<ObjectName> names =  mbs.queryNames(null, null);
    	//System.out.println(names);
        java.util.Iterator<ObjectName> it = names.iterator();
        while(it.hasNext())
        {
        	
        	ObjectName obj = it.next();
        	String typeFullName = obj.toString();
        	String typeShortName = getUniqueTypeNameString(typeFullName);
        	if( availablePerformaceTypes_.containsKey(typeShortName.toLowerCase()))
        		throw new ParseException("Type: " + typeShortName.toLowerCase() + " is duplicated, cannot build performance types list (fullname:"+typeFullName+")",0);
        	        	
        	LinkedList<String> attributes = getAvailableAttributesByLongName(typeFullName/*typeShortName.toLowerCase()*/, true);
        	if( attributes != null )
        	{
	        	LOGGER.info("PerfMon: "+typeFullName +"   =>   " + typeShortName + " ("+attributes.size() +" attributes)");
	        	if( attributes.isEmpty() )
	        	{
	        		LOGGER.info("Cannot get attributes for type: " + typeFullName +" ("+typeShortName.toLowerCase()+")" );
	        	}
	        	else
	        	{
	        		availablePerformaceTypes_.put(typeShortName.toLowerCase(), typeFullName);
	        		try
	        		{
	        			ddrManagers_.put(typeShortName.toLowerCase(), new RRDManager(folder+"/"+typeShortName.toLowerCase()+".rrd", attributes, true, readOnly, true, conf.getTempFolder() ) );
	        		}
	        		catch(DDRFileNotFoundException e)
	        		{
	        			if( readOnly )
	        				LOGGER.warn("DB file doesn't exist, will be created later when next requests occur. " + e.getMessage());
	        			else
	        				throw new IOException(e.getMessage());
	        		}
	        	}
        	}
        	//System.out.println(obj.toString());
        	/*try {
				try {
					WriteAttributes(mbs, obj);
				} catch (InstanceNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ReflectionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IntrospectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
        }
        
        //availablePerformaceTypes_.put(CUSTOM_ES_TYPE, CUSTOM_ES_TYPE);
        //LinkedList<String> attributes = getAvailableAttributes(CUSTOM_ES_TYPE.toLowerCase(), true);
        //ddrManagers_.put(CUSTOM_ES_TYPE.toLowerCase(), new RRDManager(folder+"/"+CUSTOM_ES_TYPE.toLowerCase()+".rrd", attributes, true, readOnly) );
    }
    
	/*
	 * 	java.lang:type=Memory
		java.lang:type=GarbageCollector,name=Copy
		java.lang:type=Runtime
		java.nio:type=BufferPool,name=direct
		java.lang:type=ClassLoading
		java.lang:type=MemoryManager,name=J9 non-heap manager
		java.lang:type=Threading
		java.nio:type=BufferPool,name=mapped
		java.util.logging:type=Logging
		java.lang:type=Compilation
		java.lang:type=MemoryPool,name=JIT data cache
		java.lang:type=MemoryPool,name=class storage
		java.lang:type=GarbageCollector,name=MarkSweepCompact
		java.lang:type=MemoryPool,name=JIT code cache
		java.lang:type=MemoryPool,name=Java heap
		java.lang:type=MemoryPool,name=miscellaneous non-heap storage
		java.lang:type=OperatingSystem
		JMImplementation:type=MBeanServerDelegate
	 * */
    
    

	public synchronized Object getPerformanceCounter(String shortTypeName, String attributeName/*, boolean isCompositeExpandedAttribute*/) throws MalformedObjectNameException, ReflectionException, InstanceNotFoundException 
    {
    	//if( shortTypeName.equals(CUSTOM_ES_TYPE) )
    	//{
    	//	return null;
    	//}
    	//else
    	//{
	        String longTypeName = availablePerformaceTypes_.get(shortTypeName);
	        ObjectName name    = ObjectName.getInstance(longTypeName/*"java.lang:type=OperatingSystem"*/);
	        javax.management.AttributeList list = mbs.getAttributes( name, new String[]{ attributeName/*"ProcessCpuLoad"*/ });
	
	        if (list.isEmpty())     return Double.NaN;
	
	        Attribute att = (Attribute)list.get(0); ///????????
	        return att.getValue();
    	//}
    }
    
    @Override
    public synchronized LinkedList<TimeSample> fetchCountersToRender(String target /*i.e. host.cpu.loadavgsec */, String from, String until, 
    		String format, int minSamplesCount, int maxDataPoints, boolean refreshBeforeFetch) throws IOException, ParseException, DataSourceNotFoundException, DDRFileNotFoundException
    {
    	if( target.equals("randomWalk('random walk')") ||
    			target.equals("randomWalk(random walk)") || 
    			target.equals("randomWalk('random walk'") ||
    			target.equals("randomWalk(random walk"))
		{
			return fetchRandomSamplesToRender(from, until, format, minSamplesCount, maxDataPoints);
		}
    	
    	String[] tokens = target.split("\\.");
    	if( tokens.length == 2 && tokens[1].toLowerCase().equals("select metric") )
    		return new LinkedList<TimeSample>();	//return empty list
    	
		if( tokens.length != 3)
			throw new IOException("target is wrong, 3 tokens expected, but got:" + target);
		
		String uniqType = tokens[1].toLowerCase();
		String metric = tokens[2];
		
		if( ddrManagers_.get(uniqType) == null )
		{
			ArrayList<String> availableTypes = new ArrayList<String>(ddrManagers_.keySet());
			throw new DDRFileNotFoundException("DDR database not found for:" + uniqType+ ". Available types are:" + availableTypes.toString());
		}
		
		if( refreshBeforeFetch )
		{
			ddrManagers_.get(uniqType).refresh();
		}
		
		return ddrManagers_.get(uniqType).exportGraphite(metric, from, until, format, maxDataPoints);
    	
    }
    public synchronized double convertPerformanceCounterToDouble(Object obj, boolean asPercent)
    {
    	Double value = Double.NaN;
    	if( obj == null )
		{
			
		}else if (obj instanceof Long) {
			value = Double.parseDouble( ((Long)obj).toString() );
	    }else if (obj instanceof Integer) {
			value = Double.parseDouble( ((Integer)obj).toString() );
		}
		else if (obj instanceof Double) {
			value = (Double)obj;
		}
		else if (obj instanceof Boolean) {
			Boolean boolValue = (Boolean)obj;
			if( boolValue )
				value = 1.0;
			else
				value = 0.0;
		}
		else if (obj instanceof java.lang.String) {
			//monitor.saveCounter(type, attr, monitor.getPerformanceCounterAsDouble(factor, false));
			LOGGER.debug("String counters are not supported");// " +type +" -> " + attr);
		}
		else if (obj instanceof java.lang.String[] ) {
			//monitor.saveCounter(type, attr, monitor.getPerformanceCounterAsDouble(factor, false));
			LOGGER.debug("String array counters are not supported");// " +type +" -> " + attr);
		}
		else if (obj instanceof long[] ) {
			//monitor.saveCounter(type, attr, monitor.getPerformanceCounterAsDouble(factor, false));
			LOGGER.debug("Long array counters are not supported");// " +type +" -> " + attr);
		}
		else if( obj instanceof javax.management.openmbean.TabularDataSupport ) {
			//javax.management.openmbean.TabularDataSupport tbs = (TabularDataSupport) obj;
			LOGGER.debug("TabularDataSupport counters are not supported");// " +type +" -> " + attr);
		}
		else {
			LOGGER.error("Unknown type: " + obj.toString() + ", type:" + obj.getClass());
		}
    	
	    if (value == -1.0)      return Double.NaN;  	// usually takes a couple of seconds before we get real values
	
	    if( asPercent )
	    	return ((int)(value * 1000) / 10.0);        // returns a percentage value with 1 decimal point precision
	    else
	    	return value;
    }
    
    /*public synchronized double getPerformanceCounterAsDouble(Object obj, boolean asPercent)
    {
	    Double value  = (Double)obj;
	    if (value == -1.0)      return Double.NaN;  // usually takes a couple of seconds before we get real values
	
	    if( asPercent )
	    	return ((int)(value * 1000) / 10.0);        // returns a percentage value with 1 decimal point precision
	    else
	    	return value;
    }
    public synchronized double getPerformanceCounterAsLong(Object obj, boolean asPercent)
    {
	    Long value  = (Long)obj;	
	    if( asPercent )
	    	return ((int)(value * 1000) / 10.0);        // returns a percentage value with 1 decimal point precision
	    else
	    	return value;
    }*/
    
    private void WriteAttributes(final MBeanServer mBeanServer, final ObjectName http)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException
    {
        MBeanInfo info = mBeanServer.getMBeanInfo(http);
        MBeanAttributeInfo[] attrInfo = info.getAttributes();

        System.out.println("Attributes for object: " + http +":\n");
        for (MBeanAttributeInfo attr : attrInfo)
        {
            System.out.println("  " + attr.getName() );
        }
    }

	

	public LinkedList<String> getAvailableTypes()
	{
		LinkedList<String> ret = new LinkedList<String>();
		java.util.Iterator<Entry<String, String>> it = availablePerformaceTypes_.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, String> pairs = (Map.Entry<String, String>)it.next();
            String typeName = pairs.getKey();
            //String typeFullName = pairs.getValue();
            ret.add(typeName);      
        }
		return ret;
	}
    
	
	private String getUniqueTypeNameString(String objName)
	{
		String ret = "";
		Pattern patternType = Pattern.compile("type=(\\w*)");
		Pattern patternName = Pattern.compile("name=([a-zA-Z0-9-\\[\\] ]*)");
		Matcher matcher = patternType.matcher(objName);
		if (matcher.find())
		{
			ret = matcher.group(1);
		}else{
			return null;
		}
		
		matcher = patternName.matcher(objName);
		if (matcher.find())
		{
			ret = ret+"_"+matcher.group(1).replace(" ", "");
		}
		return ret;
	}
   /* public synchronized double getCpuUsage()
    {
        if ( lastSystemTime == 0 )
        {
            baselineCounters();
            return;
        }

        long systemTime     = System.nanoTime();
        long processCpuTime = 0;

        if ( ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean )
        {
            processCpuTime = ( (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean() ).getProcessCpuTime();
        }

        double cpuUsage = (double) ( processCpuTime - lastProcessCpuTime ) / ( systemTime - lastSystemTime );

        lastSystemTime     = systemTime;
        lastProcessCpuTime = processCpuTime;

        return cpuUsage / availableProcessors;
    }

    private void baselineCounters()
    {
        lastSystemTime = System.nanoTime();

        if ( ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean )
        {
            lastProcessCpuTime = ( (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean() )..getProcessCpuTime();
        }
    }
    */
	private LinkedList<String> getAvailableAttributesByLongName(String typeFullName, boolean expandCompositeData) throws MalformedObjectNameException, NullPointerException, IntrospectionException, InstanceNotFoundException, ReflectionException
	{
		LinkedList<String> ret = new LinkedList<String>();
		
		//if( shortTypeName.equals(CUSTOM_ES_TYPE) )
		//{
			//LinkedList<String> esattributes = elasticSearchAccessor_.getESAttributesForWarehouseIndices(); 	
			//ret.addAll(esattributes);
		//	ret.add("fake");	//to avoid exception: No RRD datasource specified. At least one is needed.
		//}
		//else
		//{
			ObjectName name    = ObjectName.getInstance(typeFullName);
			MBeanInfo info = mbs.getMBeanInfo(name);
	        MBeanAttributeInfo[] attrInfo = info.getAttributes();
	
	        for (MBeanAttributeInfo attr : attrInfo)
	        {
	            //System.out.println("  " + attr.getName() );
	            String tname = attr.getType();
	            //if( tname.contains("CompositeData") )
	            //	System.out.println(tname);
	            if( expandCompositeData && 
	            	(tname.contains("javax.management.openmbean.CompositeData") ) )	//"[Ljavax.management.openmbean.CompositeData;"
	            {
	            	processAttributesList(name, attr, ret);
	            }
	            else
	            {
	            	ret.add(attr.getName());
	            }
	
	        }
		//}
		return ret;
	}
	public LinkedList<String> getAvailableAttributes(String shortTypeName, boolean expandCompositeData) throws MalformedObjectNameException, NullPointerException, IntrospectionException, InstanceNotFoundException, ReflectionException
	{
		if( availablePerformaceTypes_.containsKey(shortTypeName.toLowerCase()) == false )
			return null;
		
		return getAvailableAttributesByLongName(availablePerformaceTypes_.get(shortTypeName.toLowerCase()), expandCompositeData);
		
	}
	
	private void processAttributesList(ObjectName name, MBeanAttributeInfo attr, LinkedList<String> ret) throws InstanceNotFoundException, ReflectionException
	{
		String attrname = attr.getName(); /*"ProcessCpuLoad"*/
		javax.management.AttributeList list = mbs.getAttributes( name, new String[]{ attrname });
		if (!list.isEmpty())
        {
        	Attribute att = (Attribute)list.get(0);
        	Object factor = att.getValue();
        	//String name2 = att.getName();
        	if( factor instanceof javax.management.openmbean.CompositeData )
        	{
        		javax.management.openmbean.CompositeData cd = (CompositeData) factor;
        		processAttributesList(cd, attrname, ret);
        	}
        	else if( factor instanceof javax.management.openmbean.CompositeData[] )
        	{
        		javax.management.openmbean.CompositeData[] cds = (CompositeData[]) factor;
        		int pos = 0;
        		for(CompositeData cd : cds)
        		{
        			processAttributesList(cd, attrname+"_"+pos, ret);
        			pos++;
        		}
        	}else{
        		//javax.management.openmbean.CompositeData cd = (CompositeData) factor;
        		ret.add(attr.getName());
        	}
        }
	}

	private void processAttributesList(CompositeData cd, String attrname, LinkedList<String> ret) {
		//if( cd == null )
    	//{
    	//	ret.add(attr.getName());
    	//}else{
        	Set< String > keys = cd.getCompositeType().keySet();
			for(String key : keys)
			{
				ret.add(attrname+"_"+key);
			}
    	//}
	}

	public void saveCounter(String type, LinkedList<AtrrValue> attval /*LinkedList<String> attrs, LinkedList<Double> factors*//*, boolean dynamicallyAddSources*/)
	{
		if( !ddrManagers_.containsKey(type) )
		{
			LOGGER.error("Cannot find RRD manager for type: "+type);
		}
		else
		{
			saveCounter(ddrManagers_.get(type), type, attval );
		}
	}

	public LinkedList<TimeSample> fetchRandomSamplesToRender(String from, String until, String format, int minSamplesCount, int maxDataPoints)
	{
		Random rnd = new Random();
		LinkedList<TimeSample> samples = new LinkedList<TimeSample>();
		long start = ParseTools.convertToTime(from);
		long end = ParseTools.convertToTime(until);
		//for(int i=0; i<maxDataPoints;i++)
		long ct = start;
		for(; ct<=end;ct+=60)
		{
			samples.add(new TimeSample(ct, rnd.nextDouble()));	
		}
		if( minSamplesCount != -1 )
		{
			for(int i=samples.size();i<=minSamplesCount;i++)
			{
				samples.add(new TimeSample(ct, rnd.nextDouble()));
				ct+=60;
			}
		}
		return samples;
	}

	
	
	
	/*public void saveCounter(String type, String attr, Long factor)
	{
		if( !ddrManagers_.containsKey(type) )
		{
			LOGGER.error("Cannot find RRD manager for type: "+type);
		}
		else
		{
			long t = System.currentTimeMillis()/1000;
			attr = ddrManagers_.get(type).transformSourceName(attr);
			try {
				ddrManagers_.get(type).addValue(t, attr, factor);
				LOGGER.info("Stored Long : [" + attr + "] " + t + " => " + factor);
			} catch (IOException e) {
				LOGGER.error("Cannot store Long "+factor+ " in RRD database " + type + " : " +e.getMessage());
				e.printStackTrace();
			}
		}
		
	}*/
}
