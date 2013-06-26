package gruppe8;

import de.hpi.sam.ordermanagement.*;
import de.hpi.sam.robotino.Position;
import de.hpi.sam.robotino.actor.RobotOmniDrive;
import de.hpi.sam.robotino.logic.MoveLogic;
import de.hpi.sam.robotino.sensor.RobotNorthStar;

public class RobotCPU {
	
	// ==================================================================
	// 0. CONFIG / WERTE
	// ==================================================================
	
	
	// Schwelle, ab wann der Roboter "nahe" einer Position ist
    public static float near = 0.3f;
    
    public static int orderID = 0;
    
    // Eindeutige Robot ID (fuer Logs), wird im Constructor initialisiert
    public int robotID = 0;
    public static int nextRobotID = 1;
    
    protected StatusVerwaltung mySV;
    protected FahrVerwaltung myFV;
    
    public OrderManagement mgmt;
    public RobotOmniDrive OD;
    public MoveLogic ML;
    public RobotNorthStar NS;
	
	
    // ==================================================================
	// 1. HAUPTFUNKTIONEN
    // ==================================================================
    
    /**
     * Constructor / Initialisierung
     */
    public RobotCPU(OrderManagement mgmt, MoveLogic yourML, RobotOmniDrive yourOD, RobotNorthStar yourNS)
    {
    	// Roboter bennenen fuer Uebersicht in Logs
    	this.robotID = nextRobotID;
        nextRobotID++;
        
        this.mgmt = mgmt; // order management
        this.OD = yourOD; // omni drive
        this.ML = yourML; // move logic
        this.NS = yourNS; // north star
        
        // FahrVerwaltung und StatusVerwaltung initialisieren
        this.myFV = new FahrVerwaltung(this);
        
        // RobotCPU uebergibt sich
        this.mySV = new StatusVerwaltung(myFV, this);
        
        log("Roboter aktiviert mit ID Nummer " + this.robotID);
        
    }
    
    public boolean run()
    {    	   	
    	log("Lade Statusverwaltung...");
    	while(mySV.bearbeiteOrders())
    		log("Order ausgefuehrt.");
       return true;
        
    }
    
    
	

	
    // ==================================================================
	// 2. HILFSFUNKTIONEN
    // ==================================================================

	public Position position() {
		return new Position (this.NS.posX()/1000, this.NS.posZ()/1000);
	}
	
    public void log(String msg)
    {
    	System.out.println("[" + robotID + "] " + msg);
    }
    
    public String strPos(Position p)
    {
    	return p.xPos + "/" + p.zPos;
    }
    
    protected void warte(int zeit)
    {
    	try {
			Thread.sleep(zeit);
			this.log("backoffTime: " + zeit );
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    
    
    // 3. DEBUG FUNKTIONEN
    


}
