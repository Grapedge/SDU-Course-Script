import org.jsoup.Connection
import org.jsoup.Jsoup

class GPost {
    val ROOT = "http://bkjwxk.sdu.edu.cn"
    val LOGIN = "$ROOT/b/ajaxLogin"
    val CHOSEN = "$ROOT/f/xk/xs/yxkc"
    val SEARCH = "$ROOT/b/xk/xs/kcsearch"
    val ADD = "$ROOT/b/xk/xs/add"
    private var cookies = mapOf<String, String>()
    // post
    @Throws(Exception::class)
    private fun post(
        url: String,
        data: Map<String, String>
    ): Connection.Response {
        val con = Jsoup.connect(url)
        for ((key, value) in data)
            con.data(key, value)
        con.cookies(cookies).ignoreContentType(true)
        return con.method(Connection.Method.POST).execute()
    }

    fun login(username: String, password: String): Boolean {
        cookies = mapOf()
        val data = mapOf("j_username" to username,
            "j_password" to password.md5())
        val res = post(LOGIN, data)
        cookies = res.cookies()
        return if (res.body().indexOf("success") != -1) {
            println("成功")
            Thread.sleep(2000)
            true
        } else {
            println("失败")
            Thread.sleep(2000)
            false
        }
    }

    fun add(course: Course): Int {
        try {
            if (course.done) return 1
            val resCode = search(course)        // 在总列表中查询此课程
            if (resCode == 1) {                 // 查询到课余量
                post(
                    "$ADD/${course.courseId}/${course.courseIndex}",
                    mapOf()
                )
                //println(res.body())
                Thread.sleep(200)
                return if (check(course, true)) {   // 检查是否真的选上了
                    course.done = true
                    1
                } else 3
            } else if (resCode < 0 && !course.triedToSubmit) {
                course.triedToSubmit = true
                post(
                    "$ADD/${course.courseId}/${course.courseIndex}",
                    mapOf()
                )
                //println(res.body())
                Thread.sleep(200)
                if (check(course, true)) {
                    course.done = true
                    return 5
                }
            }
            return resCode
        } catch (e: Exception) {
            //println("出现未知错误")
            e.printStackTrace()
            return 4
        }
    }

    private var chosenList = "NULL"
    fun refreshChosenList() {
        chosenList = post(CHOSEN, mapOf()).body()
    }
    fun check(course: Course, refresh: Boolean):Boolean {     //检查课程是否已在选课成功的列表中
        if (chosenList == "NULL" || refresh) {
            refreshChosenList()
        }
        val reg = Regex("value=\"${course.courseId}\\|${course.courseIndex}\"")
        val list = reg.findAll(chosenList).toList()
        if (list.isEmpty()) return false
        return true
    }

    fun search(course: Course):Int {
        val pre = post(
            SEARCH,
            mapOf("type" to "kc",
                "currentPage" to "1",
                "kch" to course.courseId,
                "jsh" to "",
                "skxq" to "",
                "skjc" to "",
                "kkxsh" to "")
        )
        val preReg = Regex("\"totalPages\":(\\d*)")
        val temp = preReg.findAll(pre.body()).toList()
        if (temp.isEmpty()) return 4
        val pages = temp[0].groupValues[1].toInt()
        var page = 1
        while (page <= pages) {
            val res = post(
                SEARCH,
                mapOf("type" to "kc",
                    "currentPage" to page.toString(),
                    "kch" to course.courseId,
                    "jsh" to "",
                    "skxq" to "",
                    "skjc" to "",
                    "kkxsh" to "")
            )
            val reg = Regex("\"KXH\":\"${course.courseIndex}\".*?\"kyl\":([+-]?(\\d*))")
            //println("OUTPUT:${res.body()}")
            val list = reg.findAll(res.body()).toList()
            page++
            return if (list.isEmpty()) continue
            else if (list[0].groupValues[1].toInt() > 0) 1
            else list[0].groupValues[1].toInt()
        }
        return 2
    }
}