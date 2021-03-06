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
package org.elasticwarehouse.plugin.ewapi;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
//import org.elasticsearch.common.inject.Module;
//import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticwarehouse.rest.action.ewapi.ElasticWarehouseBrowse;
import org.elasticwarehouse.rest.action.ewapi.ElasticWarehouseGet;
import org.elasticwarehouse.rest.action.ewapi.ElasticWarehouseInfo;
import org.elasticwarehouse.rest.action.ewapi.ElasticWarehouseSearch;
import org.elasticwarehouse.rest.action.ewapi.ElasticWarehouseSearchAll;
import org.elasticwarehouse.rest.action.ewapi.ElasticWarehouseSummary;
import org.elasticwarehouse.rest.action.ewapi.ElasticWarehouseTask;
import org.elasticwarehouse.rest.action.ewapi.ElasticWarehouseUpload;




public class ElasticWarehousePlugin extends Plugin {

	//private final Settings settings;

    //public  ElasticWarehousePlugin(Settings settings) {
    //    this.settings = settings;
    //}
	
    public String name() {
        return "ElasticWarehouse";
    }

    public String description() {
        return "Elastic Warehouse Plugin";
    }
    
    // for Rest API
    public void onModule(final RestModule restModule) {
        restModule.addRestAction(ElasticWarehouseSearchAll.class);
        restModule.addRestAction(ElasticWarehouseSearch.class);
        restModule.addRestAction(ElasticWarehouseUpload.class);
        restModule.addRestAction(ElasticWarehouseGet.class);
        restModule.addRestAction(ElasticWarehouseSummary.class);
        restModule.addRestAction(ElasticWarehouseBrowse.class);
        restModule.addRestAction(ElasticWarehouseInfo.class);
        restModule.addRestAction(ElasticWarehouseTask.class);
    }
	
	// for Service
   /* @Override
    public Collection<Module> nodeModules() {
        final Collection<Module> modules =new ArrayList<>();
        modules.add(new ReindexingModule());
        return modules;
    }

    // for Service
    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        final Collection<Class<? extends LifecycleComponent>> services = new ArrayList<>();
        services.add(ReindexingService.class);
        return services;
    }*/
	
	//ES 1.x
	/*
	@Override public void processModule(Module module) {
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(ElasticWarehouseSearchAll.class);
        }
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(ElasticWarehouseSearch.class);
        }
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(ElasticWarehouseUpload.class);
        }
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(ElasticWarehouseGet.class);
        }
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(ElasticWarehouseSummary.class);
        }
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(ElasticWarehouseBrowse.class);
        }
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(ElasticWarehouseInfo.class);
        }
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(ElasticWarehouseTask.class);
        }
    }*/
}