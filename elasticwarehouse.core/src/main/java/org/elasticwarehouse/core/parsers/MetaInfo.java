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

import java.util.Date;

public class MetaInfo {
	private MetaInfo(String key, Long valueLong, String valueString, Date valueDate)
	{
		this.key_ = new String(key);
		if( valueLong != null )
			this.valueLong_ = new Long(valueLong);
		if( valueString != null )
			this.valueString_ = new String(valueString);
		if( valueDate != null )
			this.valueDate_ = (Date) valueDate.clone();
	}
	public MetaInfo(String key, String value)
	{
		valueString_ = value;
		key_ = key;
	}
	public MetaInfo(String key, Long value)
	{
		valueLong_ = value;
		key_ = key;
	}
	public MetaInfo(String key, Date value)
	{
		valueDate_ = value;
		key_ = key;
	}
	
	public String key_ = null;
	public Long valueLong_ = null;
	public String valueString_ = null;
	public Date valueDate_ = null;
	public MetaInfo getCopy() {
		return new MetaInfo(key_, valueLong_, valueString_, valueDate_);
	}
}
