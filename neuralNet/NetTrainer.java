package neuralNet;

import NeuronalNetwork.Net;

public class NetTrainer {

	static double learnrate = 0.1;

	public static void main(String[] args) {

		// 64 input neurons, 44 hidden neurons, 1 output neuron
		Net net = new Net(64, (int) Math.round(2.0 / 3 * 64 + 1), 1, learnrate);
//		System.out.println("we made " + Math.round(2.0 / 3 * 64 + 1) + " hidden neurons");

		TrainingDataCollector tdc = new TrainingDataCollector();
		TrainingSample sample01 = tdc.findTrainingSample(0);
	}
}
