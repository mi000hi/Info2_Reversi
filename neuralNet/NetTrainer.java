package neuralNet;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

import NeuronalNetwork.Net;
import Utils.DataReader;
import Utils.DataWriter;
import data.Terminatable;
import data.Terminator;
import reversi.Coordinates;
import reversi.OutOfBoundsException;

public class NetTrainer implements Terminatable {

	static double learnrate = 0.1;
	private static boolean killMe = false;
	private static int repetitionsPerSample = 100;
	private static TrainingDataCollector tdc = new TrainingDataCollector();

	public static void main(String[] args) {

		if (args.length == 0) {
			System.out.println("\nArguments: {minFreeFields} {maxFreeFields} {repetitionsPerSample} {nrHiddenNeurons} {filename}\n");
		}
		// start the terminator thread
		Terminator terminator = new Terminator(new NetTrainer());
		Thread terminatorThread = new Thread(terminator);
		terminatorThread.start();

		double[] input = new double[65];
		double[] output = new double[1];
		int minFreeFields = 0;
		int maxFreeFields = 10;
		int totalSamplesTrained = 0;
		int nrHidden = 24;
		String filename = "neuralNet_reversi_10_to_10_ff_100reps_24hidden.txt";

		// look for arguments
		Net net;
		if (args.length != 0) {
			minFreeFields = Integer.parseInt(args[0]);
			maxFreeFields = Integer.parseInt(args[1]);
			repetitionsPerSample = Integer.parseInt(args[2]);
			nrHidden = Integer.parseInt(args[3]);
		}
		if (args.length == 5) {
			filename = args[4];
			if(new File(filename).exists()) {
				net = DataReader.readNetFromFile(filename);
			} else {
				net = new Net(65, nrHidden, 1, learnrate);
			}
		} else {

			// 65 input neurons, 44 hidden neurons, 1 output neuron
			net = new Net(65, nrHidden, 1, learnrate);
//		Net net = new Net(65, (int) Math.round(2.0 / 3 * 65 + 1), 1, learnrate);
//		Net net = DataReader.readNetFromFile(filename);
//		System.out.println("we made " + Math.round(2.0 / 3 * 64 + 1) + " hidden neurons");

		}
		while (!killMe) {

			// nextInt is normally exclusive of the top value,
			// so add 1 to make it inclusive
			int randomNum = ThreadLocalRandom.current().nextInt(minFreeFields, maxFreeFields + 1);

			// find trainingSample and train it
			TrainingSample sample = tdc.findTrainingSample(randomNum);

			// create arrays to feed to the neural network
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
			output[0] = sample.gameResult;

			// train the sample
			net.train(new double[][] { input }, new double[][] { output }, repetitionsPerSample);

			totalSamplesTrained++;
			System.out.println("new samples trained: " + totalSamplesTrained);
		}

		// save the neural network
		DataWriter.writeNetToFile(filename, net);
	}

	@Override
	public void setTerminateProgram(boolean value) {

		killMe = value;

	}
}
