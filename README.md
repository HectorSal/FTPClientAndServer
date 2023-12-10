# FTPClientAndServer
A basic implementation of and FTP Client and Server with limited commands

## How to run the Server
1. Compile:
   ```
   javac src/FTPProject/*.java -d bin
   ```
2. Start Server:
   ```
   java -cp bin FTPProject.FTPServer
   ```

## How to run Command Line Client
1. `java -cp bin FTPProject.FTPClient`

## Running Frontend
1. Install JavaFX
2. Go to `frontend-side/.vscode/launch.json`. Edit `vmArgs` to the JavaFX path
3. Using VS Code, go to `frontend-side/src/Client.java` and click `Run` above the main function.
