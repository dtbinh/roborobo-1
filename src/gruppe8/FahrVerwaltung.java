package gruppe8;

import java.util.List;

import de.hpi.sam.ordermanagement.*;

import de.hpi.sam.robotino.ID;
import de.hpi.sam.robotino.Position;


public class FahrVerwaltung {
	
	RobotCPU cpu;
	boolean obstacleFound = false;
	
	
	FahrVerwaltung(RobotCPU yourCPU)
	{
		this.cpu = yourCPU;
	}
	
	
	// HAUPTFUNKTIONEN
	
    public void fahreZu(Position p) 
    {

    	cpu.ML.setWaipoint(p.xPos*1000, p.zPos*1000);

    	while ((!cpu.ML.driveToCoordinates() && !iAmClose(p)))
    	{
    		cpu.log("Fahre nach " + p.xPos + "/" + p.zPos + ", bin gerade " + cpu.position().xPos + "/" + cpu.position().zPos);
    		
    		// fahre ich auf zB den Warenkorb zu, koennte das Hindernis das Ziel sein
    		if ((cpu.ML.getStatusObstacleFound() && !iAmClose(p)))
    		{
    			cpu.log("Hindernis! Weiche aus.");

    			ausweichenFahreZu(1);
    		}
    		
    	}
    	cpu.log("Reached Goal!");
    }
    
    /* Die Methode transportiereZu() (bzw. die darin aufgerufene transportToCoordinates()
     * hat uns erhebliche Probleme bereitet. Da durch die Blockade des vorderen Sensors Hindernisse
     * teilweise nicht erkannt werden können, bleiben unsere Roboter an Hindernissen stecken oder 
     * kollidieren mit anderen Robotern, die nicht oder erst zu spät als Hindernis erkannt werden.
     * Dies führt dazu, dass unsere Roboter ihre Aufträge nicht abschließen können bzw. sich
     * gegenseitig dabei behindern (lediglich in Einzelfällen hat es ein Roboter geschafft seinen
     * Warenkorb wieder in der CartArea zu positionieren.
     */
	
    public void transportiereZu(Position p)
    {
    	cpu.ML.setWaipoint(p.xPos*1000, p.zPos*1000);
    	rotieren();
    	while ((!transportToCoordinates() && !iAmClose(p)))
    	{
    		cpu.log("Transportiere nach " + p.xPos + "/" + p.zPos + ", bin gerade " + cpu.position().xPos + "/" + cpu.position().zPos);
    		
    		
    		// fahre ich auf zB den Warenkorb zu, koennte das Hindernis das Ziel sein
    		if ((this.obstacleFound && !iAmClose(p)))
    		{
    			cpu.log("Hindernis! Weiche aus.");
    			ausweichenFahreZu(1);
    		}
    		
    	}
    	cpu.log("Reached Goal!");
    }

    /*
	Die Methode nimmCartauf() regelt die Suche nach einem freien Cart, wobei bereits 
	reservierte Carts per Nachrichten gekennzeichet werden, bis hin zur eigentlich Aufnahmen
	des Carts.
	*/

    public Cart nimmCartAuf(CartArea myCartArea)
    {
    	Position p = new Position(0,0);
    	CartPosition emptyCart = null;
    	
    	cpu.warte((int)(Math.random()*3000));
    	
    	
    	List<ID> communicationParticipants = cpu.mgmt.getRegisteredParticipants();
    	List<Object> messageList;
    	
    	boolean cartBesetzt = false;
    	
    	Cart myCart = null;
    	 	
		messageList = cpu.mgmt.receiveMessages(cpu.comID);
    	cpu.log("Nachrichtenabfrage erfolgt");

    	for (CartPosition pos: StatusVerwaltung.mgmt.getCartPositions(myCartArea))//cpu anders
    	{
    		
    		if (messageList != null)
    		{	
    			cpu.log("Nachrichten erfolgreich erhalten.");
	    		for (Object count : messageList)
	    		{
	    			
	    			if (pos == count)
	    			{
	    				cartBesetzt = true;
	    			}
	    		}
    		}	
    		if ((StatusVerwaltung.mgmt.getstate(pos) == ECartPositionState.EMPTY_CART) && (!cartBesetzt))//cpu anders
    			{
    				emptyCart = pos; // Cart Position, auf der ein leerer Kart steht
    				
	    				for (ID tempParticipant: communicationParticipants)
	    				{	
	    					if (tempParticipant != this.cpu.comID) {
	    						cpu.mgmt.sendMessage(cpu.comID, emptyCart, tempParticipant);
	    						cpu.log("message gesendet an: " + tempParticipant);
	    					}
	    				}
    					
    				cpu.log("Cart Position gefunden: " + emptyCart.getCoordinates().xPos + "," + emptyCart.getCoordinates().zPos);
    				break;
    			}
    		cartBesetzt = false;
    	}
    
    	p = emptyCart.getCoordinates();
        	
    	cpu.log("Driving to Empty Cart.");
    	fahreZu(p);
    	stueckNachVorne();
    	
    	cpu.log("taking cart.");
//    	Cart myCart = null;   // ist stattdessen ueber der for schleife deklariert
    	if ((myCart = StatusVerwaltung.mgmt.takeCart(emptyCart)) != null)
    	{
    		cpu.log("Habe den Kart angefordert!");
    	}
    	
    	
    	return myCart;
    	
    	
    }
    
    public boolean stelleCartAb(CartArea myCartArea, Order myOrder)
    {
    	// in designierter Cart Zone einen freien Platz finden
    	CartPosition emptyPos = null;
    	for (CartPosition pos: cpu.mgmt.getCartPositions(myCartArea))
    	{
    		if (cpu.mgmt.getstate(pos) == ECartPositionState.EMPTY)
    			{
    				emptyPos = pos;
    				cpu.log("Leere Position gefunden: " + emptyPos.getCoordinates().xPos + "," + emptyPos.getCoordinates().zPos);
    				break;
    			}
	
    	}
    	
    	transportiereZu(emptyPos.getCoordinates());
    	   
    	cpu.log("Bin an leerer Position, gebe Cart zurueck...");
    	boolean success = false;
    	if (success = cpu.mgmt.finishOrder(myOrder, cpu.mgmt.getCartPositions(myCartArea)))
    		cpu.log("Cart zurueckgegeben!");
    	else
    		cpu.log("Fehler beim zurueckgeben des Carts... :( :(");
    	
    	return success;
    	
    }
    
    public void holeProdukt(OrderItem currentItem, Cart myCart)
    {
    	// Finde den erstbesten Issuing Point fuer meinen Produkttyp
		IssuingPoint nextIP = cpu.mgmt.getIssuingPoints(currentItem.getProductType()).get(0);
	
		cpu.log("fahre zum naechsten IP: " + cpu.strPos(nextIP.getCoordinates()));
		transportiereZu(nextIP.getCoordinates());
		
		// Lade Sachen ein
		// TODO: Kapazitaet beruecksichtigen
		
		
		if(cpu.mgmt.load(currentItem.getAmount(), nextIP, myCart)) {
			cpu.log("Lade Sachen ein!");
		}	
		else
			cpu.log("Fehler beim Einladen :(");
    }
	
	
	// HILFSFUNKTIONEN
    
    /**
     *  Beschreibe einen formschoenen Haken um ein Hindernis zu umfahren.
     *  Faktor (>0) beschreibt Groesse des Hakens
     */
    
    
    
    
    protected void ausweichenTransportiereZu(int f)
    {   
    	cpu.OD.setVelocity(-200, -200*f, 0);
    	cpu.warte(1000);
    	cpu.OD.setVelocity(0,0,0);
    	




    }
    
    protected void ausweichenFahreZu(int f)
    {    	
    
    	this.cpu.OD.setVelocity(0, -100, 0);
    	cpu.warte(1000);
    	this.cpu.OD.setVelocity(0, 0, 0);
    	
    }
    
    protected void stueckNachVorne()
    {
    	cpu.OD.setVelocity(70, 0, 0);
    	cpu.warte(1000);
    	cpu.OD.setVelocity(0, 0, 0);
    }
	
    protected boolean iAmClose(Position p)
    {
    	if ((Math.abs(p.xPos - cpu.position().xPos) < RobotCPU.near) && (Math.abs(p.zPos - cpu.position().zPos) < RobotCPU.near))
    		return true;
    	else
    		return false;
    }
    
    
    
   
    private boolean rotieren() {
    	cpu.log("ROTOR!!!");
    	float currentOrientation;
		float currentSpeed = 0;
		double orientationRobotCurrent[] =
		{
				0, 0
		};
		double vectorToRobotCurrent[] =
		{
				0, 0
		};
		double tempX, tempZ, length, x1, y1, angleDifference, angle, relation;

		// System.out.println("Relation: " + relation);
		float rotationValue = 0;

		this.cpu.ML.resetStatus();
    	
    	do
    	{
			// Get current orientation via NorthStar
			currentOrientation = cpu.NS.posTheta();
			// Get the normalized vector of the current orientation with length
			// 1 from the local perspective of the robot
			orientationRobotCurrent[0] = Math.sin(currentOrientation);
			orientationRobotCurrent[1] = Math.cos(currentOrientation);
			// Get the current position of the robot via northStar
			vectorToRobotCurrent[0] = cpu.NS.posX();
			vectorToRobotCurrent[1] = cpu.NS.posZ();
			// Derive the vector between the current position of the robot and
			// the target
			tempX = this.cpu.ML.getWaypointX() - vectorToRobotCurrent[0];
			tempZ = this.cpu.ML.getWaypointZ() - vectorToRobotCurrent[1];
			// Derive the length of the defined waypoint
			length = Math.sqrt(tempX * tempX + tempZ * tempZ);
			// If length is equal to zero the robot is already at the target.
			// Additionally we need to avoid the devision by zero (see below)
			if (length == 0)
			{
				return true;
			}
			// Derive the normalized vector from the robot to the target
			x1 = tempX / length;
			y1 = tempZ / length;
			// Derive the angle between the orientation of the robot and the
			// vector to the target
			angleDifference = orientationRobotCurrent[0] * x1 + orientationRobotCurrent[1] * y1;
			// ...
			angle = Math.acos(angleDifference);
//			cpu.log("WINKEL: "+angle);

			// Calculate if current orientation of the robot is on the left or
			// the right side of the vector to the target
			relation = orientationRobotCurrent[0] * tempZ - orientationRobotCurrent[1] * tempX;

			// Depending on the relation we need to invert the angle
			if (relation > 0)
			{
				angle = angle * -1;
			}
			// If it is required to change the orientation towards the target
			
				// System.out.println(Math.toDegrees(angle));
				// Get the angle in degree
				double degree = Math.toDegrees(angle);
				// If the angle is bigger than 60 or smaller then -60
				if (Math.abs(degree) > 60)
				{
					// If it is bigger
					if (angle > 0)
					{
						// Rotate with value 60
						rotationValue = 5;

					}
					// if it is smaller
					else
					{
						// rotate with value -60
						rotationValue = -5;

					}
				
				// If the difference of the angle between the current
				// orientation of the robot and the vector to the target is
				// smaller the 60, we use directly the angle for
				// the rotation
				
			}
			
    
    
		this.cpu.OD.setVelocity(0, 0, rotationValue);
	    cpu.warte(1000);
	    this.cpu.OD.setVelocity(10, 0, 0);
	    cpu.warte(1000);
	    } while (Math.abs(angle)>0.05);
	    this.cpu.OD.setVelocity(0, 0, 0);
	    return true;
    }

	public boolean transportToCoordinates()
	{

		cpu.log("THE TRANSPORTER!!!");
		boolean targetReached = false;
		float maxMovementSpeed = 500;
		
		float currentOrientation;
		float currentSpeed = 0;
		double orientationRobotCurrent[] =
		{
				0, 0
		};
		double vectorToRobotCurrent[] =
		{
				0, 0
		};
		double tempX, tempZ, length, x1, y1, angleDifference, angle, relation;

		// System.out.println("Relation: " + relation);
		int maxMoves = 10000;
		float rotationValue = 0;


		for (int i = 0; i <= maxMoves; i++)
		{

			// Get current orientation via NorthStar
			currentOrientation = cpu.NS.posTheta();
			// Get the normalized vector of the current orientation with length
			// 1 from the local perspective of the robot
			orientationRobotCurrent[0] = Math.sin(currentOrientation);
			orientationRobotCurrent[1] = Math.cos(currentOrientation);
			// Get the current position of the robot via northStar
			vectorToRobotCurrent[0] = cpu.NS.posX();
			vectorToRobotCurrent[1] = cpu.NS.posZ();
			// Derive the vector between the current position of the robot and
			// the target
			tempX = this.cpu.ML.getWaypointX() - vectorToRobotCurrent[0];
			tempZ = this.cpu.ML.getWaypointZ() - vectorToRobotCurrent[1];
			// Derive the length of the defined waypoint
			length = Math.sqrt(tempX * tempX + tempZ * tempZ);
			// If length is equal to zero the robot is already at the target.
			// Additionally we need to avoid the devision by zero (see below)
			if (length == 0)
			{
				return true;
			}
			// Derive the normalized vector from the robot to the target
			x1 = tempX / length;
			y1 = tempZ / length;
			// Derive the angle between the orientation of the robot and the
			// vector to the target
			angleDifference = orientationRobotCurrent[0] * x1 + orientationRobotCurrent[1] * y1;
			// ...
			angle = Math.acos(angleDifference);
//			cpu.log("WINKEL: "+angle);

			// Calculate if current orientation of the robot is on the left or
			// the right side of the vector to the target
			relation = orientationRobotCurrent[0] * tempZ - orientationRobotCurrent[1] * tempX;

			// Depending on the relation we need to invert the angle
			if (relation > 0)
			{
			angle = angle * -1;
			}
			// If it is required to change the orientation towards the target
			if (Math.abs(angle) > 0.05)
			{
				// System.out.println(Math.toDegrees(angle));
				// Get the angle in degree
				double degree = Math.toDegrees(angle);
				// If the angle is bigger than 60 or smaller then -60
				if (Math.abs(degree) > 60)
				{
					// If it is bigger
					if (angle > 0)
					{
						// Rotate with value 60
						rotationValue = 60;

					}
					// if it is smaller
					else
					{
						// rotate with value -60
						rotationValue = -60;

					}
				}
				// If the difference of the angle between the current
				// orientation of the robot and the vector to the target is
				// smaller the 60, we use directly the angle for
				// the rotation
				else
				{
					rotationValue = (float) Math.toDegrees(angle);

				}
			}
			// If we still not at the target position
			if (length > 50)
			{
				// If we are more or less looking towards the target position
				if (Math.abs(angle) <= 0.05 + 0.5)
				{
					// System.out.println("Distance: " + (length /
					// MoveLogic.fieldsToAccelerate) );
					// If we are far away from the target position ...
					if ((length / 2) >= 1000)
					{
						// ... and the maximum allowed speed is not reached ...
						if (currentSpeed < maxMovementSpeed)
						{
							// ... we accelerate with a quadratic acceleration
							// factor (+1) because if the robot is not moving it
							// won't start moving anyway
							currentSpeed = ((float) Math.pow((currentSpeed / maxMovementSpeed), 2) + currentSpeed
									/ maxMovementSpeed)
									* maxMovementSpeed + 1;// currentSpeed
																// +
																// (50*(MoveLogic.sleepTime*(float)0.001))
																// ;
							// If we have derived a value for the speed bigger
							// than the maximum-allowed speed ...
							if (currentSpeed >= maxMovementSpeed)
							{
								// We set it to the maximum speed
								currentSpeed = maxMovementSpeed;
							}
						}
					}
					else
					{

						// Braking curve: ln(x/4+0,13) + 2
						// We brake according to a logarithmic function over the
						// distance to the target position
						currentSpeed = (float) (Math.log((length / (1000 * 2)) / 4 + 0.13) + 2)
								* maxMovementSpeed;
					}
				}
				// If have reached (+/- the threshold) the target position, we
				// stop:
				else
				{
					currentSpeed = 0;
				}

				// We need to consider potential obstacles
				if (this.maxVoltageObstacleWithPuck() > 0.3 && Math.abs(angle) <= 50 + 0.5)
				{
					// We get the maximum voltage of the three distance-sensors
					// at the front
					float maxVoltage = this.maxVoltageObstacleWithPuck();
					// max voltage = 2.4
					cpu.log("" + maxVoltage);
					currentSpeed = (float) ((float) ((1.5 - maxVoltage)) / 2.4 * maxMovementSpeed);
					// If we have already reached the target position, we stop
					// and return false
					if (currentSpeed <= 50)
					{
						this.cpu.OD.setVelocity(0, 0, 0);
						obstacleFound = true;
						// We have not reached the target-position
						return false;
					}

				}

				// Now we set the values
				this.cpu.OD.setVelocity(currentSpeed, 0, rotationValue);
			}
			// If we have reached the target position we stop the robot and
			// return true
			if (length <= 50)
			{
				this.cpu.OD.setVelocity(0, 0, 0);
				// We have not found an obstacle before we have reached the
				// target position
				obstacleFound = false;
				return true;
			}

			try
			{

				Thread.currentThread();
				// Now we put the thread into sleep for the specified time
				// period
				Thread.sleep(50);// sleep for 50 Ms

			}
			catch (InterruptedException ie)
			{
				// If this thread was interrupted by another thread
			}

		}

		return false;
	}

	private float maxVoltageObstacleWithPuck()
	{

		float maxVoltage = Math.max(this.cpu.dsa.getDinstanceSensorVoltage(2),
				this.cpu.dsa.getDinstanceSensorVoltage(9));

		return maxVoltage;
	}
    
}    


