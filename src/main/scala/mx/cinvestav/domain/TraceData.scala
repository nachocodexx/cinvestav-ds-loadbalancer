package mx.cinvestav.domain

import cats.data.Chain

case class TraceData(data:Chain[Float], workers:Int, loadBalancer: Int,basePort:Int)
