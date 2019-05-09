import org.jsoup.Connection
import org.jsoup.Jsoup

class GPost {
    val ROOT = "http://bkjwxk.sdu.edu.cn/b/xk/xs"
    var LOGIN = "http://bkjwxk.sdu.edu.cn/b/ajaxLogin"
    val SEARCH = "$ROOT/kcsearch"
    val ADD = "$ROOT/add"
    private var cookies = mapOf<String, String>()
    // post
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

    fun login(username: String, password: String) {
        val data = mapOf("j_username" to username,
            "j_password" to password.md5())
        val res = post(LOGIN, data)
        cookies = res.cookies()
        if (res.body().indexOf("success") != -1) {
            println("=======登录成功=======")
        }
        Thread.sleep(2000)
    }

    fun add(course: Course) {
        if (course.done)
            return
        if (search(course)) {
            val res = post(
                "$ADD/${course.courseId}/${course.courseIndex}",
                mapOf()
            )
            println(res.body())
            course.done = true
        }
    }
    private fun search(course: Course):Boolean {
        val res = post(
            SEARCH,
            mapOf("type" to "kc",
                "currentPage" to "1",
                "kch" to course.courseId,
                "jsh" to "",
                "skxq" to "",
                "skjc" to "",
                "kkxsh" to "")
        )
        val reg = Regex("\"KXH\":\"${course.courseIndex}\".*?\"kyl\":(\\d*)")
        if (reg.findAll(res.body()).toList()[0].groupValues[1].toInt() > 0)
            return true
        return false
    }
}