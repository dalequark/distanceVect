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
        table.addEntry(new DVRoutingTableEntry(router.getId(), LOCAL, 0, EXPIRATION_INF));
    }
    
    public int getNextHop(int destination)
    {
        if(table.hasEntry(destination))
        {
            return table.getEntry(destination).getInterface();
        }
        else
            return UNKNOWN;
    }
    
    public void tidyTable()
    {
        //addMyLinks();
    }
    
    public Packet generateRoutingPacket(int iface)
    {   
        // Add every entry in the routing table to this payload.
        // Also, figure out getDestination

        Payload thisPayload = new Payload();
        DVRoutingTableEntry[] entries = table.getTable();

        for(int i = 0; i < entries.length; i++)
        {
            thisPayload.addEntry(entries[i]);
        }

        RoutingPacket thisPacket = new RoutingPacket(router.getId(), Packet.BROADCAST);
        thisPacket.setPayload(thisPayload);
        return thisPacket;

    }
    
    public void processRoutingPacket(Packet packet, int iface)
    {
        // Do not process packets I send to myself
        if(packet.getSource() == router.getId())
            return;

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
                table.addEntry(DVRoutingTableEntry.fromEntry(thisEntry));
            }

        }

    }
    
    public void showRoutes()
    {
        System.out.println("Router " + router.getId());
        DVRoutingTableEntry[] thisTable = table.getTable();
        for(int i = 0; i < thisTable.length; i++)
        {
            if(thisTable[i] == null)
            {
                System.out.println("Entry " + i + " was *null*!");
            }
            else
                System.out.println(thisTable[i].toString());
        }
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

    // Takes this dynamically expanding array and returns a normal array
    // (in which entries are no longer indexed by destination)

    public DVRoutingTableEntry[] getTable()
    {
        DVRoutingTableEntry[] table = new DVRoutingTableEntry[numEntries];
        int j = 0;
        int entriesFound = 0;
        for(int i = 0; i < routingTable.length; i++)
        {
            if(routingTable[i] != null)
                entriesFound++;
            if(hasEntry(i))
            {
                table[j] = getEntry(i);
                j++;
            }
        }

        return table;
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
        // If this is a new destination, increment numEntries
        if(routingTable[id] == null)    numEntries++;
        routingTable[id] = entry;

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
        return DVRoutingTableEntry.fromEntry(routingTable[dest]);
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

    // Copy constructor

    public static DVRoutingTableEntry fromEntry(DVRoutingTableEntry oldEntry)
    {
        return new DVRoutingTableEntry(oldEntry.destination, oldEntry.interf, oldEntry.metric, oldEntry.time);
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

