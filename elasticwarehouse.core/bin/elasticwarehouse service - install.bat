@echo off
call elasticwarehouse.boot.bat
nssm.exe install "ElasticWarehouse Service" "%JAVA_HOME%\bin\java" %JAVA_OPTS% %ES_JAVA_OPTS% %ES_PARAMS% %* -cp "%ES_CLASSPATH%" "org.elasticwarehouse.bootstrap.App"
nssm.exe start "ElasticWarehouse Service"
pause
