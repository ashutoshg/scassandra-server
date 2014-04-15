package uk.co.scassandra.e2e

import uk.co.scassandra.AbstractIntegrationTest
import org.scalatest.concurrent.ScalaFutures
import com.datastax.driver.core.{ResultSet, ConsistencyLevel, SimpleStatement}
import dispatch._, Defaults._

class AdvancedPrimeCriteriaTest extends AbstractIntegrationTest with ScalaFutures {

  val whenQuery = "select * from people"
  val name = "Chris"
  val nameColumn = "name"
  val rows: List[Map[String, String]] = List(Map(nameColumn -> name))

  before {
    println("Deleting old primes")
    val svc = url("http://localhost:8043/prime").DELETE
    val response = Http(svc OK as.String)
    response()
  }

  test("Priming by default should apply to the query regardless of consistency") {
    // priming
    prime(whenQuery, rows, "success")
  
    executeQueryAndVerifyAtConsistencyLevel(ConsistencyLevel.ONE)
    executeQueryAndVerifyAtConsistencyLevel(ConsistencyLevel.TWO)
    executeQueryAndVerifyAtConsistencyLevel(ConsistencyLevel.ALL)

  }

  test("Priming for a specific consistency should only return results for that consistency") {
    // priming
    prime(whenQuery, rows, "success", consistency = List(ConsistencyLevel.ONE))

    executeQueryAndVerifyAtConsistencyLevel(ConsistencyLevel.ONE)
    executeQueryAndVerifyNoResultsConsistencyLevel(ConsistencyLevel.TWO)
  }

  test("Priming for a multiple consistencies should only return results for those consistencies") {
    // priming
    prime(whenQuery, rows, "success", consistency = List(ConsistencyLevel.ONE, ConsistencyLevel.ALL))

    executeQueryAndVerifyAtConsistencyLevel(ConsistencyLevel.ONE)
    executeQueryAndVerifyAtConsistencyLevel(ConsistencyLevel.ALL)
    executeQueryAndVerifyNoResultsConsistencyLevel(ConsistencyLevel.TWO)
  }

  test("Priming for same query with different consistencies - success for both") {
    // priming
    val anotherName: String = "anotherName"
    val someDifferentRows: List[Map[String, String]] = List(Map(nameColumn -> anotherName))
    prime(whenQuery, rows, "success", consistency = List(ConsistencyLevel.ONE, ConsistencyLevel.ALL))
    prime(whenQuery, someDifferentRows, "success", consistency = List(ConsistencyLevel.TWO, ConsistencyLevel.THREE))

    executeQueryAndVerifyAtConsistencyLevel(ConsistencyLevel.ONE, name)
    executeQueryAndVerifyAtConsistencyLevel(ConsistencyLevel.ALL, name)
    executeQueryAndVerifyAtConsistencyLevel(ConsistencyLevel.TWO, anotherName)
    executeQueryAndVerifyAtConsistencyLevel(ConsistencyLevel.THREE, anotherName)
  }

  def executeQueryAndVerifyAtConsistencyLevel(consistency: ConsistencyLevel, name : String = name) {
    val statement = new SimpleStatement(whenQuery)
    statement.setConsistencyLevel(consistency)
    val result = session.execute(statement)
    val results = result.all()
    results.size() should equal(1)
    results.get(0).getString(nameColumn) should equal(name)
  }

  def executeQueryAndVerifyNoResultsConsistencyLevel(consistency: ConsistencyLevel) {
    val statement = new SimpleStatement(whenQuery)
    statement.setConsistencyLevel(consistency)
    val result = session.execute(statement)
    val results = result.all()
    results.size() should equal(0)
  }
}