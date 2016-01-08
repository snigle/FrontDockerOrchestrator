package actors

import akka.actor.{Actor, ActorRef, PoisonPill, Props, actorRef2Scala}
import play.api.Mode
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.collection.immutable.Map
import scala.concurrent.ExecutionContext.Implicits._

/*
* Classe Port used to bind container port
* in : VM's port opened
* out : port used by the container
*/
case class Port(in : Int, protocol : String, out : Int)

/*
* Classe CreateContainer : represents the container to deploy on a VM
* image : name of the image to deploy from dockerhub
*  ports : ports used by the image
*/
case class CreateContainer(image : String, ports : Seq[Port])

/*
* Object used by websockets
*/
object ContainersActor {
  def props(out: ActorRef, ws: WSClient) = Props(new ContainersActor(out, ws))
}


/*
* Actor used to send a container to swarm master to deploy the container
*/
class ContainersActor(out: ActorRef, ws: WSClient) extends Actor {
  def receive = {
    case msg: String => {
      println(msg);
      out ! ("""{ "info" : "I received your message: """ + msg + "\"}")
      self ! PoisonPill
    }
    case CreateContainer(image, ports) => {
      out ! Json.toJson(
        Map(
          "info" -> Json.toJson("Creating container for image " + image)
        )
      ).toString()
      println("CreateContainer Ok");
      println(ports);
      println(image);
      val params = Json.parse(
        """
      {
  	    "AttachStdin": false,
  	    "AttachStdout": true,
  	    "AttachStderr": true,
  	    "Tty": false,
  	    "OpenStdin": false,
  	    "StdinOnce": false,
  	    "Image": """" + image +
          """",
  	    "ExposedPorts": {""" +
          ports.map(port => s""" "${port.in}/${port.protocol}" : {} """).mkString("", ",", "")
          //   "80/tcp": {}
          +
          """},
  	    "StopSignal": "SIGTERM",
  	    "HostConfig": {
  	      "PortBindings": { """ +
          ports.map(port => s""" "${port.in}/${port.protocol}" : [{ "HostPort" : "${port.out}"}] """).mkString("", ",", "")
          // "80/tcp": [{ "HostPort": "8081" }]
          +
          """},
  	      "PublishAllPorts": false
  	      }
  	      
  	   }
      """)

          //Remote or local ip depending of the launch configuration of the server
          val swarmMaster = current.mode match {
            case Mode.Dev => current.configuration.getString("vapp.swarm-master.ip").get+":8080"
            case Mode.Prod => "192.168.2.100:3376"
          }

  println(params);

            ws.url ("https://" + swarmMaster + "/containers/create").post (params).map (response => {
            val res =
            if (response.status == 201) {
            Json.toJson (
            Map (
            "success" ->Json.toJson ("The container has been created with image " + image)
            )
            )
            }
            else {Json.toJson (
            Map (
              "error" -> Json.toJson (response.body)
            )
            )
            }
              println ("Requete Ok " + image)
              out ! res.toString ()
              self ! PoisonPill


            })

    }
  }
}