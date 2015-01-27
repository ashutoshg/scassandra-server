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
package org.scassandra.server.cqlmessages.types

import java.lang

import akka.util.ByteIterator
import org.apache.cassandra.serializers.{CounterSerializer, LongSerializer, TypeSerializer}
import org.scassandra.server.cqlmessages.{ProtocolVersion, CqlProtocolHelper}

case object CqlCounter extends ColumnType[java.lang.Long](0x0005, "counter") {
   override def readValue(byteIterator: ByteIterator, protocolVersion: ProtocolVersion): Option[java.lang.Long] = {
     CqlProtocolHelper.readBigIntValue(byteIterator).map(new java.lang.Long(_))
   }

   def writeValue(value: Any) = {
     CqlProtocolHelper.serializeBigIntValue(value.toString.toLong)
   }

  override def convertToCorrectCollectionTypeForList(list: Iterable[_]) : List[java.lang.Long] = {
    list.map(convertToCorrectJavaTypeForSerializer).toList
  }

  override def serializer: TypeSerializer[java.lang.Long] = LongSerializer.instance

  override def convertToCorrectJavaTypeForSerializer(value: Any): lang.Long = value match {
    case bd: BigDecimal => new java.lang.Long(bd.toLong)
    case string: String => java.lang.Long.parseLong(string)
    case _ => throw new IllegalArgumentException("Expected list of BigDecimals")
  }
}
