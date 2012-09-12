/**
 * Test for the TextHelper class
 * 
 * @author Till Speicher
 */

package com.typology.test;

import static org.junit.Assert.assertArrayEquals;
import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

import com.typology.TextHelper;
import com.typology.softkeyboard.R;

public class TextHelperTest extends AndroidTestCase {
	
	public void setSeparatorsAndNumbers() {
		Context context = getContext();
		TextHelper.setSeparatorsAndNumbers(context.getResources().getString(R.string.sentence_separators),
				context.getResources().getString(R.string.word_separators),
				context.getResources().getString(R.string.no_space_before_separators),
				context.getResources().getString(R.string.space_afterwards_separators),
				context.getResources().getString(R.string.shift_space_separators),
				context.getResources().getString(R.string.numbers));
	}
	
	public void testSetSeparatorsAndNumbers() {
		Context context = getContext();
		setSeparatorsAndNumbers();
		assertEquals(TextHelper.sentenceSeparators,
				context.getResources().getString(R.string.sentence_separators));
		assertEquals(TextHelper.wordSeparators,
				context.getResources().getString(R.string.word_separators));
		assertEquals(TextHelper.noPrespaceSeparators,
				context.getResources().getString(R.string.no_space_before_separators));
		assertEquals(TextHelper.postspaceSeparators,
				context.getResources().getString(R.string.space_afterwards_separators));
		assertEquals(TextHelper.shiftSpaceSeparators,
				context.getResources().getString(R.string.shift_space_separators));
		assertEquals(TextHelper.numbers,
				context.getResources().getString(R.string.numbers));
	}
	
	public void testGetCurrentSentence() {
		setSeparatorsAndNumbers();
		
		String input = "";
		String expectedOutput = "";
		String output = TextHelper.getCurrentSentence(input);
		assertEquals(expectedOutput, output);
		
		input = "word";
		expectedOutput = "word";
		output = TextHelper.getCurrentSentence(input);
		assertEquals(expectedOutput, output);
		
		input = "sentence one.";
		expectedOutput = "";
		output = TextHelper.getCurrentSentence(input);
		assertEquals(expectedOutput, output);
		
		//sentences should be divided by sentence separators
		input = "Sentence. one ";
		expectedOutput = " one ";
		output = TextHelper.getCurrentSentence(input);
		assertEquals(expectedOutput, output);
		input = " Sentence ;two ";
		expectedOutput = "two ";
		output = TextHelper.getCurrentSentence(input);
		assertEquals(expectedOutput, output);
		input = "Sentence! three -";
		expectedOutput = " three -";
		output = TextHelper.getCurrentSentence(input);
		assertEquals(expectedOutput, output);
		input = "Sentence ? four ";
		expectedOutput = " four ";
		output = TextHelper.getCurrentSentence(input);
		assertEquals(expectedOutput, output);
		
		//dots in numbers should not divide the sentence
		input = "Numbers. 50.2 1,000.50 dot";
		expectedOutput = " 50.2 1,000.50 dot";
		output = TextHelper.getCurrentSentence(input);
		assertEquals(expectedOutput, output);
		input = "numbers? 7!8 50.2;3 1,020.4 ";
		expectedOutput = "3 1,020.4 ";
		output = TextHelper.getCurrentSentence(input);
		assertEquals(expectedOutput, output);
		input = "5. 3.1";
		expectedOutput = " 3.1";
		output = TextHelper.getCurrentSentence(input);
		assertEquals(expectedOutput, output);
		input = " .5 3.1 ";
		expectedOutput = "5 3.1 ";
		output = TextHelper.getCurrentSentence(input);
		assertEquals(expectedOutput, output);
	}
	
	public void testGetCurrentWords() {
		setSeparatorsAndNumbers();
		
		String input = "";
		String[] expectedOutput = {""};
		String[] output = TextHelper.getCurrentWords(input);
		assertArrayEquals(expectedOutput, output);
		
		//the following symbols should be processed as word separators
		input = " Here,: \t\n(are)[lots]*&@{of}/\\<>_+=|\"-symbols ";
		expectedOutput = new String[]{"","Here","","","","","","are","","lots","","","","","of",
				"","","","","","","","","","","symbols",""};
		output = TextHelper.getCurrentWords(input);
		assertArrayEquals(expectedOutput, output);
		
		//"," should be ignored in numbers
		input = ",2 just 1.0, large number: 1,000,000.0";
		expectedOutput = new String[]{"","2","just","1.0","","large","number","","1,000,000.0"};
		output = TextHelper.getCurrentWords(input);
		logOutput(output);
		assertArrayEquals(expectedOutput, output);
	}
	
	public void testGetCurrentLetters() {
		setSeparatorsAndNumbers();
		
		String input_sentence = "";
		String[] input_words = {""};
		String expectedOutput = "";
		String output = TextHelper.getCurrentLetters(input_sentence, input_words);
		assertEquals(expectedOutput, output);
		
		//a word separator should return an empty result
		input_sentence = "The current sentence ";
		input_words = new String[]{"The","current","sentence"};
		expectedOutput = "";
		output = TextHelper.getCurrentLetters(input_sentence, input_words);
		assertEquals(expectedOutput, output);
		
		//symbols different from word separators should be returned
		input_sentence = "no separator afterwards";
		input_words = new String[]{"no","separator","afterwards"};
		expectedOutput = "afterwards";
		output = TextHelper.getCurrentLetters(input_sentence, input_words);
		assertEquals(expectedOutput, output);
		input_sentence = "1,200.1";
		input_words = new String[]{"1,200.1"};
		expectedOutput = "1,200.1";
		output = TextHelper.getCurrentLetters(input_sentence, input_words);
		assertEquals(expectedOutput, output);
	}
	
	public void testGetFirstRelevantWord() {
		String[] input = {"","","","",""};
		int expectedOutput = -1;
		int output = TextHelper.getFirstRelevantWord(input);
		assertEquals(expectedOutput, output);
		
		input = new String[]{"small","sentence"};
		expectedOutput = 0;
		output = TextHelper.getFirstRelevantWord(input);
		assertEquals(expectedOutput, output);
		
		//the last word should be ignored because it is handled by a different method
		input = new String[]{"Look","at","these","5","","real","","words"};
		expectedOutput = 1;
		output = TextHelper.getFirstRelevantWord(input);
		assertEquals(expectedOutput, output);
		
		//should not take into account more than four real words
		input = new String[]{"Look","at","these","5","","","real","","words","",""};
		expectedOutput = 2;
		output = TextHelper.getFirstRelevantWord(input);
		assertEquals(expectedOutput, output);
	}
	
	public void testGetRelevantWords() {
		int input_pos = -1;
		String[] input_words = {""};
		String[] expectedOutput = {"","","",""};
		String[] output = TextHelper.getRelevantWords(input_pos, input_words);
		assertArrayEquals(expectedOutput, output);
		
		//skips the last word
		input_pos = 0;
		input_words = new String[]{"Some","text",};
		expectedOutput = new String[]{"Some","","",""};
		output = TextHelper.getRelevantWords(input_pos, input_words);
		assertArrayEquals(expectedOutput, output);
		
		//processes only up to four words before the last word
		input_pos = 1;
		input_words = new String[]{"This","is","a","short","simple","sentence"};
		expectedOutput = new String[]{"is","a","short","simple"};
		output = TextHelper.getRelevantWords(input_pos, input_words);
		assertArrayEquals(expectedOutput, output);
		
		//Skips empty words
		input_pos = 1;
		input_words = new String[]{"","A","sentence","","","containing","","holes","",""};
		expectedOutput = new String[]{"A","sentence","containing","holes"};
		output = TextHelper.getRelevantWords(input_pos, input_words);
		assertArrayEquals(expectedOutput, output);
	}
	
	public void testProcessText() {
		setSeparatorsAndNumbers();
		
		//empty input should lead to empty output
		CharSequence input = "";
		String[] expectedOutput = {"","","","",""};
		String[] output = TextHelper.processText(input);
		assertArrayEquals(expectedOutput, output);

		//inputs larger than five words should be cut off
		input = "This text consists of six words";
		expectedOutput = new String[]{"text", "consists", "of", "six", "words"};
		output = TextHelper.processText(input);
		assertArrayEquals(expectedOutput, output);
		
		//multiple word separators should be ignored
		input = ", Ignore : multiple, _ separators +";
		expectedOutput = new String[]{"Ignore","multiple","separators","",""};
		output = TextHelper.processText(input);
		assertArrayEquals(expectedOutput, output);
		
		input = "One sentence. And another one";
		expectedOutput = new String[]{"And","another","","","one"};
		output = TextHelper.processText(input);
		assertArrayEquals(expectedOutput, output);
	}
	
	public void testIsSeparator() {
		setSeparatorsAndNumbers();
		String separators = getContext().getResources().getString(R.string.sentence_separators)
				+ getContext().getResources().getString(R.string.word_separators);
		
		char input;
		boolean output;
		for (int i = 0; i < separators.length(); i++) {
			input = separators.charAt(i);
			output = TextHelper.isSeparator(input);
			assertTrue(output);
		}
		
		input = 'a';
		output = TextHelper.isSeparator(input);
		assertFalse(output);
		input = '5';
		output = TextHelper.isSeparator(input);
		assertFalse(output);
	}
}
