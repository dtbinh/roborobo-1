package gruppe8;

import de.hpi.sam.ordermanagement.*;
import de.hpi.sam.robotino.Position;

public class FahrVerwaltung {
	
	RobotCPU cpu;
	
	FahrVerwaltung(RobotCPU yourCPU)
	{
		this.cpu = yourCPU;
	}
	
	// HAUPTFUNKTIONEN
	
    public void fahreZu(Position p) 
    {
    	cpu.ML.setWaipoint(p.xPos*1000, p.zPos*1000);
    	while (!cpu.ML.driveToCoordinates() && !iAmClose(p))
    	{
    		cpu.log("Fahre nach " + p.xPos + "/" + p.zPos + ", bin gerade " + cpu.position().xPos + "/" + cpu.position().zPos);
    		
    		// fahre ich auf zB den Warenkorb zu, koennte das Hindernis das Ziel sein
    		if (cpu.ML.getStatusObstacleFound() && !iAmClose(p))
    		{
    			cpu.log("Hindernis! Weiche aus.");
    			ausweichen(1);
    		}
    		
    	}
    	cpu.log("Reached Goal!");
    }
	
    public void transportiereZu(Position p)
    {
    	cpu.ML.setWaipoint(p.xPos*1000, p.zPos*1000);
    	while (!cpu.ML.transportToCoordinates() && !iAmClose(p))
    	{
    		cpu.log("Transportiere nach " + p.xPos + "/" + p.zPos + ", bin gerade " + cpu.position().xPos + "/" + cpu.position().zPos);
    		
    		// fahre ich auf zB den Warenkorb zu, koennte das Hindernis das Ziel sein
    		if (cpu.ML.getStatusObstacleFound() && !iAmClose(p))
    		{
    			cpu.log("Hindernis! Weiche aus.");
    			ausweichen(1);
    		}
    		
    	}
    	cpu.log("Reached Goal!");
    }
    

    public Cart nimmCartAuf(CartArea myCartArea)
    {
    	Position p = new Position(0,0);
    	CartPosition emptyCart = null;
    	
    	for (CartPosition pos: StatusVerwaltung.mgmt.getCartPositions(myCartArea))
    	{
    		if (StatusVerwaltung.mgmt.getstate(pos) == ECartPositionState.EMPTY_CART)
    			{
    				emptyCart = pos;
    				// log("Cart Position gefunden: " + emptyCart.getCoordinates().xPos + "," + emptyCart.getCoordinates().zPos);
    				break;
    			}
	
    	}
    	
    	p = emptyCart.getCoordinates();
    	
    	// 2. Kart holen
    	
    	cpu.log("Driving  to Empty Cart.");
    	fahreZu(p);
    	stueckNachVorne();
    	
    	cpu.log("taking cart.");
    	Cart myCart = null;
    	if ((myCart = cpu.mgmt.takeCart(emptyCart)) != null)
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
    protected void ausweichen(int f)
    {    	
    	
    	// kleines stueck nach hinten fahren
    	cpu.OD.setVelocity(-200, -200*f, 0);
    	cpu.warte(1500*f);
    	cpu.OD.setVelocity(0,0,0);
    	
    }
    
    protected void stueckNachVorne()
    {
    	cpu.OD.setVelocity(50, 0, 0);
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
    

}
