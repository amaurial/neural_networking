#!/bin/bash

JARFILE=/home/amauri/workspacePACT/NeuralNetworking/target/NeuralNetworking.jar
PARAM="1 file:///home/amauri/workspaceAIM3/bankDataset/dataSet.txt file:///tmp/weigths.txt file:///home/amauri/workspaceAIM3/bankDataset/testSet.txt file:///tmp/neuralResult.txt"
STRATOSPHERE_HOME=/home/amauri/workspacePACT/stratosphere/stratosphere-dist/target/stratosphere-dist-0.1.2-bin/stratosphere-0.1.2

mv /tmp/debugneuranet.txt /tmp/debugneuranet.txt.old
mv /tmp/neuralResult.txt /tmp/neuralResult.txt.old

cd $STRATOSPHERE_HOME
./bin/pact-client.sh run -v -j $JARFILE -a $PARAM 
