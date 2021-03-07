package mx.cinvestav.loadbalancing

import cats.data.Chain
import cats.kernel.{Monoid, Order}
import cats.syntax._
import cats.effect.IO
import cats.implicits.catsSyntaxSemigroup
import mx.cinvestav.domain.TraceData
import fs2.Stream

import java.util.Random
object LoadBalancer {
  case class Bin[A](value:A)
  type Bins[A] = List[Bin[A]]
  case class Node(index:Int,port:Int,url:String)
  type Nodes = List[Node]
  case class BalancedNode[A](node:Node,bins:Bins[A])
  type BalancedNodes[A] = List[BalancedNode[A]]
  object BinOrdering extends Ordering[Bin[Int]]{
    override def compare(x: Bin[Int], y: Bin[Int]): Int = x.value.compare(y.value)
  }
  object BalancedNodeOrdering extends  Ordering[BalancedNode[Int]] {
    override def compare(x: BalancedNode[Int], y: BalancedNode[Int]): Int = x.bins.length.compare(y.bins.length)
  }

  implicit val balancedNodeOrder: Order[BalancedNode[Int]] =
    (x: BalancedNode[Int], y: BalancedNode[Int]) => x.bins.length.compare(y.bins.length)
  implicit val balancedNodeMonoid:Monoid[BalancedNode[Int]] = new Monoid[BalancedNode[Int]] {
    override def empty: BalancedNode[Int] = BalancedNode(Node(-1,0,""),List.empty)

    override def combine(x: BalancedNode[Int], y: BalancedNode[Int]): BalancedNode[Int] =
      BalancedNode(x.node,x.bins.concat(y.bins).sorted(BinOrdering))
  }

  private val algorithms = Map(0->roundRobin _ ,1->random _,2->twoChoices _)
  private def addToNode(value:(Int,Bin[Int]),xxs:BalancedNodes[Int]) = value match {
    case (nodeIndex,x)=>
      val node    = xxs(nodeIndex)
      val bins    = node.bins
      val newNode = node.copy(bins=bins.appended(x).sorted(BinOrdering))
      xxs.updated(nodeIndex,newNode)
  }
  def roundRobin(data:Chain[Int], nodes:BalancedNodes[Int]):BalancedNodes[Int]={
    val nodesLen = nodes.length
    data.zipWithIndex.collect{
      case (value, index) =>
        val node =index%nodesLen
        (node,Bin(value))
    }.foldRight(nodes)(addToNode)
  }
  def random(data:Chain[Int],workers:BalancedNodes[Int]):BalancedNodes[Int] ={
    val rand = new Random()
    val nodesLen = workers.length
    data.map{ value=>
      val nodeIndex = rand.nextInt(nodesLen)
      (nodeIndex,Bin(value))
    }.foldRight(workers)(addToNode)
//    List.empty
  }
  def twoChoices(data:Chain[Int],workers:BalancedNodes[Int]):BalancedNodes[Int]={
    val rand = new Random()
    val nodesLen = workers.length
    val getWinnerNode= (xs:BalancedNodes[Int],x:Int,y:Int)=>{
      val node1 = xs(x)
      val node2 = xs(y)
      Order[BalancedNode[Int]].min(node1,node2)
    }
    val res = data.toList.scanLeft(workers) { (xs, binValue) =>
      val nodeIndexes = (xs,rand.nextInt(nodesLen), rand.nextInt(nodesLen))
      val node = getWinnerNode.tupled(nodeIndexes)
      val bins = node.bins
      val nodeIndex = node.node.index
//      println(s"Bin $binValue -> node $nodeIndex -> binsLen ${bins.length}")
      val newNode = node.copy(bins = bins.appended(Bin(binValue)))
      xs.updated(nodeIndex, newNode)
    }.last
    res
  }
  def run(td:TraceData): Option[BalancedNodes[Int]] = {
    val nodes = (0 until td.workers).map(index=>Node(index,td.basePort+index,"localhost"))
      .toList
      .map(BalancedNode(_, List.empty[Bin[Int]]))
     algorithms.get(td.loadBalancer).map(_(td.data,nodes))
  }
}
