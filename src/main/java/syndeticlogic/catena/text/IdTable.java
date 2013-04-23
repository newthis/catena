package syndeticlogic.catena.text;

import java.util.Iterator;
import java.util.TreeSet;

import syndeticlogic.catena.type.Codeable;

public abstract class IdTable implements Codeable {
    protected static int PAGE_SIZE = 8192;
    protected enum TableType { Uncoded, VariableByteCoded };
    abstract void addId(int docId);
    abstract void resetIterator();
    abstract boolean hasNext();    
    abstract int peek();
    abstract int advanceIterator();
    abstract int getLastDocId();


    public TreeSet<Integer> getValues() {
        TreeSet<Integer> docIds = new TreeSet<Integer>();
        resetIterator();
        while(hasNext()) {
            docIds.add(advanceIterator());
        }
        return docIds;
    }
    
    public TreeSet<Integer> intersect(TreeSet<Integer> docIds) {
        resetIterator();
        Iterator<Integer> iter = docIds.iterator();
        if(!hasNext() || !iter.hasNext()) {
            return new TreeSet<Integer>();
        }
        
        int nextPosting = advanceIterator();
        int nextDocId = iter.next();        
        TreeSet<Integer> matches = new TreeSet<Integer>();
        while(true) {
            if (nextPosting > nextDocId) {
                if(iter.hasNext()) {
                    nextDocId = iter.next();
                } else { 
                    break;
                }
            } else if (nextPosting < nextDocId) {
                if(hasNext()) {
                    nextPosting = advanceIterator();
                } else { 
                    break;
                }
            } else {
                matches.add(nextDocId);
                if (hasNext() && iter.hasNext()) {
                    nextPosting = advanceIterator();
                    nextDocId = iter.next();
                } else {
                    break;
                }
            }
        }
        return matches;
    }
    
    public void dumpDocs() {
        resetIterator();
        while(hasNext()) {
            System.err.println(advanceIterator());
        }
        resetIterator();
    }
    
    public static int getPageSize() {
        return PAGE_SIZE;
    }
    
    public static void setPageSize(int pageSize) {
        IdTable.PAGE_SIZE = pageSize;
    }
}
