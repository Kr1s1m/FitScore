# FitScore (Leaderboard+Forum)

The idea of the project is to develop a web-based fitness self-improvement application. FitScore is designed to help users achieve their fitness goals through structured self-improvement plans, competitive leaderboards, and a supportive community forum. The system will contain information (measurements and dimensions) about real life individuals. The system will allow registered users/admins to post on the forum board and enter the leaderboards. Users and admins will also be able to upvote and downvote posts and replies which will generate account karma. Additionally, user accounts can get verified by an admin. 


## ***Basic Business Requirements***
 

- An account profile page which contains:
  - Detailed information about the individual. For example, age, height, weight, bodyfat percentage and various other specific body measurements like wrist, biceps, thigh, etc.
  - Account posts from the forum board (original poster)
  - Account replies under forum board posts (answers)
  - Account post/reply count and karma
  - Functionality to jump to a post or a reply by the account
  - Functionality for users/admins to author a post or to leave reply under one (and additionally if advanced requirements are met then also - edit own reviews/replies, delete own reviews/replies)
  - Functionality for accounts to upvote or downvote posts and replies
 
- A leaderboard page which:
  - Shows registered accounts in the account database, allowing anyone to view rankings based on any combination of measurements
  - Allows anyone to open and view account profile of a registered user/admin from the leaderboard 
  - Ranks registered accounts based on various individual measurements

- A forum page which:
  - Contains posts and replies from registered users/admins
  - Allows registered users/admins to discuss and seek advice on a wide variety of topics from nutrition to workout routines
  - Allows anyone to open and view account profile of original poster or replying user/admin
  - Displays short account information about replying users/admins to encourage others to follow or critique their advice
  

## ***Advanced Business Requirements***

- Register page

- Login page
  
- Administration registry for users with the following actions:
  - Create account 
  - Remove account
  - Modify account information
  
- Add authorization so that only Admin accounts can:
  - Create/Update/Delete posts and replies
  - Delete/Update/Verify accounts
    
- Add authorization so that User accounts can:
  - Create/Update/Delete own posts and replies

- Logout functionality with a redirect to the login page 
