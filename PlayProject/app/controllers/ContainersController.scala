package controllers

import scala.concurrent.ExecutionContext.Implicits._
import javax.inject.Inject
import play.api.libs.ws.WSClient
import play.api.mvc.Controller
import play.api.mvc.Action
import play.mvc.Results.Redirect
import play.api.data.Form
import play.api.data.Mapping
import play.api.libs.json._
import com.fasterxml.jackson.annotation.JsonValue
import akka.actor.ActorSystem
import actors.ContainersActor
import play.api.mvc.WebSocket
import actors.ContainersActor
import play.api.Play.current
import actors.CreateContainer
import play.api.mvc.WebSocket.FrameFormatter

class ContainersController @Inject() (ws: WSClient, system: ActorSystem) extends Controller {  
  
  implicit val CreateContainerFormat = Json.format[CreateContainer]
  implicit val CreateContainerFormatter = FrameFormatter.jsonFrame[CreateContainer]
  
  def create = WebSocket.acceptWithActor[CreateContainer, String] { request => out =>
    ContainersActor.props(out, ws)
  }

  
  def start(id : String) = Action.async {
    ws.url("https://192.168.30.53:8080/containers/"+id+"/start").post("").map(response =>
      {
        Redirect(routes.Application.dashboard())
      })
  }
  def stop(id : String) = Action.async {
    ws.url("https://192.168.30.53:8080/containers/"+id+"/stop").post("").map(response =>
      {
        Redirect(routes.Application.dashboard())
      })
  }
  def delete(id : String) = Action.async {
    ws.url("https://192.168.30.53:8080/containers/"+id).delete.map(response =>
      {
        Redirect(routes.Application.dashboard())
      })
  }
  


  
}