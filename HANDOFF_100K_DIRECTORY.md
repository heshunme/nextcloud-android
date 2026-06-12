<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# 100k Directory Handoff

This handoff records the current state of the large single-directory work. The original target is to make ordinary and
E2E encrypted directories with 100,000+ direct children refresh, persist, open, sort, and scroll without OOM.

## Current Scope Completed

This checkout contains an app-side foundation slice only. It does not yet implement the full streaming refresh and
Paging UI architecture.

Completed changes:

- Installed local build prerequisites on this machine:
  - Android SDK at `/root/android-sdk`
  - `platforms;android-36`
  - `build-tools;36.0.0`
  - `platform-tools`
  - `ndk;29.0.14206865`
  - `cmake;4.1.2`
  - `openjdk-17-jdk`
  - local `local.properties` with `sdk.dir=/root/android-sdk`; this file is ignored by git
- Added verified Paging dependencies:
  - `androidx.paging:paging-runtime:3.5.0`
  - `androidx.room:room-paging:2.8.4`
- Updated Gradle dependency verification metadata to trust the `androidx.paging` group.
- Bumped `ProviderMeta.DB_VERSION` from `100` to `101`.
- Added Room indices to `FileEntity` / `filelist`:
  - `file_owner, path`
  - `file_owner, parent`
  - `file_owner, parent, filename`
  - `file_owner, remote_id`
- Added manual `MIGRATION_100_101` to create those indices.
- Registered `MIGRATION_100_101` in `NextcloudDatabase`.
- Generated exported Room schema `app/schemas/com.nextcloud.client.database.NextcloudDatabase/101.json`.
- Added `MigrationTest.migrate100to101()` to validate the new indices.
- Added `FileDao` primitives needed by later streamed sync and paged list work:
  - `getFolderContentPagingSource(parentId, fileOwner)`
  - `getFolderContentPage(parentId, fileOwner, limit, offset)`
  - `getFilesByEncryptedRemotePaths(paths, fileOwner)`
  - `getFolderChildStatesByEncryptedRemotePaths(paths, fileOwner)`
  - `getFolderChildrenNotInRemotePaths(parentId, fileOwner, remotePaths, limit)`
  - `deleteFilesByIds(fileOwner, ids)`
  - `insertOrReplace(entities)`
  - `upsertBatch(entities)`
- Added lightweight projection `FolderChildState` for batch merge lookups without loading full `FileEntity` rows.

## Verification Already Run

These commands passed on this machine after lowering Gradle memory because the host has only about 3.6 GiB RAM:

```bash
export ANDROID_HOME=/root/android-sdk
export ANDROID_SDK_ROOT=/root/android-sdk
export PATH=/root/android-sdk/cmdline-tools/latest/bin:/root/android-sdk/platform-tools:$PATH

./gradlew :app:kspGplayDebugKotlin \
  --no-daemon \
  --no-configuration-cache \
  -Dorg.gradle.jvmargs='-Xmx2g -Dfile.encoding=UTF-8 -XX:+UseParallelGC -XX:MaxMetaspaceSize=768m' \
  -Dorg.gradle.workers.max=1

./gradlew :app:spotlessKotlinCheck :app:detekt \
  --no-daemon \
  --no-configuration-cache \
  -Dorg.gradle.jvmargs='-Xmx2g -Dfile.encoding=UTF-8 -XX:+UseParallelGC -XX:MaxMetaspaceSize=768m' \
  -Dorg.gradle.workers.max=1
```

Notes:

- `detekt` completed successfully after fixing this change's `MagicNumber` findings.
- `detekt` still prints existing findings in unrelated files:
  - `app/src/main/java/com/owncloud/android/ui/asynctasks/FetchRemoteFileTask.kt`
  - `app/src/main/java/com/owncloud/android/ui/fragment/contactsbackup/BackupFragment.kt`
- `:app:compileGplayDebugAndroidTestKotlin` and `:app:kspGplayDebugAndroidTestKotlin` were attempted but did not finish
  on this low-memory host. Both reached main-source Kotlin compilation, then the Kotlin/Gradle daemon disappeared.

## Not Completed Yet

The actual 100k no-OOM target is not complete. The current refresh path still uses:

```java
new ReadFolderRemoteOperation(remotePath).execute(client)
```

inside `RefreshFolderOperation.fetchAndSyncRemoteFolder()`. That operation comes from the external
`nextcloud-android-library` dependency and still returns a complete `RemoteOperationResult.data` list. As long as this
path materializes the full remote directory, a 100,000 child directory can still OOM before app-side database batching
gets a chance to help.

Major unfinished areas:

- Add a streaming WebDAV directory API in `nextcloud-android-library`.
- Update this app to consume the streaming API from `RefreshFolderOperation`.
- Replace `saveFolder(folder, updatedFiles, filesToRemove)` as the main large-directory path with batched transaction
  writes and batched delete side effects.
- Add E2E metadata streaming or at least a lightweight metadata index.
- Migrate file list UI from full `List<OCFile>` loading/sorting to Paging 3 and SQL-backed sorting/filtering.
- Add 100k stress tests for ordinary and E2E directories.

## Next Plan

### 1. Create a library branch for streaming WebDAV

Repository expected by current app dependency:

```toml
androidLibraryVersion = "1f476c0ab14fb280172be2aefd70db80e50bb17b"
android-library = { module = "com.github.nextcloud:android-library", version.ref = "androidLibraryVersion" }
```

Work in the library first:

- Add a callback interface similar to:

```java
public interface RemoteFileSink {
    void onFolder(RemoteFile folder) throws IOException;
    void onChildrenBatch(List<RemoteFile> children) throws IOException;
}
```

- Add `StreamingReadFolderRemoteOperation` or an explicit streaming constructor on `ReadFolderRemoteOperation`.
- Do not call `PropFindMethod.getResponseBodyAsMultiStatus()` in the streaming implementation.
- Parse the WebDAV multistatus response from the response stream with a pull or SAX parser.
- Convert each `<d:response>` to `RemoteFile`.
- Treat the first response as the folder and following responses as direct children.
- Flush child callbacks in fixed-size batches; default batch size should be `500`.
- Keep old `ReadFolderRemoteOperation` behavior compatible. It may reuse the streaming parser internally and collect
  results into a list for the legacy API.

Library tests to add:

- Generate multistatus XML with 100,000 responses.
- Verify the sink receives the folder once and child batches no larger than the configured batch size.
- Verify legacy `ReadFolderRemoteOperation` result order and key fields remain compatible.

After pushing the library branch, update this app's `androidLibraryVersion` to the new fork commit.

### 2. Add app-side streaming synchronizer

Add a dedicated class, for example `FolderRefreshSynchronizer`, called by `RefreshFolderOperation`.

Required behavior:

- Refresh starts by reloading only the local folder row.
- Fetch E2E metadata only if the folder is encrypted.
- Do not call `fileDataStorageManager.getFolderContent(mLocalFolder, false)` for the main large-directory path.
- For each remote batch:
  - Convert remote files to `OCFile` or `FileEntity` batch.
  - Build the remote path list for the batch.
  - Use `FileDao.getFolderChildStatesByEncryptedRemotePaths(paths, fileOwner)` for local merge state.
  - Merge local-only fields such as ids, local etags, local paths, favorites, share flags, live photo metadata, and
    E2E counters.
  - Persist the batch in a transaction with a bounded batch size, preferably `500`.
- Track seen children with a mark-and-sweep strategy:
  - Add a sync marker column if needed, or use a safe existing marker only if it cannot conflict with existing behavior.
  - After all remote batches are processed, delete local children under `parent + file_owner` not seen in this run.
  - Delete local files and trigger media scans in batches, not with one giant `Collection<OCFile>`.
- Keep `synchronizeData(List<Object>)` only for compatibility/tests, not the main refresh path.

Important current bottlenecks:

- `RefreshFolderOperation.synchronizeData()` builds `updatedFiles` for the entire directory.
- It preloads all local children into `localFilesMap`.
- It calls `fileDataStorageManager.saveFolder(remoteFolder, updatedFiles, localFilesMap.values())`, which builds one
  large `ArrayList<ContentProviderOperation>`.
- It may call `getFileByPath()` inside the remote child loop.

Those are the app-side areas that must be replaced for the 100k target.

### 3. Add E2E metadata index

Current E2E metadata handling can still hold large maps. Add a small lookup abstraction:

```kotlin
interface EncryptedMetadataIndex {
    fun counter(): Long
    fun lookupByEncryptedName(encryptedName: String): DecryptedFile?
    fun lookupRemotePath(remoteParentPath: String, encryptedName: String, isFolder: Boolean): String
}
```

Preferred implementation:

- Use Gson streaming parser for metadata JSON.
- Store only `encryptedName -> small decrypted metadata` required for batch merge.
- If streaming parsing is not stable for both metadata versions, keep the full parse temporarily but document it as the
  remaining E2E memory bottleneck and add a focused stress test.

### 4. Move list UI to Paging 3

Current `OCFileListAdapterHelper.prepareFileList()` loads all folder children, filters, merges live photos, adds offline
operations, and sorts in memory.

Next steps:

- Add SQL-backed paged queries that include:
  - hidden filter
  - temp file filter
  - mime filter
  - root personal-only filter
  - folders-before-files
  - favorites-first
  - name/date/size sorting
- Migrate `OCFileListAdapter` from `RecyclerView.Adapter` with `mFiles`/`mFilesAll` to `PagingDataAdapter`.
- Treat `getFiles()`, `mFilesAll`, and `getFileByRemoteId()` as current loaded snapshot only, or replace event updates
  with DAO writes plus Paging invalidation.
- Ensure fast-scroll popup text comes from loaded items; exact counts should come from SQL `COUNT(*)`.

### 5. Tests and acceptance

After library and app streaming are wired:

- Unit test streamed sync with 100,000 fake remote children split into batches.
- Assert no per-file `getFileByPath()` calls in the batch loop.
- Assert upserts and deletes are batched.
- Run migration validation for `100 -> 101` on a machine/emulator with enough memory.
- Add UI/paging test that inserts 100,000 local `filelist` rows and opens the directory without materializing all rows.
- Manual/performance acceptance:
  - ordinary directory with 100,000 direct children refreshes without OOM
  - list opens and scrolls without OOM
  - sort changes do not build a 100,000 item in-memory list
  - E2E encrypted directory with 100,000 direct children refreshes without OOM

## Suggested Commit Split

Do not create one large commit for the whole future implementation. Recommended split for the current working tree:

1. Database indexing and migration
   - `ProviderMeta.java`
   - `FileEntity.kt`
   - `Migration100to101.kt`
   - `NextcloudDatabase.kt`
   - `101.json`
   - `MigrationTest.kt`

2. DAO primitives and lightweight projection
   - `FileDao.kt`
   - `FolderChildState.kt`

3. Paging dependency setup
   - `gradle/libs.versions.toml`
   - `app/build.gradle.kts`
   - `gradle/verification-metadata.xml`

4. Handoff document
   - `HANDOFF_100K_DIRECTORY.md`

## Commit Notes

This fork's `AGENTS.md` has been updated to use fork-local contribution rules:

- DCO `Signed-off-by` trailers are not required for this fork unless the repository owner asks for them.
- `Assisted-by` trailers are not required unless the repository owner asks for them.
- Agents may commit and push when the repository owner asks for that workflow.

Example commit commands if manual commits are needed later:

```bash
git add app/src/main/java/com/owncloud/android/db/ProviderMeta.java \
  app/src/main/java/com/nextcloud/client/database/entity/FileEntity.kt \
  app/src/main/java/com/nextcloud/client/database/migrations/Migration100to101.kt \
  app/src/main/java/com/nextcloud/client/database/NextcloudDatabase.kt \
  app/schemas/com.nextcloud.client.database.NextcloudDatabase/101.json \
  app/src/androidTest/java/com/nextcloud/client/database/migrations/MigrationTest.kt

git commit -m "feat(files): add filelist indices for large folders"

git add app/src/main/java/com/nextcloud/client/database/dao/FileDao.kt \
  app/src/main/java/com/nextcloud/client/database/model/FolderChildState.kt

git commit -m "feat(files): add batched folder child DAO queries"

git add gradle/libs.versions.toml app/build.gradle.kts gradle/verification-metadata.xml

git commit -m "build(files): add paging dependencies"

git add HANDOFF_100K_DIRECTORY.md

git commit -m "docs(files): document large directory handoff"
```

Then push to the contributor's branch:

```bash
git push origin HEAD:<branch-name>
```
