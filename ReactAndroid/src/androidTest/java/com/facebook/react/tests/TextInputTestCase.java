/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.tests;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.testing.ReactAppInstrumentationTestCase;
import com.facebook.react.testing.ReactInstanceSpecForTest;
import com.facebook.react.testing.StringRecordingModule;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.views.textinput.ReactEditText;

/** Test to verify that TextInput renders correctly */
public class TextInputTestCase extends ReactAppInstrumentationTestCase {

  private final StringRecordingModule mRecordingModule = new StringRecordingModule();

  private interface TextInputTestModule extends JavaScriptModule {
    void setValueRef(String ref, String value);
  }

  /**
   * Test that the actual height of the text input is not dependant on the font size of the text
   * within.
   */
  public void testTextInputMeasurements() {
    View textInputViewHeightSet = getViewByTestId("textInput1");
    EditText textInputViewNoHeight = getViewByTestId("textInput2");

    int expectedHeight = Math.round(PixelUtil.toPixelFromDIP(30));
    assertEquals(expectedHeight, textInputViewHeightSet.getHeight());

    EditText editText = new EditText(textInputViewNoHeight.getContext());
    editText.setTextSize(
        TypedValue.COMPLEX_UNIT_PX, (float) Math.ceil(PixelUtil.toPixelFromSP(21.f)));
    editText.setPadding(0, 0, 0, 0);
    int measureSpec =
        View.MeasureSpec.makeMeasureSpec(
            ViewGroup.LayoutParams.WRAP_CONTENT, View.MeasureSpec.UNSPECIFIED);
    editText.measure(measureSpec, measureSpec);

    assertEquals(editText.getMeasuredHeight(), textInputViewNoHeight.getHeight());
  }

  /** Test that the cursor moves to the end of the word. */
  public void testTextInputCursorPosition() throws Throwable {
    final EditText textInputWithText = getViewByTestId("textInput3");

    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            textInputWithText.setSelection(3);
          }
        });
    getReactContext()
        .getJSModule(TextInputTestModule.class)
        .setValueRef("textInput3", "Some other value");
    waitForBridgeAndUIIdle();

    assertEquals(4, textInputWithText.getSelectionStart());
    assertEquals(4, textInputWithText.getSelectionEnd());
  }

  /** Test that the colors are applied to new text */
  public void testTextInputColors() throws Throwable {
    String testIDs[] = new String[] {"textInput4", "textInput5", "textInput6"};

    for (String testID : testIDs) {
      getReactContext().getJSModule(TextInputTestModule.class).setValueRef(testID, "NewText");
    }
    waitForBridgeAndUIIdle();

    for (String testID : testIDs) {
      ReactEditText reactEditText = getViewByTestId(testID);
      assertEquals(
          Color.GREEN,
          reactEditText
              .getText()
              .getSpans(0, 1, ForegroundColorSpan.class)[0]
              .getForegroundColor());
    }
  }

  public void testOnSubmitEditing() throws Throwable {
    String testId = "onSubmitTextInput";
    ReactEditText reactEditText = getViewByTestId(testId);

    fireEditorActionAndCheckRecording(reactEditText, EditorInfo.IME_ACTION_GO);
    fireEditorActionAndCheckRecording(reactEditText, EditorInfo.IME_ACTION_DONE);
    fireEditorActionAndCheckRecording(reactEditText, EditorInfo.IME_ACTION_NEXT);
    fireEditorActionAndCheckRecording(reactEditText, EditorInfo.IME_ACTION_PREVIOUS);
    fireEditorActionAndCheckRecording(reactEditText, EditorInfo.IME_ACTION_SEARCH);
    fireEditorActionAndCheckRecording(reactEditText, EditorInfo.IME_ACTION_SEND);
    fireEditorActionAndCheckRecording(reactEditText, EditorInfo.IME_ACTION_UNSPECIFIED);
    fireEditorActionAndCheckRecording(reactEditText, EditorInfo.IME_ACTION_NONE);
  }

  public void testRequestFocusDoesNothing() throws Throwable {
    String testId = "textInput1";

    final ReactEditText reactEditText = getViewByTestId(testId);
    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            reactEditText.clearFocus();
          }
        });
    waitForBridgeAndUIIdle();
    assertFalse(reactEditText.isFocused());

    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            reactEditText.requestFocus();
          }
        });
    waitForBridgeAndUIIdle();

    // Calling requestFocus() directly should no-op
    assertFalse(reactEditText.isFocused());
  }

  public void testRequestFocusFromJS() throws Throwable {
    String testId = "textInput1";

    final ReactEditText reactEditText = getViewByTestId(testId);

    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            reactEditText.clearFocus();
          }
        });
    waitForBridgeAndUIIdle();
    assertFalse(reactEditText.isFocused());

    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            reactEditText.requestFocusFromJS();
          }
        });
    waitForBridgeAndUIIdle();
    assertTrue(reactEditText.isFocused());
  }

  public void testAccessibilityFocus() throws Throwable {
    String testId = "textInput1";

    final ReactEditText reactEditText = getViewByTestId(testId);
    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            reactEditText.clearFocus();
          }
        });
    waitForBridgeAndUIIdle();
    assertFalse(reactEditText.isFocused());

    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            reactEditText.performAccessibilityAction(
                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
            reactEditText.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null);
          }
        });
    waitForBridgeAndUIIdle();
    assertTrue(reactEditText.isFocused());

    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            reactEditText.performAccessibilityAction(
                AccessibilityNodeInfo.ACTION_CLEAR_FOCUS, null);
          }
        });
    waitForBridgeAndUIIdle();
    assertFalse(reactEditText.isFocused());
  }

  public void testAccessibilityFocus_notEmpty_selectionSetAtEnd() throws Throwable {
    String testId = "textInput1";
    String text = "Testing";

    final ReactEditText reactEditText = getViewByTestId(testId);
    reactEditText.setText(text);
    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            reactEditText.clearFocus();
          }
        });
    waitForBridgeAndUIIdle();
    assertFalse(reactEditText.isFocused());
    assertEquals(0, reactEditText.getSelectionStart());

    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            reactEditText.performAccessibilityAction(
                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
            reactEditText.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null);
          }
        });
    waitForBridgeAndUIIdle();
    assertTrue(reactEditText.isFocused());
    assertEquals(text.length(), reactEditText.getSelectionStart());
  }

  private void fireEditorActionAndCheckRecording(
      final ReactEditText reactEditText, final int actionId) throws Throwable {
    fireEditorActionAndCheckRecording(reactEditText, actionId, "blurAndSubmit");
    fireEditorActionAndCheckRecording(reactEditText, actionId, "newline");
  }

  private void fireEditorActionAndCheckRecording(
      final ReactEditText reactEditText, final int actionId, final String submitBehavior)
      throws Throwable {
    mRecordingModule.reset();

    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            reactEditText.requestFocusFromJS();
            reactEditText.setSubmitBehavior(submitBehavior);
            reactEditText.onEditorAction(actionId);
          }
        });
    waitForBridgeAndUIIdle();

    assertEquals(1, mRecordingModule.getCalls().size());
    assertEquals(!submitBehavior.equals("blurAndSubmit"), reactEditText.isFocused());
  }

  /**
   * Test that the mentions input has colors displayed correctly. Removed for being flaky in open
   * source, December 2016 public void testMetionsInputColors() throws Throwable { EventDispatcher
   * eventDispatcher = UIManagerHelper.getEventEmitterForReactTag(reactContext, tag); ReactEditText
   * reactEditText = getViewByTestId("tokenizedInput"); String newText = "#Things and more #things";
   * int contentWidth = reactEditText.getWidth(); int contentHeight = reactEditText.getHeight(); int
   * start = 0; int count = newText.length();
   *
   * <p>eventDispatcher.dispatchEvent( new ReactTextChangedEvent( reactEditText.getId(),
   * newText.toString(), (int) PixelUtil.toDIPFromPixel(contentWidth), (int)
   * PixelUtil.toDIPFromPixel(contentHeight), reactEditText.incrementAndGetEventCounter()));
   *
   * <p>eventDispatcher.dispatchEvent( new ReactTextInputEvent( reactEditText.getId(),
   * newText.toString(), "", start, start + count - 1)); waitForBridgeAndUIIdle();
   *
   * <p>ForegroundColorSpan[] spans = reactEditText .getText().getSpans(0,
   * reactEditText.getText().length(), ForegroundColorSpan.class); assertEquals(2, spans.length);
   * assertEquals(spans[0].getForegroundColor(), spans[1].getForegroundColor()); assertEquals(0,
   * reactEditText.getText().getSpanStart(spans[1])); assertEquals(7,
   * reactEditText.getText().getSpanEnd(spans[1])); assertEquals(newText.length() - 7,
   * reactEditText.getText().getSpanStart(spans[0])); assertEquals(newText.length(),
   * reactEditText.getText().getSpanEnd(spans[0]));
   *
   * <p>String moreText = "andsuch "; String previousText = newText; newText += moreText; count =
   * moreText.length(); start = previousText.length();
   *
   * <p>eventDispatcher.dispatchEvent( new ReactTextChangedEvent( reactEditText.getId(),
   * newText.toString(), (int) PixelUtil.toDIPFromPixel(contentWidth), (int)
   * PixelUtil.toDIPFromPixel(contentHeight), reactEditText.incrementAndGetEventCounter()));
   *
   * <p>eventDispatcher.dispatchEvent( new ReactTextInputEvent( reactEditText.getId(), moreText, "",
   * start, start + count - 1)); waitForBridgeAndUIIdle();
   *
   * <p>spans = reactEditText.getText() .getSpans(0, reactEditText.getText().length(),
   * ForegroundColorSpan.class); assertEquals(2, spans.length);
   * assertEquals(spans[0].getForegroundColor(), spans[1].getForegroundColor()); assertEquals(0,
   * reactEditText.getText().getSpanStart(spans[1])); assertEquals(7,
   * reactEditText.getText().getSpanEnd(spans[1])); assertEquals(newText.length() - 15,
   * reactEditText.getText().getSpanStart(spans[0])); assertEquals(newText.length() - 1,
   * reactEditText.getText().getSpanEnd(spans[0]));
   *
   * <p>moreText = "morethings"; previousText = newText; newText += moreText; count =
   * moreText.length(); start = previousText.length();
   *
   * <p>eventDispatcher.dispatchEvent( new ReactTextChangedEvent( reactEditText.getId(),
   * newText.toString(), (int) PixelUtil.toDIPFromPixel(contentWidth), (int)
   * PixelUtil.toDIPFromPixel(contentHeight), reactEditText.incrementAndGetEventCounter()));
   *
   * <p>eventDispatcher.dispatchEvent( new ReactTextInputEvent( reactEditText.getId(), moreText, "",
   * start, start + count - 1)); waitForBridgeAndUIIdle();
   *
   * <p>spans = reactEditText.getText() .getSpans(0, reactEditText.getText().length(),
   * ForegroundColorSpan.class); assertEquals(spans[0].getForegroundColor(),
   * spans[1].getForegroundColor()); assertEquals(2, spans.length); assertEquals(0,
   * reactEditText.getText().getSpanStart(spans[1])); assertEquals(7,
   * reactEditText.getText().getSpanEnd(spans[1])); assertEquals(newText.length() - 25,
   * reactEditText.getText().getSpanStart(spans[0])); assertEquals(newText.length() - 11,
   * reactEditText.getText().getSpanEnd(spans[0])); }
   */
  @Override
  protected ReactInstanceSpecForTest createReactInstanceSpecForTest() {
    return super.createReactInstanceSpecForTest().addNativeModule(mRecordingModule);
  }

  @Override
  protected String getReactApplicationKeyUnderTest() {
    return "TextInputTestApp";
  }
}
