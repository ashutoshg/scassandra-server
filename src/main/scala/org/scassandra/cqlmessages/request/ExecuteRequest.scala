/*
 * Copyright (C) 2014 Christopher Batey and Dogan Narinc
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
package org.scassandra.cqlmessages.request

import org.scassandra.cqlmessages._
import akka.util.{ByteIterator, ByteString}
import org.scassandra.cqlmessages

object ExecuteRequest {

  import CqlProtocolHelper._

  def versionTwoWithoutTypes(stream: Byte, byteString: ByteString) : ExecuteRequest = {
    val bodyIterator: ByteIterator = byteString.iterator
    // length of the id - this is a short
    bodyIterator.drop(2)
    val preparedStatementId = bodyIterator.getInt
    val consistency = Consistency.fromCode(bodyIterator.getShort)
    val flags = bodyIterator.getByte
    val numberOfVariables = bodyIterator.getShort
    ExecuteRequest(ProtocolVersion.ClientProtocolVersionTwo, stream, preparedStatementId, consistency, numberOfVariables, List(), flags)
  }

  def versionTwoWithTypes(stream: Byte, byteString: ByteString, variableTypes: List[ColumnType[_]]) : ExecuteRequest = {
    val bodyIterator: ByteIterator = byteString.iterator
    // length of the id - this is a short
    bodyIterator.drop(2)
    val preparedStatementId = bodyIterator.getInt
    val consistency = Consistency.fromCode(bodyIterator.getShort)

    val flags = bodyIterator.getByte
    val numberOfVariables = bodyIterator.getShort

    val variableValues = variableTypes.map (varType => {
      varType.readValue(bodyIterator)
    } )
    ExecuteRequest(ProtocolVersion.ClientProtocolVersionTwo, stream, preparedStatementId, consistency, numberOfVariables, variableValues, flags)
  }

  def versionOneWithoutTypes(stream: Byte, byteString: ByteString) : ExecuteRequest = {
    val bodyIterator: ByteIterator = byteString.iterator
    // length of the id - this is a short
    bodyIterator.drop(2)
    val preparedStatementId = bodyIterator.getInt
    val numberOfVariables = bodyIterator.getShort
    ExecuteRequest(ProtocolVersion.ClientProtocolVersionOne, stream, preparedStatementId, ONE, numberOfVariables, List())
  }

  def versionOneWithTypes(stream: Byte, byteString: ByteString, variableTypes: List[ColumnType[_]]) : ExecuteRequest = {
    val bodyIterator: ByteIterator = byteString.iterator
    // length of the id - this is a short
    bodyIterator.drop(2)
    val preparedStatementId = bodyIterator.getInt
    val numberOfVariables = bodyIterator.getShort


    val variableValues = variableTypes.map (varType => {
      varType.readValue(bodyIterator)
    } )
    val consistency = Consistency.fromCode(bodyIterator.getShort)
    ExecuteRequest(ProtocolVersion.ClientProtocolVersionOne, stream, preparedStatementId, consistency, numberOfVariables, variableValues)
  }
}

case class ExecuteRequest(protocolVersion: Byte, stream: Byte, id: Int, consistency : Consistency = ONE, numberOfVariables : Int = 0, variables : List[Any] = List(), flags : Byte = 0x00) extends Request(new Header(protocolVersion, OpCodes.Execute, stream)) {

  import CqlProtocolHelper._

  def serialize(): ByteString = {
    val headerBytes: Array[Byte] = header.serialize()
    val bs = ByteString.newBuilder

    bs.putShort(4)
    bs.putInt(id)

    bs.putShort(consistency.code)
    bs.putByte(flags)

    bs.putShort(0) // 0 variables

    val body = bs.result()
    ByteString(headerBytes ++ CqlProtocolHelper.serializeInt(body.size) ++ body)
  }
}