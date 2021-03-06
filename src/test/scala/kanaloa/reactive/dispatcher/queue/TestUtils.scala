package kanaloa.reactive.dispatcher.queue

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import kanaloa.reactive.dispatcher.metrics.{ MetricsCollector, NoOpMetricsCollector }
import kanaloa.reactive.dispatcher.{ Backend, ResultChecker }
import org.specs2.specification.Scope

object TestUtils {

  case class DelegateeMessage(msg: String)
  case class MessageProcessed(msg: String)
  case object MessageFailed

  val resultChecker: ResultChecker = {
    case MessageProcessed(msg) ⇒ Right(msg)
    case m                     ⇒ Left(s"unrecognized message received by resultChecker: $m (${m.getClass})")
  }

  def iteratorQueueProps(
    iterator:         Iterator[String],
    historySettings:  DispatchHistorySettings = DispatchHistorySettings(),
    workSetting:      WorkSettings            = WorkSettings(),
    metricsCollector: MetricsCollector        = NoOpMetricsCollector
  ): Props =
    Queue.ofIterator(iterator.map(DelegateeMessage(_)), historySettings, workSetting, metricsCollector)

  class ScopeWithQueue(implicit system: ActorSystem) extends TestKit(system) with ImplicitSender with Scope {

    val delegatee = TestProbe()

    val backend = Backend(delegatee.ref)

    def defaultProcessorProps(
      queue:            QueueRef,
      settings:         ProcessingWorkerPoolSettings = ProcessingWorkerPoolSettings(startingPoolSize = 1),
      metricsCollector: MetricsCollector             = NoOpMetricsCollector
    ) = QueueProcessor.default(queue, backend, settings, metricsCollector)(resultChecker)
  }
}
