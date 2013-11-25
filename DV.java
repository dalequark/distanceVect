import java.lang.Math;
import java.util.Enumeration;

public class DV implements RoutingAlgorithm {
    
    static int LOCAL = -1;
    static int UNKNOWN = -2;
    static int INFINITY = 60; 

    static int EXPIRATION = 100;
    static int EXPIRATION_INF = 100000;

    private Router router;
    private boolean allowExpire;
    private boolean allowPReverse;
    private int updateInterval;
    private RoutingTable table;
    private int[] interfaces;

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
        if(table.hasEntry(destination))
        {
            // For debugging
            assert(table.getEntry(destination).getDestination() == destination);
            return table.getEntry(destination).getInterface();
        }
        else
            return UNKNOWN;
    }
    
    public void tidyTable()
    {
        addMyLinks();
    }
    
    public Packet generateRoutingPacket(int iface)
    {   
        // Add every entry in the routing table to this payload.
        // Also, figure out getDestination
        Payload thisPayload = new Payload();
        for(int i = 0; i < table.numEntries(); i++)
        {
            if(table.hasEntry(i))
            {
                thisPayload.addEntry(table.getEntry(i));
            }
        }

        RoutingPacket thisPacket = new RoutingPacket(router.getId(), interfaces[iface]);
        thisPacket.setPayload(thisPayload);
        return thisPacket;

    }
    
    public void processRoutingPacket(Packet packet, int iface)
    {
        Payload thisPayload = packet.getPayload();
        int linkWeight = router.getInterfaceWeight(iface);
        Enumeration entries = thisPayload.getData().elements();
        DVRoutingTableEntry thisEntry;
        int dest;

        while(entries.hasMoreElements())
        {
            thisEntry = (DVRoutingTableEntry)entries.nextElement();
            dest = thisEntry.getDestination();

            // If the path proposed by this packet is better than the one currently stored
            // in the routing table, add it.

            if( !table.hasEntry(dest) || ((thisEntry.getMetric() + linkWeight) < table.getEntry(dest).getMetric()) )
            {
                // Update metric to take into account this hop
                thisEntry.setMetric(thisEntry.getMetric() + linkWeight);

                // Change interface to be the interface on which we received this packet
                thisEntry.setInterface(iface);

                // Add the entry.
                table.addEntry(thisEntry);
            }

        }

    }
    
    public void showRoutes()
    {
    }

    private void addMyLinks()
    {
        // Assuming number of interfaces is set.
        Link[] links = router.getLinks();
        int len = router.getNumInterfaces();
        interfaces = new int[len];

        // Get the id's of all routers directly connected to this router.
        // Add them to the routing table.
        for(int i = 0; i < len; i++)
        {
            if(links[i] != null)
            {
                // Which interface is this link attached to? Add to interface table
                int interf = links[i].getInterface(0);
                // What destination does it connect to?
                int dest = links[i].getRouter(1);

                interfaces[interf] = dest;

                int metric = links[i].getInterfaceWeight(dest);
                DVRoutingTableEntry thisEntry = new DVRoutingTableEntry(dest, interf, metric, EXPIRATION);
                table.addEntry(thisEntry);
            }
        }

        // Add this router to the routing table.

        table.addEntry(new DVRoutingTableEntry(router.getId(), LOCAL, 0, EXPIRATION_INF));
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
        if(routingTable.length - 1 < id)
        {

            // Find the nearest power of two that id is less than.
            int newSize;
            for(newSize = routingTable.length; newSize - 1 < id; newSize = newSize*2){}

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
        if((dest >= routingTable.length) || (routingTable[dest] == null))    return false;
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

