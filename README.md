# Paani Reminder (Android app)

Dark purple theme, daily water tracker with reminders that ring even on Silent/Vibrate.

## Features
- Reminder **Alarm volume** par bajta hai, is liye Silent/Vibrate mode mein bhi awaaz + vibration aati hai
- **Reminders ON/OFF** switch (meeting mode) + "1 ghanta chup" / "2 ghante chup" buttons
- **Active hours**: uthne ka waqt aur sone ka waqt. Is ke bahar koi reminder nahi
  (pehla reminder uthne ke waqt + interval par aata hai, aakhri sone ke waqt tak)
- **Custom ringtone**: phone ki built-in ringtones/notification/alarm tones, ya apni audio file
- Har reminder par 2 option: **Paani pee liya** / **Abhi nahi (Skip)**. Dabate hi ringing band
- Daily goal glass mein, History: Hafta / Mahina / Saal chart, har din par tap karke piye/skip ka waqt

## APK banane ka tareeqa (GitHub se, computer ke baghair bhi)
1. github.com par free account banayein, **New repository** (naam: `paani-reminder`).
2. Is zip ke saare files repository mein upload karein. **Hidden folder `.github`** bhi shamil hona chahiye.
   - Computer se: zip extract karke poora folder drag-drop karein (Add file > Upload files).
   - Sirf phone se: **Add file > Create new file**, naam likhein `.github/workflows/build.yml`
     aur is zip ke `build.yml` ka content paste karein. Baaqi files ko bhi isi tarah ya
     phone ke file manager se folder-by-folder upload karein.
3. Commit karte hi **Actions** tab mein "Build APK and XAPK" chalega (3-6 minute).
4. Khatam hone par repository ke **Releases** (right side) mein jayein:
   - `PaaniReminder.apk`  <- **phone mein yehi install karein**
   - `PaaniReminder.xapk` <- sirf agar aap XAPK installer app (APKPure etc.) use karte hain
5. Install se pehle "Unknown sources / Install unknown apps" ki ijazat dein.

Note: XAPK asal mein APK ka zip hota hai. Is app ko XAPK ki zaroorat nahi, seedha APK install hoti hai.

## Pehli baar chalane ke baad
1. Notifications ki ijazat "Allow" karein.
2. Settings mein "Battery optimization hatayein" dabayein (Xiaomi/Oppo/Vivo/Samsung par
   "Autostart" / "Unrestricted battery" bhi on karein, warna phone reminders maar sakta hai).
3. Settings > "Reminder abhi test karein" se Silent mode mein awaaz check karein.
4. Phone ka **Alarm volume** zero na ho.

## Android Studio se banana ho to
Project kholein, **Build > Build APK(s)**, ya terminal mein `gradle assembleRelease`
(Gradle 8.7, JDK 17). Release APK is project ki `app/waterbuddy.jks` keystore se sign hoti hai, is liye
agli builds purani app ke upar update ho jayengi (history nahi jayegi).
