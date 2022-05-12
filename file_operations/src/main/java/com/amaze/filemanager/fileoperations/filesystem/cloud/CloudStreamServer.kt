/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.amaze.filemanager.fileoperations.filesystem.cloud

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.amaze.filemanager.fileoperations.filesystem.smbstreamer.Streamer
import java.io.*
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple, tiny, nicely embeddable HTTP 1.0 (partially 1.1) server in Java
 *
 *
 * NanoHTTPD version 1.24, Copyright  2001,2005-2011 Jarno Elonen (elonen@iki.fi,
 * http://iki.fi/elonen/) and Copyright  2010 Konstantinos Togias (info@ktogias.gr,
 * http://ktogias.gr)
 *
 *
 * **Features + limitations: **
 *
 *
 *  * Only one Java file
 *  * Java 1.1 compatible
 *  * Released as open source, Modified BSD licence
 *  * No fixed config files, logging, authorization etc. (Implement yourself if you need them.)
 *  * Supports parameter parsing of GET and POST methods
 *  * Supports both dynamic content and file serving
 *  * Supports file upload (since version 1.2, 2010)
 *  * Supports partial content (streaming)
 *  * Supports ETags
 *  * Never caches anything
 *  * Doesn't limit bandwidth, request time or simultaneous connections
 *  * Default code serves files and shows all HTTP parameters and headers
 *  * File server supports directory listing, index.html and index.htm
 *  * File server supports partial content (streaming)
 *  * File server supports ETags
 *  * File server does the 301 redirection trick for directories without '/'
 *  * File server supports simple skipping for files (continue download)
 *  * File server serves also very long files without memory overhead
 *  * Contains a built-in list of most common mime types
 *  * All header names are converted lowercase so they don't vary between browsers/clients
 *
 *
 *
 * **Ways to use: **
 *
 *
 *  * Run as a standalone app, serves files and shows requests
 *  * Subclass serve() and embed to your own program
 *  * Call serveFile() from serve() with your own base directory
 *
 *
 * See the end of the source file for distribution license (Modified BSD licence)
 */
abstract class CloudStreamServer {
    // ==================================================
    // API parts
    // ==================================================
    /**
     * Override this to customize the server.
     *
     *
     * (By default, this delegates to serveFile() and allows directory listing.)
     *
     * @param uri Percent-decoded URI without parameters, for example "/index.cgi"
     * @param method "GET", "POST" etc.
     * @param parms Parsed, percent decoded parameters from URI and, in case of POST, data.
     * @param header Header entries, percent decoded
     * @return HTTP response, see class Response for details
     */
    abstract fun serve(
        uri: String?, method: String?, header: Properties?, parms: Properties?, files: Properties?
    ): Response?

    /** HTTP response. Return one of these from serve().  */
    inner class Response {
        /** Default constructor: response = HTTP_OK, data = mime = 'null'  */
        constructor() {
            status = HTTP_OK
        }

        /** Basic constructor.  */
        constructor(status: String, mimeType: String?, data: CloudStreamSource?) {
            this.status = status
            this.mimeType = mimeType
            this.data = data
        }

        /** Adds given line to the header.  */
        fun addHeader(name: String, value: String) {
            header[name] = value
        }

        /** HTTP status code after processing, e.g. "200 OK", HTTP_OK  */
        var status: String

        /** MIME type of content, e.g. "text/html"  */
        var mimeType: String? = null

        /** Data of the response, may be null.  */
        var data: CloudStreamSource? = null

        /** Headers for the HTTP response. Use addHeader() to add lines.  */
        var header = Properties()
    }
    // ==================================================
    // Socket & server code
    // ==================================================
    /**
     * Starts a HTTP server to given port.
     *
     *
     * Throws an IOException if the socket is already in use
     */
    // private HTTPSession session;
    constructor(port: Int, wwwroot: File?) {
        myTcpPort = port
        myServerSocket = tryBind(myTcpPort)
        myThread = Thread {
            try {
                while (true) {
                    /*
                  if(session!=null){
                      session.interrupt();
                        try {
                          session.join();
                        } catch (InterruptedException e) {
                              e.printStackTrace();
                        }
                    }
                  */
                    val accept = myServerSocket.accept()
                    HTTPSession(accept)
                }
            } catch (ioe: IOException) {
            }
        }
        myThread.isDaemon = true
        myThread.start()
    }

    constructor(wwwroot: File?) {
        myServerSocket = tryBind(myTcpPort)
        myThread = Thread {
            try {
                while (true) {
                    /*
                    if(session!=null){
                            session.interrupt();
                            try {
                                    session.join();
                            } catch (InterruptedException e) {
                                    e.printStackTrace();
                            }
                    }
                  */
                    val accept = myServerSocket.accept()
                    HTTPSession(accept)
                }
            } catch (ioe: IOException) {
            }
        }
        myThread.isDaemon = true
        myThread.start()
    }

    /** Stops the server.  */
    open fun stop() {
        try {
            myServerSocket.close()
            myThread.join()
        } catch (ioe: IOException) {
        } catch (e: InterruptedException) {
        }
    }

    /**
     * Since CloudStreamServer and Streamer both uses the same port, shutdown the Streamer before
     * acquiring the port.
     *
     * @return ServerSocket
     */
    @Throws(IOException::class)
    private fun tryBind(port: Int): ServerSocket {
        val socket: ServerSocket
        socket = try {
            ServerSocket(port)
        } catch (ifPortIsOccupiedByStreamer: BindException) {
            Streamer.getInstance().stop()
            ServerSocket(port)
        }
        return socket
    }

    /** Handles one session, i.e. parses the HTTP request and returns the response.  */
    private inner class HTTPSession(private val socket: Socket) : Runnable {
        private var `is`: InputStream? = null

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        override fun run() {
            try {
                // openInputStream();
                handleResponse(socket)
            } finally {
                if (`is` != null) {
                    try {
                        `is`!!.close()
                        socket.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }


        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        private fun handleResponse(socket: Socket) {
            try {
                `is` = socket.getInputStream()
                if (`is` == null) return

                // Read the first 8192 bytes.
                // The full header should fit in here.
                // Apache's default header limit is 8KB.
                val bufsize = 8192
                var buf = ByteArray(bufsize)
                var rlen = `is`!!.read(buf, 0, bufsize)
                if (rlen <= 0) return

                // Create a BufferedReader for parsing the header.
                val hbis = ByteArrayInputStream(buf, 0, rlen)
                val hin = BufferedReader(InputStreamReader(hbis, StandardCharsets.UTF_8))
                val pre = Properties()
                val parms = Properties()
                val header = Properties()
                val files = Properties()

                // Decode the header into parms and header java properties
                decodeHeader(hin, pre, parms, header)
                Log.d(TAG, pre.toString())
                Log.d(TAG, "Params: $parms")
                Log.d(TAG, "Header: $header")
                val method = pre.getProperty("method")
                val uri = pre.getProperty("uri")
                var size = 0x7FFFFFFFFFFFFFFFL
                val contentLength = header.getProperty("content-length")
                if (contentLength != null) {
                    try {
                        size = contentLength.toInt().toLong()
                    } catch (ex: NumberFormatException) {
                    }
                }

                // We are looking for the byte separating header from body.
                // It must be the last byte of the first two sequential new lines.
                var splitbyte = 0
                var sbfound = false
                while (splitbyte < rlen) {
                    if (buf[splitbyte].toInt().toChar() == '\r' && buf[++splitbyte].toInt()
                            .toChar() == '\n' && buf[++splitbyte].toInt()
                            .toChar() == '\r' && buf[++splitbyte].toInt().toChar() == '\n'
                    ) {
                        sbfound = true
                        break
                    }
                    splitbyte++
                }
                splitbyte++

                // Write the part of body already read to ByteArrayOutputStream f
                val f = ByteArrayOutputStream()
                if (splitbyte < rlen) f.write(buf, splitbyte, rlen - splitbyte)

                // While Firefox sends on the first read all the data fitting
                // our buffer, Chrome and Opera sends only the headers even if
                // there is data for the body. So we do some magic here to find
                // out whether we have already consumed part of body, if we
                // have reached the end of the data to be sent or we should
                // expect the first byte of the body at the next read.
                if (splitbyte < rlen) size -= (rlen - splitbyte + 1).toLong() else if (!sbfound || size == 0x7FFFFFFFFFFFFFFFL) size =
                    0

                // Now read all the body and write it to f
                buf = ByteArray(512)
                while (rlen >= 0 && size > 0) {
                    rlen = `is`!!.read(buf, 0, 512)
                    size -= rlen.toLong()
                    if (rlen > 0) f.write(buf, 0, rlen)
                }

                // Get the raw body as a byte []
                val fbuf = f.toByteArray()

                // Create a BufferedReader for easily reading it as string.
                val bin = ByteArrayInputStream(fbuf)
                val `in` = BufferedReader(InputStreamReader(bin))

                // If the method is POST, there may be parameters
                // in data section, too, read it:
                if (method.equals("POST", ignoreCase = true)) {
                    var contentType = ""
                    val contentTypeHeader = header.getProperty("content-type")
                    var st = StringTokenizer(contentTypeHeader, "; ")
                    if (st.hasMoreTokens()) {
                        contentType = st.nextToken()
                    }
                    if (contentType.equals("multipart/form-data", ignoreCase = true)) {
                        // Handle multipart/form-data
                        if (!st.hasMoreTokens()) sendError(
                            socket,
                            HTTP_BADREQUEST,
                            "BAD REQUEST: Content type is multipart/form-data but boundary missing. Usage: GET /example/file.html"
                        )
                        val boundaryExp = st.nextToken()
                        st = StringTokenizer(boundaryExp, "=")
                        if (st.countTokens() != 2) sendError(
                            socket,
                            HTTP_BADREQUEST,
                            "BAD REQUEST: Content type is multipart/form-data but boundary syntax error. Usage: GET /example/file.html"
                        )
                        st.nextToken()
                        val boundary = st.nextToken()
                        decodeMultipartData(boundary, fbuf, `in`, parms, files)
                    } else {
                        // Handle application/x-www-form-urlencoded
                        var postLine = ""
                        val pbuf = CharArray(512)
                        var read = `in`.read(pbuf)
                        while (read >= 0 && !postLine.endsWith("\r\n")) {
                            postLine += String(pbuf, 0, read)
                            read = `in`.read(pbuf)
                            if (Thread.interrupted()) {
                                throw InterruptedException()
                            }
                        }
                        postLine = postLine.trim { it <= ' ' }
                        decodeParms(postLine, parms)
                    }
                }

                // Ok, now do the serve()
                val r = serve(uri, method, header, parms, files)
                if (r == null) sendError(
                    socket,
                    HTTP_INTERNALERROR,
                    "SERVER INTERNAL ERROR: Serve() returned a null response."
                ) else sendResponse(socket, r.status, r.mimeType, r.header, r.data)
                `in`.close()
            } catch (ioe: IOException) {
                try {
                    sendError(
                        socket,
                        HTTP_INTERNALERROR,
                        "SERVER INTERNAL ERROR: IOException: " + ioe.message
                    )
                } catch (t: Throwable) {
                }
            } catch (ie: InterruptedException) {
                // Thrown by sendError, ignore and exit the thread.
            }
        }

        /** Decodes the sent headers and loads the data into java Properties' key - value pairs  */
        @Throws(InterruptedException::class)
        private fun decodeHeader(
            `in`: BufferedReader, pre: Properties, parms: Properties, header: Properties
        ) {
            try {
                // Read the request line
                val inLine = `in`.readLine() ?: return
                val st = StringTokenizer(inLine)
                if (!st.hasMoreTokens()) sendError(
                    socket,
                    HTTP_BADREQUEST,
                    "BAD REQUEST: Syntax error. Usage: GET /example/file.html"
                )
                val method = st.nextToken()
                pre["method"] = method
                if (!st.hasMoreTokens()) sendError(
                    socket,
                    HTTP_BADREQUEST,
                    "BAD REQUEST: Missing URI. Usage: GET /example/file.html"
                )
                var uri = st.nextToken()

                // Decode parameters from the URI
                val qmi = uri!!.indexOf('?')
                uri = if (qmi >= 0) {
                    decodeParms(uri.substring(qmi + 1), parms)
                    decodePercent(uri.substring(0, qmi))
                } else Uri.decode(uri) // decodePercent(uri);

                // If there's another token, it's protocol version,
                // followed by HTTP headers. Ignore version but parse headers.
                // NOTE: this now forces header names lowercase since they are
                // case insensitive and vary by client.
                if (st.hasMoreTokens()) {
                    var line = `in`.readLine()
                    while (line != null && line.trim { it <= ' ' }.length > 0) {
                        val p = line.indexOf(':')
                        if (p >= 0) header[line.substring(0, p).trim { it <= ' ' }
                            .lowercase(Locale.getDefault())] =
                            line.substring(p + 1).trim { it <= ' ' }
                        line = `in`.readLine()
                    }
                }
                pre["uri"] = uri
            } catch (ioe: IOException) {
                sendError(
                    socket, HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.message
                )
            }
        }

        /** Decodes the Multipart Body data and put it into java Properties' key - value pairs.  */
        @Throws(InterruptedException::class)
        private fun decodeMultipartData(
            boundary: String,
            fbuf: ByteArray,
            `in`: BufferedReader,
            parms: Properties,
            files: Properties
        ) {
            try {
                val bpositions = getBoundaryPositions(fbuf, boundary.toByteArray())
                var boundarycount = 1
                var mpline = `in`.readLine()
                while (mpline != null) {
                    if (mpline.indexOf(boundary) == -1) sendError(
                        socket,
                        HTTP_BADREQUEST,
                        "BAD REQUEST: Content type is multipart/form-data but next chunk does not start with boundary. Usage: GET /example/file.html"
                    )
                    boundarycount++
                    val item = Properties()
                    mpline = `in`.readLine()
                    while (mpline != null && mpline.trim { it <= ' ' }.length > 0) {
                        val p = mpline.indexOf(':')
                        if (p != -1) item[mpline.substring(0, p).trim { it <= ' ' }
                            .lowercase(Locale.getDefault())] =
                            mpline.substring(p + 1).trim { it <= ' ' }
                        mpline = `in`.readLine()
                    }
                    if (mpline != null) {
                        val contentDisposition = item.getProperty("content-disposition")
                        if (contentDisposition == null) {
                            sendError(
                                socket,
                                HTTP_BADREQUEST,
                                "BAD REQUEST: Content type is multipart/form-data but no content-disposition info found. Usage: GET /example/file.html"
                            )
                        }
                        val st = StringTokenizer(contentDisposition, "; ")
                        val disposition = Properties()
                        while (st.hasMoreTokens()) {
                            val token = st.nextToken()
                            val p = token.indexOf('=')
                            if (p != -1) disposition[token.substring(0, p).trim { it <= ' ' }
                                .lowercase(Locale.getDefault())] =
                                token.substring(p + 1).trim { it <= ' ' }
                        }
                        var pname = disposition.getProperty("name")
                        pname = pname.substring(1, pname.length - 1)
                        var value = ""
                        if (item.getProperty("content-type") == null) {
                            while (mpline != null && mpline.indexOf(boundary) == -1) {
                                mpline = `in`.readLine()
                                if (mpline != null) {
                                    val d = mpline.indexOf(boundary)
                                    value += if (d == -1) mpline else mpline.substring(0, d - 2)
                                }
                            }
                        } else {
                            if (boundarycount > bpositions.size) sendError(
                                socket,
                                HTTP_INTERNALERROR,
                                "Error processing request"
                            )
                            val offset = stripMultipartHeaders(fbuf, bpositions[boundarycount - 2])
                            val path = saveTmpFile(
                                fbuf,
                                offset,
                                bpositions[boundarycount - 1] - offset - 4
                            )
                            files[pname] = path
                            value = disposition.getProperty("filename")
                            value = value.substring(1, value.length - 1)
                            do {
                                mpline = `in`.readLine()
                            } while (mpline != null && mpline.indexOf(boundary) == -1)
                        }
                        parms[pname] = value
                    }
                }
            } catch (ioe: IOException) {
                sendError(
                    socket, HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.message
                )
            }
        }

        /** Find the byte positions where multipart boundaries start.  */
        fun getBoundaryPositions(b: ByteArray, boundary: ByteArray): IntArray {
            var matchcount = 0
            var matchbyte = -1
            val matchbytes = Vector<Int>()
            run {
                var i = 0
                while (i < b.size) {
                    if (b[i] == boundary[matchcount]) {
                        if (matchcount == 0) matchbyte = i
                        matchcount++
                        if (matchcount == boundary.size) {
                            matchbytes.addElement(matchbyte)
                            matchcount = 0
                            matchbyte = -1
                        }
                    } else {
                        i -= matchcount
                        matchcount = 0
                        matchbyte = -1
                    }
                    i++
                }
            }
            val ret = IntArray(matchbytes.size)
            for (i in ret.indices) {
                ret[i] = matchbytes.elementAt(i) as Int
            }
            return ret
        }

        /**
         * Retrieves the content of a sent file and saves it to a temporary file. The full path to the
         * saved file is returned.
         */
        private fun saveTmpFile(b: ByteArray, offset: Int, len: Int): String {
            var path = ""
            if (len > 0) {
                val tmpdir = System.getProperty("java.io.tmpdir")
                try {
                    val temp = File.createTempFile("NanoHTTPD", "", File(tmpdir))
                    val fstream: OutputStream = FileOutputStream(temp)
                    fstream.write(b, offset, len)
                    fstream.close()
                    path = temp.absolutePath
                } catch (e: Exception) { // Catch exception if any
                    System.err.println("Error: " + e.message)
                }
            }
            return path
        }

        /** It returns the offset separating multipart file headers from the file's data.  */
        private fun stripMultipartHeaders(b: ByteArray, offset: Int): Int {
            var i = 0
            i = offset
            while (i < b.size) {
                if (b[i].toInt().toChar() == '\r' && b[++i].toInt()
                        .toChar() == '\n' && b[++i].toInt().toChar() == '\r' && b[++i].toInt()
                        .toChar() == '\n'
                ) break
                i++
            }
            return i + 1
        }

        /**
         * Decodes the percent encoding scheme. <br></br>
         * For example: "an+example%20string" -> "an example string"
         */
        @Throws(InterruptedException::class)
        private fun decodePercent(str: String): String? {
            return try {
                val sb = StringBuffer()
                var i = 0
                while (i < str.length) {
                    val c = str[i]
                    when (c) {
                        '+' -> sb.append(' ')
                        '%' -> {
                            sb.append(str.substring(i + 1, i + 3).toInt(16).toChar())
                            i += 2
                        }
                        else -> sb.append(c)
                    }
                    i++
                }
                sb.toString()
            } catch (e: Exception) {
                sendError(socket, HTTP_BADREQUEST, "BAD REQUEST: Bad percent-encoding.")
                null
            }
        }

        /**
         * Decodes parameters in percent-encoded URI-format ( e.g.
         * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given Properties. NOTE: this
         * doesn't support multiple identical keys due to the simplicity of Properties -- if you need
         * multiples, you might want to replace the Properties with a Hashtable of Vectors or such.
         */
        @Throws(InterruptedException::class)
        private fun decodeParms(parms: String?, p: Properties) {
            if (parms == null) return
            val st = StringTokenizer(parms, "&")
            while (st.hasMoreTokens()) {
                val e = st.nextToken()
                val sep = e.indexOf('=')
                if (sep >= 0) p[decodePercent(e.substring(0, sep))!!.trim { it <= ' ' }] =
                    decodePercent(e.substring(sep + 1))
            }
        }

        /**
         * Returns an error message as a HTTP response and throws InterruptedException to stop further
         * request processing.
         */
        @Throws(InterruptedException::class)
        private fun sendError(socket: Socket, status: String, msg: String) {
            sendResponse(socket, status, MIME_PLAINTEXT, null, null)
            throw InterruptedException()
        }

        /** Sends given response to the socket.  */
        private fun sendResponse(
            socket: Socket,
            status: String?,
            mime: String?,
            header: Properties?,
            data: CloudStreamSource?
        ) {
            try {
                if (status == null) throw Error("sendResponse(): Status can't be null.")
                val out = socket.getOutputStream()
                val pw = PrintWriter(out)
                pw.print("HTTP/1.0 $status \r\n")
                if (mime != null) pw.print("Content-Type: $mime\r\n")
                if (header == null || header.getProperty("Date") == null) pw.print(
                    """
    Date: ${gmtFrmt!!.format(Date())}
    
    """.trimIndent()
                )
                if (header != null) {
                    val e: Enumeration<*> = header.keys()
                    while (e.hasMoreElements()) {
                        val key = e.nextElement() as String
                        val value = header.getProperty(key)
                        pw.print("$key: $value\r\n")
                    }
                }
                pw.print("\r\n")
                pw.flush()
                if (data != null) {
                    // long pending = data.availableExact();      // This is to support partial sends, see
                    // serveFile()
                    data.open()
                    val buff = ByteArray(8192)
                    var read = 0
                    while (data.read(buff).also { read = it } > 0) {
                        // if(SolidExplorer.LOG)Log.d(CloudUtil.TAG, "Read: "+ read +", pending: "+
                        // data.availableExact());
                        out.write(buff, 0, read)
                    }
                }
                out.flush()
                out.close()
                data?.close()
            } catch (ioe: IOException) {
                // Couldn't write? No can do.
                try {
                    socket.close()
                } catch (t: Throwable) {
                }
            }
        } // private Socket mySocket;

        init {
            // mySocket = s;
            val t = Thread(this)
            t.isDaemon = true
            t.start()
        }
    }

    private var myTcpPort = 0
    private val myServerSocket: ServerSocket
    private val myThread: Thread

    companion object {
        private const val TAG = "CloudStreamServer"

        /** Some HTTP response status codes  */
        const val HTTP_OK = "200 OK"
        const val HTTP_PARTIALCONTENT = "206 Partial Content"
        const val HTTP_RANGE_NOT_SATISFIABLE = "416 Requested Range Not Satisfiable"
        const val HTTP_REDIRECT = "301 Moved Permanently"
        const val HTTP_FORBIDDEN = "403 Forbidden"
        const val HTTP_NOTFOUND = "404 Not Found"
        const val HTTP_BADREQUEST = "400 Bad Request"
        const val HTTP_INTERNALERROR = "500 Internal Server Error"
        const val HTTP_NOTIMPLEMENTED = "501 Not Implemented"

        /** Common mime types for dynamic content  */
        const val MIME_PLAINTEXT = "text/plain"

        /** GMT date formatter  */
        private var gmtFrmt: SimpleDateFormat? = null

        init {
            gmtFrmt = SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US)
            gmtFrmt!!.setTimeZone(TimeZone.getTimeZone("GMT"))
        }
    }
}