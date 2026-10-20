package com.cooper.lithic.data.backup

/** A process-local capability; it is never reconstructed from a URI or SavedState. */
internal data class RestorePreview(val token:String,val backupCounts:Map<String,Long>,val currentCounts:Map<String,Long>,val createdAt:String,val recordZoneId:String)
internal sealed interface RestoreResult {
    data object Success:RestoreResult
    data class Failed(val message:String,val currentPreserved:Boolean):RestoreResult
}
internal data class PreparedGeneration internal constructor(val id:String,val directory:java.io.File,internal val nonce:String)
