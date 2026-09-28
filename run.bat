@echo off
chcp 65001 >nul
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8
C:\Users\aleks\apache-maven\apache-maven-3.9.16\bin\mvn.cmd clean compile exec:java