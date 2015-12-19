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

class ContainersController @Inject() (ws: WSClient) extends Controller {  
  
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
  

  def create = Action.async(parse.tolerantFormUrlEncoded) { request => {
    
    val image = request.body.get("image").map(_.head).get
    val params = Json.parse("""
    {
	    "AttachStdin": false,
	    "AttachStdout": true,
	    "AttachStderr": true,
	    "Tty": false,
	    "OpenStdin": false,
	    "StdinOnce": false,
	    "Image": """"+image+"""",
	    "ExposedPorts": {
	            "80/tcp": {}
	    },
	    "StopSignal": "SIGTERM",
	    "HostConfig": {
	      "PortBindings": { "80/tcp": [{ "HostPort": "80" }] },
	      "PublishAllPorts": false
	   }
	}
    """)


//    Ok(params)
      ws.url("https://192.168.30.53:8080/containers/create").post(params).map(response =>
        {
          Ok(response.body)
          //Redirect(routes.Application.dashboard())
        })
    }
  }
  
}