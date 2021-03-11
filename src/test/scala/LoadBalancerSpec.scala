import cats.effect.{Deferred, IO}
import cats.effect.unsafe.implicits.global
import org.scalatest.funsuite.AnyFunSuite
import fs2.{Chunk, Pipe, Pull, Stream}
import cats.syntax.all._
import mx.cinvestav.domain.TraceData
import cats.data.Chain
import mx.cinvestav.loadbalancing.LoadBalancer
import mx.cinvestav.loadbalancing.LoadBalancer.BinOrdering

import scala.concurrent.duration._
import scala.language.postfixOps

class LoadBalancerSpec extends AnyFunSuite{
 val data: Chain[Float] = Chain.fromSeq((0 until 5).map(_.toFloat) )
  test("Round robin"){
    val td = TraceData(data,workers = 2,loadBalancer = 0,basePort = 6000)
    val res = LoadBalancer.run(td).get
    println(res)
  }
  test("Random"){
    val td = TraceData(data = data,workers = 2,loadBalancer = 1,basePort = 6000)
    val res = LoadBalancer.run(td).get
    println(res)
  }
  test("Two-choices"){
    val td = TraceData(data = data,workers = 4,loadBalancer = 2,basePort = 6000)
    val res = LoadBalancer.run(td).get
    println(res)
//    println(res.flatMap(_.bins).sorted(BinOrdering))
  }
}
