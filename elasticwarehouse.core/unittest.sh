#!/bin/bash
#for embedded and remote testing:
HOST=http://localhost:10200
#for plugin testing:
#HOST=http://localhost:9200
ESHOST=http://localhost:9200

#HOST=http://192.168.0.12:9200
#ESHOST=http://192.168.0.12:9200


FOLDER_TO_SCAN=/home/user/workspace/elasticwarehouse.core/src/test/resources
FOLDER_TO_RECURRENCE_SCAN=/home/user/workspace/elasticwarehouse.core/src/test/resources/recurrence
EXCLUDE_EXT='.avi|.mp4|.mkv'
RUN_TESTS_STORE_DISABLED=0
#SHOW_REQUEST='&showrequest=true'

function showok()
{
    local  msg=$1
	printf "%-100s: \e[32mOK\e[39m\n" "$msg"
}
function showerror()
{
    local  msg=$1
	printf "%-100s: \e[31mERROR\e[39m\n" "$msg"
	exit 1
}
function showwarning()
{
    local  msg=$1
	printf "%-100s: \e[33mWARNING\e[39m\n" "$msg"
}

function showinfo()
{
    local  msg=$1
	printf "\e[36m-----> %-100s\e[39m\n" "$msg"
}
function testIngestionCount()
{
	local CNT=`ls -la $FOLDER_TO_SCAN | grep -v ^d | grep -v '^total ' | wc -l`
	local CNT_EXCLUDED=`ls -la $FOLDER_TO_SCAN | grep -E $EXCLUDE_EXT | wc -l`
	#local CNT=`ls $FOLDER_TO_SCAN | wc -l`
	#local CNT_EXCLUDED=`ls $FOLDER_TO_SCAN | grep -E $EXCLUDE_EXT | wc -l`

	local INGESTED_JSON=`curl $ESHOST/elasticwarehousestorage/files/_count 2>/dev/null`
	local CNT_INGESTED=`echo $INGESTED_JSON | jq -r '.count'`	#should be = CNT + 4 folders - count of $EXCLUDE_EXT files
	local CNT_SCANNED=$(($CNT-$CNT_EXCLUDED+4))
	TOTAL_FILES_TO_BE_INGESTED=$(($CNT-$CNT_EXCLUDED))
	if [ $CNT_INGESTED -eq $CNT_SCANNED ]; then
		showok "All files correctly ingested"
	else
		showerror "Counts doesn't match, files total: $CNT_INGESTED, scanned: $CNT - $CNT_EXCLUDED +4 = $CNT_SCANNED"
		#showerror "Counts doesn't match, files total: $CNT_INGESTED, scanned: $CNT - $CNT_EXCLUDED = $CNT_SCANNED"
	fi
}
function testassert()
{
	local CURRENT_VALUE=$1
	local EXPECTED_VALUE=$2
	local MSG=$3
	if [ $CURRENT_VALUE -eq $EXPECTED_VALUE ]; then
		showok "$MSG: (=$CURRENT_VALUE)"
	else
		showerror "$MSG: current=$CURRENT_VALUE, expected=$EXPECTED_VALUE"
	fi
}
function teststringassertn()
{
	local CURRENT_VALUE=$1
	local EXPECTED_VALUE=$2
	local MSG=$3
	if [ "$CURRENT_VALUE" != "$EXPECTED_VALUE" ]; then
		showok "$MSG: (=$CURRENT_VALUE)"
	else
		showerror "$MSG: current=$CURRENT_VALUE, expected=$EXPECTED_VALUE"
	fi
}
function testjsonassert()
{
	local UPLOADED_ID=$1
	local JQPATH=$2
	local EXPECTEDVALUE=$3
	local MSG=$4

	local CVALUE=`curl -XGET "$ESHOST/elasticwarehousestorage/files/$UPLOADED_ID" 2>/dev/null | jq -r "$JQPATH"`

	if [ "$CVALUE" == "$EXPECTEDVALUE" ]; then
		showok "$MSG: (=$CVALUE)"
	else
		showerror "$MSG: current=$CVALUE, expected=$EXPECTEDVALUE"
	fi
}
function executetask()
{
	local RET=`curl -XGET "$HOST/_ewtask?$1$SHOW_REQUEST" 2>/dev/null`
	local errorcode=`echo $RET | jq -r '.errorcode'`
	local comment=`echo $RET | jq -r '.comment'`
	local taskid=`echo $RET | jq -r '.taskid'`
	local progress=`echo $RET | jq -r '.progress'`
	LAST_TASK_ID=$taskid
	LAST_TASK_PROGRESS=$progress
	#echo $errorcode
	if [ $errorcode -eq 0 ]; then
		showok "$1"
	elif [ $errorcode -eq 60 ]; then
		showwarning "$1 : $comment"
	else
		showwarning "$1 : $comment"
	fi
}

function executebrowse()
{
	local RET=`curl -XGET "$HOST/_ewbrowse?$1$SHOW_REQUEST" 2>/dev/null`
	LAST_BROWSE_IDS=( $( echo $RET | jq -r '.hits.hits[]._id' ) )
	LAST_BROWSE_COUNT=${#LAST_BROWSE_IDS[@]}
}
function executesearch()
{
	local Q=$1
	local RET=`curl -XPOST "$HOST/_ewsearch?fake=1$SHOW_REQUEST" -d "$Q" 2>/dev/null`
	LAST_SEARCH_IDS=( $( echo $RET | jq -r '.hits.hits[]._id' ) )
	LAST_SEARCH_COUNT=${#LAST_SEARCH_IDS[@]}
}
function executesearchall()
{
	local Q=$1
	local SIZE=$2
	if [ -z $SIZE ]; then
		local RET=`curl -XGET "$HOST/_ewsearchall?q=$Q$SHOW_REQUEST" 2>/dev/null`
	else
		local RET=`curl -XGET "$HOST/_ewsearchall?q=$Q&size=$SIZE$SHOW_REQUEST" 2>/dev/null`
	fi
	LAST_SEARCH_IDS=( $( echo $RET | jq -r '.hits.hits[]._id' ) )
	LAST_SEARCH_COUNT=${#LAST_SEARCH_IDS[@]}
}
function getfileinfo()
{
	local ID=$1
	local field=$2
	local RET=`curl -XGET "$HOST/_ewinfo?id=$ID" 2>/dev/null | jq -r ".$field"`
	LAST_INFO_VALUE=$RET
}
function executeupload()
{
	local SOURCEFOLDER=$1
	local FILENAME=$2
	local TARGETFOLDER=$3
	local ID=$4
	local RET=""
	if [ -z $ID ] ; then
		RET=`curl -XPOST "$HOST/_ewupload?folder=$TARGETFOLDER&filename=$FILENAME" --data-binary @$SOURCEFOLDER/$FILENAME 2>/dev/null`
	else
		RET=`curl -XPOST "$HOST/_ewupload?folder=$TARGETFOLDER&filename=$FILENAME&id=$ID" --data-binary @$SOURCEFOLDER/$FILENAME 2>/dev/null`
	fi
	UPLOADED_ID=`echo $RET | jq -r '.id'`
	UPLOADED_VERSION=`echo $RET | jq -r '.version'`
}
function executedownload()
{
	local ID=$1
	local TARGETFILEPATH=$2
	local TYPE=$3
	if [ -z $TYPE ] ; then
		curl -XGET "$HOST/_ewget?id=$ID" > $TARGETFILEPATH 2>/dev/null
	else
		curl -XGET "$HOST/_ewget?id=$ID&type=$TYPE" > $TARGETFILEPATH 2>/dev/null
	fi
}
function cleanall()
{
	curl -XDELETE "$ESHOST/elasticwarehousestorage/_query" -d' { "query": { "match_all": {} } }'
	curl -XDELETE "$ESHOST/elasticwarehousetasks/_query" -d' { "query": { "match_all": {} } }'
	echo ""
}
function waitfortask()
{
	local LAST_TASK_ID=$1
	executetask "status=$LAST_TASK_ID"
	while [ $LAST_TASK_PROGRESS -ne 100 ]; do
		showinfo "Waiting for scan to finish, progress: $LAST_TASK_PROGRESS"
		sleep 2
		executetask "status=$LAST_TASK_ID"
	done 
	showinfo "Task finished, progress: $LAST_TASK_PROGRESS"
	sleep 1
}

##########################################################
if [ $RUN_TESTS_STORE_DISABLED -eq 1 ] ; then
	cleanall
	rm /opt/upload/* 2>/dev/null

	#test store:false | folder:/opt/upload | movescanned:false
	#test store:false | folder:/opt/upload | movescanned:true
	executeupload $FOLDER_TO_SCAN "testJPEG_commented.jpg" "/home"
	testassert $UPLOADED_VERSION 1 "Checking $UPLOADED_ID"
	testjsonassert $UPLOADED_ID "._source.origin.path" "/tmp/" "Checking .origin.path"
	testjsonassert $UPLOADED_ID "._source.origin.filename" "testJPEG_commented.jpg" "Checking .origin.filename"
	testjsonassert $UPLOADED_ID "._source.filecontent" "null" "Checking .filecontent"
	CNT=`ls /opt/upload/ | wc -l`
	#upload method always move files to /opt/upload
	testassert $CNT 1 "Checking files count in /opt/upload"
	executedownload $UPLOADED_ID "/tmp/mynewfile.jpg"

	#test scan
	cleanall
	rm /opt/upload/* 2>/dev/null
	executetask "action=mkdir&folder=/home/zuko2"
	executetask "action=scan&path=$FOLDER_TO_SCAN&targetfolder=/home/zuko"
	executetask "status=$LAST_TASK_ID"
	while [ $LAST_TASK_PROGRESS -ne 100 ]; do
		showinfo "Waiting for scan to finish, progress: $LAST_TASK_PROGRESS"
		sleep 2
		executetask "status=$LAST_TASK_ID"
	done 
	showinfo "Scan finished, progress: $LAST_TASK_PROGRESS"
	sleep 1
	
	testIngestionCount

	#only when movescanned:true
	CNT=`ls /opt/upload/ | wc -l`
	testassert $CNT $TOTAL_FILES_TO_BE_INGESTED "Checking /opt/upload folder count"

	exit
fi

##########################################################
#rest of tests
cleanall
executetask "action=mkdir&folder=/"
executetask "action=mkdir&folder=/home"
executetask "action=mkdir&folder=/home/"
executetask "action=mkdir&folder=/home/////"
executetask "action=mkdir&folder=/home/zuko"
executetask "action=mkdir&folder=/home/zuko/////"
executetask "action=mkdir&folder=//////home////////zuko/////"
executetask "action=mkdir&folder=/home/zuko2"
executetask "action=mkdir&folder=home:\\\\zuko2"	#to simulate windows paths
executetask "action=mkdir&folder=home:\zuko2"		#to simulate windows paths
executetask "action=mkdir&folder=home:\/zuko2"		#to simulate windows paths

executetask "action=scan&path=$FOLDER_TO_SCAN&targetfolder=/home/zuko"
waitfortask $LAST_TASK_ID

executetask "action=rethumb"
waitfortask $LAST_TASK_ID

#check if all files have been ingested successfully
testIngestionCount

#move 2 files between folders
executebrowse "folder=/&size=10"
testassert $LAST_BROWSE_COUNT 1 "Checking / folder count"
executebrowse "folder=/home&size=10"
testassert $LAST_BROWSE_COUNT 2 "Checking /home folder count"
executebrowse "folder=/home/zuko2&size=1000"
testassert $LAST_BROWSE_COUNT 0 "Checking /home/zuko2 folder count"
executebrowse "folder=/home/zuko/&size=123"
testassert $LAST_BROWSE_COUNT 123 "Checking /home/zuko folder count"
executebrowse "folder=/home/zuko/&size=9000"
testassert $LAST_BROWSE_COUNT $TOTAL_FILES_TO_BE_INGESTED "Checking /home/zuko folder count"

#choose 3 files to test move & delete operations
FILE_TO_MOVE1=${LAST_BROWSE_IDS[5]}
FILE_TO_MOVE2=${LAST_BROWSE_IDS[10]}
FILE_TO_MOVE3=${LAST_BROWSE_IDS[11]}
FILE_TO_MOVE4=${LAST_BROWSE_IDS[12]}
FILE_TO_DELETE1=${LAST_BROWSE_IDS[15]}
#for (( POS = 0 ; POS < ${#LAST_BROWSE_IDS[@]} ; POS++ )) do
#	echo ${LAST_BROWSE_IDS[$POS]}
#done 
executetask "action=move&id=$FILE_TO_MOVE1&folder=/home/zuko2"
executetask "action=move&id=$FILE_TO_MOVE2&folder=/home/zuko2"
executetask "action=move&id=$FILE_TO_MOVE3&folder=/home"
executetask "action=move&id=$FILE_TO_MOVE4&folder=/home"

#test basic browse
executebrowse "folder=/home/zuko2&size=1000"
testassert $LAST_BROWSE_COUNT 2 "Checking /home/zuko2 folder count"		#should return 2 files

executebrowse "folder=/home&size=9000"
testassert $LAST_BROWSE_COUNT 4 "Checking /home/ folder count"			#should return 2 folders and 2 files

executebrowse "folder=/home/zuko/&size=9000"
testassert $LAST_BROWSE_COUNT $(($TOTAL_FILES_TO_BE_INGESTED-4)) "Checking /home/zuko folder count"	#4 files have been moved out from /home/zuko

#test search
executesearch '{
   "query": {
      "folder": "/home/user",
      "all": "MAGNETIC"
   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
      "size": 20,
      "from": 0
   },
   "sort": {
      "field": "fileuploaddate",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 0 "Searching in /home/user for ^MAGNETIC^"			#should return 0 files

executesearch '{
   "query": {
      "folder": "/home/zuko",
      "all": "MAGNETIC"
   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
      "size": 20,
      "from": 0
   },
   "sort": {
      "field": "fileuploaddate",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 2 "Searching in /home/zuko for ^MAGNETIC^"			#should return 2 files

FILE_TO_MOVE=${LAST_SEARCH_IDS[0]}
executetask "action=move&id=$FILE_TO_MOVE&folder=/home/zuko/otherfiles"

executesearch '{
   "query": {
      "folder": "/home/zuko",
      "all": "MAGNETIC"
   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
      "size": 20,
      "from": 0
   },
   "sort": {
      "field": "fileuploaddate",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 1 "Searching in /home/zuko for ^MAGNETIC^"			#should return 1 file

executesearch '{
   "query": {
      "folder": "/home",
      "all": "*geo*"
   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
      "size": 20,
      "from": 0
   },
   "sort": {
      "field": "fileuploaddate",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 0 "Searching in /home for ^*geo*^"			#should return 0 files

executesearch '{
   "query": {
      "folder": "/home/*",
      "all": "*geo*"
   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
      "size": 20,
      "from": 0
   },
   "sort": {
      "field": "fileuploaddate",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 13 "Searching in /home/* for ^*geo*^"			#should return 13 files

executesearch '{
   "query": {
      "folder": "/home*",
      "all": "*geo*"
   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
      "size": 20,
      "from": 0
   },
   "sort": {
      "field": "fileuploaddate",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 13 "Searching in /home* for ^*geo*^"			#should return 13 files

executesearchall "*geo*"
testassert $LAST_SEARCH_COUNT 10 "SearchingAll (size 10) for ^*geo*^"

executesearchall "*geo*" 20
testassert $LAST_SEARCH_COUNT 13 "SearchingAll (size 20) for ^*geo*^"


executesearch '{
   "query": {
      "filename": "jpg",
        "location" : { "distance" : "1m", "lat" : 56.0125, "lon" : 14.462778 }

   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
   },
   "sort": {
      "field": "score",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 2 "GEO Searching for 1m distance"

executesearch '{
   "query": {
      "filenamena": "testJPEG_commented_xnviewmp026.jpg",
        "location" : { "distance" : "1m", "lat" : 56.0125, "lon" : 14.462778 }

   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
   },
   "sort": {
      "field": "score",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 1 "GEO Searching for 1m distance"

executesearch '{
   "query": {
      "filename": "testJPEG_commented_xnviewmp026.jpg",
        "location" : { "distance" : "1m", "lat" : 56.0125, "lon" : 14.462778 }

   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
   },
   "sort": {
      "field": "score",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 2 "GEO Searching for 1m distance"

executesearch '{
   "query": {
        "location" : { "distance" : "100000000000m", "lat" : 56.0125, "lon" : 14.462778 }

   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
   },
   "sort": {
      "field": "score",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 5 "GEO Searching for 100000000000m distance"

executesearch '{
   "query": {
        "location" : { "box" : [{"lat" : 55, "lon" : -55}, {"lat" : 10, "lon" : 0}] }

   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
   },
   "sort": {
      "field": "score",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 3 "Box GEO Searching"

executesearch '{
   "query": {
        "location" : { "polygon" : [{"lon" : -5.2, "lat" : 52}, {"lon" : 14.5, "lat" : 57}, {"lon" : 14.5, "lat" : 0}, {"lon" : -5.2, "lat" : 0}] }
   },
   "options": {
      "scanembedded": "true",
      "showrequest": "true",
   },
   "sort": {
      "field": "score",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 4 "Polygon GEO Searching"

executetask "action=move&id=$FILE_TO_MOVE&folder=/home/zuko"
executetask "action=rmdir&folder=/home/zuko/otherfiles"


executesearch '{
   "query": { "folder" : "/home/*",
      "imagewidth": 64,
      "filesize": {
         "from": 24000,
         "to": 50000
      }
   },
   "options": {
      "scanembedded": "true",
      "size": 20,
      "from": 0, "highlight" : "true", "showrequest" : "true"
   },
   "sort": {
      "field": "fileuploaddate",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 1 "Searching for imagewidth=64 and filezize between 24k and 50k in /home*"

executesearch '{
   "query": { "folder" : "/home",
      "imagewidth": 64,
      "filesize": {
         "from": 24000,
         "to": 50000
      }
   },
   "options": {
      "scanembedded": "true",
      "size": 20,
      "from": 0, "highlight" : "true", "showrequest" : "true"
   },
   "sort": {
      "field": "fileuploaddate",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 0 "Searching for imagewidth=64 and filezize between 24k and 50k in /home"

executesearch '{
   "query": {
      "filename": "jpg",
      "filemeta.metavaluetext": "Adobe",
      "filesize": {
         "from": 24000,
         "to": 5000000
      }
   },
   "options": {
      "scanembedded": "true",
      "highlight" : "true",
   },
   "sort": {
      "field": "score",
      "direction": "desc"
   }
}'
testassert $LAST_SEARCH_COUNT 2 "Searching for text Adobe"


executesearch '{
    "query" : {
        "filethumb_sameasimage" : "false"
    },
    "options" : {
        "showrequest" : "true",
        "scanembedded" : "false"
    }
}'
testassert $LAST_SEARCH_COUNT 3 "Searching for filethumb_sameasimage=false"
FILE_TO_CHECK_THUMB=${LAST_SEARCH_IDS[0]}
executedownload $FILE_TO_CHECK_THUMB "/tmp/myfile1.bin" "thumb"
IMGWIDTH=`exiv2 "/tmp/myfile1.bin" 2>/dev/null | grep "Image size" | sed 's/  */ /g' | cut -d' ' -f4`
testassert $IMGWIDTH 360 "Checking thumb image width=360 for $FILE_TO_CHECK_THUMB"
getfileinfo $FILE_TO_CHECK_THUMB "filethumb_thumbdate"
CVALUE1=$LAST_INFO_VALUE
executetask "action=rethumb"
waitfortask $LAST_TASK_ID
getfileinfo $FILE_TO_CHECK_THUMB "filethumb_thumbdate"
CVALUE2=$LAST_INFO_VALUE
teststringassertn "$CVALUE1" "$CVALUE2" "Checking thumb date update $CVALUE1 != $CVALUE2 for $FILE_TO_CHECK_THUMB"

executesearch '{
    "query" : {
        "filethumb_sameasimage" : "true",
        "filetype" : "image/jpeg"
    },
    "options" : {
        "showrequest" : "true",
        "scanembedded" : "false",
	"size" : 20
    }
}'
testassert $LAST_SEARCH_COUNT 10 "Searching for filethumb_sameasimage=true"

executesearch '{
    "query" : {
        "filethumb_sameasimage" : "true",
        "filetype" : "image/jpeg",
        "imagewidth" : 103
    },
    "options" : {
        "showrequest" : "true",
        "scanembedded" : "false"
    }
}'
testassert $LAST_SEARCH_COUNT 1 "Searching for filethumb_sameasimage=true"
FILE_TO_CHECK_THUMB=${LAST_SEARCH_IDS[0]}
executedownload $FILE_TO_CHECK_THUMB "/tmp/myfile2.bin" "thumb"
IMGWIDTH=`exiv2 "/tmp/myfile2.bin" 2>/dev/null | grep "Image size" | sed 's/  */ /g' | cut -d' ' -f4`
testassert $IMGWIDTH 103 "Checking thumb image width=103"
#imagewidth

# play with actions
executetask "action=delete&id=$FILE_TO_DELETE1"		#delete 1 file from /home/zuko/
executebrowse "folder=/home/zuko&size=9000"
testassert $LAST_BROWSE_COUNT $(($TOTAL_FILES_TO_BE_INGESTED-5)) "Checking /home/zuko folder count"	#4 files have been moved out from /home/zuko and 1 file deleted

executetask "action=rmdir&folder=/home/zuko////////"	#delete folder /home/zuko

executebrowse "folder=/home/&size=9000"
testassert $LAST_BROWSE_COUNT 3 "Checking /home folder count"	#2 files and zuko2 folder

executebrowse "folder=/home/zuko2&size=1000"
testassert $LAST_BROWSE_COUNT 2 "Checking /home/zuko2 folder count"

executetask "action=move&id=$FILE_TO_MOVE2&folder=/"

executebrowse "folder=/home/zuko2&size=1000"
testassert $LAST_BROWSE_COUNT 1 "Checking /home/zuko2 folder count"

executetask "action=rmdir&folder=/home"	#delete folder /home

executebrowse "folder=/&size=1000"
testassert $LAST_BROWSE_COUNT 1 "Checking / folder count"	#1 file, because root folder is never returned (folderlevel=0)

executetask "action=rmdir&folder=/"	#try to delete folder / shoudl show WARNING - error code 60

executetask "action=move&id=$FILE_TO_MOVE2&folder=/home/zuko3"	#move file to not existing folder

executebrowse "folder=/&size=1000"
testassert $LAST_BROWSE_COUNT 1 "Checking / folder count"	#0 files, root folder is never returned (folderlevel=0), only /home folder should be returned

executebrowse "folder=/home/zuko3///&size=1000"
testassert $LAST_BROWSE_COUNT 1 "Checking /home/zuko3/ folder count"	#1 file

#test upload and download
RANDOMFILE=`ls /home/user/workspace/elasticwarehouse.core/src/test/resources | shuf -n 1`
executeupload $FOLDER_TO_SCAN $RANDOMFILE "/home/zuko3/"
testassert $UPLOADED_VERSION 1 "Checking /home/zuko3/$RANDOMFILE uploaded file version"

executeupload $FOLDER_TO_SCAN $RANDOMFILE "/home/zuko3/" $UPLOADED_ID
testassert $UPLOADED_VERSION 2 "Checking /home/zuko3/$RANDOMFILE uploaded file version"

executedownload $UPLOADED_ID "/tmp/myfile3.bin"
FILESIZE1=$(stat -c%s "$FOLDER_TO_SCAN/$RANDOMFILE")
FILESIZE2=$(stat -c%s "/tmp/myfile3.bin")
testassert $FILESIZE2 $FILESIZE1 "Comparing file size for downloaded $UPLOADED_ID"







#test recurrence scan
executetask "action=scan&path=$FOLDER_TO_RECURRENCE_SCAN&recurrence=true"
waitfortask $LAST_TASK_ID

executebrowse "folder=$FOLDER_TO_RECURRENCE_SCAN"
testassert $LAST_BROWSE_COUNT 2 "Checking $FOLDER_TO_RECURRENCE_SCAN folder count"	#2 folders

executebrowse "folder=$FOLDER_TO_RECURRENCE_SCAN/folder1"
testassert $LAST_BROWSE_COUNT 1 "Checking $FOLDER_TO_RECURRENCE_SCAN/folder1 folder count"	#1 folder

executebrowse "folder=$FOLDER_TO_RECURRENCE_SCAN/folder2"
testassert $LAST_BROWSE_COUNT 2 "Checking $FOLDER_TO_RECURRENCE_SCAN/folder2 folder count"	#2 folders

executebrowse "folder=$FOLDER_TO_RECURRENCE_SCAN/folder2/folder3"
testassert $LAST_BROWSE_COUNT 2 "Checking $FOLDER_TO_RECURRENCE_SCAN/folder2/folder3 folder count"	#2 files

executebrowse "folder=$FOLDER_TO_RECURRENCE_SCAN/folder1/folder1a"
testassert $LAST_BROWSE_COUNT 2 "Checking $FOLDER_TO_RECURRENCE_SCAN/folder1/folder1a folder count"	#1 folder + 1 file


