package org.mpack;
import java.util.ArrayList;

public class WordInfo {
    private int TF;
    private ArrayList<Integer> flags;
    private ArrayList<Integer> positions;

    WordInfo()
    {
        TF = 0;
        positions = new ArrayList<>();
        flags = new ArrayList<>(2); // title, header.
    }

    boolean setFlags(short i, int value) {
        if(i < 0) return false;
        value = Math.max(value, 0);
        flags.add(i, value);
        return true;
    }
    void incTF() {TF++;}
    void setTF(int tf){TF = Math.max(tf, 0);}
    int getTF(){return TF;}

    void addPosition(int pos)
    {
        if(pos < 0) return;
        positions.add(pos);
    }
    ArrayList<Integer> getFlags(){return new ArrayList<>(this.flags);}
    ArrayList<Integer> getPositions(){return new ArrayList<>(this.positions);}
}
