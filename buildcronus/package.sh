#!/bin/bash

rm -rf target
mkdir target
curl -sS 'https://raw.githubusercontent.com/yubin154/cronusagent/master/agent/scripts/cronus_package/package.sh' | DIR=. appName=yims version=0.1.1 platform=x86_ubuntu bash
mv *.cronus* target/

