package models

/*
 * Representation of a container
 */
case class Container(
    id: String,
    name: String,
    image: String,
    ports: Map[Int, Int], //Map of binding Ports 80->8080
    active: Boolean
    ) {

}