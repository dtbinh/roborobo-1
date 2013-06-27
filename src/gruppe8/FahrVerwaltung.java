package gruppe8;

import java.util.HashMap;
import java.util.List;

import de.hpi.sam.ordermanagement.*;

import de.hpi.sam.robotino.ID;
import de.hpi.sam.robotino.Position;

public class FahrVerwaltung {
	
	RobotCPU cpu;
	
	FahrVerwaltung(RobotCPU yourCPU)
	{
		this.cpu = yourCPU;
	}
	
//	private int manoeveranzahl = 0;
//	private boolean oftausgewichen = false;
	
	// HAUPTFUNKTIONEN
	
    public void fahreZu(Position p) 
    {
    	cpu.ML.setWaipoint(p.xPos*1000, p.zPos*1000);
    	while (!cpu.ML.driveToCoordinates() && !iAmClose(p))
    	{
    		cpu.log("Fahre nach " + p.xPos + "/" + p.zPos + ", bin gerade " + cpu.position().xPos + "/" + cpu.position().zPos);
    		
    		// fahre ich auf zB den Warenkorb zu, koennte das Hindernis das Ziel sein
    		if ((cpu.ML.getStatusObstacleFound() && !iAmClose(p)) || (cpu.bump.value()))
    		{
    			cpu.log("Hindernis! Weiche aus.");
    			if (cpu.bump.value())
    			{
    				cpu.log("Bumper aktiviert");
    			}
    			else
    			{
    				cpu.log("Bumper NICHT aktiviert");
    			}
    			ausweichenFahreZu(1);
    		}
    		
    	}
    	cpu.log("Reached Goal!");
    }
	
    public void transportiereZu(Position p)
    {
    	cpu.ML.setWaipoint(p.xPos*1000, p.zPos*1000);
    	while ((!cpu.ML.transportToCoordinates() && !iAmClose(p)))
    	{
    		cpu.log("Transportiere nach " + p.xPos + "/" + p.zPos + ", bin gerade " + cpu.position().xPos + "/" + cpu.position().zPos);
    		
    		// fahre ich auf zB den Warenkorb zu, koennte das Hindernis das Ziel sein
    		if ((cpu.ML.getStatusObstacleFound() && !iAmClose(p))  || (cpu.bump.value()))
    		{
    			cpu.log("Hindernis! Weiche aus.");
    			if (cpu.bump.value())
    			{
    				cpu.log("Bumper aktiviert");
    			}
    			else
    			{
    				cpu.log("Bumper NICHT aktiviert");
    			}
    			cpu.log("bumperValue: ");
    			ausweichenTransportiereZu(1);
    		}
    		
    	}
    	cpu.log("Reached Goal!");
    }
    

    public Cart nimmCartAuf(CartArea myCartArea)
    {
    	Position p = new Position(0,0);
    	CartPosition emptyCart = null;
    	
    	cpu.warte((int)(Math.random()*3000));
    	
    	
    	List<ID> communicationParticipants = cpu.mgmt.getRegisteredParticipants();
    	List<Object> messageList;
    	
    	boolean cartBesetzt = false;
    	
    	Cart myCart = null;
    	
    	
    	// WICHTIG ! ! ! 
    	// messageList enthaelt einen Nullpointer, nachdem wir getCartpositions hineingespeichert haben
    	// Code funktioniert auch so, allerdings ohne Abfrage, ob CartPositions bereits angefahren werden
    	// fehler NICHT gefunden
    	// TODO 
    	
    	
		messageList = cpu.mgmt.receiveMessages(cpu.comID);
    	cpu.log("Nachrichtenabfrage erfolgt");

    	for (CartPosition pos: StatusVerwaltung.mgmt.getCartPositions(myCartArea))//cpu anders
    	{
    		
    		if (messageList != null)
    		{	
    			cpu.log("BEI MIR GIBT’S NACHRICHTEN GOTTVERDAMMT!");
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
	    					cpu.mgmt.sendMessage(cpu.comID, emptyCart, tempParticipant);
	    					cpu.log("message gesendet an: " + tempParticipant);
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
		fahreZu(nextIP.getCoordinates());
		
		// Lade Sachen ein
		// TODO: Kapazitaet beruecksichtigen
		if(cpu.mgmt.load(currentItem.getAmount(), nextIP, myCart))
			cpu.log("Lade Sachen ein!");
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
    	
    	// kleines stueck nach hinten fahren

    	cpu.OD.setVelocity(-200, -200*f, 0);
    	cpu.warte(1000);
    	cpu.OD.setVelocity(0,0,0);
    }
    
    protected void ausweichenFahreZu(int f)
    {    	
    	
    	// kleines stueck nach hinten fahren

    	cpu.OD.setVelocity(-200, -200*f, 0);
    	cpu.warte(1000);
    	cpu.OD.setVelocity(0,0,0);
//    	cpu.warte((int)(Math.random()*5000));
//    	if (manoeveranzahl > 2)
//    	{
//    		oftausgewichen = true;
//    	}
//    	if (!oftausgewichen)
//    	{	
//    		cpu.OD.setVelocity(0, 0, 40);
//    	}
//    	else
//    	{
//    		cpu.OD.setVelocity(0, 0, -40);
//    	}
//    	float x = cpu.position().xPos;
//    	float z = cpu.position().zPos;
//
//
//    	if (manoeveranzahl <= 5 )
//    	{
//        	cpu.log("Position: " + x + ", " + z + " ; weiche nun aus");
//        	weicheweg(x,z);
//    	}
//    	else
//    	{
//    		//TODO was wenn der zu oft ausweicht und feststeckt?
//    	}

//    	cpu.warte(1500*f);
//    	cpu.OD.setVelocity(0,0,0);
//    	cpu.OD.setVelocity(150, 0, 0);
//    	cpu.warte(2000*f);
//    	cpu.warte((int)(1000+(Math.random()*3000)));
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
    
    
    

    
    
 // fahre zu fuer die Ausweichmethode
// 	public void weicheweg(float x, float z) 
// 	{
// 		
// 		if (!oftausgewichen)
// 		{
// 			cpu.ML.setWaipoint((x+1), (z+1));
// 		}
// 		else
// 		{
// 	 		cpu.ML.setWaipoint((x-1), (z-1));
// 		}
//    	cpu.log("weiche nach : " + (x+1) + ", " + (z+1) + " aus");
// 		while (!cpu.ML.transportToCoordinates())
//    	{
//    		if (cpu.ML.getStatusObstacleFound())
//    		{
//    			manoeveranzahl++;
//    			cpu.log("AAACHTUNG NOCHMAL Hindernis! Weiche aus. Manoever: " + manoeveranzahl + " ;boolean: "+ oftausgewichen);
//
//    			ausweichen(1);
//    		}
//    	}
// 	}
    
    
    
}


