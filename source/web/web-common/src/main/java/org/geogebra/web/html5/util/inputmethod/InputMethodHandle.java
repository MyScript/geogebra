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

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Live input method instance returned by
 * {@link InputMethodDescriptor#mount(elemental2.dom.Element, InputMethodContext)}.
 *
 * <p>Declared as a plain native {@code Object} rather than bound to a named
 * constructor: the adapter is free to return any object shaped like this, and a
 * name-bound native type would fail J2CL's native instanceof check.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class InputMethodHandle {

  /** discards the current input (e.g. erases the ink canvas) */
  public native void clear();

  /** re-measures the mount element after a layout change */
  public native void resize();

  /** releases the instance and any session it holds */
  public native void destroy();
}
