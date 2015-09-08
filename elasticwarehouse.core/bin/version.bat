@echo off
SETLOCAL
call elasticwarehouse.boot.bat

TITLE ElasticWarehouse

"%JAVA_HOME%\bin\java" %JAVA_OPTS% %ES_JAVA_OPTS% %ES_PARAMS% %* -cp "%ES_CLASSPATH%" "org.elasticwarehouse.core.Version"
goto finally

:err
echo JAVA_HOME environment variable must be set!
pause


:finally

ENDLOCAL

