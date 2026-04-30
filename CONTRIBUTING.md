# Adding units and conversions

To add a unit open `units/src/main/resources/unit_definitions.json` and create the appropriate data.

Then add at least one unit conversion to `units/src/main/resources/conversions.json`. We recommend not adding additional conversions unless the required conversions precision is not met.
Be aware, the `conversions.json` is read by the build as "JSON with Comments." You may add additional comments for clarity, existing comments should not be removed.

And at least one test conversion to `units/src/test/resources/units/conversions_to_test.csv`. The conversion provided will be tested both a from that unit and to that unit.
The required precision field is somewhat arbitrary, set it to a reasonable expected value and adjust conversions to match.

However, while you only need to make one unit conversion entry, the build will map to all possible conversions within that unit system. The unit conversion test suite
requires that *all* generated conversions are tested.


## example, adding "noggins"


### The unit
Find the area of the file that contains volume parameters.

```
    {
        "abstract-parameter": "Volume",
        "abbr": "1000 m3",
        "system": "SI",
        "name": "Thousands of cubic meters",
        "description": "Volume of 1E+03 cubic meters"
    },
```

```
    {
        "abstract-parameter": "Volume",
        "abbr": "1000 m3",
        "system": "SI",
        "name": "Thousands of cubic meters",
        "description": "Volume of 1E+03 cubic meters"
    },
    {
        "abstract-parameter": "Volume",
        "abbr": "noggin",
        "system": "EN",
        "name": "Noggins",
        "description": "An odd, but valid, unit equivalent to 1/4 of a pint "
    },
```


### The conversion

```
    ["kgal",        "gal",         "linear: unit_per_kilo 0"],
	["noggins",      "m3",          "linear: .0001420653125 0"],
```

### The conversion test

Find related conversions. Add your new conversions to the file.

```
m3,gal,6.343, 1675.6432688, .000001, .00001
noggin,m3, 1, .0001420653125, .000001, .00001

```

### verification

run `./gradlew :units:test --info` 

If tests pass you are done, submit the Pull request for the change.
If tests fail determine if:

1. Precision is invalid for some conversions, if so consider adding additional direct conversions. (NOTE: this is usually only required with units that contain extremely small values that get mapps to larger units.)
2. Add additional conversion tests.


example:

```


```

In this case we have both failures.

###

noggins was added to the repository following the above guidelines.
