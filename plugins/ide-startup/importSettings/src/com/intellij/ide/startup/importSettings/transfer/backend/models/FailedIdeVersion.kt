// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.startup.importSettings.models

import com.intellij.ide.startup.importSettings.TransferableIdeId
import org.jetbrains.annotations.Nls
import javax.swing.Icon

class FailedIdeVersion(
  val transferableId: TransferableIdeId,
  id: String,
  icon: Icon,
  name: String,
  subName: String? = null,
  @Nls val potentialReason: String? = null,
  @Nls var stepsToFix: String? = null,
  val canBeRetried: Boolean = true,
  val throwable: Throwable? = null
) : BaseIdeVersion(id, icon, name, subName)
