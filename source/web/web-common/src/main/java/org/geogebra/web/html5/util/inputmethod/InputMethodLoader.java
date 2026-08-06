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

package org.geogebra.web.html5.util.inputmethod;

import java.util.function.Consumer;

import org.geogebra.gwtutil.JavaScriptInjector;
import org.geogebra.gwtutil.ScriptLoadCallback;

/**
 * Loads an input method adapter script from a configured URL and hands back the
 * descriptor it registers. The script is fetched at most once, on first use.
 */
public final class InputMethodLoader {

  private static boolean scriptRequested = false;

  private InputMethodLoader() {
    // utility class
  }

  /**
   * @param adapterUrl URL of the adapter script
   * @param onReady called with the registered input method
   * @param onError called with a message if the script cannot be loaded
   */
  public static void load(String adapterUrl, Consumer<InputMethodDescriptor> onReady,
      Consumer<String> onError) {
    InputMethodRegistry.install();
    InputMethodRegistry.whenRegistered(onReady);
    if (scriptRequested) {
      return;
    }
    scriptRequested = true;
    JavaScriptInjector.loadJS(adapterUrl, new ScriptLoadCallback() {
      @Override
      public void onLoad() {
        // the adapter registers itself; nothing to do here
      }

      @Override
      public void onError() {
        scriptRequested = false;
        onError.accept("Failed to load input method adapter: " + adapterUrl);
      }

      @Override
      public void cancel() {
        // no-op: nothing to cancel, load either succeeds or fails
      }
    });
  }
}
