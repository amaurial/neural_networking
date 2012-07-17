package eu.stratosphere.pact.neuralnetwork;

import java.util.Vector;

public class AvgWeights{
	private Vector<String> weigths=new Vector<String>();
	
	public void setValue(String w){

		weigths.add(w);
		
	}
	public AvgWeights (){
		
	}
	public String getAvgWeigths(){        		
		Vector<Double> sum=new Vector<Double>();
		int st=weigths.size();
		String sw=new String();
		
		String[] a=weigths.elementAt(0).split(";");
		
		for (int j=0;j<a.length;j++){
			sum.add(j,0.0);
		}
		
		
		for (int i=0;i<weigths.size();i++){
			String [] stemp=weigths.elementAt(i).split(";");
			
    		for (int j=0;j<stemp.length;j++){            			
    			sum.set(j, sum.elementAt(j)+Double.valueOf(stemp[j]));            			
    		}
		}
		
		for (int j=0;j<sum.size();j++){
			sum.set(j, sum.elementAt(j)/st);
			sw=sw+sum.elementAt(j).toString()+";";
		}
		return sw;
	}
	
}


