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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import elemental2.dom.DomGlobal;
import jsinterop.annotations.JsFunction;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

/**
 * The single extension point for alternative input methods: installs
 * {@code window.GeoGebraInputMethods} so an adapter script loaded at runtime can
 * register itself, without GeoGebra having any build- or compile-time knowledge
 * of it.
 */
public final class InputMethodRegistry {

  private static final String GLOBAL_NAME = "GeoGebraInputMethods";

  private static final List<InputMethodDescriptor> registered = new ArrayList<>();
  private static final List<Consumer<InputMethodDescriptor>> waiting = new ArrayList<>();
  private static boolean installed = false;

  private InputMethodRegistry() {
    // utility class
  }

  /**
   * Registration function exposed to JavaScript.
   */
  @JsFunction
  interface RegisterFunction {
    void register(InputMethodDescriptor descriptor);
  }

  /**
   * Installs the global registry object, once per page.
   */
  public static void install() {
    if (installed) {
      return;
    }
    installed = true;
    JsPropertyMap<Object> api = JsPropertyMap.of();
    api.set("register", (RegisterFunction) InputMethodRegistry::register);
    Js.asPropertyMap(DomGlobal.window).set(GLOBAL_NAME, api);
  }

  private static void register(InputMethodDescriptor descriptor) {
    if (descriptor == null) {
      return;
    }
    registered.add(descriptor);
    List<Consumer<InputMethodDescriptor>> pending = new ArrayList<>(waiting);
    waiting.clear();
    pending.forEach(callback -> callback.accept(descriptor));
  }

  /**
   * @param callback called with the first registered input method, immediately if
   *        one is already registered, otherwise as soon as one registers
   */
  public static void whenRegistered(Consumer<InputMethodDescriptor> callback) {
    if (!registered.isEmpty()) {
      callback.accept(registered.get(0));
    } else {
      waiting.add(callback);
    }
  }
}
