#!/bin/bash

# this script is used to load all meta classes in all repositories into memory

echo "start loading metadta of all repositories"
repos=$( curl -s http://localhost:8080/cms/repositories | python -c "import json,sys;obj=json.loads(sys.stdin.read());obj[\"result\"]=obj[\"result\"] if obj.has_key(\"result\") else [];print \",\".join(map(lambda x: x[\"repositoryName\"], obj[\"result\"]))" )

IFS=","
repos=( $repos )

for r in ${repos[@]}
do
    echo "load all metadata in $r"
    code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/cms/repositories/"$r"/metadata)
    if [[ $code -ne 200 ]]
    then
        echo "ERROR:  fail to load all metadata in $r"
    fi
done
echo "load metadata of all repositories done"
