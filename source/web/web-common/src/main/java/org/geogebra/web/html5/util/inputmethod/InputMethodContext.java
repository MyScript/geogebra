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

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

/**
 * Everything GeoGebra hands to an input method when mounting it: the resolved
 * settings and the callbacks it reports back through.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class InputMethodContext {

  /** resolved values of {@link InputMethodDescriptor#settings}, keyed by setting key */
  public JsPropertyMap<String> settings;

  /** called with the recognized content (LaTeX) every time the input method produces one */
  public ResultCallback onResult;

  /** called with a human readable message when the input method fails */
  public ErrorCallback onError;

  /**
   * Recognition result callback.
   */
  @JsFunction
  public interface ResultCallback {
    /**
     * @param latex recognized content, as LaTeX
     */
    void onResult(String latex);
  }

  /**
   * Failure callback.
   */
  @JsFunction
  public interface ErrorCallback {
    /**
     * @param message human readable error message
     */
    void onError(String message);
  }

  /**
   * @param settings resolved setting values
   * @param onResult recognition callback
   * @param onError failure callback
   * @return a new context object
   */
  @JsOverlay
  public static InputMethodContext create(JsPropertyMap<String> settings,
      ResultCallback onResult, ErrorCallback onError) {
    InputMethodContext context = Js.uncheckedCast(JsPropertyMap.of());
    context.settings = settings;
    context.onResult = onResult;
    context.onError = onError;
    return context;
  }
}
