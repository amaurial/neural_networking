package eu.stratosphere.pact.neuralnetwork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.pact.common.contract.FileDataSinkContract;
import eu.stratosphere.pact.common.contract.FileDataSourceContract;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.contract.ReduceContract;
import eu.stratosphere.pact.common.contract.OutputContract.SameKey;
import eu.stratosphere.pact.common.contract.ReduceContract.Combinable;
import eu.stratosphere.pact.common.io.TextInputFormat;
import eu.stratosphere.pact.common.io.TextOutputFormat;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.common.plan.PlanAssembler;
import eu.stratosphere.pact.common.plan.PlanAssemblerDescription;
import eu.stratosphere.pact.common.stub.Collector;
import eu.stratosphere.pact.common.stub.MapStub;
import eu.stratosphere.pact.common.stub.ReduceStub;
import eu.stratosphere.pact.common.type.KeyValuePair;
import eu.stratosphere.pact.common.type.base.PactInteger;
import eu.stratosphere.pact.common.type.base.PactNull;
import eu.stratosphere.pact.common.type.base.PactString;


/**
 * Implements a training of a perceptron neural network
 * @author Amauri Albuquerque
 */
public class NeuralNetworkPact implements PlanAssembler, PlanAssemblerDescription {

		public static String delimiter=";";
		public static int ninputs=14;
		public static void DebugInfo(String msg){
			try{
				  // Create file 
				  FileWriter fstream = new FileWriter("/tmp/debugneuranet.txt",true);
				  BufferedWriter out = new BufferedWriter(fstream);
				  out.write("[DEBUG] " + msg+"\n");
				  //Close the output stream
				  out.close();
				  }catch (Exception e){//Catch exception if any
				  System.err.println("Error: " + e.getMessage());
				  }
		}
	
	
        /**
         * Converts a input string (a line) into a KeyValuePair with the string
         * being the key null and the value the string.
         */
        public static class LineInFormat extends TextInputFormat<PactNull, PactString> {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean readLine(KeyValuePair<PactNull, PactString> pair, byte[] line) {
                        pair.setKey(new PactNull());
                        pair.setValue(new PactString(new String(line)));
                        return true;
                }

        }

       public static class NeuralNetOutFormat extends TextOutputFormat<PactString, PactString> {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public byte[] writeLine(KeyValuePair<PactString, PactString> pair) {
                        String key = pair.getKey().toString();//iteraction
                        String value = pair.getValue().toString();//weights of neural net
                        //String line = key + " " + value + "\n";
                        String line = value + "\n";
                        return line.getBytes();
                }

        }

        /**
         * Receives the data set filename as a value and train the network
         * once it's finished it sends the weights to the reducer         
         */
        public static class TrainNet extends MapStub<PactNull, PactString, PactInteger,PactString> {

        	//private String TrainingSet;
        	private String WeigthsPath;
        	
        	/*
        	 * (non-Javadoc)
        	 * @see eu.stratosphere.pact.common.stub.SingleInputStub#configure(eu.stratosphere.nephele.configuration.Configuration)
        	 * Get the file of the weights
        	 */
        	@Override
        	public void configure(Configuration parameters) {
        		//TrainingSet=parameters.getString("--dataSet", "/tmp/sample.txt");
        		WeigthsPath=parameters.getString("--dataSet", "/tmp/weigths.txt");
        		WeigthsPath=WeigthsPath.replaceAll("file:///", "/");
        		DebugInfo("NeuralNet mapper configure: weights read");
        		
        	}        	
        	
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void map(PactNull key, PactString value, Collector<PactInteger,PactString > out) {

                        
                        String dataSet=value.toString(); 
                        String weights;
                        
                        DebugInfo("NeuralNet mapper:train sets: " + dataSet);                        
                        
                        BankNeuron bank=new BankNeuron(ninputs);
                        bank.setDelimiter(delimiter);
                        //set initial weigths
                        DebugInfo("NeuralNet mapper:set initial weights " + WeigthsPath);
                        bank.setWeightsFromFile(WeigthsPath);
                       // String sinput=this.open().
                        DebugInfo("NeuralNet mapper:set training set");
                        bank.setTrainingSet(dataSet);
                        //train the network   
                        DebugInfo("NeuralNet mapper:learn");
                        bank.learn();
                        //get the new weigths
                        DebugInfo("NeuralNet mapper:emit weights");
                        weights=bank.getWeigths();
                        //write the weigths
                        out.collect(new PactInteger(1), new PactString(weights));                       
                       
                }

        }

        /**
         * Receive the weigth from the mappers, average them and test on a sample data
         * if the result is not satisfied, save the weights and to be processed
         * again by the mappers
         */
        @SameKey

        public static class AverageWeigths extends ReduceStub<PactInteger,PactString, PactString, PactString> {

                /**
                 * {@inheritDoc}
                 */
        		private String TestSet;
        		private String TestResult;
        		private double precision=0.2;//input-output < precision
        		private double assurance=0.7;//70% of assurance
	        	@Override
	        	public void configure(Configuration parameters) {
	        		TestSet=parameters.getString("--testSet", "/tmp/testSet.txt");	        		
	        		TestResult=parameters.getString("--testResult", "/tmp/testResult.txt");
	        		//clear the file names
	        		TestSet=TestSet.replaceAll("file:///", "/");
	        		TestResult=TestResult.replaceAll("file:///", "/");	        		
	        		
	        		DebugInfo("NeuralNet reducer configure: data read");
	        	}
        	
	        	/*
	        	 * (non-Javadoc)
	        	 * @see eu.stratosphere.pact.common.stub.ReduceStub#reduce(eu.stratosphere.pact.common.type.Key, java.util.Iterator, eu.stratosphere.pact.common.stub.Collector)
	        	 * The reducer takes the weights from the mappers averages them and write to a file
	        	 * The reducer after create a neural net, set the averaged weights and makes tests over a sample test data set
	        	 * If the output values are over a precision and the neural net has a certain percentage of assurence
	        	 * the reducer writes a message saying that there is no need to reprocess anymore
	        	 * otherwise writes message to reprocess
	        	 */
                @Override
                public void reduce(PactInteger key, Iterator<PactString> values, Collector<PactString, PactString> out) {
                        
                        AvgWeights avg=new AvgWeights(); //objetc that calculates de average
                        String sw=new String();//averaged weights in string format
                        
                        BankNeuron bank=new BankNeuron(ninputs);//neura net to test the values
                        bank.setDelimiter(delimiter);
                        
                        double count=0;//ammount of test data
                        double countassurance=0;//amount of test set with assurance
                        double input=0;//original output value of the test data set
                        double result=-1;//result calculated from the network
                        
                        DebugInfo("NeuralNet reducer: Weigth:" + sw);
                        
                        //average data
                        while (values.hasNext()) {
                                PactString element = (PactString) values.next();
                                avg.setValue(element.getValue());               
                                DebugInfo("NeuralNet reducer: Weigth:" + element.getValue());
                        }
                        //write avg weights to output
                        sw=avg.getAvgWeigths();
                        DebugInfo("NeuralNet reducer: Average weigths:" + sw);
                        out.collect(new PactString(""), new PactString(sw));
                        
                        //Test the results
                        
                		try{
                			  String[] sdata;
                			  String strLine;
                			  String sResult="";
                			  bank.setWeights(sw);
                			  
                			  
                			  DebugInfo("NeuralNet reducer: Open test file:" + TestSet);
                			  
                			  FileInputStream fstream = new FileInputStream(TestSet);
                			  // Get the object of DataInputStream
                			  DataInputStream in = new DataInputStream(fstream);
                			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
                			  DebugInfo("NeuralNet reducer: Read and test samples.");
                			  //Read File Line By Line
                			  while ((strLine = br.readLine()) != null)   {
                				  if (strLine.length()>2){
                					  sdata=strLine.split(delimiter);
                					  input=Float.valueOf(sdata[sdata.length-1]);
                					  
                					  DebugInfo("NeuralNet reducer: Test data " + strLine);
                					  
                					  sResult+=bank.evaluate(strLine);
                					  result=bank.getResult();
                					  DebugInfo("NeuralNet reducer:"+result);
                					  count=count+1;
                					  if ((input-result)<precision){
                						  countassurance=countassurance+1;
                					  }
                				  }			  
                			  }
                			  //Close the input stream
                			  in.close();                			  
                			  
                			  //write test result
                			  DebugInfo("NeuralNet reducer: Result: " + sResult);
							   BufferedWriter outtest = new BufferedWriter(new FileWriter(TestResult));
							   outtest.write(sResult);
							   outtest.write("\n");
							   outtest.write("Assurance=" + (countassurance/count)*100 +"\n");
							   if ((countassurance/count)>=assurance){								   
								   outtest.write("Reprocess=NO");
							   }
							   else{
								   outtest.write("Reprocess=YES");
							   }
							   outtest.write("\n");
							   outtest.close();                				
                			  
                			    }catch (Exception e){//Catch exception if any
                			  System.err.println("Error: " + e.getMessage());
                			  }                        
                }

        }
                
                

        /**
         * {@inheritDoc}
         */
        @Override
        public Plan getPlan(String... args) {

        		/*
        		 Use: 1<Number of sub tasks>
        		 	  2<Path of input file the paths of the data set> 
        		 	  "..../dataset1"
        		 	  "..../dataset2"
        		 	  3<Path of file that contains the weights>
        		 	  4<Path of the file used to test the net on reducer>
        		 	  5<Path of the file to write the result and later can be analised>
        		 	  The post analise is important to determine if the network should be trainned again
        		 */
                // parse job parameters
        	
        	    //System.out.println("Number of arguments:" + args.length);	
        	    DebugInfo("Number of arguments:" + args.length);
        	
                int noSubTasks   = (args.length > 0 ? Integer.parseInt(args[0]) : 1);
                String dataInput = (args.length > 1 ? args[1] : "");
                //String output    = (args.length > 2 ? args[2] : "");
                String weights   = (args.length > 2 ? args[2] : "");
                String testSet   = (args.length > 3 ? args[3] : "");
                String testResult   = (args.length > 4 ? args[4] : "");
                
                DebugInfo("Arguments 0:" + noSubTasks);
                DebugInfo("Arguments 1:" + dataInput);
                DebugInfo("Arguments 2:" + weights);
                DebugInfo("Arguments 3:" + testSet);
                DebugInfo("Arguments 4:" + testResult);
                
                
                
                FileDataSourceContract<PactNull, PactString> data = new FileDataSourceContract<PactNull, PactString>(
                                LineInFormat.class, dataInput, "Dataset Distribution");
                data.setDegreeOfParallelism(noSubTasks);

                MapContract<PactNull, PactString, PactInteger, PactString> mapper = new MapContract<PactNull, PactString, PactInteger, PactString>(
                                TrainNet.class, "Train network");
                mapper.setDegreeOfParallelism(noSubTasks);
                mapper.setParameter("--dataSet", weights);

                ReduceContract<PactInteger,PactString,  PactString, PactString> reducer = new ReduceContract<PactInteger,PactString,  PactString, PactString>(
                		AverageWeigths.class, "Average Weigths");
                reducer.setDegreeOfParallelism(noSubTasks);
                reducer.setParameter("--testSet", testSet);
                reducer.setParameter("--testResult", testResult);

                FileDataSinkContract<PactString, PactString> out = new FileDataSinkContract<PactString, PactString>(
                		NeuralNetOutFormat.class, weights, "Weights");
                out.setDegreeOfParallelism(noSubTasks);
                
                out.setInput(reducer);
                reducer.setInput(mapper);
                
                mapper.setInput(data);
                

                return new Plan(out, "Neural Network Training");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
                return "Parameters: [noSubStasks] [input] [weights] [test] [output]";
        }
        
           
}

