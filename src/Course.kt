class Course(var courseId: String, var courseIndex: String) {
    var done = false
    var prevStatus = 2147483647
    override fun toString(): String {
        return "(${this.courseId}, ${this.courseIndex})"
    }
}