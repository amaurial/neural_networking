package eu.stratosphere.pact.neuralnetwork;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.Vector;
import java.io.*;
import java.util.Vector;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.Neuron;
import org.neuroph.core.learning.SupervisedTrainingElement;
import org.neuroph.core.learning.TrainingElement;
//import org.neuroph.core.learning.TrainingElement;
import org.neuroph.core.learning.TrainingSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.LMS;
import org.neuroph.util.TransferFunctionType;

public class BankNeuron {

	private float precision;
	private int maxIterations;	
	private NeuralNetwork neuralNet; 
	private int numInput;	
	private int numOutput;	
	private int hiddenLayers;
	private TrainingSet trainingSet = new TrainingSet();
	private String delimiter=null;
	private double result=0;
	private double maxerror=0.001;
	private double lernrate=0.3;
	
	public double getResult() {
		return result;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	private void setArraySize(){
		//inputData=new ArrayList<Float>(numInput+1);
		
	}
	
	public BankNeuron(int numberInputs,int hiddenLayers,int numberOutputs){
		 numInput=numberInputs;
		 numOutput=numberOutputs;
		 this.hiddenLayers=hiddenLayers;
		 maxIterations=10000;
		 initiate();
		 
	}
	public BankNeuron(){
		 //neuralNet = new MultiLayerPerceptron(4, 9, 2);
		 numInput=4;
		 numOutput=1;
		 hiddenLayers=9;
		 maxIterations=10000;
		 initiate();
	}
	public BankNeuron(int inputs,int maxIterations){		 
		 numInput=inputs;
		 hiddenLayers=inputs*2+1;
		 numOutput=1;
		 this.maxIterations=maxIterations;
		 initiate();
		 
		 setArraySize();
	}
	public BankNeuron(int inputs){
		numInput=inputs;
		 hiddenLayers=inputs*2+1;
		 numOutput=1;
		 this.maxIterations=10000;
		 initiate();
		 
		 setArraySize();
	}
	private void initiate(){
		neuralNet = new MultiLayerPerceptron(numInput, hiddenLayers, numOutput);
		setArraySize();
		this.setMaxIterations(maxIterations);
	}
	
    public int getMaxIterations() {
		return maxIterations;
	}
	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}
    
	public void setPrecision(float precision){
		this.precision=precision;
	}
	public float getPrecision(){
		return precision;
	}
	
	public boolean setTrainingSet(String fileName){
		try{
		  String[] sdata;
		  String strLine;
		  
		  trainingSet.clear();
		  
		  FileInputStream fstream = new FileInputStream(fileName);
		  // Get the object of DataInputStream
		  DataInputStream in = new DataInputStream(fstream);
		  BufferedReader br = new BufferedReader(new InputStreamReader(in));
		  
		  //Read File Line By Line
		  while ((strLine = br.readLine()) != null)   {
			  sdata=strLine.split(delimiter);
			  
			  if (sdata.length!=(numInput+numOutput)){
				  //TODO
				  //error
			  }
			  //load data
			  double[] inputData=new double[numInput];
			  double[] outputData=new double[numOutput+1];
			  
			  for (int i=0;i<numInput;i++){
				  inputData[i]=Double.valueOf(sdata[i]);
			  }
			  
			  int j=0;
			  for (int i=(sdata.length-1);i>sdata.length-numOutput-1;i--){
				  outputData[j]=Double.valueOf(sdata[i]);				 
				  j++;
			  }
			  if (outputData[0]==0)  outputData[1]=1;
			  else outputData[1]=0;

			  
			  trainingSet.addElement(new SupervisedTrainingElement(inputData,outputData));
			  
			  
		  }
		  //Close the input stream
		  in.close();
		    }catch (Exception e){//Catch exception if any
		  System.err.println("Error: " + e.getMessage());
		  }
		return true;
	}
	
	public boolean setWeightsFromFile(String fileName){
		try{
		  String[] sdata;
		  String strLine;
		  
		  		  
		  FileInputStream fstream = new FileInputStream(fileName);
		  // Get the object of DataInputStream
		  DataInputStream in = new DataInputStream(fstream);
		  BufferedReader br = new BufferedReader(new InputStreamReader(in));
		  
		  //Read File Line By Line
		  while ((strLine = br.readLine()) != null)   {
			  if (strLine.length()>40){
				  this.setWeights(strLine);
			  }			  
		  }
		  //Close the input stream
		  in.close();
		    }catch (Exception e){//Catch exception if any
		  System.err.println("Error: " + e.getMessage());
		  }
		return true;
	}
	
	public boolean learn(){
		((LMS) neuralNet.getLearningRule()).setMaxError(maxerror);//0-1
        ((LMS) neuralNet.getLearningRule()).setLearningRate(lernrate);//0-1
        ((LMS) neuralNet.getLearningRule()).setMaxIterations(maxIterations);//0-1        

        neuralNet.learnInSameThread(trainingSet);
        
        
		return true;
	}
	
	public String evaluate(String inputData){
		TrainingSet tSet = new TrainingSet();
		String [] stemp=inputData.split(delimiter);
		double[] dData=new double[numInput];
		String sResult="";
		
		
		tSet.clear();
		
		for (int i=0;i<stemp.length-numOutput;i++){
			dData[i]=Double.valueOf(stemp[i]);
		}		
		
		tSet.addElement(new TrainingElement(dData));
		
		for (TrainingElement testElement : tSet.trainingElements()) {			
			neuralNet.setInput(testElement.getInput());
	        neuralNet.calculate();
	        Vector<Double> networkOutput = neuralNet.getOutput();	        
	        
	        result=networkOutput.firstElement();
	        sResult+="Input: " + testElement.getInput();	        
	        sResult+="\n" + "Output: " + networkOutput+"\n";
	        sResult+="Expected Output: " + stemp[stemp.length-numOutput]+"\n";
	        
	        System.out.println("Input: " + testElement.getInput());
	        System.out.println("Output: " + networkOutput);
	        
	        
		}
		
		return sResult;
	}
	
	public String getWeigths(){
		int layers;
		int neurons;
		int weights;
		String stemp=new String();
		Neuron n;
		
		layers=neuralNet.getLayersCount();
		
		for (int i=1;i<layers;i++){
			neurons=neuralNet.getLayerAt(i).getNeuronsCount();
			for (int j=0;j<neurons;j++){
				n=neuralNet.getLayerAt(i).getNeuronAt(j);
				weights=n.getWeightsVector().size();
				for (int m=0;m<weights;m++){
					stemp+=n.getWeightsVector().get(m).getValue()+delimiter;
				}				
			}
		}
		
		return stemp;
		
	}
	public void setWeights(String Weigths){
		int layers;
		int neurons;
		int weights;
		int index=0;
		double w;
		String [] stemp=Weigths.split(delimiter);
		Neuron n;
		
		layers=neuralNet.getLayersCount();
		
		for (int i=1;i<layers;i++){
			neurons=neuralNet.getLayerAt(i).getNeuronsCount();
			for (int j=0;j<neurons;j++){
				n=neuralNet.getLayerAt(i).getNeuronAt(j);
				weights=n.getWeightsVector().size();
				for (int m=0;m<weights;m++){
					w=Double.valueOf(stemp[index]);
					n.getWeightsVector().get(m).setValue(w);
					index++;
				}				
			}
		}
		
	}
	
}
