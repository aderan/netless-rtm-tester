#!/bin/sh

while read line
do     
    # echo $line
    ./build/rtmServerDemo $line &
done < lines.txt