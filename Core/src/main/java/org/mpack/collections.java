package org.mpack;


import org.springframework.data.util.Pair;

import java.util.List;

public
class collections
{
    String url;
    String paragraph;
    String title;
    int TF_IDF;

    int subQuery;

    boolean ifDeleted;
    int token_count;
    double priority;
    double pagerank;
    List<Integer> flags;
    List<Pair<Integer,Integer>> positions;

    int wordNear;


    public int compare(collections url2){
        //positions --> the whole search query with the same order in sequence
        if(subQuery > url2.subQuery)
            return 1;
        else if(subQuery < url2.subQuery)
            return -1;

            //window size
        else if(wordNear < url2.wordNear)
            return 1;
        else if(wordNear > url2.wordNear)
            return -1;

        //tokenCount
        if(token_count > url2.token_count)
            return 1;
        else if (token_count < url2.token_count)
            return -1;

            //title
        else if (flags.get(0) > url2.flags.get(0))
            return 1;
        else if (flags.get(0) < url2.flags.get(0))
            return -1;

            //header
        else if (flags.get(1) > url2.flags.get(1))
            return 1;
        else if (flags.get(1) < url2.flags.get(1))
            return -1;

            //priority  IDF-TF -- pagerank
        else if (TF_IDF + ((1/3) * pagerank) > url2.TF_IDF + ((1/3) * url2.pagerank))
            return 1;
        else if (TF_IDF + ((1/3) * pagerank) < url2.TF_IDF + ((1/3) * url2.pagerank))
            return -1;

        else if (TF_IDF > url2.TF_IDF)
            return 1;
        else if (TF_IDF < url2.TF_IDF)
            return -1;

            //pageRank
        else if (pagerank > url2.pagerank)
            return 1;
        else if (pagerank < url2.pagerank)
            return -1;

        else
            return 0;
    }

}
