class Course(var courseId: String, var courseIndex: String) {
    var done = false
    var prevStatus = 2147483647
    var triedToSubmit = false       // 有些课程是抽签的，因此即便课余量是负的也能选上，试一下
    override fun toString(): String {
        return "(${this.courseId}, ${this.courseIndex})"
    }
}