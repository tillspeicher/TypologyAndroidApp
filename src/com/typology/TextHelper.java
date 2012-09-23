/**
 * Helper class for text processing and text-related tasks
 * 
 * @author Till Speicher
 */

package com.typology;


public class TextHelper {
	
	public static String sentenceSeparators;
	public static String wordSeparators;
	public static String noPrespaceSeparators;
	public static String postspaceSeparators;
	public static String shiftSpaceSeparators;
	public static String numbers;
	
	public static String[] mWords = new String[4];
	public static String mLetters;
	
	public static void setSeparatorsAndNumbers(String sentenceSep, String wordSep, String noSpace, String postSpace, String shiftSpace, String num) {
		sentenceSeparators = sentenceSep;
		wordSeparators = wordSep;
		noPrespaceSeparators = noSpace;
		postspaceSeparators = postSpace;
		shiftSpaceSeparators = shiftSpace;
		numbers = num;
	}
	
	/**
	 * Processes the text currently entered with the Keyboard so that it can be used to determine word suggestions.
	 * "I am eating an ice cream" --> {"am","eating","an",ice","cream"}
	 * 
	 * @param text - the text currently entered
	 * @return an array, the last four words are on pos 0-3, the currently typed word is on pos 4
	 */
	public static String[] processText(CharSequence text) {
		mWords = new String[]{"","","",""};
		mLetters = "";
		
		if (text != null) {
			String currentSentence = getCurrentSentence(text);
			int characterAmount = currentSentence.length();

			if (characterAmount > 0) {
				String[] currentWords = getCurrentWords(currentSentence);
				int wordAmount = currentWords.length;
				mLetters = getCurrentLetters(currentSentence, currentWords);
				wordAmount -= 1;

				if (wordAmount > 0) {
					int firstRelevantWord = getFirstRelevantWord(currentWords);
					mWords = getRelevantWords(firstRelevantWord, currentWords);
				}
			}
		}
		String[] result = new String[5];
		System.arraycopy(mWords, 0, result, 0, mWords.length);
		result[4] = mLetters;
		return result;
    }
	
	/**
	 * Determines the sentence the user is currently typing
	 * 
	 * @param text	- raw String from the editor
	 * @return the current sentence the user is typing
	 */
	public static String getCurrentSentence(CharSequence text) {
		int textLength = text.length();
		String currentSentence = "";
		if (text != null && textLength > 0) {
			//splits at sentence separators and dots which are not inside numbers
			String[] sentences = text.toString().split("(?<![0-9])\\.|\\.(?![0-9])|[" + sentenceSeparators + "]",-1);
			
			CharSequence lastCharacter = text.subSequence(textLength - 1, textLength);
			if (sentences.length > 0 && !sentenceSeparators.contains(lastCharacter)) {
				currentSentence = sentences[sentences.length - 1];
			}
		}
		return currentSentence;
	}
	
	/**
	 * Splits the current sentence into the words it consists of
	 * 
	 * @param currentSentence - the currently typed sentence
	 * @return the words in the current sentence
	 */
	public static String[] getCurrentWords(String currentSentence) {
		//splits at word separators and commas which are not inside numbers
		String [] currentWords = currentSentence.split("(?<![0-9]),|,(?![0-9])|[" + wordSeparators + "]",-1);
		return currentWords;
	}
	
	/**
	 * Determines the letters of the word the user is currently typing
	 * 
	 * @param curSentence - the currently typed sentence
	 * @param curWords - the words of the current sentence
	 * @return the letters of the currently typed word
	 */
	public static String getCurrentLetters(String curSentence, String[] curWords) {
		String letters = "";
		if (curSentence.length() > 0 && curWords.length > 0) {
			CharSequence lastCharacter = "" +	curSentence.charAt(curSentence.length()-1);
			if (!wordSeparators.contains(lastCharacter)) {
				letters = curWords[curWords.length-1];
			}
		}
		return letters;
	}
	
	/**
	 * Determines the position of the first not-empty word.
	 * Search does not go back more than 4 words:
	 * 5 relevant words --> position of the 2. one is returned
	 * 
	 * @param words - the words of the current sentence
	 * @return the position of the first relevant (non-empty) word
	 */
	public static int getFirstRelevantWord(String[] words) {
		int firstRelevantWord = -1;
		int relevantWords = 0;
		
		//-2 because the last word was already handled before by getCurrentLetters()
		for (int i = words.length-2; i >= 0; i--) {
			if (words[i].equals("") == false) {
				relevantWords++;
				firstRelevantWord = i;
			}
			if (relevantWords >= 4) {
				break;
			}
		}
		return firstRelevantWord;
	}
	
	/**
	 * Determines the last four relevant (non-empty) words (excluding the current word) of the sentence.
	 * {"A","sentence","which","","has","five","","words"} --> {"sentence","which","has","five"}
	 * 
	 * @param firstRelevantWord - the array index of the first relevant word
	 * @param words - the words of the current sentence
	 * @return the last four (or less) relevant words of the sentence
	 */
	public static String[] getRelevantWords(int firstRelevantWord, String[] words) {
		String[] relevantWords = {"","","",""};
		if (firstRelevantWord != -1 && words.length > 1) {
			int c = 0;
			for (int i = firstRelevantWord; i < words.length-1; i++) {
				if (!words[i].equals("")) {
					relevantWords[c] = words[i];
					c++;
				}
				if (c >= 4) {
					break;
				}
			}
		}
		return relevantWords;
	}
	
	public static String toString(String[] str) {
		String result = "";
		for (String s : str) {
			result += s + ", ";
		}
		return result;
	}
	
	public static String getSeparators() {
		return wordSeparators + sentenceSeparators + ".,";
	}
	
    public static boolean isSeparator(int code) {
        String separators = getSeparators();
        return separators.contains(String.valueOf((char)code));
    }
	
	public static boolean isShiftSpaceSeparator(char character) {
		return shiftSpaceSeparators.contains(String.valueOf(character));
	}
    
    public static boolean isNumber(int code) {
    	return numbers.contains(String.valueOf((char)code));
    }
    
    public static boolean isNoPrespaceSeparator(String character) {
    	return noPrespaceSeparators.contains(character);
    }
    
    public static boolean isPostspaceSeparator(String character) {
    	return postspaceSeparators.contains(character);
    }
    
    public static boolean isSpace(char character) {
    	return (character == ' ');
    }
    
    /**
     * Helper to determine if a given character code is alphabetic.
     */
    public static boolean isAlphabet(char character) {
        return (Character.isLetter(character));
    }
	
}
