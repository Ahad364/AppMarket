# AppMarket — Android অ্যাপ মার্কেটপ্লেস

Kotlin · Jetpack Compose · Hilt · Firebase (Auth, Firestore, Storage)
Min SDK 26 · Target SDK 34

## ফিচার

| ফিচার | কোথায় |
|---|---|
| অ্যাপ লিস্টিং, ক্যাটাগরি, সার্চ | `HomeScreen`, `StoreRepository.listApps / searchApps` |
| লগইন (Email + Google), প্রোফাইল, লগআউট | `AuthScreen`, `ProfileScreen` |
| APK ডাউনলোড (প্রগ্রেস সহ) ও ইনস্টল | `DetailScreen`, `StoreRepository.downloadApk`, `ApkInstaller` |
| রিভিউ ও রেটিং (১ ইউজার = ১ রিভিউ) | `DetailScreen`, `StoreRepository.submitReview` |

## প্রজেক্ট কাঠামো

```
app/src/main/java/com/appmarket/store/
├── MarketApp.kt                 Hilt application
├── di/AppModule.kt              Firebase instances
├── model/Models.kt              AppItem, Review, UserProfile
├── data/StoreRepository.kt      সব Firebase কাজ এক জায়গায়
├── install/ApkInstaller.kt      FileProvider + ACTION_VIEW (package installer)
└── ui/
    ├── MainActivity.kt          NavHost ও রুট
    ├── AuthScreen.kt            Login / Sign up / Google
    ├── HomeScreen.kt            সার্চ, ক্যাটাগরি, অ্যাপ লিস্ট
    ├── DetailScreen.kt          ইনস্টল, রেটিং, রিভিউ
    ├── ProfileScreen.kt         প্রোফাইল ও লগআউট
    ├── ViewModels.kt            StateFlow-ভিত্তিক ViewModel
    └── Components.kt            AppCard, RatingRow, StarPicker
firebase/
├── firestore.rules
└── storage.rules
```

## Firestore ডেটা মডেল

```
apps/{appId}
  name:          "Notes Pro"
  nameLower:     "notes pro"          ← সার্চের জন্য (অবশ্যই lowercase)
  developer:     "Acme Labs"
  packageName:   "com.acme.notes"
  versionName:   "1.2.0"
  versionCode:   12
  description:   "..."
  category:      "Productivity"       ← Productivity | Games | Social | Tools | Education
  iconUrl:       "https://firebasestorage.../icons/notes.png"
  apkPath:       "apks/com.acme.notes-12.apk"   ← Storage-এর পাথ
  sizeBytes:     15728640             ← সঠিক সাইজ (বাইটে), ক্যাশ যাচাইয়ের জন্য
  ratingSum:     0                    (number)
  ratingCount:   0                    (number)

apps/{appId}/reviews/{uid}
  uid, userName, rating (1-5), text, createdAt (server timestamp)

users/{uid}
  name, email
```

## Firebase সেটআপ

1. Firebase Console-এ প্রজেক্ট বানান, Android app যোগ করুন: `com.appmarket.store`।
2. `google-services.json` ডাউনলোড করে `app/` ফোল্ডারে রাখুন।
3. **Authentication** → Email/Password এবং Google চালু করুন।
4. **Firestore** তৈরি করে `firebase/firestore.rules` এর কন্টেন্ট Rules-এ বসান।
5. **Storage** তৈরি করে `firebase/storage.rules` বসান।
6. Google sign-in-এর জন্য Support email সেট করুন এবং `google-services.json`-এ `default_web_client_id` থাকে কিনা নিশ্চিত করুন।

### প্রথম অ্যাপ যোগ করা (অ্যাডমিন)

অ্যাপ APK ও আইকন Storage-এ আপলোড করুন (`apks/`, `icons/` ফোল্ডারে), তারপর Firestore-এ `apps` কালেকশনে একটি ডক বানান উপরের মডেল অনুযায়ী। `ratingSum` ও `ratingCount` শুরুতে `0` রাখুন।

> সার্চ কাজ করতে `nameLower` ফিল্ডটি অবশ্যই ছোট হাতের অক্ষরে থাকতে হবে।

## Termux থেকে বিল্ড

```bash
pkg install git unzip
termux-setup-storage
cd ~ && unzip ~/storage/downloads/AppMarket.zip
cd AppMarket
git init && git add . && git commit -m "AppMarket v1"
git remote add origin https://github.com/<you>/AppMarket.git
git push -u origin main
```

`.github/workflows/android.yml` GitHub-এ পুশ করলে Actions থেকে debug APK পাওয়া যাবে। Termux-এ সরাসরি Gradle বিল্ড কঠিন, তাই GitHub Actions বা Android Studio ব্যবহার করুন।

## জানা সীমাবদ্ধতা

- **সার্চ** শুধু নামের prefix দিয়ে কাজ করে। পূর্ণ-টেক্সট সার্চের জন্য Algolia বা Typesense যোগ করতে হবে।
- **লিস্টিং** প্রথম ১০০টি অ্যাপ লোড করে। বড় ক্যাটালগে pagination যোগ করতে হবে।
- **APK সাইনিং**: ইনস্টলের সময় Android নিশ্চিত করে যে নতুন APK-এর signature আগের সংস্করণের সাথে মেলে। এটি মার্কেটপ্লেসের নিজস্ব নিরাপত্তা।
- **Unknown sources**: Android 8+-এ ব্যবহারকারীকে প্রথমবার "অজানা উৎস থেকে ইনস্টল" অনুমতি দিতে হয়। অ্যাপে সেই সেটিংসে নেওয়ার ব্যবস্থা আছে।
- **ডেভেলপার আপলোড স্ক্রিন** এই সংস্করণে নেই। এটি পরের ধাপে যোগ করা যাবে।
- **Play Store-এ প্রকাশ**: নিজের APK স্টোর হিসেবে Play Store-এ অনুমোদন পাওয়া কঠিন (Play নীতিমালা অন্য স্টোরের অ্যাপ ইনস্টল/ডাউনলোড সীমিত করে)। প্রাইভেট বা sideload বিতরণের জন্য উপযুক্ত।
- কোডটি এখানে কম্পাইল করে যাচাই করা হয়নি। প্রথম বিল্ডে ছোট কম্পাইল এরর আসতে পারে, এরর মেসেজ দিলে ঠিক করে দেব।
