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

import jsinterop.base.JsPropertyMap;

/**
 * Resolves the settings an input method declares, prompting the user for any
 * that are missing and persisting the answers.
 */
public interface InputMethodSettingsProvider {

  /**
   * @param descriptor input method whose {@code settings} are to be resolved
   * @param callback called with the resolved values once all are available;
   *        not called if the user cancels
   */
  void withSettings(InputMethodDescriptor descriptor,
      Consumer<JsPropertyMap<String>> callback);
}
