-- Seeds ENGINEERINGUNIT and UNITCONVERTER tables with standard unit data
-- migrated from EngineeringUnitList.xml.
-- ENGINEERINGUNIT must be inserted before UNITCONVERTER.

-- ============================================================
-- ENGINEERING UNITS
-- MERGE ensures idempotency: existing rows are left unchanged.
-- ============================================================

MERGE INTO ENGINEERINGUNIT dest
USING (
    -- undefined
    SELECT 'raw'         AS UNITABBR, 'Raw Undefined Units'              AS NAME, 'univ'    AS FAMILY, 'undefined'              AS MEASURES FROM DUAL UNION ALL
    -- force
    SELECT 'dyn',                     'dyn',                                       'Metric',            'force'                             FROM DUAL UNION ALL
    SELECT 'lbf',                     'pound-force',                               'English',           'force'                             FROM DUAL UNION ALL
    SELECT 'N',                       'newtons',                                   'Metric',            'force'                             FROM DUAL UNION ALL
    -- area (legacy-style abbreviations)
    SELECT 'kM^2',                    'square kilometers',                         'Metric',            'area'                              FROM DUAL UNION ALL
    SELECT 'M^2',                     'square meters',                             'Metric',            'area'                              FROM DUAL UNION ALL
    SELECT 'mM^2',                    'square millimeters',                        'Metric',            'area'                              FROM DUAL UNION ALL
    SELECT 'mi^2',                    'square miles',                              'English',           'area'                              FROM DUAL UNION ALL
    SELECT 'ft^2',                    'square feet',                               'English',           'area'                              FROM DUAL UNION ALL
    SELECT 'yd^2',                    'square yards',                              'English',           'area'                              FROM DUAL UNION ALL
    SELECT 'cM^2',                    'square centimeters',                        'Metric',            'area'                              FROM DUAL UNION ALL
    SELECT 'in^2',                    'square inches',                             'English',           'area'                              FROM DUAL UNION ALL
    SELECT 'acre',                    'acres',                                     'English',           'area'                              FROM DUAL UNION ALL
    -- area (standard abbreviations)
    SELECT '1000 m2',                 'Thousands of square meters',                'Metric',            'Area'                              FROM DUAL UNION ALL
    SELECT 'ft2',                     'Square feet',                               'English',           'Area'                              FROM DUAL UNION ALL
    SELECT 'ha',                      'Hectares',                                  'English',           'Area'                              FROM DUAL UNION ALL
    SELECT 'km2',                     'Square kilometers',                         'Metric',            'Area'                              FROM DUAL UNION ALL
    SELECT 'm2',                      'Square meters',                             'Metric',            'Area'                              FROM DUAL UNION ALL
    SELECT 'mile2',                   'Square miles',                              'English',           'Area'                              FROM DUAL UNION ALL
    -- areal volume rate
    SELECT 'cfs/mi2',                 'Cfs per square mile',                       'English',           'Areal Volume Rate'                 FROM DUAL UNION ALL
    SELECT 'cms/km2',                 'Cms per square kilometer',                  'Metric',            'Areal Volume Rate'                 FROM DUAL UNION ALL
    -- volume (legacy-style abbreviations)
    SELECT 'm^3',                     'cubic meter',                               'Metric',            'volume'                            FROM DUAL UNION ALL
    SELECT 'mL',                      'milliliter',                                'Metric',            'volume'                            FROM DUAL UNION ALL
    SELECT 'acre*ft',                 'acre feet',                                 'English',           'volume'                            FROM DUAL UNION ALL
    SELECT 'floz',                    'fluid ounce',                               'English',           'volume'                            FROM DUAL UNION ALL
    SELECT 'ft^3',                    'cubic feet',                                'English',           'volume'                            FROM DUAL UNION ALL
    SELECT 'ft^3/s',                  'cubic feet per second',                     'English',           'flow'                              FROM DUAL UNION ALL
    SELECT 'qt',                      'quart',                                     'English',           'volume'                            FROM DUAL UNION ALL
    SELECT 'kL',                      'kiloliter',                                 'Metric',            'volume'                            FROM DUAL UNION ALL
    SELECT 'cc',                      'cubic centimeter',                          'Metric',            'volume'                            FROM DUAL UNION ALL
    SELECT 'uL',                      'microliter',                                'Metric',            'volume'                            FROM DUAL UNION ALL
    SELECT 'in^3',                    'cubic inches',                              'English',           'volume'                            FROM DUAL UNION ALL
    SELECT 'pt',                      'pint',                                      'English',           'volume'                            FROM DUAL UNION ALL
    SELECT 'gal',                     'gallon',                                    'English',           'volume'                            FROM DUAL UNION ALL
    SELECT 'L',                       'liter',                                     'Metric',            'volume'                            FROM DUAL UNION ALL
    SELECT 'm^3/s',                   'cubic meters per second',                   'Metric',            'flow'                              FROM DUAL UNION ALL
    -- volume (standard abbreviations)
    SELECT '1000 m3',                 'Thousands of cubic meters',                 'Metric',            'Volume'                            FROM DUAL UNION ALL
    SELECT 'ac-ft',                   'Acre-feet',                                 'English',           'Volume'                            FROM DUAL UNION ALL
    SELECT 'dsf',                     'day-second-foot',                           'English',           'Volume'                            FROM DUAL UNION ALL
    SELECT 'ft3',                     'Cubic feet',                                'English',           'Volume'                            FROM DUAL UNION ALL
    SELECT 'kaf',                     'Kiloacre-feet',                             'English',           'Volume'                            FROM DUAL UNION ALL
    SELECT 'kgal',                    'Kilogallons',                               'English',           'Volume'                            FROM DUAL UNION ALL
    SELECT 'km3',                     'Cubic kilometers',                          'Metric',            'Volume'                            FROM DUAL UNION ALL
    SELECT 'm3',                      'Cubic meters',                              'Metric',            'Volume'                            FROM DUAL UNION ALL
    SELECT 'M^3',                     'Cubic meters',                              'Metric',            'Volume'                            FROM DUAL UNION ALL
    SELECT 'mgal',                    'Millions of gallons',                       'English',           'Volume'                            FROM DUAL UNION ALL
    SELECT 'mile3',                   'Cubic miles',                               'English',           'Volume'                            FROM DUAL UNION ALL
    -- volume rate
    SELECT 'cms',                     'Cubic meters per second',                   'Metric',            'Volume Rate'                       FROM DUAL UNION ALL
    SELECT 'cfs',                     'cubic feet per second',                     'English',           'flow'                              FROM DUAL UNION ALL
    SELECT 'gpm',                     'Gallons per minute',                        'English',           'Volume Rate'                       FROM DUAL UNION ALL
    SELECT 'kcfs',                    'Kilo-cubic feet per second',                'English',           'Volume Rate'                       FROM DUAL UNION ALL
    SELECT 'mgd',                     'Millions of gallons per day',               'English',           'Volume Rate'                       FROM DUAL UNION ALL
    -- mass
    SELECT 'mt',                      'Metric ton',                                'Metric',            'mass'                              FROM DUAL UNION ALL
    SELECT 'ton',                     'tons',                                      'English',           'mass'                              FROM DUAL UNION ALL
    SELECT 'mG',                      'milligrams',                                'Metric',            'mass'                              FROM DUAL UNION ALL
    SELECT 'lb',                      'pounds',                                    'English',           'mass'                              FROM DUAL UNION ALL
    SELECT 'kG',                      'kilograms',                                 'Metric',            'mass'                              FROM DUAL UNION ALL
    SELECT 'oz',                      'ounces',                                    'English',           'mass'                              FROM DUAL UNION ALL
    SELECT 'G',                       'grams',                                     'Metric',            'mass'                              FROM DUAL UNION ALL
    SELECT 'uG',                      'micrograms',                                'Metric',            'mass'                              FROM DUAL UNION ALL
    -- ratio
    SELECT 'ppt',                     'parts per thousand',                        'univ',              'ratio'                             FROM DUAL UNION ALL
    SELECT 'ppm',                     'parts per million',                         'univ',              'ratio'                             FROM DUAL UNION ALL
    SELECT 'pct',                     'percent',                                   'univ',              'ratio'                             FROM DUAL UNION ALL
    SELECT '%',                       'percent',                                   'univ',              'ratio'                             FROM DUAL UNION ALL
    -- length (legacy-style abbreviations)
    SELECT 'mM',                      'millimeters',                               'Metric',            'length'                            FROM DUAL UNION ALL
    SELECT 'mi',                      'miles',                                     'English',           'length'                            FROM DUAL UNION ALL
    SELECT 'ft',                      'feet',                                      'English',           'length'                            FROM DUAL UNION ALL
    SELECT 'yd',                      'yards',                                     'English',           'length'                            FROM DUAL UNION ALL
    SELECT 'kM',                      'kilometers',                                'Metric',            'length'                            FROM DUAL UNION ALL
    SELECT 'M',                       'meters',                                    'Metric',            'length'                            FROM DUAL UNION ALL
    SELECT 'nmi',                     'nautical miles',                            'English',           'length'                            FROM DUAL UNION ALL
    SELECT 'cM',                      'centimeters',                               'Metric',            'length'                            FROM DUAL UNION ALL
    SELECT 'in',                      'inches',                                    'English',           'length'                            FROM DUAL UNION ALL
    SELECT 'uM',                      'micrometers',                               'Metric',            'length'                            FROM DUAL UNION ALL
    -- length (standard abbreviations)
    SELECT 'cm',                      'Centimeters',                               'Metric',            'Length'                            FROM DUAL UNION ALL
    SELECT 'km',                      'Kilometers',                                'Metric',            'Length'                            FROM DUAL UNION ALL
    SELECT 'm',                       'Meters',                                    'Metric',            'Length'                            FROM DUAL UNION ALL
    SELECT 'mm',                      'Millimeters',                               'Metric',            'Length'                            FROM DUAL UNION ALL
    -- velocity (legacy-style abbreviations)
    SELECT 'ft/s',                    'feet per second',                           'English',           'velocity'                          FROM DUAL UNION ALL
    SELECT 'yd/s',                    'yards per second',                          'English',           'velocity'                          FROM DUAL UNION ALL
    SELECT 'mph',                     'miles per hour',                            'English',           'velocity'                          FROM DUAL UNION ALL
    SELECT 'cM/s',                    'centimeters per second',                    'Metric',            'velocity'                          FROM DUAL UNION ALL
    SELECT 'kM/hr',                   'kilometers per hour',                       'Metric',            'velocity'                          FROM DUAL UNION ALL
    SELECT 'kph',                     'kilometers per hour',                       'Metric',            'velocity'                          FROM DUAL UNION ALL
    SELECT 'knots',                   'nautical miles per hour',                   'Metric',            'velocity'                          FROM DUAL UNION ALL
    SELECT 'in/s',                    'inches per second',                         'English',           'velocity'                          FROM DUAL UNION ALL
    SELECT 'M/s',                     'meters per second',                         'Metric',            'velocity'                          FROM DUAL UNION ALL
    SELECT 'mM/s',                    'millimeters per second',                    'Metric',            'velocity'                          FROM DUAL UNION ALL
    SELECT 'mi/hr',                   'miles per hour',                            'English',           'velocity'                          FROM DUAL UNION ALL
    -- linear speed (standard abbreviations)
    SELECT 'in/day',                  'Inches per day',                            'English',           'Linear Speed'                      FROM DUAL UNION ALL
    SELECT 'in/hr',                   'Inches per hour',                           'English',           'Linear Speed'                      FROM DUAL UNION ALL
    SELECT 'm/s',                     'Meters per second',                         'Metric',            'Linear Speed'                      FROM DUAL UNION ALL
    SELECT 'mm/day',                  'Millimeters per day',                       'Metric',            'Linear Speed'                      FROM DUAL UNION ALL
    SELECT 'mm/hr',                   'Millimeters per hour',                      'Metric',            'Linear Speed'                      FROM DUAL UNION ALL
    -- time
    SELECT 'min',                     'minutes',                                   'univ',              'time'                              FROM DUAL UNION ALL
    SELECT 'week',                    'weeks',                                     'univ',              'time'                              FROM DUAL UNION ALL
    SELECT 'sec',                     'second',                                    'univ',              'time'                              FROM DUAL UNION ALL
    SELECT 'day',                     'days',                                      'univ',              'time'                              FROM DUAL UNION ALL
    SELECT 'hr',                      'hours',                                     'univ',              'time'                              FROM DUAL UNION ALL
    -- pressure (legacy-style abbreviations)
    SELECT 'kpa',                     'kilopascals',                               'Metric',            'pressure'                          FROM DUAL UNION ALL
    SELECT 'inHg',                    'inches of mercury',                         'English',           'pressure'                          FROM DUAL UNION ALL
    SELECT 'mmHg',                    'millimeters of mercury',                    'Metric',            'pressure'                          FROM DUAL UNION ALL
    SELECT 'pa',                      'pascals',                                   'Metric',            'pressure'                          FROM DUAL UNION ALL
    SELECT 'psi',                     'pounds per square inch',                    'English',           'pressure'                          FROM DUAL UNION ALL
    SELECT 'atm',                     'atmospheres',                               'Metric',            'pressure'                          FROM DUAL UNION ALL
    SELECT 'bar',                     'bars',                                      'Metric',            'pressure'                          FROM DUAL UNION ALL
    SELECT 'mbar',                    'millibars',                                 'Metric',            'pressure'                          FROM DUAL UNION ALL
    -- pressure (standard abbreviations)
    SELECT 'in-hg',                   'Inches of mercury',                         'English',           'Pressure'                          FROM DUAL UNION ALL
    SELECT 'kPa',                     'Kilopascals',                               'Metric',            'Pressure'                          FROM DUAL UNION ALL
    SELECT 'mb',                      'Millibars',                                 'Metric',            'Pressure'                          FROM DUAL UNION ALL
    SELECT 'mm-hg',                   'Millimeters of mercury',                    'Metric',            'Pressure'                          FROM DUAL UNION ALL
    -- power (legacy-style)
    SELECT 'ft*lbf/s',               'foot-pounds per second',                    'English',           'power'                             FROM DUAL UNION ALL
    SELECT 'kW',                      'kilowatts',                                 'Metric',            'power'                             FROM DUAL UNION ALL
    SELECT 'W',                       'watts',                                     'Metric',            'power'                             FROM DUAL UNION ALL
    SELECT 'hp',                      'horsepower',                                'English',           'power'                             FROM DUAL UNION ALL
    -- power (standard abbreviations)
    SELECT 'GW',                      'Gigawatts',                                 'univ',              'Power'                             FROM DUAL UNION ALL
    SELECT 'MW',                      'Megawatts',                                 'univ',              'Power'                             FROM DUAL UNION ALL
    SELECT 'TW',                      'Terawatts',                                 'univ',              'Power'                             FROM DUAL UNION ALL
    -- energy (legacy-style)
    SELECT 'erg',                     'ergs',                                      'English',           'energy'                            FROM DUAL UNION ALL
    SELECT 'btu',                     'british thermal unit',                      'English',           'energy'                            FROM DUAL UNION ALL
    SELECT 'kj',                      'kilojoules',                                'Metric',            'energy'                            FROM DUAL UNION ALL
    SELECT 'j',                       'joules',                                    'Metric',            'energy'                            FROM DUAL UNION ALL
    SELECT 'kcal',                    'Kilocalories',                              'English',           'energy'                            FROM DUAL UNION ALL
    SELECT 'cal',                     'Calories',                                  'English',           'energy'                            FROM DUAL UNION ALL
    -- energy (standard abbreviations)
    SELECT 'GWh',                     'Gigawatt-hours',                            'univ',              'Energy'                            FROM DUAL UNION ALL
    SELECT 'kWh',                     'Kilowatt-hours',                            'univ',              'Energy'                            FROM DUAL UNION ALL
    SELECT 'MWh',                     'Megawatt-hours',                            'univ',              'Energy'                            FROM DUAL UNION ALL
    SELECT 'TWh',                     'Terawatt-hour',                             'univ',              'Energy'                            FROM DUAL UNION ALL
    SELECT 'Wh',                      'Watt-hours',                                'univ',              'Energy'                            FROM DUAL UNION ALL
    -- conductance
    SELECT 'uMHOs',                   'MicroMHOs per centimeter',                  'metric',            'conductance'                       FROM DUAL UNION ALL
    SELECT 'uMHO',                    'MicroMHOs per centimeter',                  'metric',            'conductance'                       FROM DUAL UNION ALL
    SELECT 'uMHOs/cm',               'MicroMHOs per centimeter',                  'metric',            'conductance'                       FROM DUAL UNION ALL
    SELECT 'mho',                     'Mhos',                                      'univ',              'Conductance'                       FROM DUAL UNION ALL
    SELECT 'S',                       'Siemens',                                   'univ',              'Conductance'                       FROM DUAL UNION ALL
    SELECT 'umho',                    'Micro-mhos',                                'univ',              'Conductance'                       FROM DUAL UNION ALL
    SELECT 'uS',                      'Micro-Siemens',                             'univ',              'Conductance'                       FROM DUAL UNION ALL
    SELECT 'uS/cm',                   'Micro Siemens per Centimeter',              'Metric',            'Conductance'                       FROM DUAL UNION ALL
    -- conductivity
    SELECT 'umho/cm',                 'Micro-mhos per centimeter',                 'univ',              'Conductivity'                      FROM DUAL UNION ALL
    -- voltage / electromotive potential
    SELECT 'V',                       'volts',                                     'Metric',            'voltage'                           FROM DUAL UNION ALL
    SELECT 'volt',                    'Volts',                                     'Metric',            'Electromotive Potential'           FROM DUAL UNION ALL
    -- temperature (legacy-style)
    SELECT 'degK',                    'degrees Kelvin',                            'Metric',            'temperature'                       FROM DUAL UNION ALL
    SELECT 'degF',                    'degrees Fahrenheit',                        'English',           'temperature'                       FROM DUAL UNION ALL
    SELECT 'degC',                    'degrees Celsius',                           'Metric',            'temperature'                       FROM DUAL UNION ALL
    -- temperature (standard abbreviations)
    SELECT 'C',                       'Centigrade',                                'Metric',            'Temperature'                       FROM DUAL UNION ALL
    SELECT 'F',                       'Fahrenheit',                                'English',           'Temperature'                       FROM DUAL UNION ALL
    -- concentration (legacy-style)
    SELECT 'g/L',                     'grams per liter',                           'Metric',            'concentration'                     FROM DUAL UNION ALL
    SELECT 'mG/L',                    'milligrams per liter',                      'Metric',            'concentration'                     FROM DUAL UNION ALL
    SELECT 'uG/L',                    'micrograms per liter',                      'Metric',            'concentration'                     FROM DUAL UNION ALL
    -- concentration (standard abbreviations)
    SELECT 'g/l',                     'Grams per liter',                           'Metric',            'Mass Concentration'                FROM DUAL UNION ALL
    SELECT 'gm/cm3',                  'Grams per cubic centimeters',               'Metric',            'Mass Concentration'                FROM DUAL UNION ALL
    SELECT 'mg/l',                    'Milligrams per liter',                      'Metric',            'Mass Concentration'                FROM DUAL UNION ALL
    -- acidity / pH
    SELECT 'pH',                      'pH',                                        'univ',              'acidity'                           FROM DUAL UNION ALL
    SELECT 'su',                      'Standard pH units',                         'univ',              'pH'                                FROM DUAL UNION ALL
    -- count
    SELECT 'count',                   'counts',                                    'univ',              'count'                             FROM DUAL UNION ALL
    SELECT 'unit',                    'Count',                                     'univ',              'Count'                             FROM DUAL UNION ALL
    -- irradiance / irradiation
    SELECT 'langley/min',             'Langley per minute',                        'English',           'Irradiance'                        FROM DUAL UNION ALL
    SELECT 'W/m2',                    'Watts per square meter',                    'Metric',            'Irradiance'                        FROM DUAL UNION ALL
    SELECT 'J/m2',                    'Joules per square meters',                  'Metric',            'Irradiation'                       FROM DUAL UNION ALL
    SELECT 'langley',                 'Langley',                                   'English',           'Irradiation'                       FROM DUAL UNION ALL
    -- phase change rate index
    SELECT 'in/deg-day',              'Inches per degree-day',                     'English',           'Phase Change Rate Index'           FROM DUAL UNION ALL
    SELECT 'mm/deg-day',              'Millimeters per degree-day',                'Metric',            'Phase Change Rate Index'           FROM DUAL UNION ALL
    -- angle
    SELECT 'deg',                     'Degrees',                                   'univ',              'angle'                             FROM DUAL UNION ALL
    SELECT 'deg*10',                  'Tens of Degrees',                           'univ',              'angle'                             FROM DUAL UNION ALL
    SELECT 'rev',                     'Revolution',                                'univ',              'Angle'                             FROM DUAL UNION ALL
    -- angular speed
    SELECT 'rpm',                     'Revolutions per minute',                    'univ',              'Angular Speed'                     FROM DUAL UNION ALL
    -- turbidity
    SELECT 'FNU',                     'Formazin Nephelometric Unit',               'univ',              'Turbidity'                         FROM DUAL UNION ALL
    SELECT 'JTU',                     'Jackson Turbitiy Unit',                     'univ',              'Turbidity'                         FROM DUAL UNION ALL
    SELECT 'NTU',                     'Nephelometric Turbidity Unit',              'univ',              'Turbidity'                         FROM DUAL UNION ALL
    -- currency
    SELECT '$',                       'Dollars',                                   'univ',              'Currency'                          FROM DUAL
) src
ON (dest.UNITABBR = src.UNITABBR)
WHEN NOT MATCHED THEN
    INSERT (UNITABBR, NAME, FAMILY, MEASURES)
    VALUES (src.UNITABBR, src.NAME, src.FAMILY, src.MEASURES);

-- ============================================================
-- UNIT CONVERTERS
-- INSERT ... SELECT with UnitConverterIdSeq.nextval evaluates
-- the sequence once per row, unlike INSERT ALL.
-- 'none' algorithm = pass-through alias (coefficients null).
-- 'linear' algorithm: Y = A*X + B.
-- ============================================================

-- Angle
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'deg'        AS FROMUNITSABBR, 'deg*10'     AS TOUNITSABBR, 'linear' AS ALGORITHM, 0.1                   AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL
);

-- Conductance / Conductivity aliases
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'uMHOs'      AS FROMUNITSABBR, 'uMHOs/cm'   AS TOUNITSABBR, 'none'   AS ALGORITHM, CAST(NULL AS DOUBLE PRECISION) AS A, CAST(NULL AS DOUBLE PRECISION) AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'uMHOs',                       'uMHO',                       'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'uMHO',                        'umho',                       'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'uMHOs/cm',                    'umho/cm',                    'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'umho',                        'mho',                        'linear',             0.000001,                             0.0,                                  NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mho',                         'S',                          'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'umho',                        'uS',                         'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'uS/cm',                       'umho/cm',                    'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Concentration
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'mG/L'       AS FROMUNITSABBR, 'uG/L'       AS TOUNITSABBR, 'linear' AS ALGORITHM, 1000.0                AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'g/L',                         'mG/L',                       'linear',             1000.0,                      0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mG/L',                        'ppm',                        'none',               NULL,                         NULL,      NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'g/L',                         'g/l',                        'none',               NULL,                         NULL,      NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mG/L',                        'mg/l',                       'none',               NULL,                         NULL,      NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'g/l',                         'gm/cm3',                     'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Count
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'count'      AS FROMUNITSABBR, 'unit'       AS TOUNITSABBR, 'none'   AS ALGORITHM, CAST(NULL AS DOUBLE PRECISION) AS A, CAST(NULL AS DOUBLE PRECISION) AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL
);

-- Energy
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'btu'        AS FROMUNITSABBR, 'j'          AS TOUNITSABBR, 'linear' AS ALGORITHM, 1055.056              AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'kcal',                        'cal',                        'linear',             1000.0,                      0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'cal',                         'j',                          'linear',             4.184,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'kj',                          'j',                          'linear',             1000.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'j',                           'erg',                        'linear',             1.0E7,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'kj',                          'Wh',                         'linear',             0.2777778,                    0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'Wh',                          'kWh',                        'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'kWh',                         'MWh',                        'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'MWh',                         'GWh',                        'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'GWh',                         'TWh',                        'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Flow
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'cfs'        AS FROMUNITSABBR, 'ft^3/s'     AS TOUNITSABBR, 'none'   AS ALGORITHM, CAST(NULL AS DOUBLE PRECISION) AS A, CAST(NULL AS DOUBLE PRECISION) AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'm^3/s',                       'cfs',                        'linear',             35.31466247,                          0.0,                                  NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'm^3/s',                       'cms',                        'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'kcfs',                        'cfs',                        'linear',             1000.0,                               0.0,                                  NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Force
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'lbf'        AS FROMUNITSABBR, 'N'          AS TOUNITSABBR, 'linear' AS ALGORITHM, 4.448222              AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'dyn',                         'N',                          'linear',             1.0E-5,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'lbf',                         'lb',                         'none',               NULL,                         NULL,      NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Irradiance
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'W/m2'       AS FROMUNITSABBR, 'langley/min' AS TOUNITSABBR, 'linear' AS ALGORITHM, 0.001434034416826004  AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'langley/min',                 'W/m2',                        'linear',             697.3333333333333,            0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Length (legacy to standard aliases)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'M'          AS FROMUNITSABBR, 'm'          AS TOUNITSABBR, 'none'   AS ALGORITHM, CAST(NULL AS DOUBLE PRECISION) AS A, CAST(NULL AS DOUBLE PRECISION) AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'cM',                          'cm',                         'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'kM',                          'km',                         'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mM',                          'mm',                         'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Length (conversions)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'ft'         AS FROMUNITSABBR, 'm'          AS TOUNITSABBR, 'linear' AS ALGORITHM, 0.3048                AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'ft',                          'in',                         'linear',             12.0,                         0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mi',                          'ft',                         'linear',             5280.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mi',                          'm',                          'linear',             1609.344,                     0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'yd',                          'ft',                         'linear',             3.0,                          0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'nmi',                         'm',                          'linear',             1852.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'm',                           'cm',                         'linear',             100.0,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'm',                           'mm',                         'linear',             1000.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'm',                           'km',                         'linear',             0.0010,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'm',                           'um',                         'linear',             1000000.0,                    0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Mass
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'G'          AS FROMUNITSABBR, 'mG'         AS TOUNITSABBR, 'linear' AS ALGORITHM, 1000.0                AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'mG',                          'uG',                         'linear',             1000.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'kG',                          'G',                          'linear',             1000.0,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mt',                          'G',                          'linear',             1000000.0,                    0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'lb',                          'G',                          'linear',             453.59237,                    0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'lb',                          'oz',                         'linear',             16.0,                         0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'ton',                         'lb',                         'linear',             2000.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- pH
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'pH'         AS FROMUNITSABBR, 'su'         AS TOUNITSABBR, 'none'   AS ALGORITHM, CAST(NULL AS DOUBLE PRECISION) AS A, CAST(NULL AS DOUBLE PRECISION) AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL
);

-- Power
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'ft*lbf/s'   AS FROMUNITSABBR, 'W'          AS TOUNITSABBR, 'linear' AS ALGORITHM, 1.355818              AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'hp',                           'W',                         'linear',             746.0,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'kW',                           'W',                         'linear',             1000.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'kW',                           'MW',                        'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'MW',                           'GW',                        'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'GW',                           'TW',                        'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Pressure (conversions)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'kpa'        AS FROMUNITSABBR, 'pa'         AS TOUNITSABBR, 'linear' AS ALGORITHM, 1000.0                AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'inHg',                        'pa',                         'linear',             3386.38,                      0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mmHg',                        'pa',                         'linear',             133.3224,                     0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'psi',                         'pa',                         'linear',             6874.75729,                   0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'atm',                         'pa',                         'linear',             101325.0,                     0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'bar',                         'pa',                         'linear',             100000.0,                     0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'kpa',                         'mbar',                       'linear',             10.0,                         0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mbar',                        'kpa',                        'linear',             0.10,                         0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Pressure (legacy to standard aliases)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'inHg'       AS FROMUNITSABBR, 'in-hg'      AS TOUNITSABBR, 'none'   AS ALGORITHM, CAST(NULL AS DOUBLE PRECISION) AS A, CAST(NULL AS DOUBLE PRECISION) AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'kpa',                         'kPa',                        'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mbar',                        'mb',                         'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mmHg',                        'mm-hg',                      'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Ratio
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT '%'          AS FROMUNITSABBR, 'pct'        AS TOUNITSABBR, 'none'   AS ALGORITHM, CAST(NULL AS DOUBLE PRECISION) AS A, CAST(NULL AS DOUBLE PRECISION) AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'ppt',                         'ppm',                        'linear',             1000.0,                               0.0,                                  NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT '%',                           'ppt',                        'linear',             10.0,                                 0.0,                                  NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Temperature
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'degC'       AS FROMUNITSABBR, 'degF'       AS TOUNITSABBR, 'linear' AS ALGORITHM, 1.8                   AS A, 32.0    AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'degC',                        'degK',                       'linear',             1.0,                          273.15,    NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'degC',                        'C',                          'none',               NULL,                         NULL,      NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'degF',                        'F',                          'none',               NULL,                         NULL,      NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Time
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'min'        AS FROMUNITSABBR, 'sec'        AS TOUNITSABBR, 'linear' AS ALGORITHM, 60.0                  AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'hr',                          'min',                        'linear',             60.0,                         0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'day',                         'hr',                         'linear',             24.0,                         0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'week',                        'day',                        'linear',             7.0,                          0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Velocity (legacy to standard aliases)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'M/s'        AS FROMUNITSABBR, 'm/s'        AS TOUNITSABBR, 'none'   AS ALGORITHM, CAST(NULL AS DOUBLE PRECISION) AS A, CAST(NULL AS DOUBLE PRECISION) AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'kM/hr',                       'kph',                        'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mph',                         'mi/hr',                      'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Velocity (conversions)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'ft/s'       AS FROMUNITSABBR, 'M/s'        AS TOUNITSABBR, 'linear' AS ALGORITHM, 0.3048                AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'ft/s',                        'mph',                        'linear',             0.681818181818,               0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'ft/s',                        'in/s',                       'linear',             12.0,                         0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'ft/s',                        'in/hr',                      'linear',             43200,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'yd/s',                        'ft/s',                       'linear',             3.0,                          0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'M/s',                         'cM/s',                       'linear',             100.0,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'cM/s',                        'mM/s',                       'linear',             10.0,                         0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mM/s',                        'mm/hr',                      'linear',             3600,                         0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mm/hr',                       'mm/day',                     'linear',             24,                           0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'in/hr',                       'in/day',                     'linear',             24,                           0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mph',                         'kM/hr',                      'linear',             1.609344,                     0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mph',                         'knots',                      'linear',             0.8689762419006,              0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Voltage
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'V'          AS FROMUNITSABBR, 'volt'       AS TOUNITSABBR, 'none'   AS ALGORITHM, CAST(NULL AS DOUBLE PRECISION) AS A, CAST(NULL AS DOUBLE PRECISION) AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL
);

-- Volume (legacy to standard aliases)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'mL'         AS FROMUNITSABBR, 'cc'         AS TOUNITSABBR, 'none'   AS ALGORITHM, CAST(NULL AS DOUBLE PRECISION) AS A, CAST(NULL AS DOUBLE PRECISION) AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'ft^3',                        'ft3',                        'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'acre*ft',                     'ac-ft',                      'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'M^3',                         'm3',                         'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Volume (conversions)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'gal'        AS FROMUNITSABBR, 'in^3'       AS TOUNITSABBR, 'linear' AS ALGORITHM, 231.0                 AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'gal',                         'qt',                         'linear',             4.0,                          0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'gal',                         'L',                          'linear',             3.785412,                     0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'gal',                         'kgal',                       'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'gal',                         'mgal',                       'linear',             0.000001,                     0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'qt',                          'pt',                         'linear',             2.0,                          0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'pt',                          'floz',                       'linear',             16.0,                         0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'L',                           'mL',                         'linear',             1000.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mL',                          'uL',                         'linear',             1000.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'kL',                          'L',                          'linear',             1000.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'm^3',                         'L',                          'linear',             1000.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'ft^3',                        'in^3',                       'linear',             1728.0,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'acre*ft',                     'ft^3',                       'linear',             43560.0,                      0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'ac-ft',                       'm3',                         'linear',             1233.4818,                    0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'ac-ft',                       'kaf',                        'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'm3',                          '1000 m3',                    'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'm3',                          'km3',                        'linear',             1.0E-9,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'km3',                         'mile3',                      'linear',             0.2399128,                    0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Area (legacy to standard aliases)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'ft^2'       AS FROMUNITSABBR, 'ft2'        AS TOUNITSABBR, 'none'   AS ALGORITHM, CAST(NULL AS DOUBLE PRECISION) AS A, CAST(NULL AS DOUBLE PRECISION) AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'kM^2',                        'km2',                        'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'M^2',                         'm2',                         'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mi^2',                        'mile2',                      'none',               NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);

-- Area (conversions)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)
SELECT UnitConverterIdSeq.nextval, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F FROM (
    SELECT 'cM^2'       AS FROMUNITSABBR, 'M^2'        AS TOUNITSABBR, 'linear' AS ALGORITHM, 1.0E-4                AS A, 0.0  AS B, CAST(NULL AS DOUBLE PRECISION) AS C, CAST(NULL AS DOUBLE PRECISION) AS D, CAST(NULL AS DOUBLE PRECISION) AS E, CAST(NULL AS DOUBLE PRECISION) AS F FROM DUAL UNION ALL
    SELECT 'mM^2',                        'M^2',                        'linear',             1.0E-6,                       0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'kM^2',                        'M^2',                        'linear',             1000000.0,                    0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'yd^2',                        'ft^2',                       'linear',             9.0,                          0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'ft^2',                        'in^2',                       'linear',             144.0,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'ft^2',                        'M^2',                        'linear',             0.09290304,                   0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mi^2',                        'ft^2',                       'linear',             2.78784E7,                    0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'mi^2',                        'acre',                       'linear',             640.0,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'acre',                        'ft^2',                       'linear',             43560.0,                      0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'acre',                        'ha',                         'linear',             0.4046856,                    0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL UNION ALL
    SELECT 'm2',                          '1000 m2',                    'linear',             0.001,                        0.0,       NULL,                                 NULL,                                 NULL,                                 NULL                                 FROM DUAL
);
