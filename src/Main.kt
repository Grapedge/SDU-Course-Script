import java.io.BufferedReader
import java.io.FileReader
import java.math.BigInteger
import java.security.MessageDigest

/******************************************
 * @author Grapes
 * @description 本科生选课系统
 ******************************************/

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

var username = ""
var password = ""
var courseList = mutableListOf<Course>()

fun readConfig(): Boolean {
    return try {
        val reader = BufferedReader(FileReader("config.ini"))
        var line = reader.readLine()
        courseList.clear()
        while (line.isNotEmpty()) {
            val params = line.split(" ")

            when (params[0]) {
                "username" -> {
                    username = params[1]
                }
                "password" -> {
                    password = params[1]
                }
                "course" -> {
                    if (params.size >= 3)
                        courseList.add(Course(params[1], params[2]))
                    else println("语法错误在：$params")
                }
            }
            line = reader.readLine()
        }
        reader.close()
        true
    } catch (e: Exception) {
        false
    }
}

fun main(args: Array<String>) {
    readConfig()
    val post = GPost()
    post.login(username, password)
    var loginTime = System.currentTimeMillis()
    var count = 0
    while (true) {
        if (count++ % 1000 == 0)
            println("======== 第${count}次抢课 ========")
        for (course in courseList) {
            post.add(course)
            if (System.currentTimeMillis() - loginTime >= 900000) {
                post.login(username, password)
                loginTime = System.currentTimeMillis();
            }
        }
    }
}

