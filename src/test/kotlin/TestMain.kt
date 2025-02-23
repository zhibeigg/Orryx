object TestMain {

    val var1 = TestMain()

    class TestMain {

        fun tell(message: String) {
            println(message)
        }

    }

    @JvmStatic
    fun main(args: Array<String>) {
        var1.tell("123")
    }

}