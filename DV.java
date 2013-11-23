import java.lang.Math;

public class DV implements RoutingAlgorithm {
    
    static int LOCAL = -1;
    static int UNKNOWN = -2;
    static int INFINITY = 60; 
    
    private Router router;
    private boolean allowExpire;
    private boolean allowPReverse;
    private int updateInterval;

    private RoutingTable table;

    public DV()
    {
        table = new RoutingTable();
    }
    
    public void setRouterObject(Router obj)
    {
        router = obj;
    }
    
    public void setUpdateInterval(int u)
    {
        updateInterval = u;
    }
    
    public void setAllowPReverse(boolean flag)
    {
        allowPReverse = true;
    }
    
    public void setAllowExpire(boolean flag)
    {
        allowExpire = true;
    }
    
    public void initalise()
    {
    }
    
    public int getNextHop(int destination)
    {
        return 0;
    }
    
    public void tidyTable()
    {
    }
    
    public Packet generateRoutingPacket(int iface)
    {
        // Add every entry in the routing table to this payload
        Payload thisPayload = new Payload();
        for(int i = 0; i < routingTable.numEntries(); i++)
        {
            if(routingTable.hasEntry(i))
            {
                thisPayload.addEntry(routingTable.getEntry(i));
            }
        }

        RoutingPacket thisPacket = new RoutingPacket(router.getId(), iface);
        thisPacket.setPayload(thisPayload);
    }
    
    public void processRoutingPacket(Packet p, int iface)
    {
    }
    
    public void showRoutes()
    {
    }
}

// This is basically a dynarray for storing DVRoutingTable Entries, where entries
// are indexed by destination. However, it doesn't shrink.

class RoutingTable
{
    private DVRoutingTableEntry[] routingTable;
    private int numEntries;

    public RoutingTable(){
        routingTable = new DVRoutingTableEntry[2];
        numEntries = 0;
    }

    public int numEntries()
    {
        return numEntries;
    }

    public void addEntry(DVRoutingTableEntry entry)
    {
        int id = entry.getDestination();

        // If this does not fit, expand the table.
        if(routingTable.length < id)
        {

            // Find the nearest power of two that id is less than.
            int newSize;
            for(newSize = 2; newSize < id; newSize = newSize*2){}

            DVRoutingTableEntry[] newTable = new DVRoutingTableEntry[newSize];
            
            // Copy everything in the old table over to the new table.
            for(int i = 0; i < routingTable.length; i++)
            {
                newTable[i] = routingTable[i];
            }

            routingTable = newTable;

        }

        routingTable[id] = entry;
        numEntries++;

    }

    // Has this destination already been logged in the table?
    public boolean hasEntry(int dest)
    {
        if(routingTable[dest] == null)    return false;
        else    return true;
    }

    // Get the entry corresponding to destination dest.

    public DVRoutingTableEntry getEntry(int dest)
    {
        if(routingTable[dest] == null)   
            throw new Error("Entry does not yet exist in table");
        return routingTable[dest];
    }


}

class DVRoutingTableEntry implements RoutingTableEntry
{
    private int destination;
    private int interf;
    private int metric;
    private int time;

    public DVRoutingTableEntry(int d, int i, int m, int t)
	{
        destination = d;
        interf = i;
        metric = m;
        time = t;
	}

    public int getDestination() {
        return destination;
    } 
    public void setDestination(int d) {
        destination = d;
    }
    public int getInterface() {
        return interf;
    }
    public void setInterface(int i) {
        interf = i;
    }
    public int getMetric() {
        return metric;
    }
    public void setMetric(int m) {
        metric = m;
    } 
    public int getTime() {
        return time;
    }
    public void setTime(int t) {
        time = t;
    }
    
    public String toString() 
	{
	    return String.format("d %d i %d m %d", destination, interf, metric);
	}
}

