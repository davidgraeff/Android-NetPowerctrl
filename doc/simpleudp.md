# SimpleUDP

## SimpleUDP Packet Format (Receive type)
* All ascii based, two characters not allowed in names and ids: newline (\n) and tabulator (\t).

### General structure (lines are separated by newlines)
Header (always SimpleUDP_info or SimpleUDP_info_ack or SimpleUDP_info_fail)
DeviceUniqueID (often Mac address of peer)
Name of device (english name of device, user may rename it within the app)
Version string (no special formatting)
List of actions (one action per line, action consists of multiple tokens)

If you send SimpleUDP_detect, you will receive a full SimpleUDP_info with all known actions. If
you send a SimpleUDP_cmd, the peer will acknowledge with a SimpleUDP_info_ack where only the affected
actions are listed.

### Structure of an action
     TYPE \t actionID string \t action name \t optional value

* **TYPE** is one of (STATELESS,TOGGLE,RANGE,NOTEXIST).
* The **actionID** may be any string but have to be unique among all actions.
* The **action name** can be any string as long as it does not contain \n and \t.
* The value is only necessary if the type is TOGGLE or RANGE. If the Header is equal to SimpleUDP_info_fail
the TYPE may be NOTEXIST if you requested a command on a non existing action.

Example packet:
     SimpleUDP_info\n
     11:22:33:44:55:66\n
     device_name\n
     1.0-2015.05.10\n
     STATELESS\tACTION1\tAction Name
     TOGGLE\tACTION2\tAction Name\t1

## SimpleUDP Packet Format (command type)
* All ascii based, two characters not allowed in names and ids: newline (\n) and tabulator (\t).

### General structure (lines are separated by newlines)
    Header (always SimpleUDP_cmd)
    UniqueID (we use the android device unique id)
    CMD_TYPE \t actionID \t VALUE \t RANDOM_REQUEST_NO

* **CMD_TYPE** is one of (SET,TOGGLE,RENAME), the actionID has been received before and is known.
* **VALUE**: if CMD_TYPE is SET,RENAME a value is necessary else just set it to 0.
* **RANDOM_REQUEST_NO**: A random request number. This is for duplicate detection. The receiver has
to check the current commands request number against the last ~3 requests and ignore (but still
ack) the received but already executed command.

Example packet:
     SimpleUDP_cmd\n
     NFKAJELAFBAGAHD\n
     SET\tACTION2\t1
