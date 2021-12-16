# CIS389WebServer
CSE389 Final Project  
Java HTTP(Web) Server    
Demo Video: https://youtu.be/-UDZoPBjX24   
(If you are not clear about anything in README, please watch the demo video)  

## README Author
- Yuhao Chen 

## Developers
- Yuhao Chen 
- Runzhou Chen 
- Modi Li
- Yujie Xu
- Siwei Peng

## Functionalities
- Handle GET/HEAD/POST Request
- Multi-threading
- Logging
- Authentication/Authorization
- SSL/Https (Not completed yet)

## File/Folders
Our project folder is `CIS389WebServer` Bear with us, it should be CSE389  
Inside the folder, you can find `index_L.html`, `index.html`, `form.html`, `README.md`, `jnp4e.keys`   
Also, there is a child folder called sourseFile  
Most of our server codes are in sourceFile, including:
- JHTTP.java
- RequestProcessor.java

## How to run the server
### Compile JHTTP.java  
You need to firsly use `cd` to go inside `sourceFile` in the terminal    
Type the following command to compile
`javac JHTTP.java`   
### Run JHTTP   
In the terminal, type the following:  
`java JHTTP XXX 888`  
Replace 'XXX' with the root directory of the webserver  
If you get this from GitHub, all the html files are in /CIS389WebServer  
In my case, I replace 'XXX' wiht `~/Documents/GitHub/CIS389WebServer`  
You should have the same format `PATH_TO_THE_PROJECT/CIS389WebServer`  
In my case, the run command is  
`java JHTTP ~/Documents/GitHub/CIS389WebServer 888`  
I picked port number 888, but you can choose others as well (make sure no conflict)  

## How to test GET/HEAD/POST Requests
Open the web browser, Firefox, Google Chrome, etc.  
Type the following URL (make sure the port number match the one you used to run the server)   
`http://localhost:888/`   
It will automatically navigate to `http://localhost:888/index_L.html` 

### Test GET/POST Request
(If you are using Firefox, you can get the HTTP HEADER LIVE to see the request and reponse)    
Type the following URL in your browser  
`http://localhost:888/form.html`  **(Send GET Request)**  
It will show you an HTML form  
You can enter the name and city, click on Submit  **(Send POST Request)**  
See the response from the HTTP Server for the POST request   

### Test HEAD Request
You can use HTTP HEADER LIVE tool to send a HEAD request asking for `form.html`   
For detailed instruction on how to use HTTP HEADER LIVE, please watch the demo videos

## How to test Multi-threading
Open the web browser, Firefox, Google Chrome, etc.   
Type the following URL (make sure the port number match the one you used to run the server)   
`http://localhost:888/`   
Then, open a new tab, or another browser, type the above URL again  
Different tabs/browsers are considered different threads, which can prove our server can handle multi clients at the same time  

## How to test Authentication/Authorization
Open the web browser, Firefox, Google Chrome, etc.   
Type the following URL (make sure the port number match the one you used to run the server)   
`http://localhost:888/`   
It will automatically navigate to `http://localhost:888/index_L.html`  
There are two users available  
- Username: admin  Password: 123456
- Username: example Password: 123456   

You will see the difference UI for normal user and administrator   
Administrators can see all the available users and passwords   
