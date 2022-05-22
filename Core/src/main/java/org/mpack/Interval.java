package org.mpack;


import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Interval {


    static public ArrayList<Pair<Integer, Integer>> findSmallestWindow(List<Pair<Integer, Integer>> positionsToWords, int noWords, boolean isPhrase) {
        if (positionsToWords.isEmpty()) {
            return null;
        }
        // 4 1   5 2     6 1    7 3`
        HashMap<Integer, Integer> counts = new HashMap<>();
        ArrayList<Pair<Integer, Integer>> retList = new ArrayList<>();
        int i = 0; //position of starting window
        int j = i;
        Pair<Integer, Integer> retPair = null; // return the position of the first word if didn't find all

        while (j < positionsToWords.size()) {
            Pair<Integer, Integer> current = positionsToWords.get(j);
            int currWord = current.getSecond();
            counts.put(currWord, counts.getOrDefault(currWord, 0) + 1); // increase count of current window
            // a b c d a s d

            if (counts.size() == noWords) {
                // try to increase i as much as possible to wrap the smallest
                Pair<Integer, Integer> firstInSeq = positionsToWords.get(i);
                int firstWord = firstInSeq.getSecond();

                while (counts.get(firstWord) > 1) {
                    counts.put(firstWord, counts.get(firstWord) - 1); // decrease it and skip it forward
                    i++; //skip the first as we hava another one
                    firstInSeq = positionsToWords.get(i);
                    firstWord = firstInSeq.getSecond();
                }

                //System.out.println(firstInSeq.getFirst() + " to " + current.getFirst()); // found a min
                if (retPair == null || current.getFirst() - firstInSeq.getFirst() < retPair.getSecond() - retPair.getFirst())
                    retPair = Pair.of(firstInSeq.getFirst(), current.getFirst());

                if(isPhrase)
                    retList.add(Pair.of(firstInSeq.getFirst(), current.getFirst()));
                counts.remove(firstWord); // remove the first to try to find a one better
                i++;
            }

            j++;

        }
        retList.add(0,retPair);

        return retList;
    }

}
