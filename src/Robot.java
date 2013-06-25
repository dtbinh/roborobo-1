
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
    public int robotID = 0;
    public static int nextRobotID = 1;
   
    // Schwelle, ab wann der Roboter "nahe" einer Position ist
    public static float near = 0.3f;

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
        
        this.robotID = nextRobotID;
        nextRobotID++;
        
        log("Hi, I'm Robot No. " + this.robotID);
        
        
    }
    
    public String strPos(Position p)
    {
    	return p.xPos + "/" + p.zPos;
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
    	System.out.println("[" + this.robotID + "] " + msg);
    }
    
    protected void warte(int zeit)
    {
    	try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    protected boolean iAmClose(Position p)
    {
    	if ((Math.abs(p.xPos - position().xPos) < near) && (Math.abs(p.zPos - position().zPos) < near))
    		return true;
    	else
    		return false;
    }
    
    protected void fahreZuTestzweckenGegenDenGelbenKegel()
    {
    	Position kegel = new Position(12.8f, -3.5f);
    	log("Jetzt fahre ich gegen den Kegel!");
    	goToPos(kegel);
    }

    
    protected void goToPos(Position p) 
    {
    	moveLogic.setWaipoint(p.xPos*1000, p.zPos*1000);
    	while (!moveLogic.driveToCoordinates() && !iAmClose(p))
    	{
    		log("On my way to " + p.xPos + "/" + p.zPos + " from " + position().xPos + "/" + position().zPos);
    		
    		// fahre ich auf zB den Warenkorb zu, koennte das Hindernis das Ziel sein
    		if (moveLogic.getStatusObstacleFound() && !iAmClose(p))
    		{
    			log("Hindernis! Weiche aus.");
    			crazyAusweichen(1);
    		}
    		
    	}
    	log("Reached Goal!");
    }
    
    /**
     *  Beschreibe einen formschoenen Haken um ein Hindernis zu umfahren.
     *  Faktor (>0) beschreibt Groesse des Hakens
     */
    protected void crazyAusweichen(int f)
    {    	
    	
    	// kleines stueck nach hinten fahren
    	omniDrive.setVelocity(-200, -200*f, 0);
    	warte(1500*f);
    	omniDrive.setVelocity(0,0,0);
    	
    }
    
    protected void drive() throws InterruptedException
    {
       
        while (!Thread.interrupted() && com.isConnected() && false == bumper.value())
        {
  
        	
        	
        	// Order holen, Cart Area bestimmen
        	// =====================================================================================
        	
        	
        	log("Let's go!");
        	
//        	if(this.robotID == 2)
//        	{	
//        		log("XXXXXX");
//        		goToPos(new Position(9, -8));
//        		log("YYYYYYY");
//        	}
        	
        	
        	//TODO: eleganteres Laden von Orders als mit ID...
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
        	
        	log("Driving  to Empty Cart.");
        	goToPos(nextHop);
        	
        	
        	
        	log("taking cart.");
        	Cart myCart = null;
        	if ((myCart = management.takeCart(emptyCart)) != null)
        	{
        		log("Habe den Kart angefordert!");
        	}
        	
        	
        	
        	// 3. Alle Order Items abarbeitne, zum Issuing Point fahren, einladen
        	
        	log("Arbeite nun " + management.getOrderItemList(myOrder).size() + " Bestellungen ab.");
        	
        	for (OrderItem currentItem: management.getOrderItemList(myOrder))
        	{
        		
        		// Finde den erstbesten Issuing Point fuer meinen Produkttyp
        		IssuingPoint nextIP = management.getIssuingPoints(currentItem.getProductType()).get(0);
        	
        		log("fahre zum naechsten IP: " + strPos(nextIP.getCoordinates()));
        		goToPos(nextIP.getCoordinates());
        		
        		// Lade Sachen ein
        		// TODO: Kapazitaet beruecksichtigen
        		if(management.load(currentItem.getAmount(), nextIP, myCart))
        			log("Lade Sachen ein!");
        		else
        			log("Fehler beim Einladen :(");
        	}
        	
        	
        	// 4. Freie Position finden und Cart zurueckbringen
        	
        	log("bringe jetzt den Kart zurueck");
        	
        	// in designierter Cart Zone einen freien Platz finden
        	CartPosition emptyPos = null;
        	for (CartPosition pos: management.getCartPositions(myCartArea))
        	{
        		if (management.getstate(pos) == ECartPositionState.EMPTY)
        			{
        				emptyPos = pos;
        				log("Leere Position gefunden: " + emptyPos.getCoordinates().xPos + "," + emptyCart.getCoordinates().zPos);
        				break;
        			}
 	
        	}
        	
        	goToPos(emptyPos.getCoordinates());
        	
        	log("Bin an leerer Position, gebe Cart zurueck...");
        	if (management.finishOrder(myOrder, management.getCartPositions(myCartArea)))
        		log("Cart zurueckgegeben!");
        	else
        		log("Fehler beim zurueckgeben des Carts... :( :(");
        
        	

        	//fahreZuTestzweckenGegenDenGelbenKegel();
        	
        	
        	
        	//moveLogic.setWaipoint(test* 1000, -1*test2 * 1000);
        	//System.out.println("reached target: "+ moveLogic.transportToCoordinates());
           
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
