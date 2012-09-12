/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/*Edited by lrAndroid*/
package com.typology;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.text.method.MetaKeyKeyListener;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.typology.keyboard.LatinKeyboard;
import com.typology.keyboard.LatinKeyboardBaseView.OnKeyboardActionListener;
import com.typology.keyboard.LatinKeyboardView;
import com.typology.softkeyboard.R;

/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft LatinKeyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService 
        implements OnKeyboardActionListener {
    static final boolean DEBUG = false;
    
    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on 
     * a QWERTY LatinKeyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = false; //mine: false
    
    private LatinKeyboardView mInputView;
    private CandidateView mCandidateView;
    
    private boolean mPredictionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mMetaState;
    
    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard;
    private LatinKeyboard mPhoneKeyboard;
    private LatinKeyboard mPhoneSymbolsKeyboard;
    
    private LatinKeyboard mCurKeyboard;
    
	public static String[] mWords = new String[4];
	public static String mLetters;
	public static SuggestionsList mSuggestions = new SuggestionsList();
	private String mLastSelection = "";
	
	private boolean mSpaceAutomaticallyInserted = false;
	
	private Vibrator mVibrate;
	private final int mVibrateDuration = 15;
	//= R.integer.vibrate_duration_ms;
    
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
    	mSuggestions.setSymbols(getResources().getStringArray(R.array.freq_used_symbols));
		TextHelper.setSeparatorsAndNumbers(getResources().getString(R.string.sentence_separators),
				getResources().getString(R.string.word_separators),
				getResources().getString(R.string.no_space_before_separators),
				getResources().getString(R.string.space_afterwards_separators),
				getResources().getString(R.string.shift_space_separators),
				getResources().getString(R.string.numbers));
		
		mSuggestions.setSuggestionsUpdateListener(new SuggestionsList.SuggestionsUpdateListener() {
			public void onSuggestionsChange(List<String> suggestions) {
				setSuggestions(suggestions, true, true);
			}
		});

		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		ServerConnector.setDeviceId(tm.getDeviceId());
		
		mVibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
        super.onCreate();
        
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the LatinKeyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwertz);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
        mPhoneKeyboard = new LatinKeyboard(this, R.xml.phone);
        mPhoneSymbolsKeyboard = new LatinKeyboard(this, R.xml.phone_symbols);
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input_typology, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mQwertyKeyboard);
        mInputView.setPhoneKeyboard(mPhoneKeyboard);
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }
        
        mPredictionOn = false;
        
        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType&EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case EditorInfo.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mPhoneKeyboard;
                mPredictionOn = false;
                break;
                
            case EditorInfo.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertyKeyboard;
                mPredictionOn = true;
                
                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType &  EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }
                
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS 
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }
                
                if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = isFullscreenMode();
                }
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic LatinKeyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // LatinKeyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }
        
        if (mPredictionOn) {
            analyseText();
            updateCandidates();
        }
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.inputType, attribute.imeOptions);
       
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);
        //Clear Suggestions
        mSuggestions.clearCache();
        
        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }
    
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected LatinKeyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
    }
    
    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        
        // If the current selection in the text view changes, we should
        // update whatever candidate text we have.
        if (mPredictionOn) {
        	analyseText();
        	if (mLetters.length() > 0) {
        		mSuggestions.setSymbolsShown(false);
        		mSuggestions.updateCurrentSuggestions();
        	} else {
        		mSuggestions.showSymbols();
        	}
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mPredictionOn) {
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }
            
            List<String> stringList = new ArrayList<String>();
            for (int i=0; i<(completions != null ? completions.length : 0); i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }
    
    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }
        

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }
        
        analyseText();
        if (mLetters.length() > 0) {
        	ic = getCurrentInputConnection();
        	char accent = ic.getTextBeforeCursor(1, 0).charAt(0);
        	int composed = KeyEvent.getDeadChar(accent, c);
        	
        	if (composed != 0)  {
        		c = composed;
        		ic.deleteSurroundingText(1, 0);
        	}
        }
        
        onKey(c, null);
        
        return true;
    }
    
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our LatinKeyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;
                
//              case KeyEvent.KEYCODE_DEL:
//              // Special handling of the delete key: if we currently are
//              // composing text for the user, we want to modify that instead
//              // of let the application to the delete itself.
//          		onKey(Keyboard.KEYCODE_DELETE, null);
//          		return true;
//                	break;
                
            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;
                
            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                	// For all other keys, if we want to do transformations on
                    // text being entered with a hard keyboard, we need to process
                    // it and do the appropriate action.
                    if (PROCESS_HARD_KEYS) {
                        if (mPredictionOn && translateKeyDown(keyCode, event)) {
                            return true;
                        }
                    }
                }
        }
        
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper to update the shift state of our LatinKeyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null 
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) {
    	if (!(TextHelper.isShiftSpaceSeparator((char)primaryCode)) && !(TextHelper.isSpace((char)primaryCode))) {
    		mSpaceAutomaticallyInserted = false;
    	}
    	if (TextHelper.isSeparator(primaryCode)) {
    		handleSeparator((char) primaryCode);
    	} else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (primaryCode == LatinKeyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            onChangeKeyboard();
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    public void onText(CharSequence text) {
    	InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }
    
    public void onChangeKeyboard() {    	
    	LatinKeyboard current = (LatinKeyboard) mInputView.getKeyboard();
        if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
            current = mQwertyKeyboard;
        } else if (current == mPhoneKeyboard) {
        	current = mPhoneSymbolsKeyboard;
        } else if (current == mPhoneSymbolsKeyboard) {
        	current = mPhoneKeyboard;
        } else {
            current = mSymbolsKeyboard;
        }
        mInputView.setKeyboard(current);
        if (current == mSymbolsKeyboard) {
            current.setShifted(false);
        }
//        mCurKeyboard.setImeOptions(getResources(), getCurrentInputEditorInfo().inputType,
//        		getCurrentInputEditorInfo().imeOptions);
    }

    /**
     *Updates the letters of the current word and the last four words before. 
     *Updates current suggestions from the cached suggestions and makes a server request. 
     *If the current word is empty symbols are shown instead.
     */
    private void updateCandidates() {
        if (mPredictionOn) {
        	analyseText();
        	if (mLetters.length() > 0) {
            	mSuggestions.setSymbolsShown(false);
        		mSuggestions.updateCurrentSuggestions();
        		ServerConnector.getSuggestions(mWords, mLetters, mLastSelection, mSuggestions.numberOfElements());
        		mLastSelection = "";
        	} else {
        		mSuggestions.showSymbols();
        		ServerConnector.lastResultsEmpty = false;
        	}
        }
    }
    
    private void analyseText() {    	
    	if (getCurrentInputConnection() != null) {
        	CharSequence text = getCurrentInputConnection().getTextBeforeCursor(100, 0);
        	String[] result = TextHelper.processText(text);
        	System.arraycopy(result, 0, mWords, 0, mWords.length);
        	mLetters = result[4];
    	}
    }
    
    public void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid) {
		
    	if (mInputView != null && mInputView.isShown() && mPredictionOn) {
    		if (suggestions != null && suggestions.size() > 0) {
                setCandidatesViewShown(true);
            } else if (isExtractViewShown()) {
                setCandidatesViewShown(true);
            }
            if (mCandidateView != null) {
                mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
            }
    	}else {
    		setCandidatesViewShown(false);
    	}
    }
    
    private void handleBackspace() {
    	if (getCurrentInputConnection() == null) {
    		return;
    	}
    	InputConnection ic = getCurrentInputConnection();
    	final int length = ic.getTextBeforeCursor(1, 0).length();
    	if (length > 0) {
    		ic.deleteSurroundingText(1, 0);
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }

    	if (mPredictionOn) {
    		updateCandidates();
    		ServerConnector.lastResultsEmpty = false;
    	}
        updateShiftKeyState(getCurrentInputEditorInfo());

    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }
        
        LatinKeyboard currentKeyboard = (LatinKeyboard) mInputView.getKeyboard();
        if (currentKeyboard == mQwertyKeyboard) {
        	if (mInputView.isShifted() && mCapsLock) {
        		mInputView.setShifted(false);
        		mCapsLock = false;
        	} else if (mInputView.isShifted()) {
        		mInputView.setShifted(true);
        		mCapsLock = true;
        	} else {
        		mInputView.setShifted(true);
        	}
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }
    
    private void handleCharacter(int primaryCode, int[] keyCodes) {
    	if (getCurrentInputConnection() == null) {
    		return;
    	}
    	if (isInputViewShown()) {
    		if (mInputView.isShifted()) {
    			primaryCode = Character.toUpperCase(primaryCode);
    		}
    	}
    	getCurrentInputConnection().commitText(String.valueOf((char) primaryCode), 1);
    	if (mPredictionOn) {
    		updateCandidates();
    	}
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleClose() {
        requestHideSelf(0);
        mInputView.closing();
    }
    
    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }
    
    public void pickSuggestionManually(int index) {
    	if (getCurrentInputConnection() == null) {
    		return;
    	}
        if (mSuggestions.numberOfElements() >= index) {
        	ServerConnector.lastResultsEmpty = false;
        	
//        	String poststring = getPoststring();
        	String poststring = " ";
        	String selectedSuggestion = mSuggestions.findByIndex(index);
        	mLastSelection = selectedSuggestion;
        	if (mSuggestions.symbolsShown()) {
        		handleSeparator(selectedSuggestion.charAt(0));
        	}else {
            	mSpaceAutomaticallyInserted = true;
        		getCurrentInputConnection().deleteSurroundingText(mLetters.length(), 0);
        		//Todo: use handleCharacter instead
        		getCurrentInputConnection().commitText(selectedSuggestion + poststring, 1);
        	}
        	mVibrate.vibrate(mVibrateDuration);
        	if (mCandidateView != null) {
        		mCandidateView.clear();
        	}
        	mSuggestions.showSymbols();
        	removeFollowing();
        	updateShiftKeyState(getCurrentInputEditorInfo());
        }
    }
    
    private void removeFollowing() {
    	if (getPoststring().contains(" ")) {
    		CharSequence text = getCurrentInputConnection().getTextAfterCursor(20, 0);
    		final int length = text.length();
    		if (length > 0) {
    			int remove = 0;
        		while (!TextHelper.isSeparator(text.charAt(remove)) && remove + 1 < length) {
        			remove++;
        		}
        		getCurrentInputConnection().deleteSurroundingText(0, remove + 1);
    		}
    	}
    }
    
    //really necessary? leads to problems when there is a space after the cursor
    public String getPoststring() {
    	String poststring = "";
    	char nextCharacter = '\0';
    	CharSequence followingText = getCurrentInputConnection().getTextAfterCursor(1, 0);
    	if (followingText.length() > 0) {
    		nextCharacter = followingText.charAt(0);
    	}
    	if (!TextHelper.isSeparator(nextCharacter)) {
    		poststring = " ";
    	}
    	return poststring;
    	
    }
    
    private void handleSeparator(char separator) {
    	mLastSelection = "";
		ServerConnector.lastResultsEmpty = false;
		
    	if (getCurrentInputConnection() == null) {
    		return;
    	}
    	
    	int code = separator;
    	CharSequence lastLetter = getCurrentInputConnection().getTextBeforeCursor(1, 0);
    	char lastCharacter = '\0';
    	if (lastLetter.length() > 0) {
    		lastCharacter = lastLetter.charAt(0);
    	}
    	
    	if (mPredictionOn) {
    		if (mSpaceAutomaticallyInserted) {
    			if (TextHelper.isSpace(separator)) {
    				mSpaceAutomaticallyInserted = false;
    			} else if (TextHelper.isShiftSpaceSeparator(separator)) {
            		if (TextHelper.isSpace(lastCharacter)) {
            			getCurrentInputConnection().deleteSurroundingText(1, 0);    				
            		}
            		sendKey(code);
            		if (TextHelper.isPostspaceSeparator(String.valueOf(separator))) {
            			getCurrentInputConnection().commitText(" ", 1);
            		}
    			} else {
    				sendKey(code);
    			}
    		} else {
    			sendKey(code);
    			if (TextHelper.isAlphabet(lastCharacter)) {
    				if (TextHelper.isSpace(separator)) {
        				mSpaceAutomaticallyInserted = true;
        			} else if (TextHelper.isPostspaceSeparator(String.valueOf(separator))) {
            			getCurrentInputConnection().commitText(" ", 1);
            			mSpaceAutomaticallyInserted = true;
            		} 
    			}
    		}
    		mSuggestions.showSymbols();
    	} else {
    		sendKey(code);
    	}
    	updateShiftKeyState(getCurrentInputEditorInfo());
    }
    
    public void swipeRight() {
        if (mPredictionOn) {
            pickDefaultCandidate();
        }
    }
    
    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    	handleShift();
    }
    
    public void onPress(int primaryCode) {
    	if (mInputView.mMiniKeyboard == null) {
    		mVibrate.vibrate(mVibrateDuration);
    	}
    }
    
    public void onRelease(int primaryCode) {
    }

	@Override
	public void onKey(int primaryCode, int[] keyCodes, int x, int y) {
		onKey(primaryCode, keyCodes);
	}

	@Override
	public void onCancel() {
	}
}
