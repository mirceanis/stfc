### STF connection tool

#### Installation

1. To install this tool, clone the repo, `cd` to it and run 

```
./gradlew install
```

The script will output the path where it installed.
You may want to add that path to your `$PATH` env variable.

2. Create a `stf.properties` file with the following content:

```
STF_ACCESS_TOKEN= the token you get from the STF web UI > Settings > Keys
STF_URL=http://your.stf.server.url 
```

#### Usage

* `stfc -h` to print usage
* `stfc -l` will list all device serials
* `stfc -c` will connect to all reserved devices
* `stfc -a` will reserve all unused devices
* `stfc -r` will release everything and disconnect