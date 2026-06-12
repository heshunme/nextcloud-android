/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.model

import androidx.room.ColumnInfo
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

data class FolderChildState(
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Long,
    @ColumnInfo(name = ProviderTableMeta.FILE_PATH)
    val path: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_PATH_DECRYPTED)
    val pathDecrypted: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_ENCRYPTED_NAME)
    val encryptedName: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_ETAG)
    val etag: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_ETAG_ON_SERVER)
    val etagOnServer: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_STORAGE_PATH)
    val storagePath: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_CONTENT_TYPE)
    val contentType: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_MODIFIED)
    val modified: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_FAVORITE)
    val favorite: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_SHARED_VIA_LINK)
    val sharedViaLink: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_SHARED_WITH_SHAREE)
    val sharedWithSharee: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILE_METADATA_LIVE_PHOTO)
    val metadataLivePhoto: String?,
    @ColumnInfo(name = ProviderTableMeta.FILE_E2E_COUNTER)
    val e2eCounter: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILE_LOCAL_ID)
    val localId: Long
)
