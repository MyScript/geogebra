/*
 * GeoGebra - Dynamic Mathematics for Everyone
 * Copyright (c) GeoGebra GmbH, Altenbergerstr. 69, 4040 Linz, Austria
 * https://www.geogebra.org
 *
 * This file is licensed by GeoGebra GmbH under the EUPL 1.2 licence and
 * may be used under the EUPL 1.2 in compatible projects (see Article 5
 * and the Appendix of EUPL 1.2 for details).
 * You may obtain a copy of the licence at:
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Note: The overall GeoGebra software package is free to use for
 * non-commercial purposes only.
 * See https://www.geogebra.org/license for full licensing details
 */

package org.geogebra.web.full.gui.keyboard;

import java.util.function.Supplier;

import org.geogebra.keyboard.base.KeyboardType;
import org.geogebra.keyboard.web.KeyboardCloseListener;
import org.geogebra.keyboard.web.KeyboardListener;
import org.geogebra.web.full.gui.util.VirtualKeyboardGUI;
import org.geogebra.web.html5.gui.GPopupPanel;
import org.geogebra.web.html5.gui.view.button.StandardButton;
import org.geogebra.web.html5.util.inputmethod.InputMethodContext;
import org.geogebra.web.html5.util.inputmethod.InputMethodDescriptor;
import org.geogebra.web.html5.util.inputmethod.InputMethodHandle;
import org.geogebra.web.html5.util.inputmethod.InputMethodLoader;
import org.geogebra.web.html5.util.inputmethod.InputMethodSettingsProvider;
import org.gwtproject.core.client.Scheduler;
import org.gwtproject.user.client.ui.FlowPanel;
import org.gwtproject.user.client.ui.RequiresResize;
import org.gwtproject.user.client.ui.Widget;

import elemental2.dom.DomGlobal;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

/**
 * Hosts an alternative input method (e.g. handwriting math recognition) supplied
 * at runtime by an adapter script, as an alternative to the typed
 * {@link OnscreenTabbedKeyboard}. The recognized content replaces the content of
 * the targeted math field.
 *
 * <p>Nothing here knows what the input method is: it is mounted through the
 * {@link InputMethodDescriptor} contract only.
 */
public class InputMethodKeyboardPanel extends FlowPanel
    implements VirtualKeyboardGUI, RequiresResize {

  private final FlowPanel mountPanel;
  private final String adapterUrl;
  private final InputMethodSettingsProvider settingsProvider;
  private final Supplier<KeyboardListener> lastSelectedItemSupplier;
  private InputMethodHandle inputMethod;
  private KeyboardListener processing;
  private boolean mounting = false;
  /** whether the user wants this panel shown; mirrors TabbedKeyboard's
   * keyboardWanted flag so a programmatic hide does not read as "closed by
   * user" (which would stop the frame from ever reopening the keyboard).
   * Nothing clears it: this panel has no close button. Clear it there if one
   * is ever added. */
  private boolean keyboardWanted = false;
  /** target resolved on the first result of the current input session, reused
   * for every subsequent result so multiple results edit the same item
   * instead of each creating a new one. */
  private KeyboardListener currentTarget;

  /**
   * @param adapterUrl URL of the input method adapter script
   * @param settingsProvider resolves the settings the input method declares
   * @param lastSelectedItemSupplier resolves a fresh listener for whichever
   *        algebra item was last selected, independent of edit focus
   * @param onSwitchToKeyboard called when the user wants to switch to the
   *        typed on-screen keyboard
   */
  public InputMethodKeyboardPanel(String adapterUrl,
      InputMethodSettingsProvider settingsProvider,
      Supplier<KeyboardListener> lastSelectedItemSupplier,
      Runnable onSwitchToKeyboard) {
    this.adapterUrl = adapterUrl;
    this.settingsProvider = settingsProvider;
    this.lastSelectedItemSupplier = lastSelectedItemSupplier;
    addStyleName("inputMethodKeyboardPanel");

    FlowPanel toolbar = new FlowPanel();
    toolbar.addStyleName("inputMethodKeyboardToolbar");
    StandardButton clearBtn = new StandardButton("Clear");
    clearBtn.addFastClickHandler(this::onClear);
    toolbar.add(clearBtn);

    StandardButton keyboardBtn = new StandardButton("Keyboard");
    keyboardBtn.addFastClickHandler(source -> {
      finalizeCurrentTarget();
      onSwitchToKeyboard.run();
    });
    toolbar.add(keyboardBtn);

    mountPanel = new FlowPanel();
    mountPanel.addStyleName("inputMethodKeyboardSurface");

    add(toolbar);
    add(mountPanel);
  }

  private void onClear(Widget source) {
    finalizeCurrentTarget();
    if (inputMethod != null) {
      inputMethod.clear();
    }
  }

  /**
   * Resolves and caches the item to edit for the current input session, so that
   * repeated results (as the user keeps writing) update that same item
   * instead of each one creating a new item.
   */
  private KeyboardListener resolveCurrentTarget() {
    if (currentTarget == null) {
      currentTarget = lastSelectedItemSupplier.get();
      if (currentTarget == null) {
        currentTarget = processing;
      }
      if (currentTarget != null) {
        // focus only once per session: re-focusing on every result can make
        // the algebra view treat the field as freshly (re)activated and spawn a
        // new row instead of continuing to edit this one
        currentTarget.setFocus(true);
      }
    }
    return currentTarget;
  }

  /**
   * Ends editing of the cached target (if any) and forgets it, so the next
   * result resolves and edits a fresh item.
   */
  private void finalizeCurrentTarget() {
    if (currentTarget != null) {
      currentTarget.endEditing();
      currentTarget = null;
    }
  }

  private void ensureMounted() {
    if (mounting || inputMethod != null) {
      return;
    }
    mounting = true;
    InputMethodLoader.load(adapterUrl,
        descriptor -> settingsProvider.withSettings(descriptor, settings ->
            // deferred: the mount panel's flex-resolved height isn't final yet in
            // this callback, and an input method typically measures its mount
            // element's size when it starts
            Scheduler.get().scheduleDeferred(() -> mount(descriptor, settings))),
        this::onError);
  }

  private void mount(InputMethodDescriptor descriptor,
      JsPropertyMap<String> settings) {
    InputMethodContext context = InputMethodContext.create(settings,
        this::onResult, this::onError);
    try {
      inputMethod = descriptor.mount(Js.uncheckedCast(mountPanel.getElement()), context);
    } catch (Exception e) {
      mounting = false;
      onError("Input method failed to start: " + e.getMessage());
    }
  }

  private void onResult(String latex) {
    if (latex == null || latex.isEmpty()) {
      return;
    }
    KeyboardListener target = resolveCurrentTarget();
    if (target != null) {
      target.replaceContent(latex);
    }
  }

  private void onError(String message) {
    DomGlobal.console.error(message);
  }

  @Override
  public void show() {
    keyboardWanted = true;
    setVisible(true);
    ensureMounted();
  }

  @Override
  public void resetKeyboardState() {
    clearAndUpdate();
  }

  @Override
  public boolean shouldBeShown() {
    return keyboardWanted;
  }

  @Override
  public int getOffsetHeight() {
    return getElement().getOffsetHeight();
  }

  @Override
  public void showOnFocus() {
    show();
  }

  @Override
  public void afterShown(Runnable runnable) {
    // ponytail: no show animation on this panel, run the callback right away
    runnable.run();
  }

  @Override
  public void prepareShow(boolean animated) {
    show();
  }

  @Override
  public void showMoreButton() {
    // no "more" tab for an input method panel
  }

  @Override
  public void hideMoreButton() {
    // no "more" tab for an input method panel
  }

  @Override
  public void setStyleName() {
    // style is fixed via inputMethodKeyboardPanel, nothing to update
  }

  @Override
  public void setProcessing(KeyboardListener makeKeyboardListener) {
    this.processing = makeKeyboardListener;
  }

  @Override
  public void setListener(KeyboardCloseListener listener) {
    // no close button in this panel; closing is driven from outside
  }

  @Override
  public void remove(Runnable runnable) {
    finalizeCurrentTarget();
    setVisible(false);
    if (inputMethod != null) {
      inputMethod.destroy();
      inputMethod = null;
    }
    mounting = false;
    // ponytail: no close animation, so the caller's callback must not wait for
    // an animationend event that never fires (it resets the frame's keyboard
    // state; without it the keyboard can never be reopened)
    runnable.run();
  }

  @Override
  public void checkLanguage() {
    // the input method owns its own recognition language, not GeoGebra's locale
  }

  @Override
  public void addAutoHidePartner(GPopupPanel popup) {
    popup.addAutoHidePartner(getElement());
  }

  @Override
  public void selectTab(KeyboardType type) {
    // single-mode panel, no tabs to select
  }

  @Override
  public void clearAndUpdate() {
    if (inputMethod != null) {
      inputMethod.clear();
    }
  }

  @Override
  public void finishAnimation() {
    // no animation on this panel
  }

  @Override
  public void onResize() {
    // the input surface is CSS-sized, but an input method may cache its own
    // pointer hit-test region when it starts and need an explicit nudge to
    // re-measure it after a layout change
    if (inputMethod != null) {
      inputMethod.resize();
    }
  }
}
