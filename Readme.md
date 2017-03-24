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
```
stfc [-a] [-c] [-f <arg>] [-h] [-l] [-q] [-r] [-V] [-v]

 -a,--allocate       allocate devices to the current user
                     By default, this reserves every available device.
                     It's best to combine with --filter option to be more specific
 -c,--connect        connect to reserved devices
                     By default it connects to every allocated device.
                     Can be combined with --filter to be more specific.
 -f,--filter <arg>   filter devices, can be used multiple times to filter by multiple fields.
                     If the same field is specified in more than one filter, ONLY the FIRST one is used.
                     Available filters are:
                     * `free` - devices that are not allocated to any user
                     * `using` - devices that are in use by current user
                     * `sdk` - devices with a specific sdk. This filter can also be ranged,
                     Ex: `sdk=18-23` OR `sdk=24+`
                     * `serial` - devices whose serial contains the specified string
                     * `connect` - devices whose connection string contains the specified string
                     * `notes` - devices whose notes contain the specified string
                     Boolean filters can be specified without the actual value, in which case they default to `true`.
                     Ex: `stfc -f using` is equivalent to `stfc -f using=true`
 -h,--help           print this help message
 -l,--list           list available devices.
                     Can be combined with --filter option to only show subsets
 -q,--quiet          only output device serials, nothing else. Negates --verbose option
 -r,--release        release allocated devices
                     By default it releases all but can be combined with --filter to be more specific
 -V,--version        show version
 -v,--verbose        show verbose output
```

#### Examples

* `stfc -l` - list every available device
* `stfc -lf using` - list devices that I'm using
* `stfc -ac -f sdk=23` - allocate and connect to all devices with SDK level 23
* `stfc -acf sdk=15+` - allocate and connect to all devices with SDK level >= 15
* `stfc -rf serial=01234` - release devices whose serial contains "01234"

#### Notes

* Running a filtered allocation command does not release previously allocated devices that don't match the filter.

* Listing devices with no filters implicitly adds the "free=true" filter

* While commands can be combined, it does not always make sense to combine them.
For example, `stfc -la` will allocate all devices but won't list anything because of the implicit filter in lists and the fact that every free device has just been allocated to the current user.
This behavior will probably change in future releases by sanitizing possible combinations and by changing behavior of commands when combined.

