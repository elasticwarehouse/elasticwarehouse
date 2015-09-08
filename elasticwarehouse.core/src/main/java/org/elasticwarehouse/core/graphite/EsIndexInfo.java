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


public class EsIndexInfo {

	public long docscount_;
	public long totatlstoresize_;
	public long pristoresize_;
	public long querycurrent_;
	public long querytime_;
	public long querycount_;
	public long indexcurrent_;
	public long indextime_;
	public long indexcount_;
	public long fetchcount_;
	public long fetchtime_;
	public long nbofshards_;
	public long nbofreplicas_;

	public EsIndexInfo(
			long nbofshards, long nbofreplicas,
			long docscount, 
			long totatlstoresize, long pristoresize, 
			long querycurrent, long querytime, long querycount, long fetchtime, long fetchcount,
			long indexcurrent, long indextime, long indexcount/*,
			long percolatecurrent, long percolatetime, long percolatecount*/
			)
	{
		nbofshards_ = nbofshards;
		nbofreplicas_ = nbofreplicas;
		
		docscount_=docscount; 
		
		totatlstoresize_=totatlstoresize;
		pristoresize_=pristoresize; 
		
		querycurrent_=querycurrent;
		querytime_=querytime;
		querycount_=querycount;
		fetchtime_=fetchtime;
		fetchcount_=fetchcount;
		
		indexcurrent_=indexcurrent;
		indextime_=indextime;
		indexcount_=indexcount;
	}

}
