package models

import scala.xml.Elem

case class Vapp(id : String, vms : Seq[Vm], indice : Int) {
  
	def isAllDigits(x: String) = x forall Character.isDigit
  
  def this(xml : Elem) {
	  this(xml.head.attribute("id").get.text.split(":").last,
	      (xml \ "Children" \ "Vm").map(node => new Vm(node)),
	      (xml \ "Children" \ "Vm").map(node => node.attribute("name").get.text.split("-").last).filter(i => i forall(Character.isDigit)).map(i => i.toInt).max
	      )
  }
}