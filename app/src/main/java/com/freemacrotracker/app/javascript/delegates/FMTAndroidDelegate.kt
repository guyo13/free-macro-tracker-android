// Copyright (c) 2020, Guy Or Please see the AUTHORS file for details.
// All rights reserved. Use of this source code is governed by a GNU GPL
// license that can be found in the LICENSE file.

package com.freemacrotracker.app.javascript.delegates

import com.freemacrotracker.app.model.FMTAlertLevel
import com.freemacrotracker.app.model.FMTFileData

interface FMTAndroidDelegate {
    fun FMTShowAlert(msg: String, level: FMTAlertLevel)
    fun FMTShowAlertBar(msg: String)
    fun FMTExportData(data: FMTFileData)
    fun FMTImportData()
    fun FMTShowPrompt(msg: String, level: FMTAlertLevel)
    fun FMTFinishedLoading()
}