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

package org.geogebra.web.full.gui.dialog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.html5.util.inputmethod.InputMethodDescriptor;
import org.geogebra.web.html5.util.inputmethod.InputMethodSetting;
import org.geogebra.web.shared.components.dialog.ComponentDialog;
import org.geogebra.web.shared.components.dialog.DialogData;
import org.gwtproject.user.client.ui.FlowPanel;
import org.gwtproject.user.client.ui.Label;
import org.gwtproject.user.client.ui.PasswordTextBox;
import org.gwtproject.user.client.ui.TextBox;

import jsinterop.base.Js;

/**
 * Prompts for the settings an input method declares in its descriptor, one text
 * field per setting. GeoGebra does not interpret the values, it only stores them
 * and hands them back to the input method.
 *
 * <p>Note: values entered here are kept in browser storage (see
 * {@link org.geogebra.web.full.gui.keyboard.InputMethodSettingsManager}). Anyone
 * with devtools access to this page can read them back, so an input method
 * should not ask here for a secret it cannot afford to expose.
 */
public class InputMethodSettingsDialog extends ComponentDialog {

  private final Map<String, TextBox> fields = new LinkedHashMap<>();

  /**
   * @param app application
   * @param descriptor input method whose settings are being asked for
   * @param callback called with the entered values, keyed by setting key
   */
  public InputMethodSettingsDialog(AppW app, InputMethodDescriptor descriptor,
      Consumer<Map<String, String>> callback) {
    super(app, new DialogData(descriptor.label, "Cancel", "OK"), false, true);
    addStyleName("inputMethodSettingsDialog");

    FlowPanel content = new FlowPanel();
    for (InputMethodSetting setting : descriptor.settings) {
      content.add(new Label(setting.label));
      TextBox field = Js.isTruthy(setting.secret) ? new PasswordTextBox() : new TextBox();
      fields.put(setting.key, field);
      content.add(field);
    }
    addDialogContent(content);

    setOnPositiveAction(() -> {
      Map<String, String> values = new LinkedHashMap<>();
      fields.forEach((key, field) -> values.put(key, field.getText().trim()));
      callback.accept(values);
    });
  }
}
