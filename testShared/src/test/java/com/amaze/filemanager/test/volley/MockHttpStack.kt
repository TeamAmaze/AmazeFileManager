package com.amaze.filemanager.test.volley

import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.toolbox.BaseHttpStack
import com.android.volley.toolbox.HttpResponse
import java.io.IOException

/**
 * Mock [BaseHttpStack] for test only.
 */
class MockHttpStack : BaseHttpStack() {

    private lateinit var mResponseToReturn: HttpResponse
    private lateinit var lastUrl: String
    private lateinit var mLastHeaders: MutableMap<String, String>
    private var lastPostBody: ByteArray? = null

    /**
     * get headers in last request
     */
    fun getLastHeaders() = mLastHeaders

    /**
     * Manually set response to return
     */
    fun setResponseToReturn(response: HttpResponse) {
        mResponseToReturn = response
    }

    @Throws(IOException::class, AuthFailureError::class)
    override fun executeRequest(
        request: Request<*>,
        additionalHeaders: Map<String, String>?
    ): HttpResponse {
        lastUrl = request.url
        mLastHeaders = HashMap()
        if (request.headers != null) {
            mLastHeaders.putAll(request.headers)
        }
        if (additionalHeaders != null) {
            mLastHeaders.putAll(additionalHeaders)
        }
        try {
            lastPostBody = request.body
        } catch (e: AuthFailureError) {
            lastPostBody = null
        }
        return mResponseToReturn
    }
}
