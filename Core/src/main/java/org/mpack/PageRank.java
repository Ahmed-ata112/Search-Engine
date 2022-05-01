package org.mpack;

import java.util.*;

class PageNode {
    public String name;
    private float current_score;
    private float new_score = 0;

    PageNode(String name) {
        this.name = name;
    }

    public void setCurrentScore(float score) {
        current_score = score;
    }

    public float getCurrentScore() {
        return current_score;
    }

    public void setNewScore(float score) {
        new_score = score;
    }

    public float getNewScore() {
        return new_score;
    }
}

public class PageRank {
    static float INITIAL_SCORE = (float) 100;
    // Nodes. Easy look up by name of node and its corresponding PageNode object.
    private static final HashMap<String, PageNode> nameToNode = new HashMap<String, PageNode>();
    // Edges. The association between a node and the nodes it sends a directed edge.
    private static final HashMap<PageNode, List<PageNode>> pagesMatrix = new HashMap<PageNode, List<PageNode>>();

    // Parse file and initialize graph represented by member variables.
    public void initRankMatrix(HashSet<String> urlsArray, Map<String, List<String>> pagesEdges) {

        // Create name_to_node.
        for (String name : urlsArray) {
            PageNode pageNode = new PageNode(name);
            pageNode.setCurrentScore(INITIAL_SCORE);
            nameToNode.put(name, pageNode);
        }

        // add Edges
        for (String s : pagesEdges.keySet()) {
            ArrayList<PageNode> Nodes = new ArrayList<>();
            for (String ss : pagesEdges.get(s)) {
                if (urlsArray.contains(ss)) {
                    PageNode N = nameToNode.get(ss);
                    Nodes.add(N);
                }
            }

            pagesMatrix.put(nameToNode.get(s), Nodes);
        }

    }

    // One iteration of updating the score.
    public void updateScore() {
        for (PageNode origin : pagesMatrix.keySet()) {
            for (PageNode dest : pagesMatrix.get(origin)) {
                int n = pagesMatrix.get(origin).size(); // number of pages current source points to
                dest.setNewScore(dest.getNewScore() + origin.getCurrentScore() / n);
            }
        }
        for (PageNode page_node : pagesMatrix.keySet()) {
            page_node.setCurrentScore(page_node.getNewScore());
            page_node.setNewScore(0);
        }
    }

    public void run() {
        int N = nameToNode.size();

        for (int i = 0; i < N; i++) {
            updateScore();
        }
        MongoDB m = new MongoDB();
        for (Map.Entry<String, PageNode> E :
                nameToNode.entrySet()) {
            //    System.out.println(E.getKey() + ": " + E.getValue().getCurrentScore());
            m.setPageRank(E.getKey(), E.getValue().getCurrentScore());
        }


    }
}