#!/bin/sh
if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

function sampleusage()
{
	echo "Usage:"
	echo "$0 <username>"
	echo "If username doesn't exist, then will be created"
	echo ""
	echo "Example:"
	echo "$0 elasticwarehouse"
	echo "$0 myfunnyuser"
}

if [ $# -lt 1 ] ; then
    sampleusage
    exit 1
fi 

SCRIPT="$0"
EW_HOME=`dirname "$SCRIPT"`/..
EW_HOME=`cd "$EW_HOME"; pwd`

USERNAME=$1
useradd $USERNAME
cp ./elasticwarehousedaemon /etc/init.d
#sed -i -e "s/MYPATHTOKEN/\$EW_HOME/g" /etc/init.d/elasticwarehousedaemon

cd /etc/rc0.d/
ln -s ../init.d/elasticwarehousedaemon K20elasticwarehousedaemon
cd /etc/rc1.d/
ln -s ../init.d/elasticwarehousedaemon K20elasticwarehousedaemon
cd /etc/rc2.d/
ln -s ../init.d/elasticwarehousedaemon S80elasticwarehousedaemon
cd /etc/rc3.d/
ln -s ../init.d/elasticwarehousedaemon S80elasticwarehousedaemon
cd /etc/rc4.d/
ln -s ../init.d/elasticwarehousedaemon S80elasticwarehousedaemon
cd /etc/rc5.d/
ln -s ../init.d/elasticwarehousedaemon S80elasticwarehousedaemon
cd /etc/rc6.d/
ln -s ../init.d/elasticwarehousedaemon K20elasticwarehousedaemon

echo "Home: $EW_HOME"

SYSCONFFILE=/etc/sysconfig/elasticwarehouse
echo "CONF_DIR=$EW_HOME/config" > $SYSCONFFILE
echo "CONF_FILE=$EW_HOME/config/elasticsearch.yml" >> $SYSCONFFILE
echo "ES_GROUP=$USERNAME" >> $SYSCONFFILE
echo "ES_HEAP_SIZE=2g" >> $SYSCONFFILE
echo "ES_HOME=$EW_HOME" >> $SYSCONFFILE
echo "EW_HOME=$EW_HOME" >> $SYSCONFFILE
echo "ES_USER=$USERNAME" >> $SYSCONFFILE
echo "LOG_DIR=$EW_HOME/logs" >> $SYSCONFFILE

