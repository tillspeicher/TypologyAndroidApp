package com.typology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SuggestionsList {
	private List<String> mCurrentSuggestions;
	private String[] mFrequentSymbols;
	private HashMap<Integer, List<String>> mSuggestionsCache;
	
	private boolean mSymbolsShown;
	final int MAX_CURRENT_SUGGESTIONS_CNT = 10;
	
	public SuggestionsList() {
		mCurrentSuggestions = new ArrayList<String>();
		mFrequentSymbols = new String[5];
		mSuggestionsCache = new HashMap<Integer, List<String>>();
	}
	
    public interface SuggestionsUpdateListener {
    	public void onSuggestionsChange(List<String> suggestions);
    }  
	
	private SuggestionsUpdateListener mListener = null;
	public void setSuggestionsUpdateListener (SuggestionsUpdateListener listener) {
		this.mListener = listener;
	}
	
	public void setSuggestions(List<String> suggestions) {
		mSymbolsShown = false;
		int wordsHash = Arrays.hashCode(SoftKeyboard.mWords);
		List<String> tempSuggestions = new ArrayList<String>();
		if (mSuggestionsCache.containsKey(wordsHash)) {		
			tempSuggestions = mSuggestionsCache.get(wordsHash);
			for (String s : suggestions) {
				if (!tempSuggestions.contains(s)) {
					tempSuggestions.add(s);
				}
			}
		} else {
			tempSuggestions = suggestions;
		}
		mSuggestionsCache.put(wordsHash, tempSuggestions);
		
		updateCurrentSuggestions();
	}
	
	public void showSymbols() {
		mSymbolsShown = true;
		mCurrentSuggestions = getSymbols();
		this.mListener.onSuggestionsChange(addSpaces(mCurrentSuggestions));
	}
	
	public List<String> getSuggestions() {
		return this.mCurrentSuggestions;
	}
	
	public boolean isEmpty() {
		if (mCurrentSuggestions == null || mCurrentSuggestions.isEmpty() == true) {
			return true;
		}else {
			return false;
		}
	}
	
	public String findByIndex(int index) {
		if (index < mCurrentSuggestions.size()) {
			return mCurrentSuggestions.get(index);
		} else {
			return "";
		}
	}
	
	public int numberOfElements() {		
		return this.mCurrentSuggestions.size();
	}
	
	public void updateCurrentSuggestions() {
		if (!mSymbolsShown) {
			String letters = SoftKeyboard.mLetters;
			int wordsHash = Arrays.hashCode(SoftKeyboard.mWords);			
			List<String> tempSuggestions = new ArrayList<String>();
			if (mSuggestionsCache.containsKey(wordsHash)) {
				tempSuggestions = mSuggestionsCache.get(wordsHash);
			}			
			List<String> sugs = new ArrayList<String>();
			int i = 0;
			if (!mSymbolsShown && tempSuggestions.size() > 0) {
				for (String s: tempSuggestions) {
					if (i >= MAX_CURRENT_SUGGESTIONS_CNT) {
						break;
					}
					if (s.startsWith(letters) && s.length() > letters.length()+1) {
						sugs.add(s);
						i++;
					}
				}
			}
			mCurrentSuggestions = sugs;
//			if (mCurrentSuggestions.size() < 10) {
//					ServerConnector.lastResultsEmpty = true;
//			}
			setUpdate();
		}
	}
	
	public void setUpdate() {
		this.mListener.onSuggestionsChange(mCurrentSuggestions);
	}
	
	public void setSymbols(String[] symbols) {
		mFrequentSymbols = symbols;
	}
	
	public List<String> getSymbols() {
		List<String> result = new ArrayList<String>();
		for (String s : mFrequentSymbols) {
			result.add(s);
		}
		return result;
	}
	
	public boolean symbolsShown() {
		return mSymbolsShown;
	}
	
	public void setSymbolsShown(boolean symbols) {
		this.mSymbolsShown = symbols;
	}
	
	public void clearCache() {
		mSuggestionsCache.clear();
		mCurrentSuggestions.clear();
		setUpdate();
	}
	
	//workaround for too narrow Symbols, replace with better
	//view structure
	public List<String> addSpaces(List<String> list) {
		List<String> result = new ArrayList<String>();
		for (String s : list) {
			result.add("   " + s + "   ");
		}
		return result;
	}
	
}
