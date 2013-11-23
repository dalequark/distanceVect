public class RoutingTable
{
    private DVRoutingTableEntry[] routingTable;

    public RoutingTable(){
        routingTable = new DVRoutingTableEntry[2];
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


    }

    public boolean hasEntry(int dest)
    {
        if(routingTable[dest] == null)    return false;
        else    return true;
    }

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

    public static void main(String[] args)
    {   
        RoutingTable table = new RoutingTable();
        table.addEntry(new DVRoutingTableEntry(1,2,3,4));
        table.addEntry(new DVRoutingTableEntry(100,2,3,4));
        table.addEntry(new DVRoutingTableEntry(10000,2,3,4));
        table.addEntry(new DVRoutingTableEntry(5,2,3,4));

        System.out.println("Really contains 1, 100, 10000, and 5");
        System.out.println("Contains 1? " + table.hasEntry(1));
        System.out.println("Contains 2? " + table.hasEntry(2));
        System.out.println("Contains 10? " + table.hasEntry(10));
        System.out.println("Contains 100? " + table.hasEntry(100));
        System.out.println("Contains 5? " + table.hasEntry(100));
        System.out.println("Contains 10000? " + table.hasEntry(10000));

        System.out.println("Entry 10000 is " + table.getEntry(10000));

    }