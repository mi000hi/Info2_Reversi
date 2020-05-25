package neuralNet;

import NeuronalNetwork.Netz;

public class NetTrainer {
	
	static double learnrate = 0.1;
	
	public static void main(String[] args) {
		
		// 64 input neurons, 44 hidden neurons, 1 output neuron
		Netz net = new Netz(64, (int) Math.round(2.0/3*64+1), 1, learnrate);
		
		System.out.println(Math.round(2.0/3*64+1));
	}
}
