#!/bin/bash


curl "localhost:8080/provider/log/path/add" -XPOST -d"path=/home/yuma/dev/robocup/ViewerManager/logs"
curl "localhost:8080/provider/log/open" -XPOST -d"path=/home/yuma/dev/robocup/ViewerManager/logs/paris.vlog"
curl "localhost:8080/viewer/open"
curl "localhost:8080/viewer/connect" -XPOST -d"viewerId=0&providerId=0"



#TODO
curl "localhost:8080/viewer/record?viewerId=0" &> /dev/null


curl "localhost:8080/server/shutdown" -XPOST


