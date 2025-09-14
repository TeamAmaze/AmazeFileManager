package com.amaze.filemanager.utils.omh

import com.amaze.filemanager.fileoperations.exceptions.CloudPluginException
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.openmobilehub.android.auth.core.OmhAuthClient
import com.openmobilehub.android.auth.core.OmhCredentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ProtocolException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Callback interface for triggering authentication when needed.
 */
interface AuthTrigger {
    /**
     * Triggers authentication for the specified OpenMode.
     * @param openMode The cloud service to authenticate with
     * @return true if authentication was successful, false otherwise
     */
    fun triggerAuthBlocking(openMode: OpenMode): Boolean
}

private val LOG: Logger = LoggerFactory.getLogger(OmhAuthClient::class.java)

private val authExecutor =
    Executors.newSingleThreadExecutor {
        Thread(it, "OmhAuth").apply {
            isDaemon = true
        }
    }

/**
 * Check if the exception has a [ProtocolException] anywhere in its cause chain.
 */
private fun hasProtocolExceptionCause(e: Throwable): Boolean {
    var cause: Throwable? = e.cause
    while (cause != null) {
        if (cause is ProtocolException) return true
        cause = cause.cause
    }
    return false
}

private fun defaultRefreshAction(openMode: OpenMode): () -> Unit =
    {
        val omhAuthClient = OMHClientHelper.getAuthClient(openMode)
        omhAuthClient.getCredentials().blockingRefreshAccessToken()
    }

/**
 * Blocking version of [retryOnUnauthorized], for better interoperability with Java.
 *
 * This calls [retryOnUnauthorizedBlocking()] with default maxRetries of 2
 * and default refreshAction.
 */
fun <T> retryOnUnauthorizedBlocking(
    openMode: OpenMode,
    trigger: AuthTrigger,
    action: () -> T,
): T =
    runBlocking {
        retryOnUnauthorized(
            openMode,
            2,
            trigger,
            defaultRefreshAction(openMode),
        ) { action() }
    }

/**
 * Blocking version of [retryOnUnauthorized], for better interoperability with Java.
 */
fun <T> retryOnUnauthorizedBlocking(
    openMode: OpenMode,
    maxRetries: Int = 2,
    trigger: AuthTrigger,
    refreshAction: () -> Unit = defaultRefreshAction(openMode),
    action: () -> T,
): T =
    runBlocking {
        retryOnUnauthorized(
            openMode,
            maxRetries,
            trigger,
            refreshAction,
        ) { action() }
    }

/**
 * Retries the given [action] up to [maxRetries] times if it throws a [CloudPluginException]
 * caused by an unauthorized access (e.g., expired token). It attempts to refresh the access token
 * using the provided [refreshAction] and triggers re-authentication via [trigger] if necessary.
 *
 * @param openMode The cloud service to authenticate with
 * @param maxRetries The maximum number of retry attempts (default is 2)
 * @param trigger The [AuthTrigger] to invoke for re-authentication
 * @param refreshAction The action to refresh the access token (default implementation provided)
 * @param action The suspend function to execute that may throw [CloudPluginException]
 * @return The result of the successful [action]
 * @throws CloudPluginException if all retry attempts fail
 */
@Suppress("LongMethod", "TooGenericExceptionCaught")
suspend fun <T> retryOnUnauthorized(
    openMode: OpenMode,
    maxRetries: Int = 2,
    trigger: AuthTrigger,
    refreshAction: () -> Unit = defaultRefreshAction(openMode),
    action: suspend () -> T,
): T {
    var attempt = 1
    var lastException: Exception? = null

    while (attempt <= maxRetries) {
        try {
            return action()
        } catch (e: CancellationException) {
            throw e
        } catch (e: CloudPluginException) {
            lastException = e
            if (hasProtocolExceptionCause(e)) {
                if (attempt == maxRetries) {
                    LOG.debug("Max attempts reached, attempting re-authentication")

                    // Run auth on a completely separate thread, no coroutines involved
                    val authFuture = CompletableFuture<Boolean>()
                    authExecutor.execute {
                        try {
                            val result = trigger.triggerAuthBlocking(openMode)
                            authFuture.complete(result)
                        } catch (ex: Exception) {
                            authFuture.completeExceptionally(ex)
                        }
                    }

                    val authSuccess =
                        try {
                            authFuture.get(120, TimeUnit.SECONDS) // 2 min timeout for browser auth
                        } catch (ex: Exception) {
                            LOG.warn("Auth trigger failed", ex)
                            false
                        }

                    if (!authSuccess) {
                        throw e
                    }
                    return action()
                }

                LOG.debug("Token unauthorized, attempting to refresh token (attempt $attempt/$maxRetries)")
                // Run refresh on separate thread
                val refreshFuture = CompletableFuture<Unit>()
                authExecutor.execute {
                    try {
                        refreshAction()
                        refreshFuture.complete(Unit)
                    } catch (ex: Exception) {
                        refreshFuture.completeExceptionally(ex)
                    }
                }
                refreshFuture.get(30, TimeUnit.SECONDS)
                attempt++
            } else {
                if (attempt == maxRetries) {
                    throw e // Don't wrap again, throw original exception
                }

                LOG.debug("Network error, retrying operation (attempt $attempt/$maxRetries)")
                delay(500L * attempt) // Exponential backoff

                attempt++
            }
        } catch (e: Exception) {
            LOG.warn("Unexpected exception in retryOnUnauthorized", e)
            throw e
        }
    }
    throw lastException ?: error("Unreachable code reached in retryOnUnauthorized")
}

/**
 * Borrowed from omh-storage, to resume an ongoing coroutine after access token is refreshed.
 */
@Suppress("TooGenericExceptionCaught")
fun OmhCredentials.blockingRefreshAccessToken(): String? {
    val future = CompletableFuture<String?>()

    val cancellable =
        refreshAccessToken()
            .addOnSuccess { result -> future.complete(result) }
            .addOnFailure { e -> future.completeExceptionally(e) }
            .execute()

    return try {
        future.get(30, TimeUnit.SECONDS)
    } catch (e: Exception) {
        cancellable.cancel()
        throw e
    }
}
