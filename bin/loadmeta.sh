#/bin/sh

CMS_CORE_META_DIR=$(dirname $0)/../cms-core/metadata

cd $CMS_CORE_META_DIR 

export CMS_HOME=../../bin;
export CMS_UI=../../cms-ui/src/main/webapp;

echo $1 $2
mvn exec:java -Dexec.mainClass=com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader -Dexec.classpathScope=test -Dexec.args="$1 $2"
