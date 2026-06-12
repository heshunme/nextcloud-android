/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val DATABASE_VERSION_100 = 100
private const val DATABASE_VERSION_101 = 101

val MIGRATION_100_101 = object : Migration(DATABASE_VERSION_100, DATABASE_VERSION_101) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_filelist_file_owner_path
            ON filelist(file_owner, path)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_filelist_file_owner_parent
            ON filelist(file_owner, parent)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_filelist_file_owner_parent_filename
            ON filelist(file_owner, parent, filename)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_filelist_file_owner_remote_id
            ON filelist(file_owner, remote_id)
            """.trimIndent()
        )
    }
}
