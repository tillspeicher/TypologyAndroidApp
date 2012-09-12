/*
 * Copyright (C) 2008 The Android Open Source Project
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
package com.typology.keyboard;

import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.text.TextPaint;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;

import com.typology.softkeyboard.R;

//@SuppressLint({ "ResourceAsColor", "ResourceAsColor", "ResourceAsColor" })
public class LatinKeyboard extends Keyboard {

	public static final boolean DEBUG_PREFERRED_LETTER = false;
	public static final String TAG = "LatinKeyboard";
	public static final int OPACITY_FULLY_OPAQUE = 255;
	public static final int SPACE_LED_LENGTH_PERCENT = 80;
	public static final int KEYCODE_ENTER = '\n';
	public static final int KEYCODE_SPACE = ' ';
	public static final int KEYCODE_MODE_CHANGE = -503;
	static final int KEYCODE_PERIOD = '.';
	public Drawable mShiftOnIcon;
	public Drawable mShiftOffIcon;
	public Drawable mSpaceIcon;
	public Drawable mSpaceAutoCompletionIndicator;
	public Drawable mSpacePreviewIcon;
	public Drawable mButtonArrowLeftIcon;
	public Drawable mButtonArrowRightIcon;
	public Key mShiftKey;
	public Key mEnterKey;
	public Key mF1Key;
	public Drawable mHintIcon;
	public Key mSpaceKey;
	public Key m123Key;
	public final int NUMBER_HINT_COUNT = 10;
	public Key[] mNumberHintKeys;
	public Drawable[] mNumberHintIcons = new Drawable[NUMBER_HINT_COUNT];
	public int mSpaceKeyIndex = -1;
	public int mSpaceDragStartX;
	public int mSpaceDragLastDiff;
	public Locale mLocale;
	// public LanguageSwitcher mLanguageSwitcher;
	public Resources mRes;
	public Context mContext;
	protected int mMode;
	// Whether this keyboard has voice icon on it
	public boolean mHasVoiceButton;
	// Whether voice icon is enabled at all
	public boolean mVoiceEnabled;
	public boolean mIsAlphaKeyboard;
	public CharSequence m123Label;
	public boolean mCurrentlyInSpace;
	public SlidingLocaleDrawable mSlidingLocaleIcon;
	public int[] mPrefLetterFrequencies;
	public int mPrefLetter;
	public int mPrefLetterX;
	public int mPrefLetterY;
	public int mPrefDistance;

	// TODO: generalize for any keyboardId
	public boolean mIsBlackSym;

	// TODO: remove this attribute when either Keyboard.mDefaultVerticalGap or
	// Key.parent becomes
	// non-public.
	public int mVerticalGap;

	public static final int SHIFT_OFF = 0;
	public static final int SHIFT_ON = 1;
	public static final int SHIFT_LOCKED = 2;

	public int mShiftState = SHIFT_OFF;

	public static final float SPACEBAR_DRAG_THRESHOLD = 0.8f;
	public static final float OVERLAP_PERCENTAGE_LOW_PROB = 0.70f;
	public static final float OVERLAP_PERCENTAGE_HIGH_PROB = 0.85f;
	// Minimum width of space key preview (proportional to keyboard width)
	public static final float SPACEBAR_POPUP_MIN_RATIO = 0.4f;
	// Height in space key the language name will be drawn. (proportional to
	// space key height)
	public static final float SPACEBAR_LANGUAGE_BASELINE = 0.6f;
	// If the full language name needs to be smaller than this value to be drawn
	// on space key,
	// its short language name will be used instead.
	public static final float MINIMUM_SCALE_OF_LANGUAGE_NAME = 0.8f;

	public static int sSpacebarVerticalCorrection;

	public LatinKeyboard(Context context, int xmlLayoutResId) {
		this(context, xmlLayoutResId, 0);
	}

	public LatinKeyboard(Context context, int xmlLayoutResId, int mode) {
		super(context, xmlLayoutResId, mode);
		final Resources res = context.getResources();
		mContext = context;
		mMode = mode;
		mRes = res;
		mShiftOnIcon = res.getDrawable(R.drawable.sym_keyboard_shift_on);
		mShiftOffIcon = res.getDrawable(R.drawable.sym_keyboard_shift_off);
		setDefaultBounds(mShiftOnIcon);
		mSpaceIcon = null;
		mSpaceAutoCompletionIndicator = null;
		mSpacePreviewIcon = res
				.getDrawable(R.drawable.sym_keyboard_feedback_space);
		sSpacebarVerticalCorrection = res
				.getDimensionPixelOffset(R.dimen.spacebar_vertical_correction);
		mSpaceKeyIndex = indexOf(KEYCODE_SPACE);
		// TODO remove this initialization after cleanup
		mVerticalGap = super.getVerticalGap();
		final int size = this.getKeys().size();
		mKeyIndex = new int[size];
		for (int i = 0; i < size; i++) {
			mKeyIndex[i] = i;
		}
	}

	int[] mKeyIndex;

	@Override
	protected Key createKeyFromXml(Resources res, Row parent, int x, int y,
			XmlResourceParser parser) {
		Key key = new LatinKey(res, parent, x, y, parser);
		switch (key.codes[0]) {
		case KEYCODE_ENTER:
			mEnterKey = key;
			break;
		case LatinKeyboardView.KEYCODE_F1:
			mF1Key = key;
			break;
		case KEYCODE_SPACE:
			mSpaceKey = key;
			break;
		case KEYCODE_MODE_CHANGE:
			m123Key = key;
			m123Label = key.label;
			break;
		case KEYCODE_SHIFT:
			mShiftKey = key;
		}

		// For number hints on the upper-right corner of key
		if (mNumberHintKeys == null) {
			// NOTE: This protected method is being called from the base class
			// constructor before
			// mNumberHintKeys gets initialized.
			mNumberHintKeys = new Key[NUMBER_HINT_COUNT];
		}
		int hintNumber = -1;
		if (LatinKeyboardBaseView.isNumberAtLeftmostPopupChar(key)) {
			hintNumber = key.popupCharacters.charAt(0) - '0';
		} else if (LatinKeyboardBaseView.isNumberAtRightmostPopupChar(key)) {
			hintNumber = key.popupCharacters.charAt(key.popupCharacters
					.length() - 1) - '0';
		}
		if (hintNumber >= 0 && hintNumber <= 9) {
			mNumberHintKeys[hintNumber] = key;
		}

		return key;
	}

	public void setImeOptions(Resources res, int mode, int options) {
		mMode = mode;
		// TODO should clean up this method
		if (mEnterKey != null) {
			// Reset some of the rarely used attributes.
			mEnterKey.popupCharacters = null;
			mEnterKey.popupResId = 0;
			mEnterKey.text = null;
			switch (options
					& (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
			case EditorInfo.IME_ACTION_GO:
				mEnterKey.iconPreview = null;
				mEnterKey.icon = null;
				mEnterKey.label = res.getText(R.string.label_go_key);
				break;
			case EditorInfo.IME_ACTION_NEXT:
				mEnterKey.iconPreview = null;
				mEnterKey.icon = null;
				mEnterKey.label = res.getText(R.string.label_next_key);
				break;
			case EditorInfo.IME_ACTION_DONE:
				mEnterKey.iconPreview = null;
				mEnterKey.icon = null;
				mEnterKey.label = res.getText(R.string.label_done_key);
				break;
			case EditorInfo.IME_ACTION_SEARCH:
				mEnterKey.iconPreview = res
						.getDrawable(R.drawable.sym_keyboard_feedback_search);
				mEnterKey.icon = res
						.getDrawable(R.drawable.sym_keyboard_search);
				mEnterKey.label = null;
				break;
			case EditorInfo.IME_ACTION_SEND:
				mEnterKey.iconPreview = null;
				mEnterKey.icon = null;
				mEnterKey.label = res.getText(R.string.label_send_key);
				mEnterKey.popupResId = R.xml.popup_smileys;
				break;
			default:
				mEnterKey.iconPreview = res
						.getDrawable(R.drawable.sym_keyboard_feedback_return);
				mEnterKey.icon = res
						.getDrawable( R.drawable.sym_keyboard_return);
				mEnterKey.label = null;
				mEnterKey.popupResId = R.xml.popup_smileys;
				break;
			}
			// Set the initial size of the preview icon
			if (mEnterKey.iconPreview != null) {
				setDefaultBounds(mEnterKey.iconPreview);
			}
		}
	}

	void enableShiftLock() {
		int index = getShiftKeyIndex();
		if (index >= 0) {
			mShiftKey = getKeys().get(index);
			if (mShiftKey instanceof LatinKey) {
				((LatinKey) mShiftKey).enableShiftLock();
			}
		}
	}

	/*
	 * public void setShiftLocked(boolean shiftLocked) { if (mShiftKey != null)
	 * { if (shiftLocked) { mShiftKey.on = false; mShiftKey.icon =
	 * mShiftLockIcon; mShiftState = SHIFT_OFF; } else { mShiftKey.on = true;
	 * mShiftKey.icon = mShiftLockIcon; mShiftState = SHIFT_LOCKED; } } }
	 */

	public boolean isShiftLocked() {
		return mShiftState == SHIFT_LOCKED;
	}

	@Override
	public boolean setShifted(boolean shiftState) {
		boolean shiftChanged = false;
		if (mShiftKey != null) {
			if (shiftState == false) {
				shiftChanged = mShiftState != SHIFT_OFF;
				mShiftState = SHIFT_OFF;
				mShiftKey.on = false;
				 mShiftKey.icon = mShiftOffIcon;
			} else {
				if (mShiftState == SHIFT_OFF) {
					shiftChanged = mShiftState == SHIFT_OFF;
					mShiftState = SHIFT_ON;
					mShiftKey.on = false;
					 mShiftKey.icon = mShiftOnIcon;
				} else {
					shiftChanged = mShiftState == SHIFT_ON;
					mShiftState = SHIFT_LOCKED;
					mShiftKey.on = true;
					 mShiftKey.icon = mShiftOnIcon;

				}
			}
		} // else {

		// return shiftChanged;
		// }
		if (shiftChanged){
			final List<Key> mKeys = getKeys();
			final int runs = mKeys.size();
			final boolean shift = mShiftState == SHIFT_ON || mShiftState == SHIFT_LOCKED;
			for (int i=0; i<runs; i++){
				final CharSequence label = mKeys.get(i).label;
				if (label!=null && label.length()==1 && Character.isLetter(label.charAt(0))){
					if (shift)
						mKeys.get(i).label = label.toString().toUpperCase();
					else
						mKeys.get(i).label = label.toString().toLowerCase();
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean isShifted() {
		if (mShiftKey != null) {
			return mShiftState != SHIFT_OFF;
		}
		return super.isShifted();

	}

	/* package */boolean isAlphaKeyboard() {
		return mIsAlphaKeyboard;
	}

	public void setColorOfSymbolIcons(boolean isBlack) {
		mIsBlackSym = isBlack;
		mShiftOnIcon = mRes
				.getDrawable(R.drawable.sym_keyboard_shift_on);
		updateDynamicKeys();
		// if (mSpaceKey != null) {
		// updateSpaceBarForLocale(isAutoCompletion, isBlack);
		// }
		updateNumberHintKeys();
	}

	public void setDefaultBounds(Drawable drawable) {
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight());
	}

	public void setVoiceMode(boolean hasVoiceButton, boolean hasVoice) {
		mHasVoiceButton = hasVoiceButton;
		mVoiceEnabled = hasVoice;
		updateDynamicKeys();
	}

	public void updateDynamicKeys() {
		update123Key();
		// updateF1Key();
	}

	public void update123Key() {
		// Update KEYCODE_MODE_CHANGE key only on alphabet mode, not on symbol
		// mode.
		if (m123Key != null && mIsAlphaKeyboard) {
			m123Key.icon = null;
			m123Key.iconPreview = null;
			m123Key.label = m123Label;
		}
	}

	/*
	 * public void updateF1Key() { // Update KEYCODE_F1 key. Please note that
	 * some keyboard layouts have no // F1 key. if (mF1Key == null) return;
	 * 
	 * if (mIsAlphaKeyboard) { if (mMode == KeyboardSwitcher.MODE_URL) {
	 * setNonMicF1Key(mF1Key, "/", R.xml.popup_slash); } else if (mMode ==
	 * KeyboardSwitcher.MODE_EMAIL) { setNonMicF1Key(mF1Key, "@",
	 * R.xml.popup_at); } else { if (mVoiceEnabled && mHasVoiceButton) {
	 * setMicF1Key(mF1Key); } else { setNonMicF1Key(mF1Key, ",",
	 * R.xml.popup_comma); } } } else { // Symbols keyboard if (mVoiceEnabled &&
	 * mHasVoiceButton) { setMicF1Key(mF1Key); } else { setNonMicF1Key(mF1Key,
	 * ",", R.xml.popup_comma); } } }
	 * 
	 * public void setMicF1Key(Key key) { // HACK: draw mMicIcon and mHintIcon
	 * at the same time final Drawable micWithSettingsHintDrawable = new
	 * BitmapDrawable(mRes, drawSynthesizedSettingsHintImage(key.width,
	 * key.height, mMicIcon, mHintIcon));
	 * 
	 * key.label = null; key.codes = new int[] { LatinKeyboardView.KEYCODE_VOICE
	 * }; key.popupResId = R.xml.popup_mic; key.icon =
	 * micWithSettingsHintDrawable; key.iconPreview = mMicPreviewIcon; }
	 * 
	 * public void setNonMicF1Key(Key key, String label, int popupResId) {
	 * key.label = label; key.codes = new int[] { label.charAt(0) };
	 * key.popupResId = popupResId; key.icon = mHintIcon; key.iconPreview =
	 * null; }
	 */
	public boolean isF1Key(Key key) {
		return key == mF1Key;
	}

	public static boolean hasPuncOrSmileysPopup(Key key) {
//		return key.popupResId == R.xml.popup_punctuation
//				|| key.popupResId == R.xml.popup_smileys;
		return false;
	}

	/**
	 * @return a key which should be invalidated.
	 */
	public Key onAutoCompletionStateChanged() {
		// updateSpaceBarForLocale(isAutoCompletion, mIsBlackSym);
		return mSpaceKey;
	}

	public void updateNumberHintKeys() {
		for (int i = 0; i < mNumberHintKeys.length; ++i) {
			if (mNumberHintKeys[i] != null) {
				mNumberHintKeys[i].icon = mNumberHintIcons[i];
			}
		}
	}

	public boolean isLanguageSwitchEnabled() {
		return mLocale != null;
	}
	
	public static boolean isRepeatableKey(int code) {
    	if (code == KEYCODE_DELETE || code == KEYCODE_SPACE) {
    		return true;
    	} else {
    		return false;
    	}
    }

	// Compute width of text with specified text size using paint.
	public static int getTextWidth(Paint paint, String text, float textSize,
			Rect bounds) {
		paint.setTextSize(textSize);
		paint.getTextBounds(text, 0, text.length(), bounds);
		return bounds.width();
	}

	// Overlay two images: mainIcon and hintIcon.
	public Bitmap drawSynthesizedSettingsHintImage(int width, int height,
			Drawable mainIcon, Drawable hintIcon) {
		if (mainIcon == null || hintIcon == null)
			return null;
		Rect hintIconPadding = new Rect(0, 0, 0, 0);
		hintIcon.getPadding(hintIconPadding);
		final Bitmap buffer = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(buffer);
		canvas.drawColor(mRes.getColor(R.color.latinkeyboard_transparent),
				PorterDuff.Mode.CLEAR);

		// Draw main icon at the center of the key visual
		// Assuming the hintIcon shares the same padding with the key's
		// background drawable
		final int drawableX = (width + hintIconPadding.left
				- hintIconPadding.right - mainIcon.getIntrinsicWidth()) / 2;
		final int drawableY = (height + hintIconPadding.top
				- hintIconPadding.bottom - mainIcon.getIntrinsicHeight()) / 2;
		setDefaultBounds(mainIcon);
		canvas.translate(drawableX, drawableY);
		mainIcon.draw(canvas);
		canvas.translate(-drawableX, -drawableY);

		// Draw hint icon fully in the key
		hintIcon.setBounds(0, 0, width, height);
		hintIcon.draw(canvas);
		return buffer;
	}

	void keyReleased() {
		mCurrentlyInSpace = false;
		mSpaceDragLastDiff = 0;
		mPrefLetter = 0;
		mPrefLetterX = 0;
		mPrefLetterY = 0;
		mPrefDistance = Integer.MAX_VALUE;
		/*
		 * if (mSpaceKey != null) { updateLocaleDrag(Integer.MAX_VALUE); }
		 */
	}

	/**
	 * Does the magic of locking the touch gesture into the spacebar when
	 * switching input languages.
	 */
	boolean isInside(LatinKey key, int x, int y) {
		return key.isInside(x, y);
	}

	public boolean inPrefList(int code, int[] pref) {
		if (code < pref.length && code >= 0)
			return pref[code] > 0;
		return false;
	}

	public int distanceFrom(Key k, int x, int y) {
		if (y > k.y && y < k.y + k.height) {
			return Math.abs(k.x + k.width / 2 - x);
		}// else {
		return Integer.MAX_VALUE;
		// }
	}

	@Override
	public int[] getNearestKeys(int x, int y) {
		// if (mCurrentlyInSpace) {
		// return new int[] { mSpaceKeyIndex };
		// }/ // else {
		// Avoid dead pixels at edges of the keyboard
		// return super.getNearestKeys(
		// Math.max(0, Math.min(x, getMinWidth() - 1)),
		// Math.max(0, Math.min(y, getHeight() - 1)));
		return mKeyIndex;
		// }
	}

	public int indexOf(int code) {
		List<Key> keys = getKeys();
		int count = keys.size();
		for (int i = 0; i < count; i++) {
			if (keys.get(i).codes[0] == code)
				return i;
		}
		return -1;
	}

	public int getTextSizeFromTheme(int style, int defValue) {
		TypedArray array = mContext.getTheme().obtainStyledAttributes(style,
				new int[] { android.R.attr.textSize });

		int textSize;

		try {
			textSize = array.getDimensionPixelSize(array.getResourceId(0, 0),
					defValue);
		} catch (Exception e) {
			textSize = defValue;
		} catch (Error error) {
			textSize = defValue;
		}

		return textSize;
	}

	// TODO LatinKey could be static class
	protected class LatinKey extends Keyboard.Key {

		// functional normal state (with properties)
		public final int[] KEY_STATE_FUNCTIONAL_NORMAL = { android.R.attr.state_single };

		// functional pressed state (with properties)
		public final int[] KEY_STATE_FUNCTIONAL_PRESSED = {
				android.R.attr.state_single, android.R.attr.state_pressed };

		public boolean mShiftLockEnabled;

		public LatinKey(Resources res, Keyboard.Row parent, int x, int y,
				XmlResourceParser parser) {
			super(res, parent, x, y, parser);
			if (popupCharacters != null && popupCharacters.length() == 0) {
				// If there is a keyboard with no keys specified in
				// popupCharacters
				popupResId = 0;
			}
		}

		public void enableShiftLock() {
			mShiftLockEnabled = true;
		}

		// sticky is used for shift key. If a key is not sticky and is modifier,
		// the key will be treated as functional.
		public boolean isFunctionalKey() {
			return !sticky && modifier;
		}

		@Override
		public void onReleased(boolean inside) {
			if (!mShiftLockEnabled) {
				super.onReleased(inside);
			} else {
				pressed = !pressed;
			}
		}

		@Override
		public int[] getCurrentDrawableState() {
			if (isFunctionalKey()) {
				if (pressed) {
					return KEY_STATE_FUNCTIONAL_PRESSED;
				} // else {
				return KEY_STATE_FUNCTIONAL_NORMAL;
				// }
			}
			return super.getCurrentDrawableState();
		}

		@Override
		public int squaredDistanceFrom(int x, int y) {
			// We should count vertical gap between rows to calculate the center
			// of this Key.
			final int verticalGap = LatinKeyboard.this.mVerticalGap;
			final int xDist = this.x + width / 2 - x;
			final int yDist = this.y + (height + verticalGap) / 2 - y;
			return xDist * xDist + yDist * yDist;
		}
	}

	/**
	 * Animation to be displayed on the spacebar preview popup when switching
	 * languages by swiping the spacebar. It draws the current, previous and
	 * next languages and moves them by the delta of touch movement on the
	 * spacebar.
	 */
	class SlidingLocaleDrawable extends Drawable {

		public final int mWidth;
		public final int mHeight;
		public final Drawable mBackground;
		public final TextPaint mTextPaint;
		public final int mMiddleX;
		//public final Drawable mLeftDrawable;
		//public final Drawable mRightDrawable;
		public final int mThreshold;
		public int mDiff;
		public boolean mHitThreshold;
		public String mCurrentLanguage;
		public String mNextLanguage;
		public String mPrevLanguage;

		public SlidingLocaleDrawable(Drawable background, int width, int height) {
			mBackground = background;
			setDefaultBounds(mBackground);
			mWidth = width;
			mHeight = height;
			mTextPaint = new TextPaint();
			mTextPaint.setTextSize(getTextSizeFromTheme(
					android.R.style.TextAppearance_Medium, 18));
			mTextPaint.setColor(mRes.getColor(R.color.latinkeyboard_transparent));
			mTextPaint.setTextAlign(Align.CENTER);
			mTextPaint.setAlpha(OPACITY_FULLY_OPAQUE);
			mTextPaint.setAntiAlias(true);
			mMiddleX = (mWidth - mBackground.getIntrinsicWidth()) / 2;
			//mLeftDrawable = mRes
			//		.getDrawable(R.drawable.sym_keyboard_feedback_language_arrows_left);
			//mRightDrawable = mRes
			//		.getDrawable(R.drawable.sym_keyboard_feedback_language_arrows_right);
			mThreshold = ViewConfiguration.get(mContext).getScaledTouchSlop();
		}

		public void setDiff(int diff) {
			if (diff == Integer.MAX_VALUE) {
				mHitThreshold = false;
				mCurrentLanguage = null;
				return;
			}
			mDiff = diff;
			if (mDiff > mWidth)
				mDiff = mWidth;
			if (mDiff < -mWidth)
				mDiff = -mWidth;
			if (Math.abs(mDiff) > mThreshold)
				mHitThreshold = true;
			invalidateSelf();
		}

		/*
		 * public String getLanguageName(Locale locale) { return
		 * LanguageSwitcher.toTitleCase(locale .getDisplayLanguage(locale)); }
		 */

		
		
		@Override
		public void draw(Canvas canvas) {
			canvas.save();
			if (mHitThreshold) {
				Paint paint = mTextPaint;
				final int width = mWidth;
				final int height = mHeight;
				final int diff = mDiff;
				//final Drawable lArrow = mLeftDrawable;
				//final Drawable rArrow = mRightDrawable;
				canvas.clipRect(0, 0, width, height);
				/*
				 * if (mCurrentLanguage == null) { final LanguageSwitcher
				 * languageSwitcher = mLanguageSwitcher; mCurrentLanguage =
				 * getLanguageName(languageSwitcher .getInputLocale());
				 * mNextLanguage = getLanguageName(languageSwitcher
				 * .getNextInputLocale()); mPrevLanguage =
				 * getLanguageName(languageSwitcher .getPrevInputLocale()); }
				 */
				// Draw language text with shadow
				final float baseline = mHeight * SPACEBAR_LANGUAGE_BASELINE
						- paint.descent();
				paint.setColor(mRes
						.getColor(R.color.latinkeyboard_feedback_language_text));
				canvas.drawText(mCurrentLanguage, width / 2 + diff, baseline,
						paint);
				canvas.drawText(mNextLanguage, diff - width / 2, baseline,
						paint);
				canvas.drawText(mPrevLanguage, diff + width + width / 2,
						baseline, paint);

				//setDefaultBounds(lArrow);
				//rArrow.setBounds(width - rArrow.getIntrinsicWidth(), 0, width,
				//		rArrow.getIntrinsicHeight());
				//lArrow.draw(canvas);
				//rArrow.draw(canvas);
			}
			if (mBackground != null) {
				canvas.translate(mMiddleX, 0);
				mBackground.draw(canvas);
			}
			canvas.restore();
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}

		@Override
		public void setAlpha(int alpha) {
			// Ignore
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			// Ignore
		}

		@Override
		public int getIntrinsicWidth() {
			return mWidth;
		}

		@Override
		public int getIntrinsicHeight() {
			return mHeight;
		}
	}
}
