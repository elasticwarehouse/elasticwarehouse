# elasticwarehouse

ElasticWarehouse is available in two versions:
 * Standalone ElasticWarehouse version (can act as ElasticSearch node or be an API gateway to ElasticSearch cluster - Embedded vs Remote configuration) 
 * As a form of ElasticSearch plugin

###Sample ElasticWarehouse standalone installation
    cd /opt
    wget http://elasticwarehouse.org/download.php?fname=elasticwarehouse-1.2.3-2.1.0.tar.gz
    tar -zxf elasticwarehouse-1.2.3-2.1.0.tar.gz
    cd elasticwarehouse-1.2.3-2.1.0/bin
    ./elasticwarehouse

###Sample installation for ElasticSearch 1.x (i.e. 1.7.1)

    plugin -install elasticwarehouseplugin -u http://elasticwarehouse.org/download.php?fname=elasticsearch-elasticwarehouseplugin-1.2.3-1.7.1-with-dependencies.zip

    -> Installing elasticwarehouseplugin...
    Trying http://elasticwarehouse.org/download.php?fname=elasticsearch-elasticwarehouseplugin-1.2.3-1.7.1-with-dependencies.zip...
    Downloading ..................................................................DONE
    Installed elasticwarehouseplugin into /opt/elasticsearch-1.7.1/plugins/elasticwarehouseplugin

###Sample installation for ElasticSearch 2.x (i.e. 2.1.0)
    plugin install http://elasticwarehouse.org/download.php?fname=elasticsearch-elasticwarehouseplugin-1.2.3-2.1.0-with-dependencies.zip

    -> Installing from http://elasticwarehouse.org/download.php?fname=elasticsearch-elasticwarehouseplugin-1.2.3-2.1.0-with-dependencies.zip...
    Plugins directory [/opt/elasticsearch-2.1.0/plugins] does not exist. Creating...
    Trying http://elasticwarehouse.org/download.php?fname=elasticsearch-elasticwarehouseplugin-1.2.3-2.1.0-with-dependencies.zip ...
    Downloading .....................................................................DONE
    Verifying http://elasticwarehouse.org/download.php?fname=elasticsearch-elasticwarehouseplugin-1.2.3-2.1.0-with-dependencies.zip checksums if available ...
    NOTE: Unable to verify checksum for downloaded plugin (unable to find .sha1 or .md5 file to verify)
    Installed elasticwarehouseplugin into /opt/elasticsearch-2.1.0/plugins/elasticwarehouseplugin

More information you can find in [README](https://github.com/elasticwarehouse/elasticwarehouse/blob/master/elasticwarehouse.core/README.md) file and on project website http://elasticwarehouse.org

Common plugin installation issues: http://elasticwarehouse.org/elasticwarehouse-plugin-installation-known-issues/

More information about ElasticWarehouse integration with your applications can be found here: http://elasticwarehouse.org/guide-api/

