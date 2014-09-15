#/bin/sh

CMS_CORE_DIR=$(dirname $0)/../cms-core
CMS_UI_DIR=$(dirname $0)/../cms-ui

cd $CMS_CORE_DIR
mvn -DskipTests clean install

cd $CMS_UI_DIR
mvn -DskipTests clean install

