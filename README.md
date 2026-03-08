# LifePauseAI 🚨
### Autonomous Silent Emergency Detection System

LifePauseAI is an intelligent safety solution designed to detect **silent emergencies** such as kidnapping or forced situations where victims cannot speak or press an SOS button.

The system uses **sensor fusion, behavioral pattern detection, and AI-based danger scoring** to identify abnormal conditions and automatically alert trusted contacts with live location.

---

## 🚀 Problem

Many emergency situations go undetected because victims cannot manually activate an SOS alert.

Examples include:
- Kidnapping
- Forced silence
- Phone seizure
- Restricted movement

Most safety applications require manual interaction, which may not always be possible in real emergencies.

---

## 💡 Solution

LifePauseAI introduces an **autonomous safety system** that detects danger automatically using multiple signals from the smartphone.

The system analyzes:
- Movement patterns
- Sound/silence detection
- GPS location changes
- Device behavior signals

A **Danger Score Engine** evaluates these signals and determines if the situation is abnormal.

If a threat is detected, the system automatically sends **emergency alerts with live location** to trusted contacts.

---

## ✨ Key Features

- 🛑 **Silent Emergency Detection**  
  Detects danger even when the victim cannot press SOS.

- 📍 **Kidnap Pattern Detection**  
  Identifies suspicious movement patterns like sudden location jumps.

- 🚨 **Automatic Emergency Alerts**  
  Sends SMS / WhatsApp alerts with live location.

- 📡 **Crime-Zone Awareness**  
  Increased sensitivity in high-risk areas.

- 🔋 **Forced Phone Switch-Off Alert**  
  Sends last location before device shutdown.

- 📊 **Danger Score Engine**  
  Combines multiple signals to determine risk level.

---

## ⚙️ How It Works

1. Smartphone sensors collect signals such as:
   - Accelerometer movement
   - Microphone silence patterns
   - GPS location
   - Device state signals

2. A **sensor fusion AI engine** analyzes these signals.

3. The system calculates a **Danger Score**.

4. If the score exceeds a threshold:
   - Emergency alerts are sent
   - Trusted contacts receive live location.

---

## 🏗 Architecture Overview

System Components:

- **Mobile Application**
  - Android (Kotlin)
  - Sensor data collection
  - Danger score computation

- **AI Processing Layer**
  - Sensor fusion
  - Behavioral anomaly detection

- **Backend Services**
  - Firebase Realtime Database
  - Firebase Authentication
  - Firebase Cloud Messaging

- **Communication Layer**
  - SMS Manager API
  - WhatsApp Intent Integration
  - Live location sharing

---

## 🧪 Prototype Performance

| Metric | Result |
|------|------|
| Detection Speed | 10 – 30 seconds |
| Detection Accuracy | ~90% |
| Alert Response Time | 3 – 5 seconds |
| Location Accuracy | 5 – 10 meters |

Tested scenarios include:
- Sudden location jumps
- Forced movement
- Silence detection
- Forced phone shutdown attempts

---

## 🛠 Technologies Used

**Frontend**
- Android Studio
- Kotlin
- XML UI Design

**Backend**
- Firebase Realtime Database
- Firebase Authentication
- Firebase Cloud Messaging

**Location & Mapping**
- Google Maps API
- Crime-zone data integration

**AI Logic**
- Sensor Fusion
- Rule-Based AI Engine
- Danger Score Algorithm

---

## 📂 Project Structure
