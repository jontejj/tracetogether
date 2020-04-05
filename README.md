# tracetogether

## Flow 1:
1. User downloads app
2. User starts app
3. App tracks who the user comes into contact (other users) (using bluetooth)
4. User suspects that they have the virus (they have symptoms ), they go to get tested
5. User goes home
6. Doctor confirms that the user has the virus. (Doctor has the user's phone number)
7. Doctor uses an admin UI to send an sms to the user
8. Sms opens the app and asks the user to confirm that their data is sent
10. App uploads users that have been close to the confirmed case
11. Server sends out push notifications to potential cases.
12. Potential cases should consider testing themselves if they also have symptoms.

## Flow 2:
1. User downloads app
2. User starts app
3. App tracks who the user comes into contact (other users) (using bluetooth)
4. User suspects that they have the virus (they have symptoms ), they go to get tested. They show the id in the app to the Doctor.
5. User goes home
6. Doctor confirms that the user has the virus.
7. Doctor uses an admin UI to create a case password for the users id
8. Doctor sends the password to the user
9. User enters password into app.
10. App uploads users that have been close to the confirmed case
11. Server sends out push notifications to potential cases.
12. Potential cases should consider testing themselves if they also have symptoms.

## Flow 3:
1. User downloads app
2. User starts app
3. App tracks who the user comes into contact (other users) (using bluetooth)
4. User suspects that they have the virus (they have symptoms ), they go to get tested. They show the id in the app to the Doctor.
5. User goes home
6. Doctor confirms that the user has the virus by entering the user id in an admin UI.
7. Server sends a push notification to the user asking them to upload their data
8. User confirms
9. App uploads users that have been close to the confirmed case
10. Server sends out push notifications to potential cases.
11. Potential cases should consider testing themselves if they also have symptoms.
