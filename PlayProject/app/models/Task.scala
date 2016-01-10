package models

import scala.xml.Elem
import play.api.libs.json.JsValue

/*
 * Vcloud director task object
 */
case class Task(id : String, status : String, message : String = "") {
}

object TaskFactory {

  def apply(xml : Elem) : Task = {
    //Parse the xml returned by vcloud director
    xml.head.label match {
      //If there is an error and not a task
      case "Error" => Task("undefined","error",xml.head.attribute("message").get.text)
      case "Task" => Task(xml.head.attribute("id").get.text.split(":").last,
        xml.head.attribute("status").get.text,
	     (xml \ "Error").map(node => node.attribute("message").get.text).mkString("\n")
	      )
      case _ => Task("undefined","error","unknow error")
    }
    
  }
}