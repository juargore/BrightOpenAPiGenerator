package ai.bright.generator

class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}