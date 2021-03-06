/*
 * Copyright (C) 2016 Christopher Batey and Dogan Narinc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scassandra.server.cqlmessages

import akka.util.ByteString
import org.scalatest.{Matchers, FunSuite}
import org.scassandra.server.actors.QueryFlags
import org.scassandra.server.cqlmessages.types.CqlText

class VersionOneMessageFactoryTest extends FunSuite with Matchers {

  implicit val protocolVersion = VersionOne
  val underTest = VersionOneMessageFactory

  test("Parsing QueryRequest") {
    val query: String = "select * from blah where name = 'Hello'"
    val message: Array[Byte] =
                CqlProtocolHelper.serializeLongString(query) ++
                CqlProtocolHelper.serializeShort(TWO.code)

    val queryRequest = underTest.parseQueryRequest(1, ByteString(message))

    queryRequest.query should equal(query)
    queryRequest.consistency should equal(TWO.code)
    queryRequest.flags should equal(0x00.toByte)
    queryRequest.parameters should equal(Nil)

  }
}
