Creates a resource pack that is synchronized with a git patch repository. Simplifies contributing by adding the following chat commands: 

/update  
   -sets up github repo  
   -sets up development pack for MC instance  
/update send  
   -detects user edits in dev pack  
   -transfers edits to repository, then Github  


Lightweight emulation of common git commands:  
/git [commit] [pattern] [message]  
/git [clone] [remote_url]  
/git [status]  
/git [push]  
/git [pull]  
/git [config] username [username]  
/git [config] email [email]  
/git [config] password [password]  
/git [config] save password  
