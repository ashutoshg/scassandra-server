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
package org.scassandra.server.priming.batch

import com.typesafe.scalalogging.LazyLogging
import org.scassandra.server.cqlmessages.{BatchType, Consistency, LOGGED}
import org.scassandra.server.priming.json.Success
import org.scassandra.server.priming.routes.PrimingJsonHelper
import org.scassandra.server.priming.{BatchExecution, PrimeResult}

class PrimeBatchStore extends LazyLogging {

  var primes: Map[BatchCriteria, BatchPrime] = Map()
  
  def record(prime: BatchPrimeSingle): Unit = {
    val result: PrimeResult = PrimingJsonHelper.convertToPrimeResult(prime.thenDo.config.getOrElse(Map()), prime.thenDo.result.getOrElse(Success))
    val consistencies: List[Consistency] = prime.when.consistency.getOrElse(Consistency.all)
    primes += (BatchCriteria(prime.when.queries, consistencies, prime.when.batchType.getOrElse(LOGGED)) -> BatchPrime(result, prime.thenDo.fixedDelay))
  }
  def findPrime(primeMatch: BatchExecution): Option[BatchPrime] = {
    logger.debug("Batch Prime Match {} current primes {}", primeMatch, primes)
    primes.find {
      case (batchCriteria, _) =>
        batchCriteria.queries == primeMatch.batchQueries.map(bq => BatchQueryPrime(bq.query, bq.batchQueryKind)) &&
          batchCriteria.consistency.contains(primeMatch.consistency) &&
          batchCriteria.batchType == primeMatch.batchType
    } map(_._2)
  }
  def clear() = {
    primes = Map()
  }
}

case class BatchCriteria(queries: Seq[BatchQueryPrime], consistency: List[Consistency], batchType: BatchType)
// batches can only be DML statements
case class BatchPrime(result: PrimeResult, delay: Option[Long])
