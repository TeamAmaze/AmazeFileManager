package com.amaze.filemanager.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract

/**
 * Custom [ActivityResultContract] with specifically input = [Intent] and output = [ActivityResult]
 * to allow arguments that is added to the intent to be accessible at the activity result.
 */
class StartActivityForResultWithSourceIntent : ActivityResultContract<Intent, ActivityResult>() {
    private var sourceArguments: Bundle? = null

    override fun createIntent(
        context: Context,
        input: Intent,
    ): Intent {
        sourceArguments = Bundle(input.extras)
        return input
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): ActivityResult =
        ActivityResult(
            resultCode,
            // If source argument is not null, return them to the requestor.
            // If activity result is null, we create an empty intent to store the arguments
            if (sourceArguments != null) {
                intent.also {
                    it?.putExtras(sourceArguments!!)
                } ?: Intent().putExtras(sourceArguments!!)
            } else {
                intent
            },
        )
}
