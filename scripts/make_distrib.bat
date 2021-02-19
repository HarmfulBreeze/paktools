@echo off
pushd "%~dp0..\out"
for /f %%i in ('jdeps --print-module-deps artifacts\paktools_jar\paktools.jar') do set MODULES=%%i
rmdir /s /q distrib
mkdir distrib
jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules %MODULES% --output distrib\java-distrib
copy artifacts\paktools_jar\paktools.jar distrib\java-distrib\bin\paktools.jar
echo @echo off> distrib\paktools.bat
echo java-distrib\bin\java -jar java-distrib\bin\paktools.jar %%*>> distrib\paktools.bat
popd
