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
package org.scassandra.server.cqlmessages.request

import java.math.BigInteger

import org.scalatest.{Matchers, FunSuite}
import org.scassandra.server.cqlmessages._
import akka.util.ByteString
import org.scassandra.server.cqlmessages.types._

class ExecuteRequestTest extends FunSuite with Matchers {

  import CqlProtocolHelper._

  test("Seralisation of a execute - version 1") {
    val stream : Byte = 0x01
    val protocolVersion : Byte = 0x1
    val consistency = TWO
    val id : Byte = 5
    val executeRequest = new ExecuteRequestV1(protocolVersion, stream, id, consistency)
    val serialisation = executeRequest.serialize().iterator

    serialisation.getByte should equal(protocolVersion)
    serialisation.getByte // ignore the flags
    serialisation.getByte should equal(stream)
    serialisation.getByte should equal(OpCodes.Execute)

    serialisation.drop(4) // length

    CqlProtocolHelper.readShortBytes(serialisation) should equal(Array[Byte](0,0,0,id))

    val numberOfOptions = serialisation.getShort
    numberOfOptions should equal(0)

    serialisation.getShort should equal(consistency.code)
    serialisation.isEmpty should equal(true)
  }

  test("Serialization of a execute - version 2") {
    implicit val protoVersion= VersionTwo
    val stream : Byte = 0x01
    val protocolVersion : Byte = 0x1
    val consistency = TWO
    val id : Byte = 5
    val variables = List(1234, 5678)
    val variableTypes = List(CqlBigint, CqlDecimal)
    val executeRequest = new ExecuteRequestV2(protocolVersion, stream, id, consistency, 2, variables, variableTypes = variableTypes)
    val serialization = executeRequest.serialize().iterator

    serialization.getByte should equal(protocolVersion)
    serialization.getByte // ignore the flags
    serialization.getByte should equal(stream)
    serialization.getByte should equal(OpCodes.Execute)

    serialization.drop(4) // length

    CqlProtocolHelper.readShortBytes(serialization) should equal(Array[Byte](0,0,0,id))
    serialization.getShort should equal(consistency.code)
    serialization.getByte should equal(0) // flags

    val numberOfVariables = serialization.getShort
    numberOfVariables should equal(2)

    CqlBigint.readValueWithLength(serialization) should equal(Some(1234))
    CqlDecimal.readValueWithLength(serialization) should equal(Some(BigDecimal("5678")))

    serialization.isEmpty should equal(true)
  }

  test("Deserialize execute with numeric variable types - version 2") {
    val stream: Byte = 5
    val v2MessageFromCassandra = ByteString(
      0, 4, // length of the prepared statement id
      0, 0, 0, 1, // prepared statement id
      0, 1, // consistency
      5, // flags
      0, 7, // number of variables
      0, 0, 0, 8,    0, 0, 0, 0, 0, 0, 4, -46, //   val bigInt : java.lang.Long = 1234
      0, 0, 0, 8,    0, 0, 0, 0, 0, 0, 9, 41,    //    val counter : java.lang.Long = 2345
      0, 0, 0, 5,    0, 0, 0, 0, 1,              //   val decimal : java.math.BigDecimal = new java.math.BigDecimal("1")
      0, 0, 0, 8,    63, -8, 0, 0, 0, 0, 0, 0,   //   val double : java.lang.Double = 1.5
      0, 0, 0, 4,    64, 32, 0, 0,               //   val float : java.lang.Float = 2.5f
      0, 0, 0, 4,    0, 0, 13, -128,             //   val int : java.lang.Integer = 3456
      0, 0, 0, 1,   123,                         //   val varint : java.math.BigInteger = new java.math.BigInteger("123")

      0, 0, 19, -120) // serial consistency?? not sure

    val types = List[ColumnType[_]](CqlBigint, CqlCounter, CqlDecimal, CqlDouble, CqlFloat, CqlInt, CqlVarint)

    val response = ExecuteRequest.versionTwoWithTypes(stream, v2MessageFromCassandra, types)

    response.consistency should equal(ONE)
    response.id should equal(1)
    response.flags should equal(5)
    response.stream should equal(stream)
    response.numberOfVariables should equal(7)
    response.variables.size should equal(7)
    response.variables should equal(
      List(Some(1234l), Some(2345l), Some(BigDecimal("1")), Some(1.5d), Some(2.5f), Some(3456), Some(BigInt("123")))
    )
  }

  test("Deserailise without parsing the variable types - version 2") {
    val stream: Byte = 5
    val v2MessageFromCassandra = ByteString(
      0, 4, // length of the prepared statement id
      0, 0, 0, 1, // prepared statement id
      0, 1, // consistency
      5, // flags
      0, 7, // number of variables
      0, 0, 0, 8,    0, 0, 0, 0, 0, 0, 4, -46, //   val bigInt : java.lang.Long = 1234
      0, 0, 0, 8,    0, 0, 0, 0, 0, 0, 9, 41,    //    val counter : java.lang.Long = 2345
      0, 0, 0, 5,    0, 0, 0, 0, 1,              //   val decimal : java.math.BigDecimal = new java.math.BigDecimal("1")
      0, 0, 0, 8,    63, -8, 0, 0, 0, 0, 0, 0,   //   val double : java.lang.Double = 1.5
      0, 0, 0, 4,    64, 32, 0, 0,               //   val float : java.lang.Float = 2.5f
      0, 0, 0, 4,    0, 0, 13, -128,             //   val int : java.lang.Integer = 3456
      0, 0, 0, 1,   123,                         //   val varint : java.math.BigInteger = new java.math.BigInteger("123")

      0, 0, 19, -120) // serial consistency?? not sure

    val response = ExecuteRequest.versionTwoWithoutTypes(stream, v2MessageFromCassandra)

    response.consistency should equal(ONE)
    response.id should equal(1)
    response.flags should equal(5)
    response.stream should equal(stream)
    response.variables.size should equal(0)
    response.numberOfVariables should equal(7)
  }

  test("Deserailise without parsing the variable types - version 1") {
    val stream: Byte = 5
    val v1MessageFromCassandra = ByteString(
      0, 4, // length of the prepared statement id
      0, 0, 0, 1, // prepared statement id
      0, 7, // number of variables
      0, 0, 0, 8,    0, 0, 0, 0, 0, 0, 4, -46, //   val bigInt : java.lang.Long = 1234
      0, 0, 0, 8,    0, 0, 0, 0, 0, 0, 9, 41,    //    val counter : java.lang.Long = 2345
      0, 0, 0, 5,    0, 0, 0, 0, 1,              //   val decimal : java.math.BigDecimal = new java.math.BigDecimal("1")
      0, 0, 0, 8,    63, -8, 0, 0, 0, 0, 0, 0,   //   val double : java.lang.Double = 1.5
      0, 0, 0, 4,    64, 32, 0, 0,               //   val float : java.lang.Float = 2.5f
      0, 0, 0, 4,    0, 0, 13, -128,             //   val int : java.lang.Integer = 3456
      0, 0, 0, 1,   123,                         //   val varint : java.math.BigInteger = new java.math.BigInteger("123")

      0, 2) // consistency

    val response = ExecuteRequest.versionOneWithoutTypes(stream, v1MessageFromCassandra)

    response.consistency should equal(TWO)
    response.id should equal(1)
    response.stream should equal(stream)
    response.variables.size should equal(0)
    response.numberOfVariables should equal(7)
  }
}
