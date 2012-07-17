#!/bin/bash

STRATOSPHERE_HOME=$HOME/workspacePACT/stratosphere/stratosphere-dist/target/stratosphere-dist-0.1.2-bin/stratosphere-0.1.2/bin
MAX_ITERATION=10
flag=0
NEURALNET_HOME=$HOME/workspacePACT/NeuralNetworking
RESULT_FILE=/tmp/neuralResult.txt

iter=0
while [$flag -eq 0]
do

  #start execution
  $NEURALNET_HOME/script/run.sh
  
  #wait the program run 
  $STRATOSPHERE_HOME/pact-client.sh list -r -s |grep Neural
  
  while [$? -eq 0]
  do
    sleep(1000)
    $STRATOSPHERE_HOME/pact-client.sh list -r -s |grep Neural
  done
  
  #check to stop processing
  if [ -f $RESULT_FILE]; then
      grep "Reprocess=NO" $RESULT_FILE
      if [$? -eq 0];then
	$flag=1;
      fi
  fi

  if [ $iter -gt $ MAX_ITERATION];then
    $flag=1;
  fi
  
  $iter=$($iter + 1)

  //randomize input

done