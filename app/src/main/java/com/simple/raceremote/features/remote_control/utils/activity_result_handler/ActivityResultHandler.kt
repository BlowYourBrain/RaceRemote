package com.simple.raceremote.features.remote_control.utils.activity_result_handler

import android.content.Intent

//todo remove, reason: not used
/**
 * Handle activity result
 *
 * @param onSuccess invokes when activity requestCode has been matched and intent was successfully handled.
 * @param onFailure invokes only if activity result requestCode has been matched but request hasn't been handled.
 *
 * If request code was matched then returns true doesn't matter which callback [onSuccess] or [onFailure] return.
 * If request code wasn't matched then do nothing and [handle] returns false.
 * */
abstract class ActivityResultHandler<R>(
    val onSuccess: (R) -> Unit = {},
    val onFailure: () -> Unit = {}
) {

    /**
     * @return true if result is handled, false otherwise
     * */
    abstract fun handle(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean

}