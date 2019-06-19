import java.io.BufferedReader
import java.io.FileReader
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

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

var post = GPost()

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

fun waitForLogin() {
    print("正在尝试登录教务系统...")
    while (!post.login(username, password)) {
        print("正在重新尝试登录教务系统...")
    }
}

fun checkCourseList(): Int {
    var sumCount = courseList.size
    println("正在执行程序预检查：")
    var allCheckedOut = true
    post.refreshChosenList()
    for (course in courseList) {
        if (post.check(course, false)) {
            println("\t课程${course}已在您的课程列表中")
            course.done = true
            sumCount--
        } else {
            if(post.search(course) == 1) {
                println("\t课程${course}未能找到该课程，请注意检查配置文件")
                allCheckedOut = false
            } else {
                println("\t课程${course}目标确认")
            }
        }
    }
    println("===================================")
    if (sumCount == 0) {
        println("所有课程已存在于您的列表中，没有可执行的操作")
        println("程序退出")
        return -1
    } else {
        if (allCheckedOut) println("程序预检查完毕，共 $sumCount 个目标，开始执行")
        else {
            println("有课程未能通过预检查，可能是因为课程序号填写错误，也可能是因为该课程暂未开放选课。")
            println("建议您检查配置文件中填写的内容，确认填写的课程序号都是正确的。")
            println("如果您确认您填写的内容都是正确的，那么您可以先启动本程序。等到该课程开放时，您可以第一时间选上该课程。")
            println("如果您确认要开始操作，请输入\"y\"，并按下回车键确认")
            if (Scanner(System.`in`).next() != "y") {
                println("操作已取消，程序退出")
                return -1
            }
        }
    }
    return sumCount
}

fun aftCheck() {
    println("任务已完成，正在重新登陆教务系统进行自动检查")
    Thread.sleep(1000)
    waitForLogin()
    println("自动检查结果：")
    var failNum = 0
    post.refreshChosenList()
    for (course in courseList) {
        if (post.check(course, false)) {
            println("\t课程${course}已确认在课程列表中")
        } else {
            println("\t课程${course}未成功在列表中检测到，操作可能无效")
            failNum++
        }
    }
    if (failNum == 0) {
        println("您的所有选课操作均已成功，建议您再次打开教务系统 ${post.ROOT} 进行确认")
    } else {
        println("有 $failNum 个课程未在列表中检测到，请登录教务网 ${post.ROOT} 手动确认课程状况")
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
        else println("设置加载已完成\n========山大软件园交通委提醒您========" +
                "\n        抢课千万条，安全第一条。\n        抢完不检查，亲人两行泪。\n===================================")
    } else {
        println("设置加载过程出现严重错误，系统自动退出")
        return
    }
    waitForLogin()
    var loginTime = System.currentTimeMillis()
    var count = 0
    val sumCount = checkCourseList()
    if (sumCount < 0) return
    var successCount = 0
    while (successCount < sumCount) {
        count++
        var loopInfoHeaderOut = false
        for (course in courseList) {
            if (course.done) continue
            val statusCode = post.add(course)
            if (statusCode != course.prevStatus) {
                if (!loopInfoHeaderOut) {
                    loopInfoHeaderOut = true
                    println("在第 $count 次监听操作中：")
                }
                print("\t课程$course")
                when (statusCode) {
                    1 -> {
                        println("已成功选择")
                        successCount++
                    }
                    2 -> {
                        println("未找到，建议您检查配置文件")
                    }
                    3 -> {
                        println("检测到课余量但未能成功选择，建议您打开教务系统 ${post.ROOT} ，确认该课程是否与其他课程冲突")
                    }
                    4 -> {
                        println("出现未知错误")
                    }
                    5 -> {
                        println("课余量不足但选课成功，该课程可能是抽签课程，建议您打开教务系统 ${post.ROOT} 确认")
                        successCount++
                    }
                    else -> {
                        println("当前课余量不足（${statusCode}），正在监听课余量变化")
                    }
                }
                course.prevStatus = statusCode
            }
            if (System.currentTimeMillis() - loginTime >= 900000) {
                println("教务系统会话已超时，系统将自动重新登录")
                waitForLogin()
                loginTime = System.currentTimeMillis()
            }
        }
        if (loopInfoHeaderOut || count % 1000 == 0)
            println("【已进行 $count 次监听操作，进度 $successCount/${sumCount} 】")
    }
    println("===================================")
    aftCheck()
}

