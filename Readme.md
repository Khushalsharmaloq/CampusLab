# CampusLab — How to Run the Project

## 1. Requirements

Make sure these are installed on your Windows PC:

- Java JDK
- Maven
- MySQL Server
- Apache Tomcat 10.1.60
- VS Code

Project location:

`C:\z\Project\CampusLab`

## 2. Every Time You Open the Project

### Step 1: Open the project

Open VS Code and open the folder:

`C:\z\Project\CampusLab`

Open a new PowerShell terminal in VS Code.

Run:

```powershell
cd C:\z\Project\CampusLab
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
```

### Step 2: Make sure MySQL is running

```powershell
Get-Service MySQL267
```

If its status is `Running`, continue. If it is stopped, open PowerShell as Administrator and run:

```powershell
Start-Service MySQL267
```

### Step 3: Run the desktop application

To start the CampusLab Swing desktop application, run:

```powershell
.\scripts\run-desktop.ps1
```

The desktop login window should open.

### Step 4: Run the web application

Open a **second VS Code terminal** and run:

```powershell
cd C:\z\Project\CampusLab
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force

$env:CATALINA_HOME = 'C:\apache-tomcat-10.1.60'
$env:CATALINA_BASE = 'C:\apache-tomcat-10.1.60'

.\scripts\run-web.ps1 -TomcatHome 'C:\apache-tomcat-10.1.60'
```

If Tomcat does not start automatically, start it in that terminal:

```powershell
& "$env:CATALINA_HOME\bin\catalina.bat" run
```

Keep the Tomcat terminal open while using the web application.

### Step 5: Open CampusLab in your browser

Open your browser and visit:

**http://localhost:8080/campuslab/login**

If the page does not load, check that Tomcat is running and that port 8080 is available.

## 3. When You Close VS Code or Shut Down Your PC

Your project files remain saved. Next time, repeat Steps 1–5 above.

You do not normally need to reinstall Java, Maven, MySQL, or Tomcat, or rebuild the project manually. The launch scripts handle the project's normal build/run steps.

Do not delete the `.env` file; it contains your local configuration. Never upload it to GitHub.

## 4. Troubleshooting

- **PowerShell script blocked:** Run `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force` in the current terminal.
- **MySQL stopped:** Start the `MySQL267` service.
- **Browser shows connection refused:** Check that Tomcat is running.
- **Browser shows 404:** Verify the deployment and use `/campuslab/login`.
- **Login reports a database error:** Check the MySQL configuration in `.env` and review the Tomcat logs.
- **Port 8080 already in use:** Check for an existing Tomcat process before starting another instance.

