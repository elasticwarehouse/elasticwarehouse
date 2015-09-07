h1. ElasticWarehouse

h2. A Distributed RESTful File Storage & Search Engine

h3. "http://www.elasticwarehouse.org":http://www.elasticwarehouse.org

ElasticWarehouse is a file data storage build on the top of ElasticSearch (distributed RESTful 
search engine built for the cloud) and it has all its features:

* Distributed and Highly Available Search Engine.
** Each index is fully sharded with a configurable number of shards.
** Each shard can have one or more replicas.
** Read / Search operations performed on either one of the replica shard.
* Multi Tenant with Multi Types.
** Support for more than one index.
** Support for more than one type per index.
** Index level configuration (number of shards, index storage, ...).
* Various set of APIs
** HTTP RESTful API
** Native Java API.
** All APIs perform automatic node operation rerouting.
* Document oriented
** No need for upfront schema definition.
** Schema can be defined per type for customization of the indexing process.
* Reliable, Asynchronous Write Behind for long term persistency.
* (Near) Real Time Search.
* Built on top of Lucene
** Each shard is a fully functional Lucene index
** All the power of Lucene easily exposed through simple configuration / plugins.
* Per operation consistency
** Single document level operations are atomic, consistent, isolated and durable.
* Open Source under Apache 2 License.

h2. Getting Started

Goal of ElasticWarehouse is to organize your files, make them searchable and take care about fault 
tolerance. Thanks to ElasticWarehouse you can store terabytes of data in the cloud. This short 
introduction shows how to install and configure ElasticWarehouse cluster, how to import your files 
to the cloud and how to access them using REST API.

ElasticWarehouse is an open-source project and it has nothing common with Elastic.co, except fact 
ElasticWarehouse has been build on the top of ElasticSearch.

h3. Installation

* "Download":http://www.elasticwarehouse.org/download and unzip the ElasticWarehouse official distribution.
* Run @bin/elasticsearch@ on nix systems, or @bin\elasticsearch.bat@ on windows.
* Run @curl -X GET http://localhost:10200/@.
* Start more servers ...

h3. Uploading files to teh cloud

Let's try to upload your first file to the cloud (the @folder@ path will be created automatically):
<pre>
curl -XPOST "http://localhost:10200/_ewupload?folder=/files/mypictures/&filename=myimage.jpg" --data-binary @myimage.jpg
</pre>

Response:
<pre>
{"id":"aYLro1V_TzO0tfLNmbp4gA","version":1,"created":true}
</pre>
Returned @id@ is an unique file identifier in the cluster.
 
Upload an update?
<pre>
curl -XPOST "http://localhost:10200/_ewupload?folder=/files/mypictures/&filename=myimage.jpg&id=aYLro1V_TzO0tfLNmbp4gA" --data-binary @myimage.jpg
</pre>

Response:
<pre>
{"id":"aYLro1V_TzO0tfLNmbp4gA","version":2,"created":false}
</pre>

Now, let's see the the indexed file information:
<pre>
curl -XGET "http://localhost:10200/_ewinfo?id=aYLro1V_TzO0tfLNmbp4gA"
</pre>

<pre>
{
	"imageheight": 68,
	"filemeta": [{
		"metakey": "Software",
		"metavaluetext": "Adobe Photoshop CS3 Macintosh"
	},
	{
		"metakey": "Copyright Notice",
		"metavaluetext": "(c) ?????? ????????? (c) ?????? ?????????"
	},
	{
		"metakey": "Model",
		"metavaluetext": "Canon EOS 40D"
	},
	{
		"metakey": "Metering Mode",
		"metavaluetext": "Multi-segment"
	},
	{
		"metavaluedate": "2009-10-02 23:02:49",
		"metakey": "meta:save-date"
	},
	{
		"metakey": "Exif Version",
		"metavaluetext": "2.21"
	},
	{
		"metakey": "Exposure Mode",
		"metavaluetext": "Auto exposure"
	},
	{
		"metakey": "Coded Character Set",
		"metavaluetext": "%G \u001B%G"
	},
	{
		"metavaluelong": 68,
		"metakey": "tiff:ImageLength"
	},
	{
		"metakey": "exif:Flash",
		"metavaluetext": "false"
	},
	{
		"metavaluedate": "2009-08-11 09:09:45",
		"metakey": "Creation-Date"
	},
	{
		"metavaluelong": 400,
		"metakey": "ISO Speed Ratings"
	},
	{
		"metakey": "X Resolution",
		"metavaluetext": "240 dots per inch"
	},
	{
		"metakey": "Shutter Speed Value",
		"metavaluetext": "1/1599 sec"
	},
	{
		"metavaluelong": 100,
		"metakey": "tiff:ImageWidth"
	},
	{
		"metavaluelong": 2,
		"metakey": "Application Record Version"
	},
	{
		"metakey": "tiff:XResolution",
		"metavaluetext": "240.0"
	},
	{
		"metakey": "Image Width",
		"metavaluetext": "100 pixels"
	},
	{
		"metakey": "Keywords",
		"metavaluetext": "canon-55-250"
	},
	{
		"metakey": "GPS Longitude",
		"metavaluetext": "-54.0° 7.0' 24.239999980010225\""
	},
	{
		"metakey": "GPSLongitudeRef",
		"metavaluetext": "W"
	},
	{
		"metavaluedate": "2009-10-0223: 02: 49",
		"metakey": "Last-Save-Date"
	},
	{
		"metakey": "exif: FNumber",
		"metavaluetext": "5.6"
	},
	{
		"metakey": "F-Number",
		"metavaluetext": "F5.6"
	},
	{
		"metakey": "ColorSpace",
		"metavaluetext": "Undefined"
	},
	{
		"metakey": "ResolutionUnits",
		"metavaluetext": "inch"
	},
	{
		"metakey": "DataPrecision",
		"metavaluetext": "8bits"
	},
	{
		"metakey": "Sharpness",
		"metavaluetext": "None"
	},
	{
		"metavaluelong": 8,
		"metakey": "tiff: BitsPerSample"
	},
	{
		"metakey": "tiff: YResolution",
		"metavaluetext": "240.0"
	},
	{
		"metavaluedate": "2009-10-0223: 02: 49",
		"metakey": "Last-Modified"
	},
	{
		"metakey": "YCbCrPositioning",
		"metavaluetext": "Centerofpixelarray"
	},
	{
		"metakey": "ComponentsConfiguration",
		"metavaluetext": "YCbCr"
	},
	{
		"metakey": "CompressionType",
		"metavaluetext": "Baseline"
	},
	{
		"metakey": "Saturation",
		"metavaluetext": "None"
	},
	{
		"metavaluelong": 400,
		"metakey": "exif: IsoSpeedRatings"
	},
	{
		"metakey": "X-Parsed-By",
		"metavaluetext": "org.apache.tika.parser.DefaultParser"
	},
	{
		"metavaluedate": "2009-10-0223: 02: 49",
		"metakey": "modified"
	},
	{
		"metakey": "MaxApertureValue",
		"metavaluetext": "F5.7"
	},
	{
		"metakey": "ExifImageHeight",
		"metavaluetext": "2592pixels"
	},
	{
		"metakey": "FocalLength",
		"metavaluetext": "194.0mm"
	},
	{
		"metavaluelong": 0,
		"metakey": "ImageNumber"
	},
	{
		"metakey": "ExposureBiasValue",
		"metavaluetext": "0EV"
	},
	{
		"metakey": "WhiteBalanceMode",
		"metavaluetext": "Manualwhitebalance"
	},
	{
		"metakey": "Make",
		"metavaluetext": "Canon"
	},
	{
		"metakey": "tiff: Make",
		"metavaluetext": "Canon"
	},
	{
		"metakey": "Date/TimeOriginal",
		"metavaluetext": "2009: 08: 1109: 09: 45"
	},
	{
		"metakey": "dc: subject",
		"metavaluetext": "canon-55-250"
	},
	{
		"metakey": "subject",
		"metavaluetext": "canon-55-250"
	},
	{
		"metakey": "ExifImageWidth",
		"metavaluetext": "3888pixels"
	},
	{
		"metakey": "SceneCaptureType",
		"metavaluetext": "Standard"
	},
	{
		"metavaluedate": "2009-08-1109: 09: 45",
		"metakey": "dcterms: created"
	},
	{
		"metakey": "exif: ExposureTime",
		"metavaluetext": "6.25E-4"
	},
	{
		"metavaluedate": "2009-10-0223: 02: 49",
		"metakey": "date"
	},
	{
		"metakey": "Component1",
		"metavaluetext": "Ycomponent: Quantizationtable0, Samplingfactors1horiz/1vert"
	},
	{
		"metakey": "GPSLatitude",
		"metavaluetext": "12.0°32.0'35.556000000000694\""
	},
	{
		"metakey": "Component 2",
		"metavaluetext": "Cb component: Quantization table 1, Sampling factors 1 horiz/1 vert"
	},
	{
		"metakey": "Component 3",
		"metavaluetext": "Cr component: Quantization table 1, Sampling factors 1 horiz/1 vert"
	},
	{
		"metakey": "Focal Plane X Resolution",
		"metavaluetext": "73/324000 inches"
	},
	{
		"metakey": "GPS Latitude Ref",
		"metavaluetext": "N"
	},
	{
		"metakey": "tiff:ResolutionUnit",
		"metavaluetext": "Inch"
	},
	{
		"metakey": "Flash",
		"metavaluetext": "Flash did not fire, auto"
	},
	{
		"metakey": "Date/Time Digitized",
		"metavaluetext": "2009:08:11 09:09:45"
	},
	{
		"metakey": "Focal Plane Y Resolution",
		"metavaluetext": "162/720247 inches"
	},
	{
		"metakey": "meta:keyword",
		"metavaluetext": "canon-55-250"
	},
	{
		"metakey": "Copyright",
		"metavaluetext": "(c) ?????? ?????????"
	},
	{
		"metakey": "Contrast",
		"metavaluetext": "None"
	},
	{
		"metakey": "Resolution Unit",
		"metavaluetext": "Inch"
	},
	{
		"metavaluelong": 49,
		"metakey": "Sub-Sec Time Original"
	},
	{
		"metakey": "tiff:Software",
		"metavaluetext": "Adobe Photoshop CS3 Macintosh"
	},
	{
		"metakey": "IPTC-NAA record",
		"metavaluetext": "107 bytes binary data"
	},
	{
		"metakey": "Focal Plane Resolution Unit",
		"metavaluetext": "Inches"
	},
	{
		"metavaluelong": 3,
		"metakey": "Number of Components"
	},
	{
		"metakey": "Aperture Value",
		"metavaluetext": "F5.6"
	},
	{
		"metakey": "tiff:Model",
		"metavaluetext": "Canon EOS 40D"
	},
	{
		"metakey": "Image Height",
		"metavaluetext": "68 pixels"
	},
	{
		"metavaluelong": 49,
		"metakey": "Sub-Sec Time Digitized"
	},
	{
		"metakey": "geo:lat",
		"metavaluetext": "12.54321"
	},
	{
		"metakey": "Exposure Time",
		"metavaluetext": "1/1600 sec"
	},
	{
		"metavaluedate": "2009-08-11 09:09:45",
		"metakey": "exif:DateTimeOriginal"
	},
	{
		"metakey": "exif:FocalLength",
		"metavaluetext": "194.0"
	},
	{
		"metakey": "FlashPix Version",
		"metavaluetext": "1.00"
	},
	{
		"metakey": "Date/Time",
		"metavaluetext": "2009:10:02 23:02:49"
	},
	{
		"metakey": "Custom Rendered",
		"metavaluetext": "Normal process"
	},
	{
		"metakey": "geo:long",
		"metavaluetext": "-54.1234"
	},
	{
		"metakey": "Exposure Program",
		"metavaluetext": "Shutter priority"
	},
	{
		"metakey": "Enveloped Record Version",
		"metavaluetext": ""
	},
	{
		"metakey": "GPS Version ID",
		"metavaluetext": "2.200"
	},
	{
		"metakey": "Y Resolution",
		"metavaluetext": "240 dots per inch"
	}],
	"stats": {
		"parseTime": 1074
	},
	"folderlevel": 3,
	"isfolder": false,
	"version": 2,
	"folder": "/files/mypictures/",
	"filesize": 16482,
	"fileuploaddate": "2015-07-22 15:18:03",
	"filemodifydate": "2015-07-22 15:19:00",
	"filename": "myimage.jpg",
	"filetype": "image/jpeg",
	"imagewidth": 100,
	"filemodificationdate": "2009-10-02 23:02:49",
	"filecreationdate": "2009-08-11 09:09:45",
	"imageyresolution": 240,
	"imagexresolution": 240
}
</pre>

Want to download it back from the cloud?
<pre>
curl -XGET "http://localhost:10200/_ewget?id=E0Jjq7MZTPa3boax6OSw0w" > /tmp/myimage.jpg
</pre>

h3. Searching

Search for files search can be done by ElasticWarehouse REST API or Elasticsearch API (it's up to you),
hovewer in this short guide we show how to use ElasticWarehouse REST API and for more advanced search 
we recommend you to read tutorials on Elastic.co website.

_ewsearchall restpoint allows you to search for files by its contents and do the most generic search, 
like search everywhere where possible.
<pre>
curl "http://localhost:10200/_ewsearchall?q=Market"
</pre>

We can also use results highlighting feature:
<pre>
curl "http://localhost:10200/_ewsearchall?q=Market&highlight=true"
</pre>

_ewsearch restpoint allows you to search for files by its contents in more advance way than _searchall.

Find all files which contain phase *geo* in /home/user folder:
<pre>
curl -XPOST "http://localhost:10200/_ewsearch" -d '{
   "query": {
      "folder": "/home/user",
      "all": "*geo*"
   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
      "size": 20,
      "from": 2
   },
   "sort": {
      "field": "fileuploaddate",
      "direction": "desc"
   }
}'
</pre>

Find all files with width equal to 64 pixels and filesize between 24KB and 50KB. In this example we also
use scanembedded=true to scan extracted embedded files. This is useful when you suspect file you are 
looking for has been added i.e. to some WORD or PDF files. Sort results by upload timestamp:
<pre>
curl -XPOST "http://localhost:10200/_ewsearch" -d '{
   "query": {
      "imagewidth": 64,
      "filesize": {
         "from": 24000,
         "to": 50000
      }
   },
   "options": {
      "scanembedded": "true",
      "size": 20,
      "from": 0
   },
   "sort": {
      "field": "fileuploaddate",
      "direction": "desc"
   }
}'
</pre>

Final all files with *brand* text in the filename and height between 200 and 500 pixels. Sort 
results by relevancy:
<pre>
curl -XGET 'http://localhost:9200/twitter/_search?pretty=true' -d '
{ 
    "query" : { 
        "range" : { 
            "postDate" : { "from" : "2009-11-15T13:00:00", "to" : "2009-11-15T14:00:00" } 
        } 
    },
    "sort": {
      "field": "score",
      "direction": "desc"
   }
}'
</pre>

Find all files uploaded between "2015-07-20 13:10:30" and "2015-07-28 13:10:33":
<pre>
curl -XPOST "http://localhost:10200/_ewsearch" -d '{
   "query": {
      "fileuploaddate": {
         "from": "2015-07-20 13:10:30",
         "to": "2015-07-28 13:10:33"
      }
   },
   "sort": {
      "field": "fileuploaddate",
      "direction": "desc"
   }
}'
</pre>

Find all images made by Canon 40D:
<pre>
curl -XPOST "http://localhost:10200/_ewsearch" -d '{
   "query": {
      "filename": "jpg",
      "filemeta.metavaluetext": "canon 40D"
   },
   "options": {
      "scanembedded": "true"
   },
   "sort": {
      "field": "score",
      "direction": "desc"
   }
}'
</pre>

Find all images modified by Photoshop:
<pre>
curl -XPOST "http://localhost:10200/_ewsearch" -d '{
   "query": {
      "filename": "jpg",
      "filemeta.metavaluetext": "Photoshop"
   },
   "options": {
      "scanembedded": "true"
   },
   "sort": {
      "field": "score",
      "direction": "desc"
   }
}'
</pre>

There are more options to perform search, to read more about them visit http://elasticwarehouse.org/elasticwarehouse-rest-api/.

h3. Distributed, Highly Available

ElasticWarehouse is build on the top of Elasticsearch - highly available and distributed search engine. 
Each index is broken down into shards, and each shard can have one or more replica. By default, an index 
is created with 5 shards and 1 replica per shard (5/1). There are many topologies that can be used, 
including 1/10 (improve search performance), or 20/1 (improve indexing performance, with search executed 
in a map reduce fashion across shards).

In order to play with Elasticsearch distributed nature, simply bring more nodes up and shut down nodes. 
The system will continue to serve requests (make sure you use the correct http port) with the latest data 
indexed.

h3. Where to go from here?

We have just covered a very small portion of ElasticWarehouse features. For more information, please 
refer to the "elasticwarehouse.org":http://www.elasticwarehouse.org website.

h3. Upgrading to new ElasticWarehouse version

In order to ensure a smooth upgrade process from earlier versions of ElasticWarehouse it is recommended to:
- make a full backup of /data folder when lucene indices are stored
- upgrade binaries
- perform a full cluster restart. 
Please see the "Upgrading" section http://elasticwarehouse.org/upgrade/.

h1. License

<pre>
This software is licensed under the Apache 2 license, quoted below.

Copyright 2015 ElasticWarehouse <http://www.elasticwarehouse.org>

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
</pre>
