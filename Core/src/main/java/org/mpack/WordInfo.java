package org.mpack;
import java.util.ArrayList;

public class WordInfo {
    private int TF;
    private float normalizedTF;
    int wordCount;
    private ArrayList<Integer> flags;
    private ArrayList<Integer> positions;
    private float pageRank;

    WordInfo(int wordCount)
    {
        TF = 0;
        this.wordCount = wordCount;
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
    float getNormalizedTF() {
        normalizedTF = (((float) TF) / wordCount);
        return normalizedTF;
    }

    void addPosition(int pos)
    {
        if(pos < 0) return;
        positions.add(pos);
    }
    void setPageRank(float rank)
    {
        pageRank = Float.max(0, rank);
    }
    ArrayList<Integer> getFlags(){return new ArrayList<>(this.flags);}
    ArrayList<Integer> getPositions(){return new ArrayList<>(this.positions);}
    float getPageRank() {
        return pageRank;
    }
}
