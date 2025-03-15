package com.mad.studentsignin.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.mad.studentsignin.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.*

class StartPageViewModel : ViewModel() {

    val auth: FirebaseAuth = FirebaseAuth.getInstance() // Retrieving authentication instance

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance() // Retrieving db instance

    // Data class of student matching that which is in Firebase
    data class Student(
        val UID: String = "",
        val role: String = "",
        val email: String = "",
        val faceUrl: String? = null,
        val faceImageUrl: String? = null
    )

    // A private MutableLiveData to hold the list of students
    private val _students = MutableLiveData<List<Student>>()

    // A public LiveData for observing the student list
    val students: LiveData<List<Student>> = _students

    // Loading students belonging to a specific module
    fun loadStudents(moduleName: String) {
        // Querying the "users" collection in the Firebase database
        db.collection("users").whereEqualTo("course", moduleName).whereEqualTo("role", "student")
            .get().addOnSuccessListener { snapshot ->
                // Converting the snapshot of documents to a list of Student objects
                val studentList = snapshot.toObjects(Student::class.java)
                // Updating the LiveData with the retrieved student list
                _students.value = studentList
                Log.d("AttendanceViewing", "Students loaded: ${studentList.size}")
            }.addOnFailureListener { exception ->
                Log.e("AttendanceViewing", "Error loading students: ${exception.message}")
            }
    }

    private var isUserSignedIn = false // User being signed in as default false

    var userCourse: String? = null // User course as a default of null

    val loginError = MutableLiveData<String?>() // Creating loginError message

    // Function to allow user to login
    fun loginUser(kemail: String, password: String, onLoginSuccess: (String, String) -> Unit) {
        if (kemail.isEmpty() || password.isEmpty()) {
            return loginError.postValue("Email and Password may not be empty")
        }

        // Signing in the user via Firebase Authentication
        auth.signInWithEmailAndPassword(kemail, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get the currently logged-in user
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    Log.d("Login", "User signed in: ${currentUser.uid}")

                    // Retrieve user details from the "users" collection
                    db.collection("users").document(currentUser.uid).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                // Extract the user's role (student or lecturer)
                                val role = document.getString("role")
                                Log.d("Login", "User role: $role")
                                if (role != null) {
                                    if (role == "student") {
                                        // If the user is a student, fetch their course
                                        val course = document.getString("course")
                                        if (course != null) {
                                            userCourse = course
                                            onLoginSuccess(
                                                role, course
                                            )
                                        } else {
                                            loginError.postValue("Course not found for user")
                                        }
                                    } else if (role == "lecturer") {
                                        // If the user is a lecturer, no course is needed
                                        onLoginSuccess(role, "")
                                    }
                                } else {
                                    loginError.postValue("Role not found for user")
                                }
                            } else {
                                loginError.postValue("User not found")
                            }
                        }.addOnFailureListener { e ->
                            loginError.postValue("Error fetching user data")
                        }
                }
            } else {
                loginError.postValue("Incorrect email or password")
            }
        }
    }

    // Data class of a timetableEntry
    data class TimetableEntry(
        val id: String = "",
        val subject: String = "",
        val startTime: String = "",
        val endTime: String = "",
        val day: String = "",
        val classId: String = "",
        val course: String = "",
        val totalClasses: Int = 0
    )

    // LiveData object to store the list of timetable entries
    val timetableEntries = MutableLiveData<List<TimetableEntry>>()

    // Function to load timetable entries
    fun loadTimetableEntries(course: String) {
        db.collection("timetable").whereEqualTo("course", course).orderBy("startTime").get()
            .addOnSuccessListener { result ->
                // Convert Firestore documents to TimetableEntry objects
                val entries = result.map { document ->
                    document.toObject(TimetableEntry::class.java)
                }
                timetableEntries.postValue(entries)
            }.addOnFailureListener { e ->
                Log.e("Firestore", "Error loading timetable entries", e)
            }
    }

    // Function to load timetable entries
    fun loadLecturerTimetableEntries(lecturerId: String) {
        db.collection("timetable").whereEqualTo("lecturerId", lecturerId).get()
            .addOnSuccessListener { documents ->
                val timetableEntries = mutableListOf<TimetableEntry>()
                for (document in documents) {
                    // Convert Firestore documents to TimetableEntry objects
                    val entry = document.toObject(TimetableEntry::class.java)
                    timetableEntries.add(entry)
                }

                // Sorting timetable by startTime
                timetableEntries.sortBy { it.startTime }

                // Posting sorted timetable to the timetableEntries data class
                this.timetableEntries.postValue(timetableEntries)
            }.addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }

    // Adapter class for displaying timetable entries in a RecyclerView
    class TimetableAdapter(private val entries: List<TimetableEntry>) :
        RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {

        // ViewHolder class to bind timetable entry data to the UI
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val subjectTextView: TextView = view.findViewById(R.id.subjectTextView)
            val timeTextView: TextView = view.findViewById(R.id.timeTextView)
            val dayTextView: TextView = view.findViewById(R.id.dayTextView)
        }

        // Inflating the layout for each item in the RecyclerView
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.timetable_item, parent, false)
            return ViewHolder(view)
        }


        // Binding timetable entry data to the UI elements
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = entries[position]
            holder.subjectTextView.text = entry.subject
            holder.timeTextView.text = "${entry.startTime} - ${entry.endTime}"
            holder.dayTextView.text = entry.day
        }

        // Returning the total number of items within dataset
        override fun getItemCount() = entries.size
    }

    // Repository to handle timetable data
    class TimetableRepository {
        private val db = FirebaseFirestore.getInstance()
    }

    // Fragment used to display a timetable using a RecyclerView
    class TimetableFragment : Fragment() {

        private lateinit var timetableAdapter: TimetableAdapter
        private lateinit var timetableRepository: TimetableRepository
        private lateinit var viewModel: StartPageViewModel

        // Inflating the fragment layout and setting up the RecyclerView
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.fragment_timetable, container, false)
            val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(context)

            // Initializing the ViewModel to load timetable entries
            viewModel = ViewModelProvider(this).get(StartPageViewModel::class.java)

            // Course argument being passed to the fragment
            val course = arguments?.getString("course") ?: ""

            // Observing timetable entries and updating the RecyclerView adapter
            viewModel.loadTimetableEntries(course)
            viewModel.timetableEntries.observe(viewLifecycleOwner, { entries ->
                timetableAdapter = TimetableAdapter(entries)
                recyclerView.adapter = timetableAdapter
            })

            return view
        }
    }

    // Function to mark the attendance of a student for a specific class
    fun markAttendance(timetableEntryId: String, classId: String, onSuccess: () -> Unit) {
        // Prevent marking attendance if the user is already signed in to the class
        if (isUserSignedIn) {
            return
        }

        // Getting the current authenticated user's ID
        val userId = auth.currentUser?.uid ?: return

        // Creating a map to store the attendance data
        val attendanceData = hashMapOf(
            "userId" to userId,
            "timetableEntryId" to timetableEntryId,
            "classId" to classId,
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Adding the attendance data to the Firestore collection
        db.collection("attendance").add(attendanceData).addOnSuccessListener {
            // Mark the user as signed in and invoke the success callback
            isUserSignedIn = true
            onSuccess()
        }.addOnFailureListener { e ->
            Log.e("StartPageViewModel", "Failed to mark attendance", e)
        }
    }

    // Function to generates a QR Code bitmap from the given data
    fun generateQRCode(data: String): Bitmap {
        // Creating a BitMatrix for the QR code using the given data
        val bitMatrix = MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 200, 200)

        // Getting the dimensions (Width and Height) of the BitMatrix
        val width = bitMatrix.width
        val height = bitMatrix.height

        // Creating an empty bitmap with the same dimensions as the BitMatrix
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        // Populating the empty bitmap with black and white pixels based on the BitMatrix values
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        // Returning the generated QR code bitmap
        return bmp
    }

    // Funtion to update session information in Firestore with the unique identifier and expiration time
    fun updateSessionInFirestore(uniqueIdentifier: String, classId: String, expirationTime: Long) {
        val sessionRef = FirebaseFirestore.getInstance().collection("sessions").document(classId)
        val sessionData = hashMapOf(
            "uniqueIdentifier" to uniqueIdentifier, "expirationTime" to expirationTime
        )

        sessionRef.set(sessionData).addOnSuccessListener {
            Log.d("Firestore", "Session updated successfully!")
        }.addOnFailureListener { exception ->
            Log.e("Firestore", "Error updating session: ", exception)
        }
    }

    private var qrCodeTimer: Timer? = null // Setting QR timer to a default of null

    private var isRegenerating = false // Track if regeneration of QR Code is running

    // Starts a timer to regenerate QR code every 15 seconds
    fun startQRCodeRegenerationTimer(classId: String, navController: NavHostController) {
        // Prevent multiple timers from being started
        if (isRegenerating) return

        isRegenerating = true
        qrCodeTimer = Timer()
        qrCodeTimer?.schedule(object : TimerTask() {
            override fun run() {
                val newUniqueIdentifier = "classSessionID_${classId}_${UUID.randomUUID()}"
                val newExpirationTime = System.currentTimeMillis() + 15 * 1000 // 15 seconds

                val newQRCodeBitmap = generateQRCode(newUniqueIdentifier)
                updateSessionInFirestore(newUniqueIdentifier, classId, newExpirationTime)

                Log.d("QRCodeGeneration", "Generated new QR Code: $newUniqueIdentifier")

                // Navigate to the new QR code page, passing the new unique identifier
                Handler(Looper.getMainLooper()).post {
                    navController.navigate("qrCodePage/$newUniqueIdentifier")
                }
            }
        }, 0, 15000) // repeat every 15 seconds
    }

    // Stops the QR code regeneration timer
    fun stopQRCodeRegenerationTimer() {
        qrCodeTimer?.cancel()
        qrCodeTimer = null
        isRegenerating = false
        Log.d("QRCodeGeneration", "QR Code regeneration stopped.")
    }

    // Function to check if QR Code is regenerating
    fun isQRCodeRegenerating(): Boolean {
        return isRegenerating
    }

    // Generates a QR Code for a specific class session and navigates to the QR code page
    fun generateQRCodeForClass(classId: String, navController: NavHostController) {
        // Creating a unique identifier for the class session by combining the classId and a randomly generated UUID
        // (Universally Unique Identifier)
        var uniqueIdentifier = "classSessionID_${classId}_${UUID.randomUUID()}"
        Log.d("QRCodeGeneration", "Generated uniqueIdentifier: $uniqueIdentifier")

        // Setting expiration time
        var expirationTime = System.currentTimeMillis() + 15 * 1000 // 15 seconds

        val qrCodeBitmap = generateQRCode(uniqueIdentifier)

        // Update the session information in Firestore with the unique identifier and expiration time
        updateSessionInFirestore(uniqueIdentifier, classId, expirationTime)

        // Navigate to the QR code display page, passing the unique identifier as a parameter
        navController.navigate("qrCodePage/$uniqueIdentifier")

        startQRCodeRegenerationTimer(classId, navController)
    }

    // Function to check if the QR code is still valid
    fun isQRCodeValid(uniqueIdentifier: String, callback: (Boolean) -> Unit) {
        db.collection("sessions").whereEqualTo("uniqueIdentifier", uniqueIdentifier).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    callback(false) // QR code not found
                } else {
                    val session = documents.first()
                    val expirationTime = session.getLong("expirationTime") ?: 0
                    val currentTime = System.currentTimeMillis()

                    if (currentTime < expirationTime) {
                        callback(true) // QR code is still valid
                    } else {
                        callback(false) // QR code has expired
                    }
                }
            }.addOnFailureListener {
                callback(false) // Error checking QR code validity
            }
    }

    // Funtion to store the user's face data to Firebase Storage
    fun storeFaceData(faceBitmap: Bitmap) {
        val userId = auth.currentUser?.uid
        Log.d("UserID", "Current User UID: $userId")
        if (userId == null) {
            Log.e("FaceData", "User ID is null. User might not be authenticated.")
            return
        }

        // Reference to Firebase Storage where the face data will be stored
        val storageRef = FirebaseStorage.getInstance().reference.child("faces/$userId.jpg")
        Log.d("FaceData", "Storing face data at path: faces/$userId.jpg")

        // Compress the Bitmap image of face into a byte array
        val baos = ByteArrayOutputStream()
        faceBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        // Upload the face data to Firebase Storage
        val uploadTask = storageRef.putBytes(data)
        uploadTask.addOnSuccessListener {

            // On success, retrieve the download URL of the uploaded image
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                Log.d("FaceData", "Download URL: $uri")

                // Update the Firestore users collection with the URL of the face image
                db.collection("users").document(userId).update("faceUrl", uri.toString())
                    .addOnSuccessListener {
                        Log.d("FaceData", "Face URL successfully updated in Firestore")
                    }.addOnFailureListener { e ->
                        Log.e("FaceData", "Failed to update face URL in Firestore", e)
                    }
            }.addOnFailureListener { e ->
                Log.e("FaceData", "Failed to get download URL", e)
            }
        }.addOnFailureListener { e ->
            Log.e("FaceData", "Image upload failed", e)
        }
    }

    // Funtion to normalize the features using standard deviation
    fun standardDeviationNormalize(features: FloatArray): FloatArray {
        val mean = features.average().toFloat()
        val stdDev =
            Math.sqrt(features.map { Math.pow((it - mean).toDouble(), 2.0) }.average()).toFloat()
        return features.map { (it - mean) / stdDev }.toFloatArray()
    }

    // Function to compare two faces by extracting landmark features and calculating distance between landmarks
    fun compareFaceFeaturesWithEuclidean(face1: Face, face2: Face): Boolean {
        val threshold = 0.58f

        // Calculate Euclidean distance between two feature vectors
        fun euclideanDistance(vector1: FloatArray, vector2: FloatArray): Float {
            return Math.sqrt(vector1.zip(vector2) { a, b -> (a - b) * (a - b) }.sum().toDouble())
                .toFloat()
        }

        // Normalize the feature vectors to standard deviation
        fun normalizeFeatures(features: FloatArray): FloatArray {
            return standardDeviationNormalize(features)
        }

        // Extract landmarks for both faces
        val face1Landmarks = floatArrayOf(
            face1.getLandmark(FaceLandmark.LEFT_EYE)?.position?.x ?: 0f,
            face1.getLandmark(FaceLandmark.LEFT_EYE)?.position?.y ?: 0f,
            face1.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.x ?: 0f,
            face1.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.y ?: 0f,
            face1.getLandmark(FaceLandmark.NOSE_BASE)?.position?.x ?: 0f,
            face1.getLandmark(FaceLandmark.NOSE_BASE)?.position?.y ?: 0f,
            face1.getLandmark(FaceLandmark.MOUTH_LEFT)?.position?.x ?: 0f,
            face1.getLandmark(FaceLandmark.MOUTH_LEFT)?.position?.y ?: 0f,
            face1.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position?.x ?: 0f,
            face1.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position?.y ?: 0f,
            face1.getLandmark(FaceLandmark.LEFT_CHEEK)?.position?.x ?: 0f,
            face1.getLandmark(FaceLandmark.LEFT_CHEEK)?.position?.y ?: 0f,
            face1.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position?.x ?: 0f,
            face1.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position?.y ?: 0f
        )

        val face2Landmarks = floatArrayOf(
            face2.getLandmark(FaceLandmark.LEFT_EYE)?.position?.x ?: 0f,
            face2.getLandmark(FaceLandmark.LEFT_EYE)?.position?.y ?: 0f,
            face2.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.x ?: 0f,
            face2.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.y ?: 0f,
            face2.getLandmark(FaceLandmark.NOSE_BASE)?.position?.x ?: 0f,
            face2.getLandmark(FaceLandmark.NOSE_BASE)?.position?.y ?: 0f,
            face2.getLandmark(FaceLandmark.MOUTH_LEFT)?.position?.x ?: 0f,
            face2.getLandmark(FaceLandmark.MOUTH_LEFT)?.position?.y ?: 0f,
            face2.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position?.x ?: 0f,
            face2.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position?.y ?: 0f,
            face2.getLandmark(FaceLandmark.LEFT_CHEEK)?.position?.x ?: 0f,
            face2.getLandmark(FaceLandmark.LEFT_CHEEK)?.position?.y ?: 0f,
            face2.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position?.x ?: 0f,
            face2.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position?.y ?: 0f
        )

        // Normalize the extracted landmarks of both faces
        val normalizedFace1Landmarks = normalizeFeatures(face1Landmarks)
        val normalizedFace2Landmarks = normalizeFeatures(face2Landmarks)

        // Calculate the Euclidean distance between the normalized feature vectors
        val distance = euclideanDistance(normalizedFace1Landmarks, normalizedFace2Landmarks)

        Log.d("FaceComparison", "Euclidean Distance: $distance")

        // Return true if the distance is below the threshold (indicating high chance of a match)
        return distance < threshold
    }

    // Function using Google ML Kit to compare two face bitmaps (live image vs firebase image)
    private fun compareFacesWithMLKit(
        bitmap1: Bitmap, bitmap2: Bitmap, onResult: (Boolean) -> Unit
    ) {
        // Configuring face detector options for accuracy
        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).build()

        // Creating face detector client
        val faceDetector = FaceDetection.getClient(faceDetectorOptions)

        // Converting the bitmaps to InputImage format
        val image1 = InputImage.fromBitmap(bitmap1, 0)
        val image2 = InputImage.fromBitmap(bitmap2, 0)

        // Processing the first image to detect faces
        faceDetector.process(image1).addOnSuccessListener { faces1 ->
            if (faces1.isNotEmpty()) {
                val face1 = faces1.first()

                // Processing the second image to detect faces
                faceDetector.process(image2).addOnSuccessListener { faces2 ->
                    if (faces2.isNotEmpty()) {
                        val face2 = faces2.first()
                        val isMatch = compareFaceFeaturesWithEuclidean(face1, face2)
                        onResult(isMatch)
                    } else {
                        onResult(false)
                    }
                }.addOnFailureListener {
                    onResult(false)
                }
            } else {
                onResult(false)
            }
        }.addOnFailureListener {
            onResult(false)
        }
    }

    // Function to compare a detected face with the stored face data in Firestore
    fun compareFaces(detectedFace: Bitmap, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onResult(false)

        // Fetching the stored face URL from Firestore
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            val faceUrl = document.getString("faceUrl")
            if (faceUrl != null) {
                // Retrieve the stored face image from Firebase Storage
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(faceUrl)
                storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                    val storedFace = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    // Compare the faces using Google ML Kit
                    compareFacesWithMLKit(detectedFace, storedFace, onResult)
                }.addOnFailureListener {
                    onResult(false)
                }
            } else {
                onResult(false)
            }
        }.addOnFailureListener {
            onResult(false)
        }
    }

    // Function to retrieve all classes the user has signed in for today
    @RequiresApi(Build.VERSION_CODES.O)
    fun getUserSignedInClasses(course: String, onResult: (Set<String>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        // Get the current date in the format of YYYY-MM-DD (ISO)
        val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        // Query the attendance collection for the user's attendance records
        db.collection("attendance").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { snapshot ->
                // Filter the attendance records for today's date and the course
                val signedInClassIds = snapshot.documents.filter {
                    it.getString("timetableEntryId")
                        ?.contains(course) == true && it.getDate("timestamp")?.toInstant()
                        ?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
                        .toString() == currentDate // Check if the date matches today's date
                }.mapNotNull { it.getString("classId") }
                    .toSet() // Map and collect class IDs as a set
                onResult(signedInClassIds) // Pass the set of signed-in class IDs to the result callback
            }
    }

    // Data class attendance matching that which is in Firebase
    data class Attendance(
        val userId: String = "",
        val classId: String = "",
        val timetableEntryId: String = "",
        val timestamp: Timestamp? = null
    )

    // Data class of Attendance records in Firebase
    data class AttendanceRecord(
        val userId: String, val classId: String, val timetableEntryId: String, val timestamp: Date
    )

    // A private MutableLiveData to hold the list of attendance
    private val _attendanceRecords = MutableLiveData<List<Attendance>>()

    // A publicly LiveData for observing the attendance records
    val attendanceRecords: LiveData<List<Attendance>> = _attendanceRecords

    // Function to load attendance records for a specific class and date
    fun loadAttendance(classId: String, courseName: String, todaysDate: String) {
        val calendar = Calendar.getInstance()

        // Set start of the day (00:00:00.000)
        val startOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // Set end of the day (23:59:59.999)
        val endOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        // Converting start and end time to Firebase Timestamp format
        val startTimestamp = Timestamp(startOfDay)
        val endTimestamp = Timestamp(endOfDay)

        // Querying the attendance collection for records matching the class ID, course name, and timestamp range
        db.collection("attendance").whereEqualTo("timetableEntryId", courseName)
            .whereEqualTo("classId", classId).whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .whereLessThanOrEqualTo("timestamp", endTimestamp)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("AttendanceViewing", "Error loading attendance: ${exception.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Converting snapshot data to a list of Attendance objects
                    val attendanceList = snapshot.toObjects(Attendance::class.java)
                    _attendanceRecords.value = attendanceList
                    Log.d("AttendanceViewing", "Attendance updated: ${attendanceList.size}")
                }
            }
    }

    // Function to remove attendance for a specific user
    fun removeAttendanceForDay(
        userId: String,
        classId: String,
        timetableEntryId: String,
        specificDate: Date,
        onSuccess: () -> Unit,  // Callback for successful deletion
        onFailure: (String) -> Unit // Callback for failure messages
    ) {
        // Defining the start of the selected day (00:00:00.000)
        val startOfDay = Calendar.getInstance().apply {
            time = specificDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // Defining the end of the selected day (23:59:59.999)
        val endOfDay = Calendar.getInstance().apply {
            time = specificDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        // Convert start and end time to Firebase Timestamp
        val startTimestamp = com.google.firebase.Timestamp(startOfDay)
        val endTimestamp = com.google.firebase.Timestamp(endOfDay)

        // Query attendance records for the given user, class, and date range
        db.collection("attendance").whereEqualTo("userId", userId).whereEqualTo("classId", classId)
            .whereEqualTo("timetableEntryId", timetableEntryId)
            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .whereLessThanOrEqualTo("timestamp", endTimestamp).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onFailure("No attendance record found for the specified criteria and date.")
                    return@addOnSuccessListener
                }

                // Start a batch operation to delete records
                val batch = db.batch()
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }

                // Committing batch deletion
                batch.commit().addOnSuccessListener {
                        // Removing deleted records from local LiveData
                        _attendanceRecords.value = _attendanceRecords.value?.filter {
                            it.userId != userId || it.timetableEntryId != timetableEntryId
                        }
                        Log.d("AttendanceRemoval", "Attendance records successfully deleted.")
                        onSuccess()
                    }.addOnFailureListener { exception ->
                        Log.e(
                            "AttendanceRemoval",
                            "Error removing attendance records: ${exception.message}"
                        )
                        onFailure("Error removing attendance records: ${exception.message}")
                    }
            }.addOnFailureListener { exception ->
                Log.e(
                    "AttendanceRemoval", "Error querying attendance records: ${exception.message}"
                )
                onFailure("Error querying attendance records: ${exception.message}")
            }
    }

    // Function to add attendance on a specific day
    fun addAttendance(studentId: String, classId: String, courseName: String, date: Date) {
        val attendanceRecord = AttendanceRecord(
            userId = studentId, timestamp = date, timetableEntryId = courseName, classId = classId
        )

        val db = FirebaseFirestore.getInstance()

        db.collection("attendance").add(attendanceRecord)
            .addOnSuccessListener { documentReference ->
                Log.d("Attendance", "Attendance marked for $studentId")

                // Updating UI by adding a new Attendance object to LiveData
                val newRecord = Attendance(
                    userId = studentId,
                    timestamp = com.google.firebase.Timestamp(date),
                    timetableEntryId = courseName,
                    classId = classId
                )

                // Updating attendance list in real time for user
                val updatedList = _attendanceRecords.value?.toMutableList() ?: mutableListOf()
                updatedList.add(newRecord)
                _attendanceRecords.value = updatedList
            }.addOnFailureListener { e ->
                Log.e("Attendance", "Error marking attendance", e)
            }
    }

    // Data class of attendance statistics for a specific subject
    data class AttendanceStat(
        val subject: String, val percentage: Float // Percentage of attended classes as a Float
    )

    // MutableLiveData to store the list of attendance statistics
    private val _attendanceStats = MutableLiveData<List<AttendanceStat>>()

    // Public LiveData to present the attendance statistics to the UI
    val attendanceStats: LiveData<List<AttendanceStat>> = _attendanceStats

    fun calculateAttendance(userId: String, moduleName: String, course: String) {
        viewModelScope.launch {
            val db = Firebase.firestore

            try {
                Log.d(
                    "AttendanceStats",
                    "Calculating attendance for User: $userId, Module: $moduleName, Course: $course"
                )

                // Fetching the timetable entry for the module
                val moduleSnapshot = db.collection("timetable").whereEqualTo("course", course)
                    .whereEqualTo("classId", moduleName).get().await()

                if (moduleSnapshot.isEmpty) {
                    Log.e("AttendanceStats", "Module not found: $moduleName in course: $course")
                    return@launch
                }

                val module = moduleSnapshot.documents.first().toObject(TimetableEntry::class.java)!!
                val classId = module.classId
                Log.d(
                    "AttendanceStats",
                    "Fetched module details - Class ID: $classId, Subject: ${module.subject}"
                )

                // Fetching the semester start date for the specific course
                val courseSnapshot = db.collection("courses")
                    .document(course)  // Assuming 'course' matches the courseId in Firestore
                    .get().await()

                if (!courseSnapshot.exists()) {
                    Log.e("AttendanceStats", "Course document not found: $course")
                    return@launch
                }

                val semesterStartDate = courseSnapshot.getDate("semesterStart")
                if (semesterStartDate == null) {
                    Log.e("AttendanceStats", "Semester start date is null for course: $course")
                    return@launch
                }

                Log.d("AttendanceStats", "Semester start date for $course: $semesterStartDate")

                // Calculate the number of weeks since semester start
                val currentDate = Calendar.getInstance().time
                val weeksPassed =
                    ((currentDate.time - semesterStartDate.time) / (1000 * 60 * 60 * 24 * 7)).toInt() + 1
                Log.d("AttendanceStats", "Weeks passed since semester start: $weeksPassed")

                // Fetching attendance records for this user, course, and module
                val attendanceSnapshot = db.collection("attendance").whereEqualTo("userId", userId)
                    .whereEqualTo("timetableEntryId", course).whereEqualTo("classId", classId).get()
                    .await()

                val attendedClasses = attendanceSnapshot.size()
                Log.d(
                    "AttendanceStats",
                    "Attendance records found: $attendedClasses for User: $userId in Module: $moduleName"
                )

                // Calculating attendance percentage
                val percentage =
                    if (weeksPassed > 0) (attendedClasses / weeksPassed.toFloat()) * 100 else 0f
                Log.d("AttendanceStats", "Attendance percentage calculated: $percentage%")

                // Posting the attendance stats
                val stats = listOf(AttendanceStat(module.subject, percentage))
                _attendanceStats.postValue(stats)

            } catch (e: Exception) {
                Log.e("AttendanceStats", "Error calculating attendance: ${e.message}")
            }
        }
    }
}