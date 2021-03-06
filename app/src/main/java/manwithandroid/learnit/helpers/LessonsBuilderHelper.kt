package manwithandroid.learnit.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import manwithandroid.learnit.app.LiApplication
import manwithandroid.learnit.models.Class
import manwithandroid.learnit.models.Lesson
import manwithandroid.learnit.models.Subject
import manwithandroid.learnit.utilities.TimeUtilities

/**
 * Created by Roi Amiel on 18/01/2018.
 */
object LessonsBuilderHelper {

    /* Database params */
    val lessonDurationVeryLong = FirebaseRemoteConfig.getInstance().getLong("lessonDurationVeryLong")
    val lessonDurationLong = FirebaseRemoteConfig.getInstance().getLong("lessonDurationLong")
    val lessonDurationMedium = FirebaseRemoteConfig.getInstance().getLong("lessonDurationMedium")
    val lessonDurationShort = FirebaseRemoteConfig.getInstance().getLong("lessonDurationShort")
    val lessonDurationVeryShort = FirebaseRemoteConfig.getInstance().getLong("lessonDurationVeryShort")

    /* Finals */
    private const val ALREADY_SET_ALARM_TAG = "AlreadySetBuildTaskAlarm"

    private const val INTERVAL_WEEK = AlarmManager.INTERVAL_DAY * 7

    private const val BUILD_TASK_CODE = 3345

    lateinit var lessonBuilderTaskIntent: PendingIntent

    fun initBuildTaskIntent(context: Context) {
        lessonBuilderTaskIntent = PendingIntent.getBroadcast(
                context,
                LessonsBuilderHelper.BUILD_TASK_CODE,
                Intent(context, LessonsBuilderHelper.LessonsBuildTaskReceiver::class.java),
                PendingIntent.FLAG_CANCEL_CURRENT)
    }

    /* Lessons build task */
    fun initBuildTask(force: Boolean = false, time: Long = TimeUtilities.getNextFirstDay()) {
        if (force || !LiApplication.pref.getBoolean(ALREADY_SET_ALARM_TAG, false)) {

            LiApplication.alarmManager.cancel(lessonBuilderTaskIntent)
            LiApplication.alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time, INTERVAL_WEEK, lessonBuilderTaskIntent)

            LiApplication.pref.edit().putBoolean(ALREADY_SET_ALARM_TAG, true).apply()
        }
    }

    fun updateBuildTask() {
        if (!UserHelper.isConnectedUser()) return

        if (UserHelper.getConnectedUser()?.lastLessonsBuildTask == null) return

        if (UserHelper.getConnectedUser()?.lastLessonsBuildTask?.time!! + INTERVAL_WEEK < TimeUtilities.currentClearTimeMillis()) {
            initBuildTask(true, TimeUtilities.getNextFirstDay(UserHelper.getConnectedUser()?.lastLessonsBuildTask?.time!!))
            buildTask()
        }
    }

    fun buildTask(firstBuildClass: Class? = null) {

        // Build classes
        fun buildClasses() {
            // Get classes list
            val firstBuild = firstBuildClass != null
            val classes = if (firstBuild) listOf(firstBuildClass!!) else UserHelper.getConnectedUser()?.classes

            // Check classes list validate
            if (classes == null || classes.isEmpty()) return

            val user = UserHelper.getConnectedUser()!!

            if (user.weekLessons == null) user.weekLessons = mutableListOf()

            // Move the uncompleted lessons to their list
            if (!firstBuild) {
                if (user.uncompletedLessons == null) user.uncompletedLessons = mutableListOf()
                if (user.weekLessons != null) user.uncompletedLessons!!.addAll(user.weekLessons!!)

                user.weekLessons?.clear()
            }

            // Build lessons for each class
            for (classObject in classes) {
                user.weekLessons?.addAll(buildLessons(classObject.key, firstBuild))
            }

            if (!firstBuild) {
                UserHelper.updateLastBuildTaskTime()
            }

            // Update the user data to the server
            UserHelper.updateUser {

            }
        }

        // Check if needs to auto login
        if (!UserHelper.isConnectedUser()) {
            UserHelper.connectUser(loginListener = {
                if (it.isSuccessful) buildClasses()
            })

        } else {
            buildClasses()
        }
    }

    private fun buildLessons(classKey: String, firstBuild: Boolean): List<Lesson> {

        val program = UserHelper.getProgramOf(classKey)

        // Returns the list of all the subjects that left for this class
        val subjectsLeft: List<Subject> = ClassesHelper.getSubjectsLeftFor(classKey)

        ClassesHelper.setUsedSubjects(classKey, listOf(0, 1, 2))

        //todo add content

        // placeholder code
        val lesson = Lesson()
        lesson.classKey = classKey
        lesson.name = "test lesson"
        lesson.description = "this is a test class"
        lesson.toWeekOfYear = 3
        lesson.toYear = 2018

        print(firstBuild)

        return listOf(lesson, lesson, lesson)
    }

    class LessonsBuildTaskReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            buildTask()
        }
    }
}