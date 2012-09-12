/*
 * Represents data sent from client/server
 *
 * @author Paul Wagner
 *
 */
package com.typology;

import java.util.TreeMap;

public class CompletData {
    // TO SERVER

    public String[] words;
    public String offset;
    //ID
    public String uid;
    public final String uid_type = "DEV";
    public boolean force_primitive = false;
    public final boolean use_primitive = false;
    public String sel = "";
    
    // FROM SERVER
    public TreeMap<Integer, String> result;

    // Client Construct
    public CompletData(String[] words, String offset, String ID, boolean force_primitive, String lastSelection) {
        this.words = words;
        this.offset = offset;
        this.result = null;
        this.uid = ID;
        this.force_primitive = force_primitive;
        this.sel = lastSelection;
    }

    // Server Construct
    public CompletData(TreeMap<Integer, String> result) {
        this.result = result;
        this.words = null;
        this.offset = "";
    }

    @Override
	public String toString() {
        if (result == null) {
            return "";
        }
        String s = "";
        for (Integer val : result.keySet()) {
            s += result.get(val) + "(" + val + "), ";
        }
        return s;
    }

    public String toSentence() {
        String s = "";
        for (int i = 0; i < words.length; i++) {
            s += words[i] + " ";
        }
        s += offset;
        return s;
    }
}
