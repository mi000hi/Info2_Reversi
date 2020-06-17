package neuralNet;

import NeuronalNetwork.*;
import Utils.DataReader;
import reversi.Coordinates;
import reversi.OutOfBoundsException;

public class Testclass {

	public static void main(String[] args) {

		Net reversiNet = DataReader.readNetFromFile("neuralNet_10_to_10_ff_100reps_15hidden.txt");

		TrainingDataCollector tdc = new TrainingDataCollector();
		TrainingSample sample01 = tdc.findTrainingSample(10);
		double[] input01 = getInputVector(sample01);
		TrainingSample sample02 = tdc.findTrainingSample(10);
		double[] input02 = getInputVector(sample02);

		System.out.println("\n**** TRAINING SAMPLE: ****");
		System.out.print(sample01.gb);
		System.out.println("GameResult: " + sample01.gameResult);
		System.out.println("Prediction: " + reversiNet.compute(input01)[0]);
		
		System.out.println("\n**** TRAINING SAMPLE: ****");
		System.out.print(sample02.gb);
		System.out.println("GameResult: " + sample02.gameResult);
		System.out.println("Prediction: " + reversiNet.compute(input02)[0]);
	}

	private static double[] getInputVector(TrainingSample sample) {

		// create arrays to feed to the neural network
		double[] input = new double[65];
		input[0] = sample.nextPlayer;
		for (int y = 1; y <= 8; y++) {
			for (int x = 1; x <= 8; x++) {
				try {
					input[(y - 1) * 8 + x] = sample.gb.getOccupation(new Coordinates(y, x));
				} catch (OutOfBoundsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return input;
	}

}
