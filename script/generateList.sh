#!/bin/bash

p=`pwd`
for f in `ls $1*`;
do
    echo "$p/$f"
done

