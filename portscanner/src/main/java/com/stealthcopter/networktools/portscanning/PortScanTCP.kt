/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.stealthcopter.networktools.portscanning

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Created by mat on 13/12/15.
 */
object PortScanTCP {
    /**
     * Check if a port is open with TCP
     *
     * @param ia            - address to scan
     * @param portNo        - port to scan
     * @param timeoutMillis - timeout
     * @return - true if port is open, false if not or unknown
     */
    @JvmStatic
    @Suppress("LabeledExpression")
    fun scanAddress(ia: InetAddress?, portNo: Int, timeoutMillis: Int): Boolean {
        return Socket().let { s ->
            runCatching {
                s.connect(InetSocketAddress(ia, portNo), timeoutMillis)
                return@let true
            }.also {
                runCatching {
                    s.close()
                }.getOrNull()
            }.getOrDefault(false)
        }
    }
}
