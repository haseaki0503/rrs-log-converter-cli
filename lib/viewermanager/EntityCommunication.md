Specification of Manager Entity Communications
===

## 1. Communication

### Purpose

- Viewer Receive Timestep by Manager
- Viewer selectable Entity needed to Drawing.

###  Overview of ViewerManager

- Viewer and Manager communicate using TCP/IP
- Viewer connect to Manager and join any Session
- Manager will send viewer to received data by assigned Provider
- Viewer can specify Entity communicates by Request
    * To reduce TCP/IP Communication

```graph
                 +---------+                                      
                 | Manager |                                      
                 +---------+                                      
+--------+ *.      1. | 1.      1. +----------+    +------------+ 
| Viewer |------->[Session]--------| Provider |----| RCRSKernel | 
+--------+         ^  |            +----------+    +------------+ 
                   |  |   a Session has a Provider                
+--------+ *.      |  |   a Session can has many Viewer           
| Viewer |---------+  |                                           
+--------+            |                                           
                      |                                           
+--------+ *.      1. | 1.      1. +----------+   +---------+     
| Viewer |------->[Session]--------| Provider |---| LogFile |     
+--------+                         +----------+   +---------+                                                    
```

### Communication

- Uses TCP/IP
- Sequence of Communication is following.

```graph
  +--------+        +--------+
  | Viewer |        | Server |
  +--------+        +--------+
      |                 | # Opening
      |  Request Open   |
      |---------------->| accept
      |   Send Initial  |
      |<----------------|
      |                 | # Updated
      |   Send Update   +<--- handle Timestep
      |<----------------|
      |                 | # Type specific
      |  Type Specific  |
      |---------------->|
      |   Send  Update  +<--- handle Timestep
      |<----------------|
      |                 | # Request Old Data
      |   Request Log   |
      |---------------->|
      |   Send Log Data |
      |<----------------|
      |                 | # Session Changed
      |                 +<--- Session Changed
      |   Send Initial  |
      |<----------------|
      |                 | # Closing
      |  Request Close  |
      |---------------->|
      |       OK        |
      |<----------------|
      |                 x close
      x
close, or reuse
```

#### Communication Message Format

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
    * Data: Entity Records Packed as MsgPack Object
```

## 2. Request and Result Data Format

### Request Data Format
- Request Message that's sent Viewer to Manager is MsgPack Object

```json
{
    "request": "request message bit array   #AS int"
    , "time":  "requested log timestep      #AS int"
}
```

- `request` is expressed by bit array specified by following.
    * ex) If you want to UPDATE and ACTION by each updating,
    you can get it by Request (ACTION|UPDATE) == 12(dec)

| Bit Number | Name | Mean |
|:----------:|:----:|:----:|
| 0 | OPEN | Open Connection |
| 1 | CLOSE | Close Connection |
| 2 | ACTION | Get Command of Agent |
| 3 | UPDATE | Get Update of World |
| 4 | WORLD | Get World Entities |
| 5 | PERCEPTION | Get Agent's Perception |
| 6 | CONFIG | Get Simulation Config |
| 7 | MAP | Get Map Area Info |


### Result Data Format

- Result Message that's sent Manager to Viewer is MsgPack Object

```json
{
    "request":        "request using pack to this message   #AS int"
    , "message":      "message from Manager                 #AS string"
    , "result":       "result of request/packing            #AS bool"
    , "time":         "timestep packed record contains      #AS int"
    , "record":  {
        "time":       "timestep sign this record            #AS int"
        , "map":      "map information                      #AS MapInfo"
        , "world":    "current information of all entities  #AS Entity"
        , "commands": "action agent published               #AS Action"
        , "changes":  "updates differ of each entity        #AS Entity"
        , "config":   "simulation config                    #AS string[string]"
    }
}
```

#### Entity Data Format

##### MapInfo
- Map Size and Area Entity information

```json
{
    "width":        "map width"
    , "height":     "map height"
    , "entities":   "map's area information by each entity contained"
}
```

**AreaInfo**
- Area Shape and Connection Specific

```json
{
    "id":           "area's entity id     #AS int"
    , "type":       "area type            #AS string"
    , "x":          "area's coordinate-x  #AS int"
    , "y":          "area's coordinate-y  #AS int"
    , "edges":      "area's edges         #AS Edge[]"
    , "neighbours": "area's neighbours    #AS int[]"
}
```

- Edge expressed with Point

- Point

```json
{
    "x": "coordinate-x     #AS int"
    , "y": "coordinate-y   #AS int"
}
```

- Edge

```json
{
    "start": "start point of Edge  #AS Point"
    , "end": "end point of Edge    #AS Point"
    , "adjacent": "neighbour's id  #AS int"
}
```

##### Entity
- Entity is expressed as 

```json
{
    "id":           "entity id                         #AS int"
    , "type":       "type of entity                    #AS string"
    , "created":    "is entity created                 #AS bool"
    , "deleted":    "is entity deleted                 #AS bool"
    , "x":          "entity coordinate-x               #AS int"
    , "y":          "entity coordinate-y               #AS int"
    , "position":   "entity id of current position     #AS int"
    , "damage":     "agent's current damage            #AS int"
    , "buried":     "agent's current buriedness        #AS int"
    , "hp":         "agent's current hp                #AS int"
    , "history":    "agent's position history          #AS Point[]"
    , "travel":     "agent's travel distance on step   #AS int"
    , "board":      "boarding agent's entity id        #AS int"
    , "water":      "water that agent tanked           #AS int"
    , "blockades":  "assigned blockade ids             #AS int[]"
    , "temp":       "temperature of building           #AS int"
    , "broken":     "brokenness of building            #AS int"
    , "fiery":      "fieryness of building             #AS int"
    , "repairCost": "blockades repair cost             #AS int"
    , "apexes":     "blockades apexes                  #AS Point[]"

}
```

- Entity::type is specified as

| Type | Value | Mean | Assigned Properties |
|:----:|:-----:|:----:|:-------------------:|
| Unknown | Unknown | Unknown Entity Type | Nil |
| Tactics Ambulance | TacticsAmbulance | Entity of Human | x,y,position,damage,buried,hp,history,travel,board | 
| Tactics Fire | TacticsFire | Entity of Human | x,y,position,damage,buried,hp,history,travel |
| Tactics Police | TacticsPolice | Entity of Human | x,y,damage,position,buried,hp,history,travel,water |
| Control Ambulance | ControlAmbulance | Entity of Building | temp,broken,fiery |
| Control Fire | ControlFire | Entity of Building | temp,broken,fiery |
| Control Police | ControlPolice | Entity of Building | temp,broken,fiery |
| Civilian | Civilian | Entity of Human | x,y,position,damage,buried,hp,history,travel |
| Refuge | Refuge | Entity of Building | temp,broken,fiery |
| Building | Building | Entity of Area | temp,broken,fiery |
| Road | Road | Entity of Area | blockades |
| Blockade | Blockade | | x,y,position,apexes,repairCost |
| Gas Station | GasStation | Entity of Building | temp,broken,fiery |
| Hydrant | Hydrant | Entity of Road | blockades |
| Area | Area | | temp,broken,fiery |


##### Action
- Action is expressed as 

```json
{
    "id":        "entity id that publish this Action"
    , "type":    "type of Action"
    , "path":    "path to agent moves "
    , "x":       "target coordinate-x of action"
    , "y":       "target coordinate-y of action"
    , "channel": "message channel"
    , "target":  "target of Action"
    , "water":   "consume quantity of water"
}
```


- Action::type is specified as

| Type | Value | Mean | Assigned Properties |
|:----:|:-----:|:----:|:-------------------:|
| UNKNOWN | Unknown | Unknown Command | |
| MOVE | Move | ActionMove | path,x,y,target |
| REST | Rest | ActionRest | |
| LOAD | Load | ActionLoad | target |
| UNLOAD | Unload | ActionUnload | 
| RESCUE | Rescue | ActionRescue | target |
| EXTINGUISH | Extinguish | ActionExtinguish |
| CLEAR | Clear | ActionClear | x,y |
| LCLEAR | LClear | AKClear (Legacy) | target |
| RADIO | Radio | Message::Radio | channel |
| VOICE | Voice | Message::Voice | |
| SUBSCRIBE | Subscribe | AKSubscribe (Legacy) | |
| TELL | Tell | AKTell (Legacy) | |
