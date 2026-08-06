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
 * One user-supplied setting declared by an input method (e.g. a credential),
 * as published by the adapter in {@code descriptor.settings}.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class InputMethodSetting {

  /** storage key, unique within the input method */
  public String key;

  /** human readable field label */
  public String label;

  /** whether the value should be masked on input; may be undefined */
  public Object secret;
}
