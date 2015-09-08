#!/bin/bash
HOST=http://localhost
PORT=10200
CMD=
GET_ID=
GET_TARGETPATH=
UPLOAD_FILE=
UPLOAD_FOLDER=
UPLOAD_ID=
INFO_SHOWMETA=0
BROWSE_ALL=0
BROWSE_FOLDER=
BROWSE_FROM=0
BROWSE_TO=0
BROWSE_SIZE=
SEARCH_Q=
SEARCH_HIGHLIGHT=0

SHOWGETLINKS=0
AVAILABLECOMMANDS="browse get upload info searchall"
#SHOW_REQUEST="&showrequest=true"

function sampleusage()
{
	BNAME=`basename $0`
	echo "
Shell command tool for ElasticWarehouse (v1.3)

Usage: $0 -c [COMMAND] [ATTRIBUTES] [OPTIONS]...

Commands:
    -c              Choose one of: browse get upload info searchall

Global options:
    --server        Attribute to provide ElasticWarehouse hostname. Default is http://localhost
    --port          Attribute to provide ElasticWarehouse port. Default is 10200

Browse (-c browse):
  Options:
    --all           Show all matched results. As default response from ElasticWarehouse is split to pages 
                    with default size of 10 results. Pagination of results can be done by using the ^from^ 
                    and ^size^ parameters
    --from          The ^from^ parameter defines the offset from the first result you want to fetch. 
    --to            The ^to^ parameter defines the offset from the first result you want to fetch.
    --size          The ^size^ parameter allows you to configure the maximum amount of documents to be returned
    --showgetlinks  For each file link is beeing generated to download binary file content.
  
  Attributes:
    <folder>        An attribute to define name of the folder to be listed

  Examples:  
    $BNAME --server 127.0.0.1 --port 10200 -c browse /home/user --all 1
    $BNAME -c browse /
    $BNAME -c browse /home/user
    $BNAME -c browse /home/user --all 1
    $BNAME -c browse /home/user --from 10
    $BNAME -c browse /home/user --from 10 --to 50
    $BNAME -c browse /home/user --from 10 --size 2
    $BNAME -c browse /home/user --from 10 --size 2 --showgetlinks 1

Search (-c searchall):
  Options:
    --all           Show all matched results. As default response from ElasticWarehouse is split to pages 
                    with default size of 10 results. Pagination of results can be done by using the ^from^ 
                    and ^size^ parameters
    --from          The ^from^ parameter defines the offset from the first result you want to fetch. 
    --to            The ^to^ parameter defines the offset from the first result you want to fetch.
    --size          The ^size^ parameter allows you to configure the maximum amount of documents to be returned
    --highlight     Enables words highlighting. A document context will be returned with all matched words.
    --showgetlinks  For each file link is beeing generated to download binary file content.
  
  Attributes:
    <query>         A search query to be executed
  
  Examples:  
    $BNAME -c searchall Canon
    $BNAME -c searchall Canon --all 1
    $BNAME -c searchall Canon --from 10
    $BNAME -c searchall Canon --from 10 --to 50 --highlight 1
    $BNAME -c searchall Canon --from 10 --size 2 --highlight 1 
    $BNAME -c searchall Canon --from 10 --size 2 --highlight 1 --showgetlinks 1

Upload (-c upload):
  Attributes:
    <filename>      A local filename with path to be uploaded to ElasticWarehouse cluster
    <folder>        An attribute to define name of the folder where file to be uploaded. If folder doesn't 
                    exist, then will be created.
    <id>            File id whose update is being requested. If id is not passed to the request, then file 
                    will be uploaded as a new one.

  Examples:  
    $BNAME -c upload myfile.jpg /home/myfiles
    $BNAME -c upload myfile.jpg /home/myfiles P7beEttbS62MECMYUwln5g

Download options (-c get):
  Attributes:
    <id>            File id whose binary content is being requested
    <folder>        Local path to download file

  Examples:  
    $BNAME -c get P7beEttbS62MECMYUwln5g /tmp
    $BNAME -c get P7beEttbS62MECMYUwln5g

Info options (-c info):
  Options:
    --showmeta      Option to show extracted file meta information 

  Attributes:
    <id>            File id whose detailed information is being requested

  Examples:  
    $BNAME -c info P7beEttbS62MECMYUwln5g
    $BNAME -c info P7beEttbS62MECMYUwln5g --showmeta
"
	
}
function showinfo()
{
    local  msg=$1
	printf "\e[36m-----> %-100s\e[39m\n" "$msg"
}
function showerror()
{
	local  msg=$1
	printf "%-100s: \e[31mERROR\e[39m\n" "$msg"
	sampleusage
	exit 1
}

checkEnv()
{
	command -v curl >/dev/null 2>&1 || { 
		printf "%-100s: \e[31mERROR\e[39m\n" "curl command is not available."
		echo "use:"
		echo "yum install curl"
		echo "or (for debian based nix)":
		echo "apt-get install curl"
		exit 1; 
	}

	command -v jq >/dev/null 2>&1 || { 
		printf "%-100s: \e[31mERROR\e[39m\n" "jq command is not available."
		echo "use:"
		echo "yum install jq"
		echo "or (for debian based nix)":
		echo "apt-get install jq"
		exit 1; 
	}
}

readCommandLineAttributes()
{
	if [ $# -lt 1 ] ; then
		sampleusage
		exit 1
	fi

	while [[ $# > 1 ]]
	do
		key="$1"
		shift
		#echo "key=$key"
		case $key in
			-s|--server)
			HOST="$1"
			shift
			;;
			-p|--port)
			PORT=$1
			shift
			;;
			--from)
			BROWSE_FROM=$1
			shift
			;;
			--to)
			BROWSE_TO=$1
			shift
			;;
			--size)
			BROWSE_SIZE=$1
			shift
			;;
			--all)
			BROWSE_ALL=$1
			shift
			;;
			--highlight)
			SEARCH_HIGHLIGHT=$1
			shift
			;;
			--showmeta)
			INFO_SHOWMETA=$1
			shift
			;;
			--showgetlinks)
			SHOWGETLINKS=$1
			shift
			;;
			-c|--cmd|--command)
			CMD=$1
			if [ "$CMD" == "browse" ]; then
				BROWSE_FOLDER=$2
				shift
			fi
			if [ "$CMD" == "get" ]; then
				GET_ID=$2
				GET_TARGETPATH=$3
				shift
			fi
			if [ "$CMD" == "upload" ]; then
				UPLOAD_FILE=$2
				UPLOAD_FOLDER=$3
				UPLOAD_ID=$4
				shift
			fi
			if [ "$CMD" == "info" ]; then
				GET_ID=$2
				shift
			fi
			if [ "$CMD" == "searchall" ]; then
				SEARCH_Q=$2
				shift
			fi
			
			shift
			;;
			*)
			echo "input parameter '$key' not recognized"
			sampleusage
			exit 1
			;;
		esac
	done

	if [ -z $CMD ] ; then
		showerror "Please provide command"
	fi

	[[ $AVAILABLECOMMANDS =~ $CMD ]] && echo '' || showerror "Please provide correct command to be executed. Possible values are: $AVAILABLECOMMANDS"

	

}

checkCommandLineAttributes()
{
	if [ "$CMD" == "browse" ] && [ -z $BROWSE_FOLDER ] ; then
		showerror "Please provide valid parameters for $CMD command"
	fi
	if [ "$CMD" == "upload" ] && [ -z $UPLOAD_FOLDER ] ; then
		showerror "Please provide valid parameters for $CMD command"
	fi
	if [ "$CMD" == "get" ] && [ -z $GET_ID ] ; then
		showerror "Please provide valid ID for $CMD command"
	fi
	if [ "$CMD" == "info" ] && [ -z $GET_ID ] ; then
		showerror "Please provide valid ID for $CMD command"
	fi
	if [ "$CMD" == "searchall" ] && [ -z $SEARCH_Q ] ; then
		showerror "Please provide valid search query for $CMD command"
	fi
}

function executeinfo()
{
	local ID=$1
	local SHOWMETA=$2
	local TMPFILE=/tmp/ewshell$RANDOM.json
	local TMPFILEOUTPUT=/tmp/ewshelloutput$RANDOM.json
	curl -XGET "$HOST:$PORT/_ewinfo?id=$ID$SHOW_REQUEST" > $TMPFILE 2>/dev/null
	#cat $TMPFILE
	
	local version=`cat $TMPFILE | jq -r '.version'`
	local isfolder=`cat $TMPFILE | jq -r '.isfolder'`
	local folder=`cat $TMPFILE | jq -r '.folder'`
	local filesize=`cat $TMPFILE | jq -r '.filesize'`
	local filename=`cat $TMPFILE | jq -r '.filename'`
	local filetype=`cat $TMPFILE | jq -r '.filetype'`
	local filemodificationdate=`cat $TMPFILE | jq -r '.filemodificationdate'`
	local filecreationdate=`cat $TMPFILE | jq -r '.filecreationdate'`
	local fileuploaddate=`cat $TMPFILE | jq -r '.fileuploaddate'`
	local filemodifydate=`cat $TMPFILE | jq -r '.filemodifydate'`
	local children=`cat $TMPFILE | jq -r '.children[]'`
	
	local originhost=`cat $TMPFILE | jq -r '.origin.host'`
	local originsource=`cat $TMPFILE | jq -r '.origin.source'`
	local originpath=`cat $TMPFILE | jq -r '.origin.path'`
	local originfilename=`cat $TMPFILE | jq -r '.origin.filename'`
	
	if [ -z "$filemodifydate" ] ; then
		filemodifydate="n/a"
	fi
	if [ -z "$filemodificationdate" ] ; then
		filemodificationdate="Unknown"
	fi
	if [ -z "$filecreationdate" ] ; then
		filecreationdate="Unknown"
	fi
	echo "" > $TMPFILEOUTPUT

	echo "== File/Folder information ==" >> $TMPFILEOUTPUT
	echo "Current version:|$version" >> $TMPFILEOUTPUT
	if [ "$isfolder" == "true" ]; then
		echo "Type:|Folder" >> $TMPFILEOUTPUT
	else
		echo "Type:|File" >> $TMPFILEOUTPUT
	fi
	echo "Folder:|$folder" >> $TMPFILEOUTPUT
	echo "Filename:|$filename" >> $TMPFILEOUTPUT
	echo "Filesize:|$filesize" >> $TMPFILEOUTPUT
	echo "Type:|$filetype" >> $TMPFILEOUTPUT
	echo "Last modyfication date:|$filemodificationdate" >> $TMPFILEOUTPUT
	echo "File creation date:|$filecreationdate" >> $TMPFILEOUTPUT
	echo "First upload:|$fileuploaddate" >> $TMPFILEOUTPUT
	echo "Last upload:|$filemodifydate" >> $TMPFILEOUTPUT
	echo "Extracted embedded files:|$children" >> $TMPFILEOUTPUT
	
	echo "== Original file information ==" >> $TMPFILEOUTPUT
	echo "Uploaded by node:|$originhost" >> $TMPFILEOUTPUT
	echo "Type of upload:|$originsource" >> $TMPFILEOUTPUT
	echo "Original path:|$originpath" >> $TMPFILEOUTPUT
	echo "Original filename:|$originfilename" >> $TMPFILEOUTPUT
	
	if [ $SHOWMETA -eq 1 ] ; then
		echo "== File meta ==" >> $TMPFILEOUTPUT
		orgIFS=$IFS
		IFS=$'\n'
		local metakeys=( $(cat $TMPFILE | jq -r '.filemeta[].metakey') )
		local CNT=${#metakeys[@]}
		IFS=$orgIFS
		local MAXLEN=0
		for (( POS = 0 ; POS < $CNT ; POS++ )) do
			CLEN=`echo ${metakeys[$POS]} | wc -c`
			MAXLEN=$(( $MAXLEN > $CLEN ? $MAXLEN : $CLEN ))
		done
		local columns=$(tput cols)
		local VALLEN=$(( $columns - $MAXLEN -7 ))
		#echo "VALLEN=$VALLEN"
		#echo "columns=$columns"
		#echo "MAXLEN=$MAXLEN"
		for (( POS = 0 ; POS < $CNT ; POS++ )) do
			local RET=`cat $TMPFILE | jq -r '.filemeta['$POS']'`
			local metavaluelong=`echo $RET | jq -r '.metavaluelong'`
			local metavaluedate=`echo $RET | jq -r '.metavaluedate'`
			local metavaluetext=`echo $RET | jq -r '.metavaluetext'`
			#echo ${metakeys[$POS]}
			if [ -n "$metavaluelong" ] && [ "$metavaluelong" != "null" ] ; then
				echo ${metakeys[$POS]}"(L):|$metavaluelong" | tr -d '\n' >> $TMPFILEOUTPUT
				echo >> $TMPFILEOUTPUT
			elif [ -n "$metavaluedate" ] && [ "$metavaluedate" != "null" ] ; then
				echo ${metakeys[$POS]}"(D):|$metavaluedate" | tr -d '\n' >> $TMPFILEOUTPUT
				echo >> $TMPFILEOUTPUT
			else
				echo ${metakeys[$POS]}"(T):" | tr -d '\n' >> $TMPFILEOUTPUT
				#echo $metavaluetext|sed -r 's/(.{'$VALLEN'})/\1\n/g' >> $TMPFILEOUTPUT
				echo $metavaluetext|sed -r 's/(.{'$VALLEN'})/\1\n/g' | sed 's/^/ |/' >> $TMPFILEOUTPUT
				#echo "abcdefghijklmnopqr"|sed -r 's/(.{'$MYLEN'})/\1\n/g'
			fi
		done
		
		#cat $TMPFILE | jq -r '.filemeta[] | .metakey + "|@#"+(.metavaluelong|tostring)+"#@@#"+(.metavaluedate|tostring)+"#@@#"+.metavaluetext+"#@"' | sed 's/@#null#@//g' | sed 's/@##@//g' | sed 's/@#//g' | sed 's/#@//g' | sort >> $TMPFILEOUTPUT
	fi
	
	cat $TMPFILEOUTPUT | column -s'|' -t
	
	rm $TMPFILE
	#echo "$TMPFILEOUTPUT"
	#rm $TMPFILEOUTPUT
}

function executeupload()
{
	local FILE=$1
	local FOLDER=$2
	local ID=$3
	if [ -z "$FOLDER" ] ; then
		showerror "Folder attribute is mandatory"
	fi
	if [ -r $FILE ] ; then
		local FBASENAME=`basename $FILE`
		if [ -z "$ID" ] ; then
			curl -XPOST "$HOST:$PORT/_ewupload?folder=$FOLDER&filename=$FBASENAME" --data-binary @$FILE
		else
			curl -XPOST "$HOST:$PORT/_ewupload?folder=$FOLDER&filename=$FBASENAME&id=$ID" --data-binary @$FILE
		fi
	else
		showerror "File $FILE is not readable"
	fi
	
}
function executeget()
{
	local ID=$1
	local TARGETPATH=$2
	local RET=`curl -XGET "$HOST:$PORT/_ewinfo?id=$ID$SHOW_REQUEST" 2>/dev/null`
	local FILENAME=`echo $RET | jq -r '.filename'`
	
	if [ -z $TARGETPATH ] ; then
		showinfo "Downloading to: $FILENAME"
		curl -XGET $HOST:$PORT/_ewget?id=$ID > $FILENAME
	else
		showinfo "Downloading to: $TARGETPATH/$FILENAME"
		curl -XGET $HOST:$PORT/_ewget?id=$ID > $TARGETPATH/$FILENAME
	fi
}

function executesearch()
{
	echo "Searching for: $1"
	local Q=$1
	local HIGHLIGHT=$2
	local FROM=$3
	local TO=$4
	local SIZE=$5
	local REQSIZE=
	if [ -z $FROM ] ; then
		FROM=0
	fi
	if [ ! -z $TO ] && [ $TO -ne 0 ]; then
		COMPSIZE=$(( $TO - $FROM ))
		REQSIZE="&size=$COMPSIZE"
	fi
	if [ ! -z $HIGHLIGHT ] && [ $HIGHLIGHT -ne 0 ]; then
		REQHIGHLIGHT="&highlight=true"
	fi
	if [ ! -z $SIZE ] ; then
		REQSIZE="&size=$SIZE"
	fi
	
	local TMPFILE=/tmp/ewshell$RANDOM.json
	local TMPFILESOURCES=/tmp/ewshellsource$RANDOM.json
	curl -XGET "$HOST:$PORT/_ewsearchall?q=$Q$SHOW_REQUEST&from=$FROM$REQSIZE&$REQHIGHLIGHT&pretag=*&posttag=*&fragmentSize=50" > $TMPFILE 2>/dev/null
	cat $TMPFILE | jq -r '.hits.hits[]._source' > $TMPFILESOURCES
	local versions=( $(cat $TMPFILE | jq -r '.hits.hits[]._version') )
	local ids=( $(cat $TMPFILE | jq -r '.hits.hits[]._id') )
	local IDSSIZE=${#ids[@]}
	highlights=()
	
	if [ ! -z $HIGHLIGHT ] && [ $HIGHLIGHT -ne 0 ]; then
		for (( POS = 0 ; POS < $IDSSIZE ; POS++ )) do
			local highlightobject=`cat $TMPFILE | jq -r '.hits.hits['$POS'].highlight' | sed 's/filemeta.metavaluetext/filemetametavaluetext/g'`
			#echo $highlightobject
			local filemetametavaluetext=`echo $highlightobject | jq -r '.filemetametavaluetext[]' 2>/dev/null | sed ':a;N;$!ba;s/\n/, /g'`
			local filename=`echo $highlightobject | jq -r '.filename[]' 2>/dev/null | sed ':a;N;$!ba;s/\n/, /g'`
			local filetitle=`echo $highlightobject | jq -r '.filetitle[]' 2>/dev/null | sed ':a;N;$!ba;s/\n/, /g'`
			local filetext=`echo $highlightobject | jq -r '.filetext[]' 2>/dev/null | sed ':a;N;$!ba;s/\n/, /g'`
			local highlight=''
			if [ ! -z "$filemetametavaluetext" ] ; then 
				highlight+="Meta[$filemetametavaluetext] "
			fi
			if [ ! -z "$filename" ] ; then 
				highlight+="Filename[$filename] "
			fi
			if [ ! -z "$filetitle" ] ; then 
				highlight+="Title[$filetitle] "
			fi
			if [ ! -z "$filetext" ] ; then 
				highlight+="Content[$filetext] "
			fi
			
			#echo $highlight
			highlights+=( "$highlight" )
		done
	fi
	#cat $TMPFILE
	showbrowsesearchresults $TMPFILESOURCES $FROM versions ids highlights
	rm $TMPFILE
	rm $TMPFILESOURCES
	
	if [ ! -z $BROWSE_NEXT_PAGE ]; then
		echo "Notice: To show next page, run: $0 -c searchall $1 --from $BROWSE_NEXT_PAGE"
		echo "Notice: To show next page and change files count limit, run: $0 -c searchall $1 --from $BROWSE_NEXT_PAGE --to $BROWSE_NEXT_PAGE2"
		echo "Notice: To show next page and change files count limit, run: $0 -c searchall $1 --from $BROWSE_NEXT_PAGE --size 40"
		echo "Notice: To show all files, run: $0 -c searchall $1 --all 1"
	fi
}

function executebrowse()
{
	echo "Current folder is: $1"
	local FROM=$3
	local TO=$4
	local SIZE=$5
	local REQSIZE=
	if [ -z $FROM ] ; then
		FROM=0
	fi
	if [ ! -z $TO ] && [ $TO -ne 0 ]; then
		COMPSIZE=$(( $TO - $FROM ))
		REQSIZE="&size=$COMPSIZE"
	fi
	if [ ! -z $SIZE ] ; then
		REQSIZE="&size=$SIZE"
	fi
	
	local TMPFILE=/tmp/ewshell$RANDOM.json
	local TMPFILESOURCES=/tmp/ewshellsource$RANDOM.json
	curl -XGET "$HOST:$PORT/_ewbrowse?$2$SHOW_REQUEST&from=$FROM$REQSIZE" > $TMPFILE 2>/dev/null
	cat $TMPFILE | jq -r '.hits.hits[]._source' > $TMPFILESOURCES
	local versions=( $(cat $TMPFILE | jq -r '.hits.hits[]._version') )
	local ids=( $(cat $TMPFILE | jq -r '.hits.hits[]._id') )
	showbrowsesearchresults $TMPFILESOURCES $FROM versions ids
	rm $TMPFILE
	rm $TMPFILESOURCES
	
	if [ ! -z $BROWSE_NEXT_PAGE ]; then
		echo "Notice: To show next page, run: $0 -c browse $1 --from $BROWSE_NEXT_PAGE"
		echo "Notice: To show next page and change files count limit, run: $0 -c browse $1 --from $BROWSE_NEXT_PAGE --to $BROWSE_NEXT_PAGE2"
		echo "Notice: To show next page and change files count limit, run: $0 -c browse $1 --from $BROWSE_NEXT_PAGE --size 40"
		echo "Notice: To show all files, run: $0 -c browse $1 --all 1"
	fi
}
function showbrowsesearchresults()
{
	local TMPFILESOURCES=$1
	local FROM=$2
	name1=$3[@]
	name2=$4[@]
	name3=$5[@]
    local versions=("${!name1}")
	local ids=("${!name2}")
	local highlights=("${!name3}")
	local highlightsCNT=${#highlights[@]}
	#local TMPFILE=/tmp/ewshell$RANDOM.json
	#local TMPFILESOURCES=/tmp/ewshellsource$RANDOM.json
	local TMPFILEOUTPUT=/tmp/ewshell$RANDOM.list
	local TMPFILEHIGHLIGHTOUTPUT=/tmp/ewshellhighlight$RANDOM.list
	local TMPFILEGETLINKSOUTPUT=/tmp/ewshelllinks$RANDOM.list
	#curl -XGET "$HOST:$PORT/_ewbrowse?$2$SHOW_REQUEST&from=$FROM$REQSIZE" > $TMPFILE 2>/dev/null
	#local TOTAL=`echo $RET | ./jq -r '.hits.total'`
	local TOTAL=`cat $TMPFILE | jq -r '.hits.total'`
	local TOTALBYTES=0
	
	#cat $TMPFILE | jq -r '.hits.hits[]._source' > $TMPFILESOURCES
	#local versions=( $(cat $TMPFILE | jq -r '.hits.hits[]._version') )
	#local ids=( $(cat $TMPFILE | jq -r '.hits.hits[]._id') )
	local isfolders=( $(cat $TMPFILESOURCES | jq -r '.isfolder') )
	local folders=( $(cat $TMPFILESOURCES | jq -r '.folder') )
	orgIFS=$IFS
	IFS=$'\n'
	local filenames=( $(cat $TMPFILESOURCES | jq -r '.filename' | sed 's/^$/folder/') )
	local fileuploaddates=( $(cat $TMPFILESOURCES | jq -r '.fileuploaddate') )
	local filemodifydates=( $(cat $TMPFILESOURCES | jq -r '.filemodifydate') )
	IFS=$orgIFS
	local filesizes=( $(cat $TMPFILESOURCES | jq -r '.filesize') )
	echo "" > $TMPFILEOUTPUT
	echo "" > $TMPFILEHIGHLIGHTOUTPUT
	echo "" > $TMPFILEGETLINKSOUTPUT
	
	local PAGESIZE=${#ids[@]}
	
	local MAXLEN=0
	for (( POS = 0 ; POS < $PAGESIZE ; POS++ )) do
		CLEN=`echo ${filenames[$POS]} | wc -c`
		MAXLEN=$(( $MAXLEN > $CLEN ? $MAXLEN : $CLEN ))
	done
	local columns=$(tput cols)
	local VALLEN=$(( $columns - $MAXLEN -7 ))
		
	for (( POS = 0 ; POS < $PAGESIZE ; POS++ )) do	
		local version=${versions[$POS]}
		local idd=${ids[$POS]}
		local highlight=${highlights[$POS]}
		
		local isfolder=${isfolders[$POS]}
		local folder=${folders[$POS]}
		local filename=${filenames[$POS]}
		local fileuploaddate=${fileuploaddates[$POS]}
		local filemodifydate=${filemodifydates[$POS]}
		local filesize=${filesizes[$POS]}
		
		TOTALBYTES=$(( $TOTALBYTES + $filesize ))

		local TG_FLDR="f"
		local TG_NAME=""
		if [ "$isfolder" == "true" ]; then
			TG_FLDR="d"
		fi
		if [ "$isfolder" == "true" ]; then
			TG_NAME=$folder
		else
			TG_NAME=$filename
		fi
		if [ "$filemodifydate" == "null" ]; then
			filemodifydate="n/a        n/a"
		fi

		#local BROWSEHITSCNT=${#BROWSEHITS[@]}
		#echo "cnt=$BROWSEHITSCNT"
	
#		echo $BROWSEHIT
		echo "$TG_FLDR|$version|$TG_NAME|$fileuploaddate|$filemodifydate|$filesize|bytes|$idd" >> $TMPFILEOUTPUT
		
		
		
		echo "$TG_NAME:" | tr -d '\n' >> $TMPFILEHIGHLIGHTOUTPUT
		echo $highlight|sed -r 's/(.{'$VALLEN'})/\1\n/g' | sed 's/^/ |/' >> $TMPFILEHIGHLIGHTOUTPUT
		#echo "$TG_NAME|$highlight" >> $TMPFILEHIGHLIGHTOUTPUT
		if [ $SHOWGETLINKS -eq 1 ] ; then
			echo "$TG_NAME|$HOST:$PORT/_ewget?id=$idd" >> $TMPFILEGETLINKSOUTPUT
		fi
	done

	#cat $TMPFILEOUTPUT
	
	(printf "TYPE|VER|FILENAME|UPLOADDATE UPLOADTIME|MODIFYDATE MODIFYTIME|SIZE|UNIT|ID\n" ; cat $TMPFILEOUTPUT | sed 1d ) | column -s'|' -t
	echo "Total $TOTAL files ($PAGESIZE on page, $TOTALBYTES bytes)"
	
	#echo "highlightsCNT=$highlightsCNT";
	if [ $highlightsCNT -ne 0 ] ; then 
		echo "Highlighted content:"
		(printf "FILE|CONTENT FOUND\n" ; cat $TMPFILEHIGHLIGHTOUTPUT | sed 1d ) | column -s'|' -t
	fi
	if [ $SHOWGETLINKS -eq 1 ] ; then
		echo "Download links:"
		(printf "FILE|LINK\n" ; cat $TMPFILEGETLINKSOUTPUT | sed 1d ) | column -s'|' -t
	fi
	if [ $PAGESIZE -lt $TOTAL ]; then
		local NEXTPAGE=$(( $FROM + $PAGESIZE ))
		local NEXTPAGE2=$(( $NEXTPAGE + $PAGESIZE ))
		if [ $PAGESIZE -ne 0 ]; then
			TOTAL_PAGES=$(( $TOTAL / $PAGESIZE ))
			TOTAL_PAGES=$(( $TOTAL_PAGES + 1 ))
			echo "Files from $FROM to $NEXTPAGE"
		fi
		echo "Notice: Files are counted from 0"
		BROWSE_NEXT_PAGE=$NEXTPAGE
		BROWSE_NEXT_PAGE2=$NEXTPAGE2
	fi
	#rm $TMPFILE
	#rm $TMPFILESOURCES
	rm $TMPFILEOUTPUT
	rm $TMPFILEHIGHLIGHTOUTPUT
	rm $TMPFILEGETLINKSOUTPUT
}

# check
checkEnv

# read command line attributes and stop script execution if validation failed
readCommandLineAttributes $*

# check
checkCommandLineAttributes

if [ "$CMD" == "browse" ]; then
	#echo $CMD
	#echo $BROWSE_FOLDER
	#echo $BROWSE_FROM
	#echo $BROWSE_TO
	#echo $BROWSE_SIZE
	if [ $BROWSE_ALL -eq 1 ]; then
		executebrowse $BROWSE_FOLDER "folder=$BROWSE_FOLDER&size=999999999"
	else
		executebrowse $BROWSE_FOLDER "folder=$BROWSE_FOLDER" $BROWSE_FROM $BROWSE_TO $BROWSE_SIZE
	fi
fi

if [ "$CMD" == "get" ]; then
	executeget $GET_ID $GET_TARGETPATH
fi
if [ "$CMD" == "upload" ]; then
	executeupload $UPLOAD_FILE $UPLOAD_FOLDER $UPLOAD_ID
fi
if [ "$CMD" == "info" ]; then
	executeinfo $GET_ID $INFO_SHOWMETA
fi
if [ "$CMD" == "searchall" ]; then
	if [ $BROWSE_ALL -eq 1 ]; then
		executesearch "$SEARCH_Q" $SEARCH_HIGHLIGHT 0 999999999
	else
		executesearch "$SEARCH_Q" $SEARCH_HIGHLIGHT $BROWSE_FROM $BROWSE_TO $BROWSE_SIZE
	fi
fi


