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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.geogebra.common.gui.inputfield.HasLastItem;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoInputBox;
import org.geogebra.common.main.App;
import org.geogebra.common.main.App.InputPosition;
import org.geogebra.common.main.settings.GeneralSettings;
import org.geogebra.common.main.settings.SettingListener;
import org.geogebra.gwtutil.NavigatorUtil;
import org.geogebra.keyboard.base.KeyboardType;
import org.geogebra.keyboard.web.HasKeyboard;
import org.geogebra.keyboard.web.KeyboardCloseListener;
import org.geogebra.keyboard.web.KeyboardListener;
import org.geogebra.keyboard.web.TabbedKeyboard;
import org.geogebra.web.editor.MathFieldProcessing;
import org.geogebra.web.full.gui.AlgebraMathFieldProcessing;
import org.geogebra.web.full.gui.dialog.text.GeoTextEditor;
import org.geogebra.web.full.gui.dialog.text.TextEditPanelProcessing;
import org.geogebra.web.full.gui.openfileview.HeaderFileView;
import org.geogebra.web.full.gui.util.ScriptArea;
import org.geogebra.web.full.gui.util.VirtualKeyboardGUI;
import org.geogebra.web.full.gui.view.algebra.AlgebraViewW;
import org.geogebra.web.full.gui.view.algebra.RadioTreeItem;
import org.geogebra.web.full.gui.view.algebra.RetexKeyboardListener;
import org.geogebra.web.full.util.keyboard.AutocompleteProcessing;
import org.geogebra.web.full.util.keyboard.ScriptAreaProcessing;
import org.geogebra.web.html5.gui.GPopupPanel;
import org.geogebra.web.html5.gui.inputfield.AutoCompleteTextFieldW;
import org.geogebra.web.html5.gui.util.Dom;
import org.geogebra.web.html5.gui.util.MathKeyboardListener;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.html5.util.keyboard.KeyboardManagerInterface;
import org.gwtproject.dom.client.Style;
import org.gwtproject.user.client.ui.Panel;
import org.gwtproject.user.client.ui.RequiresResize;
import org.gwtproject.user.client.ui.RootPanel;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Handles creating, showing and updating the keyboard
 */
public final class KeyboardManager
		implements RequiresResize, KeyboardManagerInterface, SettingListener<GeneralSettings> {

	private final AppW app;
	/** currently active keyboard, either {@link #typedKeyboard} or {@link #inputMethodKeyboard} */
	private @Nullable VirtualKeyboardGUI keyboard;
	private OnscreenTabbedKeyboard typedKeyboard;
	private InputMethodKeyboardPanel inputMethodKeyboard;
	private Panel currentAppFrame;

	private String originalBodyPadding;
	private final Style bodyStyle;
	private KeyboardListener processing;
	private final KeyboardDetachController detachController;

	/**
	 * Constructor
	 *
	 * @param appWFull the application
	 */
	public KeyboardManager(AppW appWFull) {
		this.app = appWFull;
		this.bodyStyle = RootPanel.getBodyElement().getStyle();
		detachController = new KeyboardDetachController(app.getAppletId(),
				app.getAppletParameters().getDetachKeyboardParent(),
				app.getGeoGebraElement().getParentElement(),
				shouldDetach());
		app.getSettings().getGeneral().addListener(this);
	}

	/**
	 *
	 * @return list of view ids which have keyboard.
	 */
	public List<Integer> getKeyboardViews() {
		ArrayList<Integer> keyboardViews = getKeyboardViewsNoEV();
		Predicate<GeoInputBox> filter = geo -> NavigatorUtil.isMobile()
				|| geo.isSymbolicMode()
				|| geo.needsSymbolButton();
		if (app.getKernel().getConstruction().hasInputBoxes(filter)) {
			keyboardViews.add(App.VIEW_EUCLIDIAN);
			keyboardViews.add(App.VIEW_EUCLIDIAN2);
		}
		return keyboardViews;
	}

	private ArrayList<Integer> getKeyboardViewsNoEV() {
		ArrayList<Integer> keyboardViews = new ArrayList<>();
		if (app.showAlgebraInput()
				&& app.getInputPosition() == InputPosition.algebraView) {
			keyboardViews.add(App.VIEW_ALGEBRA);
		}
		keyboardViews.addAll(Arrays.asList(App.VIEW_CAS, App.VIEW_SPREADSHEET,
				App.VIEW_PROBABILITY_CALCULATOR));
		return keyboardViews;
	}

	/**
	 * Update keyboard style.
	 */
	private void updateStyle() {
		if (keyboard != null) {
			Dom.toggleClass(keyboard.asWidget(), "detached", shouldDetach());
		}
	}

	/**
	 *
	 * @return keyboard is detachable, no view uses it
	 */
	public boolean shouldDetach() {
		if (!"auto".equals(app.getAppletParameters().getParamDetachKeyboard())) {
			return Boolean.parseBoolean(app.getAppletParameters().getParamDetachKeyboard());
		}
		for (Integer viewId : this.getKeyboardViewsNoEV()) {
			if (app.showView(viewId)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * OpenFileView width if open, app width (dockpanel width) otherwise
	 * @return the preferred keyboard width
	 */
	public double getKeyboardWidth() {
		double appWidth = app.getWidth();
		if (app.getGuiManager().isOpenFileViewLoaded()) {
			HeaderFileView headerFileView = (HeaderFileView) app.getGuiManager().getBrowseView();
			if (headerFileView != null && headerFileView.getPanel().getOffsetWidth() > 0) {
				appWidth = headerFileView.getPanel().getOffsetWidth();
			}
		}

		return detachController.isEnabled()
				? detachController.getParentWidth()
				: appWidth;
	}

	/**
	 * @return height inside of the geogebra window
	 */
	public int estimateKeyboardHeight() {
		int realHeight = ensureKeyboardsExist().getOffsetHeight();
		if (realHeight > 0) {
			return realHeight;
		}
		return estimateHiddenKeyboardHeight();
	}

	@Override
	public int estimateHiddenKeyboardHeight() {
		return TabbedKeyboard.TOTAL_HEIGHT;
	}

	/**
	 * @param appFrame
	 *            frame of the applet
	 */
	public void addKeyboard(Panel appFrame) {
		ensureKeyboardsExist();
    this.currentAppFrame = appFrame;
    attachActiveKeyboard();
    updateStyle();
  }

  private void attachActiveKeyboard() {
		if (detachController.isEnabled()) {
			detachController.addAsDetached(keyboard);
			app.addWindowResizeListener(this);
    } else if (currentAppFrame != null) {
      currentAppFrame.add(keyboard);
    }
  }

  /**
   * Switches the currently shown panel between the typed on-screen keyboard
   * and the alternative input method panel, keeping the same trigger points
   * (button click / equation-edit focus) and text field.
   */
  public void toggleInputMethod() {
    VirtualKeyboardGUI previous = keyboard;
    boolean switchingToInputMethod = previous == typedKeyboard;
    keyboard = switchingToInputMethod ? ensureInputMethodKeyboardExists()
        : ensureTypedKeyboardExists();
    if (processing != null) {
      keyboard.setProcessing(processing);
    }
    if (previous == keyboard) {
      return;
    }
    if (previous != null && currentAppFrame != null && !detachController.isEnabled()) {
      currentAppFrame.remove(previous.asWidget());
    }
    attachActiveKeyboard();
    keyboard.show();
  }

  private InputMethodKeyboardPanel ensureInputMethodKeyboardExists() {
    if (inputMethodKeyboard == null) {
      inputMethodKeyboard = new InputMethodKeyboardPanel(getInputMethodUrl(),
          new InputMethodSettingsManager(app),
          this::getLastSelectedItemListener, this::toggleInputMethod);
		}
    return inputMethodKeyboard;
  }

  /**
   * @return the configured input method adapter URL, or an empty string when no
   *         alternative input method is configured (the feature is then off)
   */
  private String getInputMethodUrl() {
    return app.getAppletParameters().getParamInputMethodUrl();
  }

  /**
   * @return a fresh {@link KeyboardListener} targeting the algebra item the user
   *         is editing (the input row when no existing item is being edited),
   *         or {@code null} if there is none.
   */
  private KeyboardListener getLastSelectedItemListener() {
    if (!(app.getAlgebraView() instanceof AlgebraViewW)) {
      return null;
    }
    AlgebraViewW algebraView = (AlgebraViewW) app.getAlgebraView();
    // the edited item, not the selected one: a selection can point at an
    // unrelated row (or a leftover from an earlier click), which would make the
    // recognized content overwrite that row instead of the one being edited
    RadioTreeItem item = algebraView.getActiveTreeItem();
    if (item == null) {
      GeoElement lastSelected = algebraView.getLastSelectedGeo();
      item = lastSelected == null ? null : algebraView.getNode(lastSelected);
    }
    if (item == null) {
      return null;
    }
    return new AlgebraMathFieldProcessing(item, app.getLastItemProvider());
	}

	/**
	 *
	 * @return true if the keyboard is not attached to the frame.
	 */
	public boolean isKeyboardOutsideFrame() {
		return detachController.isEnabled();
	}

	@Override
	public void onResize() {
		if (keyboard != null) {
			keyboard.onResize();
		}
	}

	/**
	 * Update keyboard processor and close listener.
	 *
	 * @param textField
	 *            textfield adapter
	 * @param listener
	 *            open/close listener
	 */
	public void setListeners(MathKeyboardListener textField,
			KeyboardCloseListener listener) {
		VirtualKeyboardGUI keyboardUI = ensureKeyboardsExist();
    keyboardUI.clearAndUpdate();
		if (textField != null) {
			setOnScreenKeyboardTextField(textField);
		}
		keyboardUI.setListener(listener);
	}

	/**
	 * Lazy loading getter
	 * @return the keyboard
	 */
	public @NonNull VirtualKeyboardGUI getOnScreenKeyboard() {
		return ensureKeyboardsExist();
	}

  /**
   * @return the typed on-screen keyboard, creating it if needed. Does not
   *         change which panel is currently active.
   */
  private OnscreenTabbedKeyboard ensureTypedKeyboardExists() {
    if (typedKeyboard == null) {
			boolean showMoreButton = app.getConfig().showKeyboardHelpButton()
					&& !shouldDetach();
      typedKeyboard = new OnscreenTabbedKeyboard((HasKeyboard) app, showMoreButton,
          getInputMethodUrl().isEmpty() ? null : this::toggleInputMethod);
			if (processing != null) {
        typedKeyboard.setProcessing(processing);
			}
    }
    return typedKeyboard;
  }

  /**
   * @return the currently active keyboard panel (typed or input method),
   *         defaulting to the typed keyboard on first use.
   */
  private VirtualKeyboardGUI ensureKeyboardsExist() {
    if (keyboard == null) {
      keyboard = ensureTypedKeyboardExists();
		}
		return keyboard;
	}

	@Override
	public void updateKeyboardLanguage() {
		if (keyboard != null) {
			keyboard.checkLanguage();
		}
	}

	@Override
	public void clearAndUpdateKeyboard() {
		if (keyboard != null) {
			keyboard.clearAndUpdate();
		}
	}

	@Override
	public void removeFromDom() {
		if (detachController.removeKeyboardRootFromDom()) {
			keyboard = null;
		}
	}

	@Override
	public void setOnScreenKeyboardTextField(MathKeyboardListener textField) {
		processing = makeKeyboardListener(textField, app.getLastItemProvider());
		if (keyboard != null) {
			keyboard.setProcessing(processing);
			if (textField != null) {
				addExtraSpaceForKeyboard();
			} else {
				removeExtraSpaceForKeyboard();
			}
		}
	}

	private void addExtraSpaceForKeyboard() {
		if (extraSpaceNeededForKeyboard()) {
			originalBodyPadding = bodyStyle.getPaddingBottom();
			bodyStyle.setProperty("paddingBottom", estimateKeyboardHeight() + "px");
		}
	}

	private void removeExtraSpaceForKeyboard() {
		if (!Objects.equals(originalBodyPadding, bodyStyle.getPaddingBottom())) {
			bodyStyle.setProperty("paddingBottom", originalBodyPadding);
		}
	}

	private boolean extraSpaceNeededForKeyboard() {
		if (shouldDetach() && !detachController.hasCustomParent()) {
			double appletBottom = app.getFrameElement().getAbsoluteBottom();
			return NavigatorUtil.getWindowHeight() - appletBottom < estimateKeyboardHeight();
		}

		return false;
	}

	/**
	 * Notify keyboard about finished editing
	 */
	public void onScreenEditingEnded() {
		if (keyboard != null) {
			removeExtraSpaceForKeyboard();
		}
	}

	/**
	 * Update keyboard size.
	 */
	public void resizeKeyboard() {
		if (keyboard != null) {
			keyboard.onResize();
			keyboard.setStyleName();
		}
	}

	@Override
	public boolean isKeyboardClosedByUser() {
		return this.keyboard != null && !this.keyboard.shouldBeShown();
	}

	@Override
	public void addKeyboardAutoHidePartner(GPopupPanel popup) {
		if (keyboard != null) {
			keyboard.addAutoHidePartner(popup);
		}
	}

	/**
	 * @param tab tab to be activated
	 */
	public void selectTab(KeyboardType tab) {
		if (keyboard != null) {
			keyboard.selectTab(tab);
		}
	}

	/**
	 * Create keyboard adapter for text editing object.
	 * Implemented here so that the components from web-common can have
	 * processing implementation in web.
	 */
	private static KeyboardListener makeKeyboardListener(
			MathKeyboardListener textField, HasLastItem lastItemProvider) {
		if (textField instanceof RetexKeyboardListener) {
			return new MathFieldProcessing(
					((RetexKeyboardListener) textField).getMathField());
		}
		if (textField instanceof RadioTreeItem) {
			return new AlgebraMathFieldProcessing(
					(RadioTreeItem) textField,
					lastItemProvider);
		}
		if (textField instanceof KeyboardListener) {
			return (KeyboardListener) textField;
		}
		if (textField instanceof GeoTextEditor) {
			return new TextEditPanelProcessing((GeoTextEditor) textField);
		}
		if (textField instanceof AutoCompleteTextFieldW) {
			return new AutocompleteProcessing(
					(AutoCompleteTextFieldW) textField);
		}

		if (textField instanceof ScriptArea) {
			return new ScriptAreaProcessing((ScriptArea) textField);
		}

		return null;
	}

	@Override
	public void settingsChanged(GeneralSettings settings) {
		if (keyboard != null) {
			keyboard.checkLanguage();
		}
	}
}
