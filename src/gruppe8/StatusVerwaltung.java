package gruppe8;

import de.hpi.sam.ordermanagement.Cart;
import de.hpi.sam.ordermanagement.CartArea;
import de.hpi.sam.ordermanagement.Order;
import de.hpi.sam.ordermanagement.OrderItem;
import de.hpi.sam.ordermanagement.OrderManagement;

public class StatusVerwaltung {
	
	protected FahrVerwaltung myFV;
	protected RobotCPU cpu;
	public static OrderManagement mgmt;
	
	// Konstruktor
	StatusVerwaltung(FahrVerwaltung yourFV, RobotCPU yourCPU)
	{
		this.myFV = yourFV;
		this.cpu = yourCPU;
		mgmt = cpu.mgmt;
	}
	
	/** 
	 * Order holen und ausfuehren
	 */
	public boolean bearbeiteOrders()
	{
		cpu.warte((int)(Math.random()*3000)); // Wartezeit, um zu verhindern, dass 2mal der selbe Auftrag aufgenommen wird
    	cpu.log("BackOffTime generated");
    	
		Order myOrder = mgmt.getOrderList().get(0);
		cpu.log("Habe neuen Auftrag: " + myOrder.hashCode());
		
		
		// Order aus Liste der bereitstehenden Orders loeschen
		mgmt.getOrderList().remove(myOrder);
		
		return fuehreAuftragAus(myOrder);
	}
	
	/**
	 * gegebene Order ausfuehren
	 * @param myOrder
	 * @return
	 */
	public boolean fuehreAuftragAus(Order myOrder)
	{
		
		
		// CartArea bestimmen und daraus einen freien Kart holen
    	CartArea myCartArea = mgmt.getCartArea(myOrder);
    	Cart myCart = null;
    	myCart = myFV.nimmCartAuf(myCartArea);
    		
    	
    	// Alle Order Items abarbeiten, zum Issuing Point fahren, einladen
    	
    	cpu.log("Arbeite nun " + mgmt.getOrderItemList(myOrder).size() + " Bestellungen ab.");
    	
    	for (OrderItem currentItem: mgmt.getOrderItemList(myOrder))
    	{
    		myFV.holeProdukt(currentItem, myCart);    		
    	}
    	
    	
    	boolean success = myFV.stelleCartAb(myCartArea, myOrder);
    
       
        //com.waitForUpdate();
		
		return success;
	}

}
