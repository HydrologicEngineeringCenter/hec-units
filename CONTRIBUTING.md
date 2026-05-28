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
        "system": "English",
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

1. Precision is invalid for some conversions, if so consider adding additional direct conversions. (NOTE: this is usually only required with units that contain extremely small values that get mapped to larger units.)
2. Additional conversion tests are required.


example:

```
UnitConversionTest > executionError FAILED
    org.opentest4j.AssertionFailedError: Not all possible conversions were performed.
    The following conversions have no test:
        ...
        ft3 -> noggins
        noggins -> ac-ft

```


In this case we see that we don't have sufficient testing of all unit conversions. Repeat the procedure until all test pass.

After adding more unit conversion we see we have issue 1 with our current unit as well as interference with other converions.

```
 ./gradlew :units:test --info | grep noggin
    org.opentest4j.AssertionFailedError: Unable to perform forward conversion using ( m3 -> ac-ft with 220339.0 i * 2.71108032E8 /,chain is: m3 -> noggins -> ac-ft ) within 1.0E-7 ==> expected: <8.10714E-4> but was: <8.127350502105375E-4>
UnitConversionTest > test_units(String, String, double, double, double, double) > [24] from=ac-ft, to=noggins, in=1, expected=10427333, delta=10, inverseDelta=10 FAILED
    org.opentest4j.AssertionFailedError: Unable to perform forward conversion using ( ac-ft -> noggins with 1.0427232E7 i *,chain is: ac-ft -> noggins ) within 10.0 ==> expected: <1.0427333E7> but was: <1.0427232E7>
    org.opentest4j.AssertionFailedError: Unable to perform forward conversion using ( ac-ft -> m3 with 2.71108032E8 i * 220339.0 /,chain is: ac-ft -> noggins -> m3 ) within 1.0E-4 ==> expected: <1233.48183754752> but was: <1230.4132813528245>
    org.opentest4j.AssertionFailedError: Unable to perform forward conversion using ( kaf -> m3 with 2.71108032E11 i * 220339.0 /,chain is: kaf -> ac-ft -> noggins -> m3 ) within 0.01 ==> expected: <1233481.83754752> but was: <1230413.2813528245>
    org.opentest4j.AssertionFailedError: Unable to perform inverse conversion using ( ac-ft -> 1000 m3 with 3.3888504E7 i * 2.7542375E7 /,chain is: ac-ft -> noggins -> m3 -> 1000 m3 ) within 1.0E-4 ==> expected: <1.0> but was: <0.9975122810070747>
    org.opentest4j.AssertionFailedError: Unable to perform forward conversion using ( ac-ft -> 1000 m3 with 3.3888504E7 i * 2.7542375E7 /,chain is: ac-ft -> noggins -> m3 -> 1000 m3 ) within 1.0E-7 ==> expected: <1.23348183754752> but was: <1.2304132813528246>
    org.opentest4j.AssertionFailedError: Unable to perform inverse conversion using ( kaf -> 1000 m3 with 2.71108032E8 i * 220339.0 /,chain is: kaf -> ac-ft -> noggins -> m3 -> 1000 m3 ) within 1.0E-4 ==> expected: <1000.0> but was: <997.5122810070746>
    org.opentest4j.AssertionFailedError: Unable to perform forward conversion using ( kaf -> 1000 m3 with 2.71108032E8 i * 220339.0 /,chain is: kaf -> ac-ft -> noggins -> m3 -> 1000 m3 ) within 0.001 ==> expected: <1233.48183754752> but was: <1230.4132813528245>
        m3 -> ac-ft     kaf -> m3       ac-ft -> noggins        kaf -> 1000 m3  ac-ft -> m3     ac-ft -> 1000 m3
```

The current solution is to add appropriate "short cut" conversions to that the chains are smaller. However, we will be implementing additional weighting variables in the future so that such units can still be used.


NOTE: At this time the unit conversion tests are still being implemented, you will likely see the test above fail with more units than you've added.
Focus on your new unit and ignore the others.

###

Noggins was added to the repository following the above guidelines; however, the unit definition and conversions are currently commented out as they are too disruptive without the mechanism of additional weights.
This unit was used for this example because it is not required for any options and is bit silly which can help with learning. The majority of units and conversions you attempt to add to this repository should not suffer this issue.
