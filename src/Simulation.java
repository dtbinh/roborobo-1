import de.hpi.sam.ordermanagement.OrderManagement;

public class Simulation
{

	public static void main(String[] args)
	{
		OrderManagement management = new OrderManagement();
		
		/*
		 * Throughput vs. energy 
		 * 
		 * For simulation purposes, we set these values in this main-method. 
		 * Throughput + Energy is always 1.0 (100%).
		 * 
		 */
		
		final double energy = 0.3;
		final double throughput = 0.7;
		
		Robot robot1 = new Robot("127.0.0.1:8080", management, energy, throughput);
		Robot robot2 = new Robot("127.0.0.1:8082", management, energy, throughput);
		Robot robot3 = new Robot("127.0.0.1:8084", management, energy, throughput);

		Thread thread1 = new Thread(robot1);
		Thread thread2 = new Thread(robot2);
		Thread thread3 = new Thread(robot3);

		management.addRobot(robot1);
		management.addRobot(robot2);
		management.addRobot(robot3);
		

		System.out.println("Start simulation of robot 1:");
		thread1.start();
		System.out.println("Start simulation of robot 2:");
		thread2.start();
		System.out.println("Start simulation of robot 3:");
		thread3.start();
	}
}