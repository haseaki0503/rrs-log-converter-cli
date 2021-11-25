Specification of Manager Control Messages
===

1. ControlMessage Format
---

### Purpose

- Control Viewer Manager
- Provide Controller like GUI
    + Open, Close Provider that'll read/receive Viewer's Log Data
    + Connect Provider and Viewer as Session
    + Gathering Information of Session


### Communication

- Uses TCP/IP
- Sequence of Communication is following.

```graph
  +------------+    +--------+
  | Controller |    | Server |
  +------------+    +--------+
        | ControlRequest |
        |--------------->| accept
        |                +--+
        |                -  | Managing
        |                -  |  Processing
        |     Result     +<-+
        |<---------------|
        |                x close
        x
close, or reuse
```

- Control Request format is folloing.

```graph
+--------+------+
| Header | Data |
+--------+------+

- Part of Header
+-------------+
| Data Length |
+-------------+
    * Data Length: type of Integer
        + packed with MsgPack
        + Length: 3 or 5, 9 bytes
    * Data: different by ControlRequest and ControlResult
```

- Request Message that Controller send to Manager specified by following.

```graph
- Part of Data
+-------------+---------------+
| Data Header | {Data Object} |
+-------------+---------------+

    * Data Header: fixed format MsgPack Array
    +------------+-----------+
    | MsgVersion | CallMsgID |
    +------------+-----------+
        + MsgVersion: Control Message Specification Version String.
        + CallMsgID: Calling Control Function ID specified on this Document

    * {Data Object}: Fixed format MsgPack Object specified by CallMsgID
```

- Result Message that Manager send to specified by following

```graph
 - Part of Data
 +-------------+---------------+
 | Data Header | {Data Object} |
 +-------------+---------------+
    
    * Data Header: fixed format MsgPack Array
    +-----------+--------+
    | CallMsgID | Result |
    +-----------+--------+
        + CallMsgID: Function ID required on Request.
        + Result: boolean value of result. True or False

    * {Data Object}: Fixed format MsgPack Object specified by CallMsgID
```


2. Overview of ViewerManager
---

```graph
                 +---------+                                      
                 | Manager |                                      
                 +---------+                                      
+--------+ *.      1. | 1.      1. +----------+    +------------+ 
| Viewer |--------[Session]--------| Provider |----| RCRSKernel | 
+--------+         |  |            +----------+    +------------+ 
                   |  |   a Session has a Provider                
+--------+ *.      |  |   a Session can has many Viewer           
| Viewer |---------+  |                                           
+--------+            |                                           
                      |                                           
+--------+ *.      1. | 1.      1. +----------+   +---------+     
| Viewer |--------[Session]--------| Provider |---| LogFile |     
+--------+                         +----------+   +---------+     
                                                                  
                                                                  
```

3. ControlMessage Types and Function Provided
---

### Type of Function

```
[System Management Functions] - 0 to 99
|- Exit                         1
|- GetErrorList                 2
|- GetViewerInformation         3
|- GetManagerInformation        4

[Provider Management Functions]   - 100 to 199
|- Open                             101
|- Close                            102
|- GetProviderInformation           103

[Session Management Functions]    - 200 to 299
|- Connect                          201
|- Disconnect                       202
|- Refresh                          203
|- GetSessionInformation            204
```

### Reference

#### Exit
- Close Manager and Exit Processing
- Arguments:
    * Empty Object
- Results:
    * (succeeded) Result True
    * (failed) Result False, and Message from Manager


#### Open
- Open Server/LogFile as Provider
- Type of Function:
    * Open (Address as (String, Int)) : high priority
    * Open (LogFileName as String)
- Arguments:
    * Address as (String, Int)
    * FileName as String
- Results:
    * Provider's ID that Manager given
    * (failed) Message from Manager


#### Close
- Close Provider
- Arguments:
    * ProviderID as Int[]
- Results:
    * (failed) Message from Manager
- Note:
    * If specified ProviderID is already closed, Results will be True
    * But specified Provider still not provided, Result will be False


#### Connect
- Connect Viewer to Provider (Create Session)
- Arguments:
    * {ViewerID, ProviderID} as Object
- Results:
    * (succeeded) SessionID list
    * (failed) Nil as ID, and Message from Manager


#### Disconnect
- Disconnect Viewer from Session
- Arguments:
    * Disconnect (ViewerID as Int[])
- Results:
    * (succeeded) True
    * (failed) Message from Manager


#### Refresh
- Refresh (Kernel) Provider's connection
- Arguments:
    * ProviderID List as Int[]
- Results:
    * updated ProviderID List
- Note:
    * If arguments is Nil, Refresh all refreshable


#### Get Information
- Get Information of Provider/Session/Viewer
- Type of Function
    * GetProviderInfo
    * GetSessionInfo
    * GetViewerInfo
- Arguments:
    * (ALL) Nil if request to get all information of category
    * (Provider) ProviderID as Int[]
    * (Session) SessionID as Int[]
    * (Viewer) ViewerID as Int[]
- Results:
    * (succeeded) List of Information
    * (failed) Message from Manager
- Note
    * If specified ProviderID is already closed, Results will be empty List
    * And specified ProviderID still not provided, Results will be empty List too.
- Information Type Specification
    * ProviderInfo
    ```json
    {
        "id":        "ID of provider                                          #AS int"
        , "type":    "Type of provider, RCRS/RCRS-LOG/VIEWER-LOG              #AS string"
        , "ids":     "ID String of provider, Address String, FileName, etc.   #AS string"
        , "status":  "Status of provider, online/closed/errored               #AS string"
        , "time":    "current received time                                   #AS int"
        , "maxtime": "max timestep of simulation                              #AS int"
    }
    ```
    * ViewerInfo
    ```json
    {
        "id": "ID of viewer                                 #AS int"
        , "address": {
            "host": "hostName or hostAddress of connection  #AS string"
            "port": "connection port                        #AS int"
        }
        , status: "Status of viewer, online/closed/errored  #AS string"
    }
    ```
    * SessionInfo
    ```json
    {
        "id":         "ID of session                            #AS int"
        , "provider": "connected provider ID                    #AS int"
        , "viewer":   "connected viewer IDs                     #AS int[]"
        , "status":   "Status of session, online/closed/errored #AS string"
    }
    ```

#### Get Error List
- Get Error List Occurred on Managing
- Results:
    * List of Errors as String[]
    * (failed) Message from Manager

#### Get Manager Information
- Get Manager's Information
- Results:
    * ManagerInfo
- Information Type Specification
    * ManagerInfo
    ```json
    {
        "port":        "Waiting Port for Viewer    #AS int"
        , "providers": "Connected Provider Count   #AS int"
        , "vieweres":  "Connected Viewer Count     #AS int"
        , "sessions":  "Opened Session Count       #AS int"
        , "errors":    "Occurred Error Count       #AS int"
        , "status":    "Status of Manager          #AS String"
        , "version":   "Manager Version            #AS String"
    }
    ```


### Mixed List of Arguments / Results

#### Arguments
```json
{
    "address": {"host": "HostName", "port": 0}
    , "logfilename": "FileName"
    , "provider": [0, 0, 0, ...]
    , "session": [0, 0, 0, ...]
    , "viewer": [0, 0, 0, ...]
    , "connection": {"provider": 0, "viewer": 0}
}
```

#### Results
```json
{
    "message": "Message" # some error occurred
    , "provider": 0     # on provider opened
    , "session": 0      # on session created
    , "providerinfo": [
        {"id": 0, "type": "fake", "ids": "empty", "status": "closed", "time": 0, "maxtime": "0"}
        , {"id": 1, "type": "rcrs", "ids": "127.0.0.1/8000", "status": "online", "time": 100, "maxtime": 300}
        , {"id": 2, "type": "log", "ids": "/var/log/rcrs/paris.log", "status": "closed", "time": 150, "maxtime": 300}
        , ...]          # on provider opened, or information required
    , "viewerinfo": [
        {"id": 0, "address": {"host": "HostName", "port": 0}, "status": "online"}
        , {"id": 1, "address": {"host": "HostName", "port": 17777}, "status": "closed"}
        , {"id": 2, "address": {"host": "HostName", "port": 17777}, "status": "closed"}
        , ...]          # on information required
    , "sessioninfo": [
        {"id": 0, "provider": 0, "viewer": [0, 1], "status": "ok"}
        , {"id": 0, "provider": 1, "viewer": [2], "status": "ok"}
        , ...]          # on session created, or information required
    , "manager": {"key": "value", ...}          # on required
    , "errors": ["err", "err", "err", ...]      # on required
}
```
