package com.moncake94.pos

import android.app.Application
import com.moncake94.pos.data.AppDatabase
import com.moncake94.pos.data.BackupRepository
import com.moncake94.pos.data.PosRepository
import com.moncake94.pos.printer.PrinterService

class MoncakePosApp : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { PosRepository(database) }
    val backupRepository by lazy { BackupRepository(this, database) }
    val printerService by lazy { PrinterService(this) }
}
