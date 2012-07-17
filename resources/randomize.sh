#!/bin/bash

#input=$1
#lines=$2
input="bank_data_norm.txt" 
lines=100
prefix="trainSet" 
outList="dataSet.txt"
testFile="testSet.txt"


echo $lines
echo $input

rm $prefix*
cp $input aaa.tmp
rm $input
shuf aaa.tmp > $input
split -l $lines $input $prefix

rm aaa.tmp
./generateList.sh $prefix > bbb.tmp
head -n 4 bbb.tmp > $outList
a=`shuf -n 1 bbb.tmp`
cp $a $testFile
rm bbb.tmp
