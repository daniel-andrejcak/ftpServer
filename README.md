# Simple FTP server in JAVA 

## Supported functions
`get` - sends content of the file specified by the path

`list` - lists the contents of the directory specified by the path

`tree` - recursively lists the contents of the directory specified by the path

All the requests are sent as plain text and have the following form

>command path

*for example:* 
`echo "list /dir/subdir1" | nc localhost 1234`


## Setup
Program takes 2 arguments

1. Port on which the server listens for incoming requests
2. Directory whose content is served (server cant operate outside of this directory)

*for example:*
`java FTPserver 1234 ~/serverHomeDir`


Server supports communication with multiple clients at once
