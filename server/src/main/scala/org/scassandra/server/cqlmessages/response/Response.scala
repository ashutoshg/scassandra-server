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
package org.scassandra.server.cqlmessages.response

import akka.util.ByteString
import org.scassandra.server.cqlmessages._
import CqlProtocolHelper._

object ResponseHeader {
  val FlagsNoCompressionByte = 0x00
  val DefaultStreamId : Byte = 0x00
  val ZeroLength = Array(0x00, 0x00, 0x00, 0x00).map(_.toByte)
}

abstract class Response(header : Header) extends CqlMessage(header)

case class Ready(stream : Byte = ResponseHeader.DefaultStreamId)(implicit protocolVersion: ProtocolVersion) extends Response(new Header(protocolVersion.serverCode, opCode = OpCodes.Ready, streamId = stream)) {

  val MessageLength = 0

  override def serialize() : ByteString = {
    val bs = ByteString.newBuilder
    bs.putBytes(header.serialize())
    bs.putInt(MessageLength)
    bs.result()
  }
}

case class Supported(stream : Byte = ResponseHeader.DefaultStreamId, map: Map[String,Set[String]] = Map("CQL_VERSION" -> Set("3.0.0")))(implicit protocolVersion: ProtocolVersion)
  extends Response(new Header(protocolVersion.serverCode, opCode = OpCodes.Supported, streamId = stream)) {

  override def serialize() : ByteString = {
    val bs = ByteString.newBuilder
    bs.putBytes(header.serialize())
    val mapBytes = CqlProtocolHelper.serializeStringMultiMap(map)
    bs.putInt(mapBytes.size)
    bs.putBytes(mapBytes)
    bs.result()
  }
}