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

import java.util.function.Consumer;

import org.geogebra.web.full.gui.dialog.InputMethodSettingsDialog;
import org.geogebra.web.html5.gui.util.BrowserStorage;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.html5.util.inputmethod.InputMethodDescriptor;
import org.geogebra.web.html5.util.inputmethod.InputMethodSetting;
import org.geogebra.web.html5.util.inputmethod.InputMethodSettingsProvider;

import jsinterop.base.JsPropertyMap;

/**
 * Resolves an input method's declared settings from {@link BrowserStorage},
 * prompting once via {@link InputMethodSettingsDialog} for anything missing so
 * it isn't asked again on this browser.
 */
public class InputMethodSettingsManager implements InputMethodSettingsProvider {

  private final AppW app;

  /**
   * @param app application, needed to show the settings dialog
   */
  public InputMethodSettingsManager(AppW app) {
    this.app = app;
  }

  @Override
  public void withSettings(InputMethodDescriptor descriptor,
      Consumer<JsPropertyMap<String>> callback) {
    InputMethodSetting[] declared = descriptor.settings;
    if (declared == null || declared.length == 0) {
      callback.accept(JsPropertyMap.of());
      return;
    }
    JsPropertyMap<String> stored = readStored(descriptor, declared);
    if (stored != null) {
      callback.accept(stored);
      return;
    }
    new InputMethodSettingsDialog(app, descriptor, values -> {
      JsPropertyMap<String> resolved = JsPropertyMap.of();
      values.forEach((key, value) -> {
        BrowserStorage.LOCAL.setItem(storageKey(descriptor, key), value);
        resolved.set(key, value);
      });
      callback.accept(resolved);
    }).show();
  }

  /**
   * @return all declared settings read from storage, or {@code null} if any of
   *         them is missing (in which case the user has to be asked again)
   */
  private JsPropertyMap<String> readStored(InputMethodDescriptor descriptor,
      InputMethodSetting[] declared) {
    JsPropertyMap<String> stored = JsPropertyMap.of();
    for (InputMethodSetting setting : declared) {
      String value = BrowserStorage.LOCAL.getItem(storageKey(descriptor, setting.key));
      if (value == null || value.isEmpty()) {
        return null;
      }
      stored.set(setting.key, value);
    }
    return stored;
  }

  private static String storageKey(InputMethodDescriptor descriptor, String settingKey) {
    return "inputMethod." + descriptor.id + "." + settingKey;
  }
}
