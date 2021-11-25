#!/bin/bash


CSC="mcs"
HDR="Task.cs Entity.cs AsyncSocket.cs"
OPT="/reference:MsgPack.dll"



if [ -n $1 ]; then
    ${CSC} ${OPT} ${1} ${HDR}
else
    ${CSC} ${OPT} ${HDR}
fi


