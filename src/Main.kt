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
        while (line != null && line.isNotEmpty()) {
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
                    else println("语法错误在：$params: 课程应该同时包含课程号和课序号")
                }
            }
            line = reader.readLine()
        }
        reader.close()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun main() {
    if (readConfig()) {
        if (username.isBlank() || password.isBlank() || username == "账号" || password == "密码") {
            println("用户名密码设置不正确，系统自动退出")
            return
        }
        else if (courseList.size == 0) {
            println("有效的备选课程列表为空，无法继续，请检查配置文件是否正确")
            return
        }
        else println("设置加载已完成")
    } else {
        println("设置加载过程出现严重错误，系统自动退出")
        return
    }
    val post = GPost()
    print("正在尝试登录教务系统...")
    while (!post.login(username, password)) {
        print("正在重新尝试登录教务系统...")
    }
    var loginTime = System.currentTimeMillis()
    var count = 0
    var successCount = 0
    while (true) {
        count++
        var loopInfoHeaderOut = false
        if (loopInfoHeaderOut) println("在第 $count 次抢课过程中：")
        for (course in courseList) {
            val statusCode = post.add(course)
            if (statusCode != course.prevStatus) {
                if (!loopInfoHeaderOut) {
                    loopInfoHeaderOut = true
                    println("在第${count}次抢课过程中：")
                }
                print("\t课程(${course.courseId}, ${course.courseIndex})")
                when (statusCode) {
                    0 -> {
                        println("已成功被选择")
                        successCount++
                    }
                    1 -> {
                        println("返回列表为空，系统可能未开放或已失效")
                    }
                    2 -> {
                        println("正在进行中")
                    }
                    else -> {
                        println("出现未知错误")
                    }
                }
                course.prevStatus = statusCode
            }
            if (System.currentTimeMillis() - loginTime >= 900000) {
                print("教务系统会话已超时，正在尝试重新登录...")
                while (!post.login(username, password)) {
                    print("正在重新尝试登录教务系统...")
                }
                loginTime = System.currentTimeMillis()
            }
        }
        if (!loopInfoHeaderOut && count % 1 == 0)
            println("【已进行 $count 次抢课，进度 $successCount/${courseList.size} 】")
    }
}

