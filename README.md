# ADND
Projects for Android Basic and Kotlin Developer Nanodegrees

## Project #3: Build an app with an application loading status
Main focus of this project is on giving the user contextual information via interactive notifications and toasts, as well
as implementing custom views and animations with MotionLayout.

### 1. Implement Download Manager to successfully download a file from the internet.
- [x] Download manager wasnt' covered in the course, so research on how to implement it.
- [x] Should take a URL and download the file.  Learn how to get the progress of the download the result. (failed, etc).

### 2. Implement Notification Manager
- [x] Show a notification as the download starts.  Add a cancel/pause action. Since the user will be in the app, make it low priority.
- [x] Show progress of the download within the notification.
- [x] Once the download is finished, show the status along with an action to navigate to it's detail page. Heads Up/High Priority.
- [ ] ~OPTIONAL:  Possibly find a way to support downloading multiple files at once.~
  - [ ] ~We'd need to generate a new notification ID upon starting the download and cache it so we can update the notification.~
  
### 3. Create a custom button and animate it.
- [x] Circle button with a download arrow in the center.
- [x] Upon starting the download, button turns blue and a circle progress bar will follow the edge of the button, relative
to the progress of the download.  Also, below the download arrow, % completed text will also update in real time.
- [x] Animate button to completed/failed state. (Light green background, Dark Green Check.  For Failed, red and X)

### 4. Detail Screen with MotionLayout
- [x] Clicking the action in notification will lead the user to the detail screen.
- [x] Requires some sort of animation via MotionLayout.  Might update the status in real time if I let the user
access the page before the download is finished.
- [x] Will revisit this part depending on which option features I implement.

### 5. OPTIONAL FEATURES:
- [ ] ~Once the file is downloaded, save it to local database.~
  - [ ] ~Path to the file will be the id~
  - [ ] ~Store the status, repository, file size, etc.~
- [ ] ~Show all downloaded files in a RecyclerView~
  - [ ] ~Load from database, and allow user to navigate to the detail screen by clicking the list item.~
  - [ ] ~Show the repository and status~
- [ ] ~Allow user to delete file from within the app.~
