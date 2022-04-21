package org.mpack;
import java.util.ArrayList;

public class wordInfo {
    private int TF;
    private ArrayList<Integer> flags;
    private ArrayList<Integer> positions;

    wordInfo()
    {
        //this.url = url;
        TF = 0;
        positions = new ArrayList<Integer>();
       // positions.set(0, firstPosition);
        flags = new ArrayList<Integer>(2); // title, header.
    }

    boolean setFlags(short i, int value) {
        if(!(i >= 0 && i < flags.size())) return false;
        value = Math.max(value, 0);
        flags.set(i, value);
        return true;
    }
    void incTF() {TF++;}
    void setTF(int tf){TF = Math.max(tf, 0);}
    int getTF(){return TF;}

    boolean addPosition(int pos)
    {
        if(pos < 0) return false;
        positions.add(pos);
        return true;
    }
    ArrayList<Integer> getFlags(){return new ArrayList<Integer>(this.flags);}
    ArrayList<Integer> getPositions(){return new ArrayList<Integer>(this.positions);}
}
