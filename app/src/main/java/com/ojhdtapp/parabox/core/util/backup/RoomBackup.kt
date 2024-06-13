package com.ojhdtapp.parabox.core.util.backup

import androidx.compose.runtime.staticCompositionLocalOf
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

val LocalRoomBackup = staticCompositionLocalOf<RoomBackup> {
    error("No RoomBackup provided!")
}