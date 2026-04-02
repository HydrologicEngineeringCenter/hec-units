# HEC Units

This library contains our defined Unit definitions and conversion values.

## CONTRIBUTING

Formal contributing documents to be created soon. For the moment, open pull request, wait for review.

## WARNINGS

1. This is an initial move of this data and code out of a cwms-database branch. It is not complete, the tests correctly fail as not all conversions are tested. 

2. The precision allowed in the test csv is arbitrarily set such that the tests pass reasonable to validate the overall design. However, please suggest better values

## General Workings

1. Provided Unit conversions are loaded
2. All possible, valid, conversions are generated (each from/to pair within a measurement type), at this point there are likely duplicates.
3. For each possible from -> to the shorted path (number of conversions to get to the final desired "to" unit) is determined.
4. A symbolic algebra processor is used to reduce the set of conversions to a single algebric function. Most commonly a simple ax+b equation
5. algebra processor then calculates the inverse conversion "to -> from"
6. All shortest path expanded conversions are stored


## Rough Roadmap

### Short term.

1. Setup as a proper working library
2. Fill out the units/src/main/resources/db/custom/units_and_parameters/conversions.json file
    a. And the corrisponding conversions_to_test.csv test file
3. Generate a jar containing the db_parameters_units.def and unitConversions.def files for existing tools to reference
4. Better organize and name things. package name will be changed to mil.usace.army.hec

5. Release to maven central
6. Release Consumable JSON of all the expanded units and conversions for other languages

### Mid Term

1. Generate JAR that implements JSR 385 (java units of measure). Most likely as an extension to https://github.com/unitsofmeasurement/indriya if the various abstract systems make sense


### Mid/Long term (unless you want to help)

1. Generate libraries for other languages, such as Python and DotNet

## History

Previously this data was generated as part of the https://github.com/hydrologicengineeringcenter/cwms-database build. However multiple applications use the generated results and the release cadance of the database is not suitable for those applications.
As we were already in the processed of modernizing the unit generation anyways, we've moved the unit definitions, conversions, and graph search code to this respository to encourage participation.
