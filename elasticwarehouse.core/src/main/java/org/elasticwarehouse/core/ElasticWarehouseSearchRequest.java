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

import groovy.json.JsonException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.tika.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.IO;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.GeoPolygonFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticwarehouse.core.parsers.ParseTools;
import org.json.*;

import ucar.unidata.geoloc.LatLonPoint;
import static org.elasticsearch.common.xcontent.XContentFactory.*;

public class ElasticWarehouseSearchRequest extends ElasticWarehouseReqRespHelper{

	
	//Options object
	int size_ = 10;
	int from_ = 0;
	boolean scanembedded_ = false;
	boolean showrequest_ = false;
	//Query object
	HashMap<String, String> queryfields_ = new HashMap<String, String>();
	
	private final static Logger LOGGER = Logger.getLogger(ElasticWarehouseSearchRequest.class.getName());

	//Fields object
	LinkedList<String> fields_ = new LinkedList<String>();
	
	//Sort obejct
	String sortField_;
	SortOrder sortDirection_;
	private ElasticWarehouseConf conf_;
	private boolean highlight_;
	private int fragmentsize_;
	private String pretag_;
	private String posttag_;
	private int minimum_should_match_ = 0;
	private double sortGeoLat_ = 0.0;
	private double sortGeoLon_ = 0.0;
	//private boolean recurrence_;
	private ElasticWarehouseSearchRequest(LinkedList<String> fields, 
										 HashMap<String, String> queryfields/*, boolean recurrence*/,
										 boolean scanembedded, int size, int from, boolean showrequest, 
										 boolean highlight, int fragmentsize, String pretag, String posttag, int minimum_should_match,
										 String sortField, SortOrder sortDirection,
										 double sortGeoLat, double sortGeoLon, ElasticWarehouseConf conf)
	{
		this.fields_ = fields; 
		this.queryfields_ = queryfields;
		//this.recurrence_ = recurrence;	//search in subfolders
		this.scanembedded_ = scanembedded;
		this.size_ = size;
		this.from_= from;
		this.sortField_ = sortField;
		this.sortDirection_ = sortDirection;
		this.sortGeoLat_ = sortGeoLat;
		this.sortGeoLon_  = sortGeoLon;
		
		this.showrequest_ = showrequest;
		
		this.highlight_ = highlight;
		this.fragmentsize_ = fragmentsize;
		this.pretag_ = pretag;
		this.posttag_ = posttag;
		
		this.minimum_should_match_ = minimum_should_match;
		
		this.conf_ = conf;
	}
	
	public static ElasticWarehouseSearchRequest parseSearchRequest(String postData, OutputStream os, ElasticWarehouseConf conf) throws IOException
	{
		//Reader reader = postData.getReader();
	    //String postData = IO.toString(reader);
	    
		/*StringBuffer jb = new StringBuffer();
		String line = null;
		try {
			InputStream is = request.getInputStream();
			byte[] binaryContent_ = IOUtils.toByteArray(is);
		    BufferedReader reader = request.getReader();
		    while ((line = reader.readLine()) != null)
		      jb.append(line);
		} catch (Exception e) { 
			  System.out.println(e.getMessage());
			  EWLogger.logerror(e);
			  e.printStackTrace();
		}*/

		  /*try {
		    JSONObject jsonObject = JSONObject.fromObject(jb.toString());
		  } catch (ParseException e) {
		  EWLogger.logerror(e);
		    // crash and burn
		    throw new IOException("Error parsing JSON request string");
		  }*/
		  
		/*StringBuilder stringBuilder = new StringBuilder();
		if (request != null)
		{
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request));
			char[] charBuffer = new char[128];
			int bytesRead = -1;
			while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
		         stringBuilder.append(charBuffer, 0, bytesRead);
			}
		 } else {
		   stringBuilder.append("");
		 }*/
		
		ElasticWarehouseSearchRequest ret = parseJsonRequest(postData, os, conf);
		return ret;
	}

	
	private static ElasticWarehouseSearchRequest parseJsonRequest(String json, OutputStream os, ElasticWarehouseConf conf) throws IOException
	{
		ElasticWarehouseReqRespHelper helper = new ElasticWarehouseReqRespHelper();
		if( json == null || json.length() == 0 )
		{
			//os.write(helper.errorMessage("Format of Json request is wrong", ElasticWarehouseConf.URL_GUIDE_SEARCH));
			os.write(helper.errorMessage("Json request is empty", ElasticWarehouseConf.URL_GUIDE_SEARCH));
			return null;
		}
		LOGGER.debug("Parsing:"+json);
		//JSONParser parser=new JSONParser();
		try {
			
			LinkedList<String> fields = new LinkedList<String>();
			HashMap<String, String> queryfields = new HashMap<String, String>();
			
			JSONObject obj = new JSONObject( json );
		
			JSONArray arrfields = null;
			if( obj.has("fields") )
				arrfields = obj.getJSONArray("fields");
			
			JSONObject query = obj.getJSONObject("query");
		
			boolean scanembedded = false;
			boolean showrequest = false;
			boolean highlight = false;
			//boolean recurrence = false;
			int size = 10;
			int from = 0;
			int fragmentsize = 300;
			String pretag="<em>";
			String posttag="</em>";
			if( obj.has("options") )
			{
				if( obj.getJSONObject("options").has("scanembedded"))
					scanembedded = obj.getJSONObject("options").getBoolean("scanembedded");
				if( obj.getJSONObject("options").has("showrequest"))
					showrequest = obj.getJSONObject("options").getBoolean("showrequest");
				if( obj.getJSONObject("options").has("size"))
					size = obj.getJSONObject("options").getInt("size");
				if( obj.getJSONObject("options").has("from"))
					from = obj.getJSONObject("options").getInt("from");
				
				if( obj.getJSONObject("options").has("fragmentsize"))
					fragmentsize = obj.getJSONObject("options").getInt("fragmentsize");
				if( obj.getJSONObject("options").has("highlight"))
					highlight = obj.getJSONObject("options").getBoolean("highlight");
				if( obj.getJSONObject("options").has("pretag"))
					pretag = obj.getJSONObject("options").getString("pretag");
				if( obj.getJSONObject("options").has("posttag"))
					posttag = obj.getJSONObject("options").getString("posttag");
				
				//if( obj.getJSONObject("options").has("recurrence"))
				//	recurrence = obj.getJSONObject("options").getBoolean("recurrence");
				
			}
			
			String sortfield = "";
			String sortdirection = "";
			
			double sortGeoLat = 0.0;
			double sortGeoLon = 0.0;
			
			if( obj.has("sort") )
			{
				if( obj.getJSONObject("sort").has("field"))
					sortfield = obj.getJSONObject("sort").getString("field");
				if( obj.getJSONObject("sort").has("direction"))
					sortdirection = obj.getJSONObject("sort").getString("direction").toLowerCase();
				
				if( sortfield.equals(ElasticWarehouseMapping.FIELDLOCATION) )
		    	{
					if( obj.getJSONObject("sort").has(ElasticWarehouseMapping.FIELDLOCATION) == false )
					{
						os.write(helper.errorMessage("Field sort.location is needed to sort by Geo location.", ElasticWarehouseConf.URL_GUIDE_SEARCH));
						return null;
					}
					JSONObject sortlocation = obj.getJSONObject("sort").getJSONObject(ElasticWarehouseMapping.FIELDLOCATION);
					if( sortlocation.has("lat") == false || sortlocation.has("lon") == false )
					{
						os.write(helper.errorMessage("Sort location is not complete. 2 fields are expected 'lat' and 'lon'.", ElasticWarehouseConf.URL_GUIDE_SEARCH));
						return null;
					}
					sortGeoLat = sortlocation.getDouble("lat");
					sortGeoLon = sortlocation.getDouble("lon");
		    	}
			}
			

		
			//validate fields to return
			if( arrfields != null )
			{
				for (int i = 0; i < arrfields.length(); i++)
				{
					String fieldName = arrfields.getString(i);
					if( /*fieldName.equals(ElasticWarehouseMapping.FIELDALL) == false &&*/ ElasticWarehouseMapping.availableFields.contains(fieldName) == false )
					{
						os.write(helper.errorMessage("Field " + fieldName + " is not valid field name", ElasticWarehouseConf.URL_GUIDE_SEARCH));
						return null;
					}
					fields.add(fieldName);
				}
			}
			//validate sort field
			if( sortfield.length()>0 && ElasticWarehouseMapping.availableFields.contains(sortfield) == false && sortfield.equals(ElasticWarehouseMapping.FIELDSCORE) == false )
			{
				os.write(helper.errorMessage("Field " + sortfield + " is not valid field name", ElasticWarehouseConf.URL_GUIDE_SEARCH));
				return null;
			}
			
			if( sortfield.length()>0 &&  sortdirection.equals("asc") == false && sortdirection.equals("desc") == false )
			{
				os.write(helper.errorMessage("Sort direction is wrong. Choose one of: 'asc' or 'desc'", ElasticWarehouseConf.URL_GUIDE_SEARCH));
				return null;
			}
		
			Iterator<String> itr = query.keys();
			while(itr.hasNext())
			{
		         String key = itr.next();
		         String value = query.getString(key);//.toLowerCase();
		         if( key.equals("folder") )
		         {
		        	 key = "folderna";
		        	 value = ResourceTools.preprocessFolderName(value.toLowerCase());
		         }
		         if( isNotAnalyzedFiled(key) == false )
		        	 value = value.toLowerCase();
		         queryfields.put(key, value);
		    }
			
			int minimum_should_match = 0;
			//if( queryfields.containsKey(ElasticWarehouseMapping.FIELDALL) && queryfields.containsKey(ElasticWarehouseMapping.FIELDFOLDER) == false && queryfields.size()>1)
			if( queryfields.size() == 2 &&
				queryfields.containsKey(ElasticWarehouseMapping.FIELDALL) && 
				( queryfields.containsKey(ElasticWarehouseMapping.FIELDFOLDER) || queryfields.containsKey(ElasticWarehouseMapping.FIELDFOLDERNA) ) 
				)
			{
				//fine
				minimum_should_match = 1;
			}
			else if( queryfields.size() == 1 && (
					queryfields.containsKey(ElasticWarehouseMapping.FIELDALL) || 
					queryfields.containsKey(ElasticWarehouseMapping.FIELDFOLDER) ) )
			{
				//fine
			}
			else if( queryfields.size() > 2 && queryfields.containsKey(ElasticWarehouseMapping.FIELDALL) )
			{
				os.write(helper.errorMessage("Field 'all' cannot be combined with other field names except 'folder'", ElasticWarehouseConf.URL_GUIDE_SEARCH));
				return null;
			}
		
		
			return new ElasticWarehouseSearchRequest(fields, 
				 queryfields, /*recurrence,*/
				 scanembedded, size, from, showrequest,
				 highlight, fragmentsize, pretag, posttag,
				 minimum_should_match,
				 sortfield, (sortdirection.equals("desc")?SortOrder.DESC:SortOrder.ASC),
				 sortGeoLat ,sortGeoLon,
				 conf);
		
		} catch (JSONException e) {
			EWLogger.logerror(e);
			e.printStackTrace();
			os.write(helper.errorMessage("Format of Json request is wrong: " + e.getMessage(), ElasticWarehouseConf.URL_GUIDE_SEARCH));
			return null;
		}
	}



	private static boolean isNotAnalyzedFiled(String fieldname) {
		if( fieldname.equals("fieldname") || fieldname.equals("filenamena") )
			return true;
		else
			return false;
	}

	public SearchResponse process(Client esClient, OutputStream os) throws IOException, ElasticWarehouseAPIExecutionException
	{	
		SearchRequestBuilder request = esClient.prepareSearch(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_NAME) /*ElasticWarehouseConf.defaultIndexName_*/);
		if( scanembedded_ )
			request.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/, 
					conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_CHILDTYPE) /*ElasticWarehouseConf.defaultChildsTypeName_*/);
		else
			request.setTypes(conf_.getWarehouseValue(ElasticWarehouseConf.ES_INDEX_STORAGE_TYPE) /*ElasticWarehouseConf.defaultTypeName_*/);
		request.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		
		if( fields_.size() > 0 )
			request.setFetchSource(fields_.toArray(new String[fields_.size()]), null );
		else
			request.setFetchSource(null, new String[] {"filecontent","filetext"} );
	        //.setQuery(QueryBuilders.termQuery("_all", q))
	    LinkedList<String> higlightfields = new LinkedList<String>();
	    if( queryfields_.size() > 0 )
	    {
	    	BoolQueryBuilder multiQuery = QueryBuilders.boolQuery();
	    	BoolFilterBuilder multiFilter = FilterBuilders.boolFilter();
	    	boolean useFilter = false;
	    	Iterator<Entry<String, String>> it = queryfields_.entrySet().iterator();
	        while (it.hasNext())
	        {
	            Map.Entry<String, String> pairs = (Map.Entry<String, String>)it.next();
	            String fieldname = pairs.getKey();
	            String fieldvalue = pairs.getValue();
	            
	            if( ElasticWarehouseMapping.isGeoField( fieldname ) )
	            {
	            	buildGeoQuery(multiQuery, multiFilter, fieldname, fieldvalue);
	            	useFilter = true;
	            }
	            else if( ElasticWarehouseMapping.isIntegerField( fieldname ) )
	            {
	            	buildMatchOrRangeIntegerQuery(multiQuery, fieldname, fieldvalue);
	            }
	            else if( ElasticWarehouseMapping.isDateField( fieldname ) )
	            {
	            	buildMatchOrRangeDateQuery(multiQuery, fieldname, fieldvalue);
	            }
	            else
	            {
	            	//text fields
		            if( fieldvalue.contains("*") )
		            {
		            	if( fieldname.equals(ElasticWarehouseMapping.FIELDALL) )
		            	{
		            		multiQuery.should(QueryBuilders.wildcardQuery("filename", fieldvalue));
		            		multiQuery.should(QueryBuilders.wildcardQuery("filetitle", fieldvalue));
		            		multiQuery.should(QueryBuilders.wildcardQuery("filetext", fieldvalue));
		            		multiQuery.should(QueryBuilders.wildcardQuery("filemeta.metavaluetext", fieldvalue));
		            		
		            		higlightfields.add("filename");
		            		higlightfields.add("filetitle");
		            		higlightfields.add("filetext");
		            		higlightfields.add("filemeta.metavaluetext");
		            	}
		            	else
		            	{
		            		if( fieldname.equals("filename") ) {
			            		multiQuery.must( QueryBuilders.wildcardQuery("filename", fieldvalue) );
			            		multiQuery.should( QueryBuilders.wildcardQuery("filenamena", fieldvalue.toLowerCase()) );
			            		//minimum_should_match_+=1;
			            	}else if( fieldname.equals("filenamena") ) {
			            		multiQuery.must( QueryBuilders.wildcardQuery("filenamena", fieldvalue.toLowerCase()) );
			            		multiQuery.should( QueryBuilders.wildcardQuery("filename", fieldvalue) );
			            	}else{
			            		multiQuery.must(QueryBuilders.wildcardQuery(fieldname, fieldvalue));
			            	}
		            		higlightfields.add(fieldname);
		            	}
		            		
		            }
		            else
		            {
			            if( fieldname.equals(ElasticWarehouseMapping.FIELDALL) )
			            {
			            	multiQuery.should(QueryBuilders.matchQuery("filename", fieldvalue));
		            		multiQuery.should(QueryBuilders.matchQuery("filetitle", fieldvalue));
		            		multiQuery.should(QueryBuilders.matchQuery("filetext", fieldvalue));
		            		multiQuery.should(QueryBuilders.matchQuery("filemeta.metavaluetext", fieldvalue));
		            		
			            	//multiQuery.must(QueryBuilders.multiMatchQuery(queryfields_.get(fieldname), "filename","filetitle", "filetext", 
							//		"filemeta.metavaluetext" /*, "filemeta.metavaluedate", "filemeta.metavaluelong"*/));
			            	higlightfields.add("filename");
		            		higlightfields.add("filetitle");
		            		higlightfields.add("filetext");
		            		higlightfields.add("filemeta.metavaluetext");
			            }else{
			            	if( fieldname.equals("filename") ) {
			            		multiQuery.must( buildMatchQuery("filename", fieldvalue) );
			            		multiQuery.should( buildMatchQuery("filenamena", fieldvalue.toLowerCase()) );
			            		//minimum_should_match_+=1;
			            	}else if( fieldname.equals("filenamena") ) {
			            		multiQuery.must( buildMatchQuery("filenamena", fieldvalue.toLowerCase()) );
			            		multiQuery.should( buildMatchQuery("filename", fieldvalue) );
			            	}else{
			            		multiQuery.must( buildMatchQuery(fieldname, fieldvalue) );
			            	}
			            	higlightfields.add(fieldname);
			            }
		            }
	            }
	        }
	        if(minimum_should_match_ > 0 )
	        	multiQuery.minimumNumberShouldMatch(minimum_should_match_);
	        request.setQuery(multiQuery);
	        if( useFilter )
	        	request.setPostFilter(multiFilter);
	    }
	    //else if( queryfields_.size() == 1 )
	    //{
	    //	Set<String> firstEntry = queryfields_.keySet();
	    //	String key = firstEntry.iterator().next();
	    //	
	    //	if( key.equals(ElasticWarehouseMapping.FIELDALL) )
	    //		request.setQuery(QueryBuilders.multiMatchQuery(queryfields_.get(key), "filename","filetitle", "filetext", 
	     //   											"filemeta.metavaluetext" /*, "filemeta.metavaluedate", "filemeta.metavaluelong"*/));
	    //	else
	    //		request.setQuery(buildMatchQuery(key, queryfields_.get(key)) );
	    //}
	        //.setPostFilter(FilterBuilders.rangeFilter("age").from(12).to(18))   // Filter
	        //.setExplain(true)
	    if( sortField_.length()>0 )
	    {
	    	
	    	if( sortField_.equals(ElasticWarehouseMapping.FIELDSCORE) )
	    	{
	    		request.addSort("_score", sortDirection_);
	    	}
	    	if( sortField_.equals(ElasticWarehouseMapping.FIELDLOCATION) )
	    	{
	    		request.addSort(SortBuilders.geoDistanceSort(ElasticWarehouseMapping.FIELDLOCATION).point(sortGeoLat_, sortGeoLon_).order(sortDirection_));
	    		request.setTrackScores(true);
	    	}
	    	else
	    	{
	    		//request.addSort(sortField_, sortDirection_);
	    		request.addSort(SortBuilders.fieldSort(sortField_).order(sortDirection_).ignoreUnmapped(true));
	    		request.setTrackScores(true);
	    	}
	    }
	    request.setSize(size_);
	    request.setFrom(from_);
	    request.setVersion(true);
	    
	    if( highlight_ )
		{
	    	for(String field : higlightfields)
	    	{
	    		if( field.equals("filename") || field.equals("filetitle") || field.equals("filetext") || field.equals("filemeta.metavaluetext") )
	    			request.addHighlightedField(new HighlightBuilder.Field(field).highlighterType("fvh").fragmentSize(fragmentsize_).preTags(pretag_).postTags(posttag_) );
	    		else if ( field.equals("folderna") )
	    			request.addHighlightedField(new HighlightBuilder.Field("folder").highlighterType("plain").fragmentSize(fragmentsize_).preTags(pretag_).postTags(posttag_) );
	    		else
	    			request.addHighlightedField(new HighlightBuilder.Field(field).highlighterType("plain").fragmentSize(fragmentsize_).preTags(pretag_).postTags(posttag_) );
	    	}
		}
	    
	    if( showrequest_ )
	    {
	    	System.out.println(request.toString());
	    }
		SearchResponse response = request.execute().actionGet();
		
		return response;
	}

	private void buildGeoQuery(BoolQueryBuilder multiQuery, BoolFilterBuilder multiFilter, String fieldname, String fieldvalue) throws ElasticWarehouseAPIExecutionException
	{
    	String distance = null;
    	JSONArray box = null;
    	JSONArray polygon = null;

    	try
    	{
    		JSONObject obj = new JSONObject( fieldvalue );
    		if( obj.has("distance") )
    		{
    			distance = obj.getString("distance");
    			if( obj.has("lat") == false || obj.has("lon") == false )
    				throw new ElasticWarehouseAPIExecutionException( "Format of Json request is wrong. Distance geo search expects two parameters 'lan' and 'lon'" );
    			double lat = obj.getDouble("lat");
    			double lon = obj.getDouble("lon");
    			multiFilter.must( FilterBuilders.geoDistanceFilter(fieldname).distance(distance).lat(lat).lon(lon) );
    		}
    		else if( obj.has("box") )
    		{
    			box = obj.getJSONArray("box");
    			if( box.length() != 2 || 
    			  ( box.length() == 2 && (box.getJSONObject(0).has("lat") == false || box.getJSONObject(1).has("lat") == false ||
    					  				  box.getJSONObject(0).has("lon") == false || box.getJSONObject(1).has("lon") == false) ) )
    			{
    				throw new ElasticWarehouseAPIExecutionException( "Format of Json request is wrong. Box geo search expects exactly two points to define 'top left' and 'bottom right' box corners" );
    			}
    			else
    			{
					double lat1 = box.getJSONObject(0).getDouble("lat");
					double lon1 = box.getJSONObject(0).getDouble("lon");
					double lat2 = box.getJSONObject(1).getDouble("lat");
					double lon2 = box.getJSONObject(1).getDouble("lon");
					multiFilter.must( FilterBuilders.geoBoundingBoxFilter(fieldname).topLeft(lat1, lon1).bottomRight(lat2, lon2) );
    			}
    		}
    		else if( obj.has("polygon") )
    		{
    			polygon = obj.getJSONArray("polygon");
    			GeoPolygonFilterBuilder polygonbuilder = FilterBuilders.geoPolygonFilter(fieldname);
    			if( polygon.length() < 3 )
    				throw new ElasticWarehouseAPIExecutionException( "Format of Json request is wrong. Polygon geo search expects at least 3 geo points to define polygon corners." );

    			for(int i=0;i<polygon.length();i++)
    			{
    				if( polygon.getJSONObject(i).has("lat") == false || polygon.getJSONObject(i).has("lat") == false )
    					throw new ElasticWarehouseAPIExecutionException( "Format of Json request is wrong. One of polygon geo search points has 'lat' or 'lon' missing." );
    				double lat = polygon.getJSONObject(i).getDouble("lat");
					double lon = polygon.getJSONObject(i).getDouble("lon");
					polygonbuilder.addPoint(lat, lon);
    			}
    			multiFilter.must( polygonbuilder );
    		}else{
    			throw new ElasticWarehouseAPIExecutionException( "Format of Json request is wrong. Geo search must be one of: 'distance', 'box' or 'polygon'" );
    		}
    	}
    	catch(JSONException e)
    	{
    		EWLogger.logerror(e);
    		throw new ElasticWarehouseAPIExecutionException(e.getMessage());
    	}
	}

	private void buildMatchOrRangeDateQuery(BoolQueryBuilder multiQuery, String fieldname, String fieldvalue)
	{
		//check if value is a from/to Json object or not
    	String dfrom = null;
    	String dto = null;

    	try
    	{
    		JSONObject obj = new JSONObject( fieldvalue );
    		if( obj.has("from") )
    		{
    			dfrom = obj.getString("from");
    		}
    		if( obj.has("to") )
    		{
    			dto = obj.getString("to");
    		}
    	}
    	catch(JSONException e)
    	{
    		EWLogger.logerror(e);
    	}
    	if( dfrom!=null || dto!=null )
    	{
    		if( dfrom!=null && dto!=null  )
    			multiQuery.must(QueryBuilders.rangeQuery(fieldname).from(dfrom).to(dto));
    		else if( dfrom!=null )
    			multiQuery.must(QueryBuilders.rangeQuery(fieldname).from(dfrom));
			else if( dto!=null  )
				multiQuery.must(QueryBuilders.rangeQuery(fieldname).to(dto));
    	}
    	else
    	{
    		multiQuery.must( buildMatchQuery(fieldname, fieldvalue) );
    	}
	}

	private void buildMatchOrRangeIntegerQuery(BoolQueryBuilder multiQuery, String fieldname, String fieldvalue)
	{
		//check if value is a from/to Json object or not
    	int ifrom = 0;
    	int ito = 0;
    	boolean useRangeFrom = false;
    	boolean useRangeTo = false;
    	try
    	{
    		JSONObject obj = new JSONObject( fieldvalue );
    		if( obj.has("from") )
    		{
    			ifrom = obj.getInt("from");
    			useRangeFrom = true;
    		}
    		if( obj.has("to") )
    		{
    			ito = obj.getInt("to");
    			useRangeTo = true;
    		}
    	}
    	catch(JSONException e)
    	{
    		EWLogger.logerror(e);
    	}
    	if( useRangeFrom || useRangeTo )
    	{
    		if( useRangeFrom && useRangeTo )
    			multiQuery.must(QueryBuilders.rangeQuery(fieldname).from(ifrom).to(ito));
    		else if( useRangeFrom )
    			multiQuery.must(QueryBuilders.rangeQuery(fieldname).from(ifrom));
			else if( useRangeTo )
				multiQuery.must(QueryBuilders.rangeQuery(fieldname).to(ito));
    	}
    	else
    	{
    		multiQuery.must( buildMatchQuery(fieldname, fieldvalue) );
    	}
	}

	private QueryBuilder buildMatchQuery(String field, String queryString) {
		return QueryBuilders.matchQuery(field, queryString );
	}

}
