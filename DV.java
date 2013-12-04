import java.lang.Math;
import java.util.Enumeration;

public class DV implements RoutingAlgorithm {
    
    static int LOCAL = -1;
    static int UNKNOWN = -2;
    static int INFINITY = 60; 

    private Router router;
    private boolean allowExpire;
    private boolean allowPReverse;
    private int updateInterval;
    private RoutingTable table;
    private int[] interfaces;

    private int timeout;
    private int gc;

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
        timeout = u*4;
        gc = u*3;
    }
    
    public void setAllowPReverse(boolean flag)
    {
        allowPReverse = flag;
    }
    
    public void setAllowExpire(boolean flag)
    {
        allowExpire = flag;
    }
    
    public void initalise()
    {
        table.addEntry(new DVRoutingTableEntry(router.getId(), LOCAL, 0, 1));
    }
    
    public int getNextHop(int destination)
    {
        if(table.hasEntry(destination))
        {
            DVRoutingTableEntry thisEntry = table.getEntry(destination);
            if(thisEntry.getMetric() >= INFINITY || !router.getInterfaceState(thisEntry.getInterface()))
                return UNKNOWN;
            else
                return thisEntry.getInterface();
        }
        else
            return UNKNOWN;
    }
    
    public void tidyTable()
    {
        int t1 = router.getCurrentTime();
        
        // Check to make sure that no links are down.

        Link[] myLinks = router.getLinks();
        for(int i = 0; i < myLinks.length; i++)
        {
            if(!myLinks[i].isUp())
            {
                int downIface;
                if( myLinks[i].getRouter(0) == router.getId() )
                    downIface = myLinks[i].getInterface(0);
                else
                    downIface = myLinks[i].getInterface(1);

                DVRoutingTableEntry[] routingTable = table.getTable();
                for(int j = 0; j < routingTable.length; j++)
                {
                    if(routingTable[j].getMetric() != INFINITY && routingTable[j].getInterface() == downIface)
                    {
                        // If an interface is down, replace its entry with a metric of infinity
                        
                        DVRoutingTableEntry updatedEntry = DVRoutingTableEntry.fromEntry(routingTable[j]);
                        updatedEntry.setMetric(INFINITY);
                        updatedEntry.setTime(t1);
                        table.addEntry(updatedEntry);
                        
                    }

                }
            } 
        }

        // expire stale entries
        if(allowExpire)
        {
            DVRoutingTableEntry[] routingTable = table.getTable();
            DVRoutingTableEntry thisEntry;
            int t0;
            
            for(int i = 0; i < routingTable.length; i++)
            {
                thisEntry = routingTable[i];
                t0 = thisEntry.getTime();

                // GC timer
                if(thisEntry.getMetric() == INFINITY && ((t1 - t0) > gc))
                {
                    table.removeEntry(thisEntry.getDestination());
                } 

                // timeout timer
                /*
                else if( ((t1 - t0) > timeout) && thisEntry.getInterface() != LOCAL)
                {
                    thisEntry.setMetric(INFINITY);
                    thisEntry.setTime(t1);
                    table.addEntry(thisEntry);
                }*/
                    
            }
        }

        

    }
    
    public Packet generateRoutingPacket(int iface)
    {   
        if(!router.getInterfaceState(iface))
        {
            return null;
        }
        // Add every entry in the routing table to this payload.
        Payload thisPayload = new Payload();
        DVRoutingTableEntry[] entries = table.getTable();

        for(int i = 0; i < entries.length; i++)
        {
            DVRoutingTableEntry thisEntry = entries[i];
            if(thisEntry.getInterface() == iface && allowPReverse)
            {
                thisEntry.setMetric(INFINITY);
            }
           thisPayload.addEntry(thisEntry);
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
            // in the routing table, add it. Also, always adopt new value for metric for dest
            // x if the interface we currently send to x over is iface. Do not add a value 
            // if its metric is infinity if it is not in the routing table.

            if( !table.hasEntry(dest)  || 
                (table.getEntry(dest).getInterface() == iface) ||
                ((thisEntry.getMetric() + linkWeight) < table.getEntry(dest).getMetric()))
            {

                // Do not add an entry that is infinity unless its invalidating a previous route.
                if(allowExpire && thisEntry.getMetric() == INFINITY && !table.hasEntry(dest))   continue;
                if(table.hasEntry(thisEntry.getDestination()) && table.getEntry(thisEntry.getDestination()).getMetric() == INFINITY)    continue;
                
                if(thisEntry.getMetric() != INFINITY)
                    thisEntry.setMetric(thisEntry.getMetric() + linkWeight);

                // if we got an update repeating that our entry to dest is stale,
                // do not change the table (i.e. interrupt GC timer)


                // Change interface to be the interface on which we received this packet
                thisEntry.setInterface(iface);


                thisEntry.setTime(router.getCurrentTime());
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

    public void removeEntry(int dest)
    {
        routingTable[dest] = null;
        numEntries--;
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

