/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.fabric;

import com.facebook.proguard.annotations.DoNotStrip;

@DoNotStrip
public interface ComponentNameResolver {

  /* returns a list of all the component names that are registered in React Native. */
  String[] getComponentNames();
}
