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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class ElasticWarehouseServerAPINotifier {

	public static final int API_INITIALIZED = 0;
	private ArrayList<Integer> waitlist = new ArrayList<Integer>();
	public ElasticWarehouseServerAPINotifier()
	{
		waitlist.add(API_INITIALIZED);
	}
	public void notifyListeners(int msgType) {
		synchronized(waitlist.get(msgType) )
		{
			waitlist.get(msgType).notifyAll();
		}
	}

	public void waitFor(int msgType) throws InterruptedException
	{
		synchronized(waitlist.get(msgType) )
		{
			waitlist.get(msgType).wait();
		}
	}

}
