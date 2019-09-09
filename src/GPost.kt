import org.jsoup.Connection
import org.jsoup.Jsoup

class GPost {
    val ROOT_SYS = "http://bkjwxk.sdu.edu.cn"
    val ROOT_AUTH = "http://passt.sdu.edu.cn"
    val LOGIN = "$ROOT_AUTH/cas/login?service=http://bkjwxk.sdu.edu.cn/f/j_spring_security_thauth_roaming_entry"
    val CHOSEN = "$ROOT_SYS/f/xk/xs/yxkc"
    val SEARCH = "$ROOT_SYS/b/xk/xs/kcsearch"
    val ADD = "$ROOT_SYS/b/xk/xs/add"

    var engine = DESEngine()

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

        val indexPage = post(LOGIN, mapOf())
        cookies = indexPage.cookies()

        var reg = Regex("<form id=\"loginForm\" action=\"(.*?)\" method=\"post\">")
        var list = reg.findAll(indexPage.body()).toList()
        if (list.isEmpty()) return false
        val submitAddr = list[0].groupValues[1]

        reg = Regex("<input type=\"hidden\" id=\"lt\" name=\"lt\" value=\"(.*?)\" />")
        list = reg.findAll(indexPage.body()).toList()
        if (list.isEmpty()) return false
        val token = list[0].groupValues[1]

        reg = Regex("<input type=\"hidden\" name=\"execution\" value=\"(.*?)\" />")
        list = reg.findAll(indexPage.body()).toList()
        if (list.isEmpty()) return false
        val execution = list[0].groupValues[1]

        reg = Regex("<input type=\"hidden\" name=\"_eventId\" value=\"(.*?)\" />")
        list = reg.findAll(indexPage.body()).toList()
        if (list.isEmpty()) return false
        val eventId = list[0].groupValues[1]

        val data = mapOf(
            "rsa" to engine.encode(username + password + token , "1" , "2" , "3"),
            "ul" to username.length.toString(),
            "pl" to password.length.toString(),
            "lt" to token,
            "execution" to execution,
            "_eventId" to eventId
        )
        var res = post(ROOT_AUTH + submitAddr , data)
        if (!res.hasCookie("sduxk")) {
            println("失败")
            Thread.sleep(2000)
            return false;
        }
        cookies = res.cookies()
        val xk = res.cookie("sduxk")
        res = post(LOGIN , mapOf())

        cookies = mapOf(
            "sduxk" to xk,
            "JSESSIONID" to res.cookie("JSESSIONID"),
            "index" to "1"
        )

        println("成功")
        Thread.sleep(2000)
        return true
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
                Thread.sleep(3000)
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
                Thread.sleep(3000)
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