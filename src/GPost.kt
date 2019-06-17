import org.jsoup.Connection
import org.jsoup.Jsoup

class GPost {
    val ROOT = "http://bkjwxk.sdu.edu.cn/b/xk/xs"
    val LOGIN = "http://bkjwxk.sdu.edu.cn/b/ajaxLogin"
    val SEARCH = "$ROOT/kcsearch"
    val ADD = "$ROOT/add"
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
        if (res.body().indexOf("success") != -1) {
            println("成功")
            Thread.sleep(2000)
            return true
        } else {
            println("失败")
            Thread.sleep(2000)
            return false
        }
    }

    fun add(course: Course): Int {
        try {
            if (course.done) return 0
            val resCode = search(course)
            if (resCode == 0) {
                val res = post(
                    "$ADD/${course.courseId}/${course.courseIndex}",
                    mapOf()
                )
                //println(res.body())
                course.done = true
            }
            return resCode
        } catch (e: Exception) {
            //println("出现未知错误")
            e.printStackTrace()
            return -1
        }
    }
    private fun search(course: Course):Int {
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
        if (temp.isEmpty()) return -1
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
            if (list.isEmpty()) continue
            else if (list[0].groupValues[1].toInt() > 0) return 0
            else return 2
        }
        return 1
    }
}