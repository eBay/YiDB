#!/bin/bash

CMS_HOME=~/cms
CATALINA_HOME=/home/cms/apache-tomcat-6.0.35
LOG_DATE=`date +%F-%H-%M-%S`

echo "archive the tomcat logs"
tar czvf $CMS_HOME/logs/tomcat-log.$LOG_DATE.tar.gz $CATALINA_HOME/logs
rm $CATALINA_HOME/logs/*

