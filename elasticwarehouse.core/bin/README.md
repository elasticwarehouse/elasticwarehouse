# ewshell.sh

Shell command tool for ElasticWarehouse (v1.3)

Usage: ewshell.sh -c [COMMAND] [ATTRIBUTES] [OPTIONS]...

#### Commands:
    -c              Choose one of: browse get upload info searchall

#### Global options:
    --server        Attribute to provide ElasticWarehouse hostname. Default is http://localhost
    --port          Attribute to provide ElasticWarehouse port. Default is 10200

#### Browse (-c browse):
##### Options:
    --all           Show all matched results. As default response from ElasticWarehouse is split to pages 
                    with default size of 10 results. Pagination of results can be done by using the ^from^ 
                    and ^size^ parameters
    --from          The ^from^ parameter defines the offset from the first result you want to fetch. 
    --to            The ^to^ parameter defines the offset from the first result you want to fetch.
    --size          The ^size^ parameter allows you to configure the maximum amount of documents to be returned
    --showgetlinks  For each file link is beeing generated to download binary file content.
  
##### Attributes:
    <folder>        An attribute to define name of the folder to be listed

##### Examples:  
    ewshell.sh --server 127.0.0.1 --port 10200 -c browse /home/user --all 1
    ewshell.sh -c browse /
    ewshell.sh -c browse /home/user
    ewshell.sh -c browse /home/user --all 1
    ewshell.sh -c browse /home/user --from 10
    ewshell.sh -c browse /home/user --from 10 --to 50
    ewshell.sh -c browse /home/user --from 10 --size 2
    ewshell.sh -c browse /home/user --from 10 --size 2 --showgetlinks 1

#### Search (-c searchall):
##### Options:
    --all           Show all matched results. As default response from ElasticWarehouse is split to pages 
                    with default size of 10 results. Pagination of results can be done by using the ^from^ 
                    and ^size^ parameters
    --from          The ^from^ parameter defines the offset from the first result you want to fetch. 
    --to            The ^to^ parameter defines the offset from the first result you want to fetch.
    --size          The ^size^ parameter allows you to configure the maximum amount of documents to be returned
    --highlight     Enables words highlighting. A document context will be returned with all matched words.
    --showgetlinks  For each file link is beeing generated to download binary file content.
  
##### Attributes:
    <query>         A search query to be executed
  
##### Examples:  
    ewshell.sh -c searchall Canon
    ewshell.sh -c searchall Canon --all 1
    ewshell.sh -c searchall Canon --from 10
    ewshell.sh -c searchall Canon --from 10 --to 50 --highlight 1
    ewshell.sh -c searchall Canon --from 10 --size 2 --highlight 1 
    ewshell.sh -c searchall Canon --from 10 --size 2 --highlight 1 --showgetlinks 1

#### Upload (-c upload):
##### Attributes:
    <filename>      A local filename with path to be uploaded to ElasticWarehouse cluster
    <folder>        An attribute to define name of the folder where file to be uploaded. If folder doesn't 
                    exist, then will be created.
    <id>            File id whose update is being requested. If id is not passed to the request, then file 
                    will be uploaded as a new one.

##### Examples:  
    ewshell.sh -c upload myfile.jpg /home/myfiles
    ewshell.sh -c upload myfile.jpg /home/myfiles P7beEttbS62MECMYUwln5g

#### Download options (-c get):
##### Attributes:
    <id>            File id whose binary content is being requested
    <folder>        Local path to download file

##### Examples:  
    ewshell.sh -c get P7beEttbS62MECMYUwln5g /tmp
    ewshell.sh -c get P7beEttbS62MECMYUwln5g

#### Info options (-c info):
##### Options:
    --showmeta      Option to show extracted file meta information 

##### Attributes:
    <id>            File id whose detailed information is being requested

##### Examples:  
    ewshell.sh -c info P7beEttbS62MECMYUwln5g
    ewshell.sh -c info P7beEttbS62MECMYUwln5g --showmeta