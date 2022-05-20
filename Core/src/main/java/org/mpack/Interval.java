package org.mpack;


import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;

public class Interval {


    static public Pair<Integer, Integer> findSmallestWindow(ArrayList<Pair<Integer, Integer>> positionsToWords, int noWords) {
        if (positionsToWords.isEmpty()) {
            return null;
        }
        // 4 1   5 2     6 1    7 3
        HashMap<Integer, Integer> counts = new HashMap<>();
        int i = positionsToWords.get(0).getFirst(); //position of starting window
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

                counts.remove(firstWord); // remove the first to try to find a one better
                i++;
            }
            j++;
        }

        return retPair;
    }

    public static void main(String[] args) {
        // a b b b b b a c a c
        int no_words = 3;
        ArrayList<Pair<Integer, Integer>> positionsToWords = new ArrayList<>();
        positionsToWords.add(Pair.of(0, 0));
        positionsToWords.add(Pair.of(40, 1));
        positionsToWords.add(Pair.of(42, 1));
        positionsToWords.add(Pair.of(43, 1));
        positionsToWords.add(Pair.of(44, 1));
        positionsToWords.add(Pair.of(55, 0));
        positionsToWords.add(Pair.of(66, 2));
        positionsToWords.add(Pair.of(77, 0));
        positionsToWords.add(Pair.of(80, 2));
        positionsToWords.add(Pair.of(91, 1));

        var a = findSmallestWindow(positionsToWords, no_words);
        System.out.println(a);
    }
}
