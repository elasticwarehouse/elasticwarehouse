# elasticwarehouse
elasticwarehouse

ElasticWarehouse is distributed in two versions:
 * Standalone ElasticSearch version (can act as ElasticSearch node or be a gateway API to ElasticSearch cluster - Embedded vs Remote configuration) 
 * As a form of ElasticSearch plugin
 
Sample installation for ElasticSearch 1.x (i.e. 1.7.1)

    plugin -install elasticwarehouseplugin -u http://elasticwarehouse.org/download.php?fname=elasticsearch-elasticwarehouseplugin-1.2.3-1.7.1-with-dependencies.zip

    -> Installing elasticwarehouseplugin...
    Trying http://elasticwarehouse.org/download.php?fname=elasticsearch-elasticwarehouseplugin-1.2.3-1.7.1-with-dependencies.zip...
    Downloading ..................................................................DONE
    Installed elasticwarehouseplugin into /opt/elasticsearch-1.7.1/plugins/elasticwarehouseplugin


More information you can find in REDME file (https://github.com/elasticwarehouse/elasticwarehouse/blob/master/elasticwarehouse.core/README.md) or on project website http://elasticwarehouse.org

Common plugin installtion issues: http://elasticwarehouse.org/elasticwarehouse-plugin-installation-known-issues/
