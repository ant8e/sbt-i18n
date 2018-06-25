package simple

/**
  * A simple class and objects to write tests against.
  */
class Main {
  val text = org.example.i18n.Bundle.fr.test
  val text2 = org.example.i18n.Bundle.fr.test2("hi")
}

object Main {

  def main(args: Array[String]): Unit = {
    val m = new Main()
    println(m.text)
    println(m.text2)
  }
}
