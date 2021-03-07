package mx.cinvestav
import cats.data.Chain
import cats.effect.{ExitCode, IO, IOApp}
import fs2.INothing
import fs2.io.net.Network
import mx.cinvestav.loadbalancing.LoadBalancer
import fs2.Stream
import fs2.text
import cats.implicits._
import cats.effect.std.Console
import cats.effect.Concurrent
import cats.kernel.Monoid
import com.comcast.ip4s._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser.decode
import mx.cinvestav.loadbalancing.LoadBalancer.Bin
import io.circe.syntax._
import mx.cinvestav.config.DefaultConfig
import mx.cinvestav.domain.TraceData
import mx.cinvestav.loadbalancing.LoadBalancer.{BalancedNode, Node}
import pureconfig._
import pureconfig.generic.auto._

object LoadBalancerMain extends  IOApp{
  implicit val traceDataDecoder:Decoder[TraceData]=deriveDecoder
  implicit val nodeEncoder:Encoder[Node] = Encoder.forProduct2("port","url"){node=>
    (node.port,node.url)
  }
  implicit val binEncoder:Encoder[Bin[Int]] =Encoder.forProduct1("value"){x=>(x.value)}
  implicit val balancedNodeEncoder:Encoder[BalancedNode[Int]] = Encoder.forProduct2("node","bins"){
    x=>(x.node,x.bins)
  }

  def serverMessage[F[_]:Console](_port:Int): Stream[F, INothing] =
    Stream.exec(Console[F].println(s"Server is running on port ${_port}"))

  def echoServer[F[_]: Concurrent:Network:Console](_port:Int): Stream[F, INothing] = {
    serverMessage(_port)++
      Network[F].server(port =Port.fromInt(_port)).map { client =>
      client.reads
        .through(text.utf8Decode)
        .through(text.lines)
        .filter(x=>x.nonEmpty)
        .fmap(decode[TraceData])
        .fmap {
          case Left(_) =>
            TraceData(Chain.empty[Int],Monoid[Int].empty,Monoid[Int].empty,Monoid[Int].empty)
          case Right(value) =>
            value
        }
        .debug(x=>s"trace size: ${x.data.length}\nWorkers: ${x.workers}\nLoad Balancer: ${x.loadBalancer}\n___________")

        .fmap(LoadBalancer.run)
        .fmap(x=>x.get)
        .fmap(x=>x.asJson)
        .fmap(x=>x.noSpaces)
        .debug()
        .through(text.utf8Encode)
        .through(client.writes)
    }
      .parJoin(10)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigSource.default.load[DefaultConfig]
    config match {
      case Left(error) =>
        println(s"ERROR: ${error.head.description}")
        IO.unit.as(ExitCode.Error)
      case Right(config) =>
        echoServer[IO](config.port)
          .compile
          .drain
          .as(ExitCode.Success)
    }

  }
}
