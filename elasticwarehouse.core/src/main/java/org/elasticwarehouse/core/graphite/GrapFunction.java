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


public class GrapFunction
{
	public GrapFunction(String fname, String fparameter)
	{
		this.fname = fname;
		this.fparameter = fparameter;
	}
	public String fname;
	public String fparameter;
	
	@Override
	public String toString()
	{
		if( fparameter.length() > 0 )
			return fname+"(<value>, " + fparameter+")";
		else
			return fname+"(<value>)";
	}

	public void apply(TimeSample timeSample, TimeSample refsample)
	{
		if( fname.equals("scale") )
		{
			timeSample.v *= Double.parseDouble(fparameter);
		}
		else if( fname.equals("divideSeries") )
		{
			if( refsample!=null && refsample.v!=0)
				timeSample.v /= refsample.v;
			else
				timeSample.v = -1.0;
		}
		else if( fname.equals("sumSeries") )
		{
			timeSample.v += refsample.v;
		}
		else if( fname.equals("diffSeries") )
		{
			timeSample.v -= refsample.v;
		}
	}

	public boolean isAliasFunction() {
		if( fname.equals("alias") )
			return true;
		else
			return false;
	}

	public boolean isReferenceSamplesNeeded() {
		if( fname.equals("divideSeries") || fname.equals("sumSeries") || fname.equals("diffSeries") )
			return true;
		else
			return false;
	}
}