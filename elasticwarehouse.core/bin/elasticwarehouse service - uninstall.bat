@echo off
nssm.exe stop "ElasticWarehouse Service"
nssm.exe remove "ElasticWarehouse Service" confirm
pause
