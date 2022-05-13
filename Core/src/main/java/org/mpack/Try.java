package org.mpack;

public class Try {
    public static void main(String[] args)
    {
        String parag = "If you enjoyed hi sa asd ads sdef fsjd this frightening Replay then be sure to check out our recent episodes fsd fsdrie erwor fsdfs featuring Dead Space 2 or Amnesia: d";
        System.out.println(Ranker.text(parag, "Amnesia: d", parag.indexOf("Amnesia: d")));
    }
}
