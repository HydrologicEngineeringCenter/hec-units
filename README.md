# HEC Units

This library contains our defined Unit definitions and conversion values.

## WARNINGS

1. This is an initial move of this data and code out of a cwms-database branch. It is not complete, the tests correctly fail as not all conversions are tested. 

2. The precision allowed in the test csv is arbitrarily set such that the tests pass reasonable to validate the overall design. However, please suggest better values


## Rough Roadmap

### Short term.

1. Fill out the units/src/main/resources/db/custom/units_and_parameters/conversions.json file
    a. And the corrisponding conversions_to_test.csv test file
2. Generate a jar containing the db_parameters_units.def and unitConversions.def files for existing tools to reference
3. Better organize and name things. package name will be changed to mil.usace.army.hec
4. Release to maven central
5. Release Consumable JSON of all the expanded units and conversions for other languages

### Mid Term

1. Generate JAR that implements JSR 385 (java units of measure). Most likely as an extension to https://github.com/unitsofmeasurement/indriya if the various abstract systems make sense


### Mid/Long term (unless you want to help)

1. Generate libraries for other languages, such as Python.

## History

Previously this data was generated as part of the https://github.com/hydrologicengineeringcenter/cwms-database build. However multiple applications use the generated results and the release cadance of the database is not suitable for those applications.
As we were already in the processed of modernizing the unit generation anyways, we've moved the unit definitions, conversions, and graph search code to this respository to encourage participation.