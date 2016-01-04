package actors

import akka.actor.ActorRef
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits._
import akka.actor.Actor
import akka.actor.PoisonPill
import scala.concurrent.duration._
import models.Task
import models.TaskFactory
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Await
import play.api.libs.ws.WSClient
import models.VappFactory
import play.api.libs.ws.WSResponse
import scala.concurrent.Future
import models.Vapp

class DeployMessageType(val task: Task)

abstract class VappActor(val out: ActorRef, val ws: WSClient, val func: () => Future[WSResponse], val cookie: () => String) extends Actor {
  
  def response_json(status: String, message: String) = {
    Json.toJson(Map(status -> message)).toString()
  }
  
  def waitTask(m: DeployMessageType, waitingMessage: String, success: () => Unit) = {
    if (m.task.status == "queued" || m.task.status == "running") {
      out ! response_json("info", waitingMessage)
      context.system.scheduler.scheduleOnce(2 seconds, self, m)
    } else if (m.task.status == "success") {
      success()
    } else {
      println("Close socket")
      out ! response_json("error", m.task.message)
      self ! PoisonPill
    }
  }
  
  def updateTask(task: Task) : Task= {

    if (task.id != "undefined") {
      val req =
        ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/task/" + task.id).withHeaders(
          "Cookie" -> cookie(),
          "Accept" -> "application/*+xml;version=1.5").get.map(response =>
            {
              TaskFactory(response.xml)
            })
      Await.ready(req, 20 seconds).value.get match {
        case Success(x) => x
        case Failure(x) => throw new Exception("Can't parse the task")
      }
    } else {
      task
    }
  }
  
  def getVapp : Vapp= {
    val req =
      func().map(response =>
        {
          VappFactory(response.xml)
        })
    Await.ready(req, 10 seconds).value.get match {
      case Success(x) => x
      case Failure(x) => throw new Exception("Can't parse the VApp")
    }
  }
  
}