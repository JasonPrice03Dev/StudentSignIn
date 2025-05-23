# 🎓 StudentSignIn – Final Year Project

An Android attendance-tracking app that uses QR codes and facial recognition to streamline student sign-ins and provide real-time attendance data to lecturers.

---

## 📌 Overview

**StudentSignIn** is a mobile application designed to automate classroom attendance using QR code validation and facial recognition. Students scan a QR code presented by a lecturer and verify their identity using facial recognition to mark attendance securely and reliably. Lecturers can monitor and track attendance stats in real time via a dashboard.

---

## 📱 Features

- 🔐 Secure Login for Students and Lecturers  
- 📅 Course-Based Timetable View  
- 📷 QR Code Scanner to Validate Attendance Sessions  
- 🧠 Facial Recognition via Google ML Kit  
- 📝 Automatic Attendance Recording  
- 📊 Real-Time Attendance Dashboard for Lecturers  
- 🧾 QR Expiry Mechanism to Prevent Screenshot Reuse  

---

## 🛠️ Built With

| Layer       | Technology                         |
|-------------|-------------------------------------|
| Language    | Kotlin                              |
| UI          | Jetpack Compose                     |
| Backend     | Firebase Firestore & Authentication |
| Scanning    | ZXing QR Code Library               |
| Face Auth   | Google ML Kit                       |
| IDE         | Android Studio                      |
| State Mgmt  | ViewModel, Navigation, Coroutines   |

---

📲 App Flow
Login Page – Students/lecturers authenticate using email/password.
Hub Page – Students see class timetables based on selected course.
QR Code Scanner – Scan the code provided by the lecturer for the current session.
Facial Recognition – Camera verifies student identity.
Attendance Confirmation – Attendance is automatically marked and stored.

---

👨‍💻 Author
**Jason Price** – Final Year BSc. (Hons) Internet Systems Development student  
📍 Ireland  
🔗 [LinkedIn](https://www.linkedin.com/in/jasonpricedev/)
