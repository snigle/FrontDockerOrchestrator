package controllers

import javax.inject.Inject

import actors.{ContainersActor, CreateContainer, Port}
import akka.actor.ActorSystem
import play.api.Mode
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller, WebSocket}
import play.api.mvc.WebSocket.FrameFormatter

import scala.concurrent.ExecutionContext.Implicits._

class ContainersController @Inject() (ws: WSClient, system: ActorSystem) extends Controller {  
  
  /*Json formatters to get data from websocket*/
  implicit val PortFormat = Json.format[Port]
  implicit val PortFormatter = FrameFormatter.jsonFrame[Port]
  
  implicit val CreateContainerFormat = Json.format[CreateContainer]
  implicit val CreateContainerFormatter = FrameFormatter.jsonFrame[CreateContainer]
  
  /*Create a container with the CreateContainer object*/
  def create = WebSocket.acceptWithActor[CreateContainer, String] { request => out =>
    ContainersActor.props(out, ws)
  }

  /*Get the ip of local or external if we are in Prod or Dev Mode*/
  val swarmMaster = current.mode match {
    case Mode.Dev => current.configuration.getString("vapp.swarm-master.ip").get+":8080"
    case Mode.Prod => "192.168.2.100:3376"
  }
  
  /*
   * Start a container with the id
   */
  def start(id : String) = Action.async {
    ws.url("https://"+swarmMaster+"/containers/"+id+"/start").post("").map(response =>
      {
        Redirect(routes.Application.dashboard())
      })
  }
  /*
   * Stop a container with the id
   */
  def stop(id : String) = Action.async {
    ws.url("https://"+swarmMaster+"/containers/"+id+"/stop").post("").map(response =>
      {
        Redirect(routes.Application.dashboard())
      })
  }
  /*
   * Dekete a container with the id
   */
  def delete(id : String) = Action.async {
    ws.url("https://"+swarmMaster+"/containers/"+id).delete.map(response =>
      {
        Redirect(routes.Application.dashboard())
      })
  }
  


  
}