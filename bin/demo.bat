REM This script is used to start a local cms server for test.
REM It need a local running mongo server to run. 

REM If a -initData is provided as the first parameter, it 
# will clean all the existing data in mongo and load test data.  


set WORKING_DIRECTORY=%cd%
set CMS_CORE_WEB_DIR=%WORKING_DIRECTORY%\..\cms-core\web


echo %CMS_CORE_WEB_DIR%


set CMS_UI=%WORKING_DIRECTORY%\..\cms-ui\src\main\webapp
echo %CMS_UI%
cd %CMS_CORE_WEB_DIR% 
mvn exec:java -Dexec.mainClass=com.ebay.cloud.cms.web.RunTestServer -Dexec.classpathScope=test 
cd %WORKING_DIRECTORY%