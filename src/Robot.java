
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import java.util.ArrayList;
import java.util.List;

import de.hpi.sam.ordermanagement.Cart;
import de.hpi.sam.ordermanagement.CartArea;
import de.hpi.sam.ordermanagement.CartPosition;
import de.hpi.sam.ordermanagement.ECartPositionState;
import de.hpi.sam.ordermanagement.IOutStock;
import de.hpi.sam.ordermanagement.IssuingPoint;
import de.hpi.sam.ordermanagement.Order;
import de.hpi.sam.ordermanagement.OrderItem;
import de.hpi.sam.ordermanagement.OrderManagement;
import de.hpi.sam.robotino.IRobot;
import de.hpi.sam.robotino.Position;
import de.hpi.sam.robotino.actor.RobotOmniDrive;
import de.hpi.sam.robotino.logic.MoveLogic;
import de.hpi.sam.robotino.sensor.RobotBumper;
import de.hpi.sam.robotino.sensor.RobotCom;
import de.hpi.sam.robotino.sensor.RobotDistanceSensorArray;
import de.hpi.sam.robotino.sensor.RobotNorthStar;
import de.hpi.sam.robotino.sensor.RobotPowerManagement;

public class Robot implements Runnable, IRobot
{
	public OrderManagement management;
    protected final String hostname;
    protected final RobotCom 	com;
    protected final RobotOmniDrive omniDrive;
    protected final RobotBumper bumper;
    protected final RobotNorthStar northStar;
    protected final MoveLogic moveLogic;
    protected final RobotDistanceSensorArray distanceSensorArray;
    protected final RobotPowerManagement powerManagement;
    
    protected final double energy;
    protected final double throughput;
    
    public static int orderID = 0;

    public Robot(String hostname, OrderManagement management, double energy, double throughput)
    {
    	System.out.println("Initializing...");
    	this.management = management;
        this.hostname = hostname;
        com = new RobotCom();
        com.setAddress(hostname);
        omniDrive = new RobotOmniDrive(com);
        bumper = new RobotBumper(com);
        distanceSensorArray = new RobotDistanceSensorArray(com);
        northStar = new RobotNorthStar(com);
        moveLogic = new MoveLogic(omniDrive, northStar, distanceSensorArray);
        powerManagement = new RobotPowerManagement(com);
        
        this.energy = energy;
        this.throughput = throughput;
    }

    public void run()
    {
        System.out.println("Robot started.");
        
        try
        {
            System.out.println("Connecting...");
            com.connect();
            System.out.println("Connected.");
            System.out.println("======================================.");
            
            System.out.println("Driving...");
            drive();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            disconnect();
        }

        System.out.println("Done.");
    }

    protected void disconnect()
    {
        com.disconnect();
    }
    
    
    protected void log(String msg)
    {
    	System.out.println("[" + this.hashCode() + "] " + msg);
    }

    protected void drive() throws InterruptedException
    {
       
        while (!Thread.interrupted() && com.isConnected() && false == bumper.value())
        {
  
        	
        	
        	// Order holen, Cart Area bestimmen
        	// =====================================================================================
        	
        	int myOrderID = orderID; // statisch fuer alle Roboter
           	orderID++; // Naechster Robotoer nimmt naechste Bestellung an
        	
        	log("fetching order no. " + myOrderID + "...");
        	Order myOrder = management.getOrderList().get(myOrderID);
        	CartArea myCartArea = management.getCartArea(myOrder);
     
        	
        	// Freies Cart in der Area finden und Position speichern
        	Position nextHop = new Position(0,0);
        	CartPosition emptyCart = null;
        	
        	for (CartPosition pos: management.getCartPositions(myCartArea))
        	{
        		if (management.getstate(pos) == ECartPositionState.EMPTY_CART)
        			{
        				emptyCart = pos;
        				log("Cart Position gefunden: " + emptyCart.getCoordinates().xPos + "," + emptyCart.getCoordinates().zPos);
        				break;
        			}
 	
        	}
        	
        	nextHop = emptyCart.getCoordinates();
        	
        	// 2. Kart holen
        	moveLogic.setWaipoint(nextHop.xPos*1000, nextHop.zPos*1000);
        	log("On my way to Empty Cart.");
        	moveLogic.transportToCoordinates();
        	log("Reached Empty Cart.");
        	
        	log("taking cart.");
        	Cart myCart = null;
        	if ((myCart = management.takeCart(emptyCart)) != null)
        	{
        		log("Success.");
        	}
        	
        	
        	// zurueck zum amnfang
        	
//        	moveLogic.setWaipoint(10000, -1000);
//        	moveLogic.transportToCoordinates();
        	
        	
        	// 3. Alle Elemente der Liste abarbeiten, ggf. neuen Kart holen
        	
        	
        	// 4. Kart wegbringen
        	
        	
        	// debug: disconecct
        	com.disconnect();
        	
        	
        	
        	//moveLogic.setWaipoint(test* 1000, -1*test2 * 1000);
        	System.out.println("reached target: "+ moveLogic.transportToCoordinates());
           
            com.waitForUpdate();
           
            System.out.println("DONE");
        }
    }

    /**
     * Rotate a 2 dimensional vector
     * 
     * @param in Input vector
     * @param deg Rotation in degrees
     * @return Output vector
     */
    private float[] rotate(float[] in, float deg)
    {
        final float pi = 3.14159265358979f;

        float rad = 2 * pi / 360.0f * deg;

        float[] out = new float[2];
        out[0] = (float) (cos(rad) * in[0] - sin(rad) * in[1]);
        out[1] = (float) (sin(rad) * in[0] + cos(rad) * in[1]);
        return out;
    }

	@Override
	public Position position() {
		return new Position (this.northStar.posX()/1000, this.northStar.posZ()/1000);
	}
}
