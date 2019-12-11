package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Terminator implements Runnable {
	
	private final String TERMINATION_KEYWORD = "stop";
	private final String INPUT_REQUEST = "TERMINATOR: type in '" + TERMINATION_KEYWORD + "' to terminate the program:\n";

	public Terminator() {

		GameCoordinator.setTerminateProgram(false);

	}

	@Override
	public void run() {
		
		String input = "";
		
		try {
			
			while(!input.equals("stop")) {
				
				// wait for user to type "stop" into the console
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		        System.out.println("\n" + INPUT_REQUEST);
		        input = br.readLine();
		        System.out.println("TERMINATOR: you typed in '" + input + "'");
				
			}
			
			// terminate the program
			System.out.println("TERMINATOR: terminating the program, please wait while data is being saved.");
			GameCoordinator.setTerminateProgram(true);
		
		} catch(IOException e) {
			
		}
		
	}
	
	/**
	 * prints a message to the console to let the user know what he can do
	 */
	public void printInputRequest() {
		
		System.out.println("\n" + INPUT_REQUEST);
		
	}

}
