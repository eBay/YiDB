#/bin/sh

##############################################
# This script is used to start a local cms server for test.
# It need a local running mongo server to run. 
#
# If a -initData is provided as the first parameter, it 
# will clean all the existing data in mongo and load test data.  
##############################################

CMS_CORE_WEB_DIR=$(dirname $0)/../cms-core/web

cd $CMS_CORE_WEB_DIR 

export CMS_HOME=../../bin;
export CMS_UI=../../cms-ui/src/main/webapp;
#echo $CMS_UI
if [ $1 ] ; then
  mvn exec:java -Dexec.mainClass=com.ebay.cloud.cms.web.RunTestServer -Dexec.classpathScope=test -Dexec.args="$1"
else
  mvn exec:java -Dexec.mainClass=com.ebay.cloud.cms.web.RunTestServer -Dexec.classpathScope=test 
fi
