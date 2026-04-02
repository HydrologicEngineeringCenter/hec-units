-- Seeds ENGINEERINGUNIT and UNITCONVERTER tables with standard unit data
-- migrated from EngineeringUnitList.xml.
-- ENGINEERINGUNIT must be inserted before UNITCONVERTER due to the
-- unit_conv_check_trigger that validates unit existence on insert.

-- ============================================================
-- ENGINEERING UNITS
-- ============================================================

INSERT INTO ENGINEERINGUNIT (UNITABBR, NAME, FAMILY, MEASURES) VALUES
-- undefined
('raw',         'Raw Undefined Units',              'univ',    'undefined'),
-- force
('dyn',         'dyn',                              'Metric',  'force'),
('lbf',         'pound-force',                      'English', 'force'),
('N',           'newtons',                          'Metric',  'force'),
-- area (legacy-style abbreviations)
('kM^2',        'square kilometers',                'Metric',  'area'),
('M^2',         'square meters',                    'Metric',  'area'),
('mM^2',        'square millimeters',               'Metric',  'area'),
('mi^2',        'square miles',                     'English', 'area'),
('ft^2',        'square feet',                      'English', 'area'),
('yd^2',        'square yards',                     'English', 'area'),
('cM^2',        'square centimeters',               'Metric',  'area'),
('in^2',        'square inches',                    'English', 'area'),
('acre',        'acres',                            'English', 'area'),
-- area (standard abbreviations)
('1000 m2',     'Thousands of square meters',       'Metric',  'Area'),
('ft2',         'Square feet',                      'English', 'Area'),
('ha',          'Hectares',                         'English', 'Area'),
('km2',         'Square kilometers',                'Metric',  'Area'),
('m2',          'Square meters',                    'Metric',  'Area'),
('mile2',       'Square miles',                     'English', 'Area'),
-- areal volume rate
('cfs/mi2',     'Cfs per square mile',              'English', 'Areal Volume Rate'),
('cms/km2',     'Cms per square kilometer',         'Metric',  'Areal Volume Rate'),
-- volume (legacy-style abbreviations)
('m^3',         'cubic meter',                      'Metric',  'volume'),
('mL',          'milliliter',                       'Metric',  'volume'),
('acre*ft',     'acre feet',                        'English', 'volume'),
('floz',        'fluid ounce',                      'English', 'volume'),
('ft^3',        'cubic feet',                       'English', 'volume'),
('ft^3/s',      'cubic feet per second',            'English', 'flow'),
('qt',          'quart',                            'English', 'volume'),
('kL',          'kiloliter',                        'Metric',  'volume'),
('cc',          'cubic centimeter',                 'Metric',  'volume'),
('uL',          'microliter',                       'Metric',  'volume'),
('in^3',        'cubic inches',                     'English', 'volume'),
('pt',          'pint',                             'English', 'volume'),
('gal',         'gallon',                           'English', 'volume'),
('L',           'liter',                            'Metric',  'volume'),
('m^3/s',       'cubic meters per second',          'Metric',  'flow'),
-- volume (standard abbreviations)
('1000 m3',     'Thousands of cubic meters',        'Metric',  'Volume'),
('ac-ft',       'Acre-feet',                        'English', 'Volume'),
('dsf',         'day-second-foot',                  'English', 'Volume'),
('ft3',         'Cubic feet',                       'English', 'Volume'),
('kaf',         'Kiloacre-feet',                    'English', 'Volume'),
('kgal',        'Kilogallons',                      'English', 'Volume'),
('km3',         'Cubic kilometers',                 'Metric',  'Volume'),
('m3',          'Cubic meters',                     'Metric',  'Volume'),
('M^3',         'Cubic meters',                     'Metric',  'Volume'),
('mgal',        'Millions of gallons',              'English', 'Volume'),
('mile3',       'Cubic miles',                      'English', 'Volume'),
-- volume rate
('cms',         'Cubic meters per second',          'Metric',  'Volume Rate'),
('cfs',         'cubic feet per second',            'English', 'flow'),
('gpm',         'Gallons per minute',               'English', 'Volume Rate'),
('kcfs',        'Kilo-cubic feet per second',       'English', 'Volume Rate'),
('mgd',         'Millions of gallons per day',      'English', 'Volume Rate'),
-- mass
('mt',          'Metric ton',                       'Metric',  'mass'),
('ton',         'tons',                             'English', 'mass'),
('mG',          'milligrams',                       'Metric',  'mass'),
('lb',          'pounds',                           'English', 'mass'),
('kG',          'kilograms',                        'Metric',  'mass'),
('oz',          'ounces',                           'English', 'mass'),
('G',           'grams',                            'Metric',  'mass'),
('uG',          'micrograms',                       'Metric',  'mass'),
-- ratio
('ppt',         'parts per thousand',               'univ',    'ratio'),
('ppm',         'parts per million',                'univ',    'ratio'),
('pct',         'percent',                          'univ',    'ratio'),
('%',           'percent',                          'univ',    'ratio'),
-- length (legacy-style abbreviations)
('mM',          'millimeters',                      'Metric',  'length'),
('mi',          'miles',                            'English', 'length'),
('ft',          'feet',                             'English', 'length'),
('yd',          'yards',                            'English', 'length'),
('kM',          'kilometers',                       'Metric',  'length'),
('M',           'meters',                           'Metric',  'length'),
('nmi',         'nautical miles',                   'English', 'length'),
('cM',          'centimeters',                      'Metric',  'length'),
('in',          'inches',                           'English', 'length'),
('uM',          'micrometers',                      'Metric',  'length'),
-- length (standard abbreviations)
('cm',          'Centimeters',                      'Metric',  'Length'),
('km',          'Kilometers',                       'Metric',  'Length'),
('m',           'Meters',                           'Metric',  'Length'),
('mm',          'Millimeters',                      'Metric',  'Length'),
-- velocity (legacy-style abbreviations)
('ft/s',        'feet per second',                  'English', 'velocity'),
('yd/s',        'yards per second',                 'English', 'velocity'),
('mph',         'miles per hour',                   'English', 'velocity'),
('cM/s',        'centimeters per second',           'Metric',  'velocity'),
('kM/hr',       'kilometers per hour',              'Metric',  'velocity'),
('kph',         'kilometers per hour',              'Metric',  'velocity'),
('knots',       'nautical miles per hour',          'Metric',  'velocity'),
('in/s',        'inches per second',                'English', 'velocity'),
('M/s',         'meters per second',                'Metric',  'velocity'),
('mM/s',        'millimeters per second',           'Metric',  'velocity'),
('mi/hr',       'miles per hour',                   'English', 'velocity'),
-- linear speed (standard abbreviations)
('in/day',      'Inches per day',                   'English', 'Linear Speed'),
('in/hr',       'Inches per hour',                  'English', 'Linear Speed'),
('m/s',         'Meters per second',                'Metric',  'Linear Speed'),
('mm/day',      'Millimeters per day',              'Metric',  'Linear Speed'),
('mm/hr',       'Millimeters per hour',             'Metric',  'Linear Speed'),
-- time
('min',         'minutes',                          'univ',    'time'),
('week',        'weeks',                            'univ',    'time'),
('sec',         'second',                           'univ',    'time'),
('day',         'days',                             'univ',    'time'),
('hr',          'hours',                            'univ',    'time'),
-- pressure (legacy-style abbreviations)
('kpa',         'kilopascals',                      'Metric',  'pressure'),
('inHg',        'inches of mercury',                'English', 'pressure'),
('mmHg',        'millimeters of mercury',           'Metric',  'pressure'),
('pa',          'pascals',                          'Metric',  'pressure'),
('psi',         'pounds per square inch',           'English', 'pressure'),
('atm',         'atmospheres',                      'Metric',  'pressure'),
('bar',         'bars',                             'Metric',  'pressure'),
('mbar',        'millibars',                        'Metric',  'pressure'),
-- pressure (standard abbreviations)
('in-hg',       'Inches of mercury',                'English', 'Pressure'),
('kPa',         'Kilopascals',                      'Metric',  'Pressure'),
('mb',          'Millibars',                        'Metric',  'Pressure'),
('mm-hg',       'Millimeters of mercury',           'Metric',  'Pressure'),
-- power (legacy-style)
('ft*lbf/s',    'foot-pounds per second',           'English', 'power'),
('kW',          'kilowatts',                        'Metric',  'power'),
('W',           'watts',                            'Metric',  'power'),
('hp',          'horsepower',                       'English', 'power'),
-- power (standard abbreviations)
('GW',          'Gigawatts',                        'univ',    'Power'),
('MW',          'Megawatts',                        'univ',    'Power'),
('TW',          'Terawatts',                        'univ',    'Power'),
-- energy (legacy-style)
('erg',         'ergs',                             'English', 'energy'),
('btu',         'british thermal unit',             'English', 'energy'),
('kj',          'kilojoules',                       'Metric',  'energy'),
('j',           'joules',                           'Metric',  'energy'),
('kcal',        'Kilocalories',                     'English', 'energy'),
('cal',         'Calories',                         'English', 'energy'),
-- energy (standard abbreviations)
('GWh',         'Gigawatt-hours',                   'univ',    'Energy'),
('kWh',         'Kilowatt-hours',                   'univ',    'Energy'),
('MWh',         'Megawatt-hours',                   'univ',    'Energy'),
('TWh',         'Terawatt-hour',                    'univ',    'Energy'),
('Wh',          'Watt-hours',                       'univ',    'Energy'),
-- conductance
('uMHOs',       'MicroMHOs per centimeter',         'metric',  'conductance'),
('uMHO',        'MicroMHOs per centimeter',         'metric',  'conductance'),
('uMHOs/cm',    'MicroMHOs per centimeter',         'metric',  'conductance'),
('mho',         'Mhos',                             'univ',    'Conductance'),
('S',           'Siemens',                          'univ',    'Conductance'),
('umho',        'Micro-mhos',                       'univ',    'Conductance'),
('uS',          'Micro-Siemens',                    'univ',    'Conductance'),
('uS/cm',       'Micro Siemens per Centimeter',     'Metric',  'Conductance'),
-- conductivity
('umho/cm',     'Micro-mhos per centimeter',        'univ',    'Conductivity'),
-- voltage / electromotive potential
('V',           'volts',                            'Metric',  'voltage'),
('volt',        'Volts',                            'Metric',  'Electromotive Potential'),
-- temperature (legacy-style)
('degK',        'degrees Kelvin',                   'Metric',  'temperature'),
('degF',        'degrees Fahrenheit',               'English', 'temperature'),
('degC',        'degrees Celsius',                  'Metric',  'temperature'),
-- temperature (standard abbreviations)
('C',           'Centigrade',                       'Metric',  'Temperature'),
('F',           'Fahrenheit',                       'English', 'Temperature'),
-- concentration (legacy-style)
('g/L',         'grams per liter',                  'Metric',  'concentration'),
('mG/L',        'milligrams per liter',             'Metric',  'concentration'),
('uG/L',        'micrograms per liter',             'Metric',  'concentration'),
-- concentration (standard abbreviations)
('g/l',         'Grams per liter',                  'Metric',  'Mass Concentration'),
('gm/cm3',      'Grams per cubic centimeters',      'Metric',  'Mass Concentration'),
('mg/l',        'Milligrams per liter',             'Metric',  'Mass Concentration'),
-- acidity / pH
('pH',          'pH',                               'univ',    'acidity'),
('su',          'Standard pH units',                'univ',    'pH'),
-- count
('count',       'counts',                           'univ',    'count'),
('unit',        'Count',                            'univ',    'Count'),
-- irradiance / irradiation
('langley/min', 'Langley per minute',               'English', 'Irradiance'),
('W/m2',        'Watts per square meter',           'Metric',  'Irradiance'),
('J/m2',        'Joules per square meters',         'Metric',  'Irradiation'),
('langley',     'Langley',                          'English', 'Irradiation'),
-- phase change rate index
('in/deg-day',  'Inches per degree-day',            'English', 'Phase Change Rate Index'),
('mm/deg-day',  'Millimeters per degree-day',       'Metric',  'Phase Change Rate Index'),
-- angle
('deg',         'Degrees',                          'univ',    'angle'),
('deg*10',      'Tens of Degrees',                  'univ',    'angle'),
('rev',         'Revolution',                       'univ',    'Angle'),
-- angular speed
('rpm',         'Revolutions per minute',           'univ',    'Angular Speed'),
-- turbidity
('FNU',         'Formazin Nephelometric Unit',      'univ',    'Turbidity'),
('JTU',         'Jackson Turbitiy Unit',            'univ',    'Turbidity'),
('NTU',         'Nephelometric Turbidity Unit',     'univ',    'Turbidity'),
-- currency
('$',           'Dollars',                          'univ',    'Currency')
ON CONFLICT (UNITABBR) DO NOTHING;

-- ============================================================
-- UNIT CONVERTERS
-- Note: 'none' algorithm = pass-through alias (coefficients null).
--       'linear' algorithm: Y = A*X + B.
-- ============================================================

-- Angle
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'deg',       'deg*10',    'linear', 0.1,                   0.0,  NULL, NULL, NULL, NULL);

-- Conductance / Conductivity aliases
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'uMHOs',     'uMHOs/cm',  'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'uMHOs',     'uMHO',      'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'uMHO',      'umho',      'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'uMHOs/cm',  'umho/cm',   'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'umho',      'mho',       'linear', 0.000001,              0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mho',       'S',         'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'umho',      'uS',        'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'uS/cm',     'umho/cm',   'none',   NULL,                  NULL, NULL, NULL, NULL, NULL);

-- Concentration
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'mG/L',      'uG/L',      'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'g/L',       'mG/L',      'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mG/L',      'ppm',       'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'g/L',       'g/l',       'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mG/L',      'mg/l',      'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'g/l',       'gm/cm3',    'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL);

-- Count
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'count',     'unit',      'none',   NULL,                  NULL, NULL, NULL, NULL, NULL);

-- Energy
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'btu',       'j',         'linear', 1055.056,              0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kcal',      'cal',       'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'cal',       'j',         'linear', 4.184,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kj',        'j',         'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'j',         'erg',       'linear', 1.0E7,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kj',        'Wh',        'linear', 0.2777778,             0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'Wh',        'kWh',       'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kWh',       'MWh',       'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'MWh',       'GWh',       'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'GWh',       'TWh',       'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL);

-- Flow
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'cfs',       'ft^3/s',    'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'm^3/s',     'cfs',       'linear', 35.31466247,           0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'm^3/s',     'cms',       'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kcfs',      'cfs',       'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL);

-- Force
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'lbf',       'N',         'linear', 4.448222,              0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'dyn',       'N',         'linear', 1.0E-5,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'lbf',       'lb',        'none',   NULL,                  NULL, NULL, NULL, NULL, NULL);

-- Irradiance
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'W/m2',      'langley/min','linear', 0.001434034416826004, 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'langley/min','W/m2',     'linear', 697.3333333333333,     0.0,  NULL, NULL, NULL, NULL);

-- Length (legacy to standard aliases)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'M',         'm',         'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'cM',        'cm',        'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kM',        'km',        'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mM',        'mm',        'none',   NULL,                  NULL, NULL, NULL, NULL, NULL);

-- Length (conversions)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'ft',        'm',         'linear', 0.3048,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ft',        'in',        'linear', 12.0,                  0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mi',        'ft',        'linear', 5280.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mi',        'm',         'linear', 1609.344,              0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'yd',        'ft',        'linear', 3.0,                   0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'nmi',       'm',         'linear', 1852.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'm',         'cm',        'linear', 100.0,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'm',         'mm',        'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'm',         'km',        'linear', 0.0010,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'm',         'um',        'linear', 1000000.0,             0.0,  NULL, NULL, NULL, NULL);

-- Mass
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'G',         'mG',        'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mG',        'uG',        'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kG',        'G',         'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mt',        'G',         'linear', 1000000.0,             0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'lb',        'G',         'linear', 453.59237,             0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'lb',        'oz',        'linear', 16.0,                  0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ton',       'lb',        'linear', 2000.0,                0.0,  NULL, NULL, NULL, NULL);

-- pH
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'pH',        'su',        'none',   NULL,                  NULL, NULL, NULL, NULL, NULL);

-- Power
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'ft*lbf/s',  'W',         'linear', 1.355818,              0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'hp',        'W',         'linear', 746.0,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kW',        'W',         'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kW',        'MW',        'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'MW',        'GW',        'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'GW',        'TW',        'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL);

-- Pressure (conversions)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'kpa',       'pa',        'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'inHg',      'pa',        'linear', 3386.38,               0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mmHg',      'pa',        'linear', 133.3224,              0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'psi',       'pa',        'linear', 6874.75729,            0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'atm',       'pa',        'linear', 101325.0,              0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'bar',       'pa',        'linear', 100000.0,              0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kpa',       'mbar',      'linear', 10.0,                  0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mbar',      'kpa',       'linear', 0.10,                  0.0,  NULL, NULL, NULL, NULL);

-- Pressure (legacy to standard aliases)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'inHg',      'in-hg',     'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kpa',       'kPa',       'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mbar',      'mb',        'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mmHg',      'mm-hg',     'none',   NULL,                  NULL, NULL, NULL, NULL, NULL);

-- Ratio
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), '%',         'pct',       'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ppt',       'ppm',       'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), '%',         'ppt',       'linear', 10.0,                  0.0,  NULL, NULL, NULL, NULL);

-- Temperature
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'degC',      'degF',      'linear', 1.8,                   32.0, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'degC',      'degK',      'linear', 1.0,                   273.15, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'degC',      'C',         'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'degF',      'F',         'none',   NULL,                  NULL, NULL, NULL, NULL, NULL);

-- Time
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'min',       'sec',       'linear', 60.0,                  0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'hr',        'min',       'linear', 60.0,                  0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'day',       'hr',        'linear', 24.0,                  0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'week',      'day',       'linear', 7.0,                   0.0,  NULL, NULL, NULL, NULL);

-- Velocity (legacy to standard aliases)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'M/s',       'm/s',       'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kM/hr',     'kph',       'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mph',       'mi/hr',     'none',   NULL,                  NULL, NULL, NULL, NULL, NULL);

-- Velocity (conversions)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'ft/s',      'M/s',       'linear', 0.3048,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ft/s',      'mph',       'linear', 0.681818181818,        0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ft/s',      'in/s',      'linear', 12.0,                  0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ft/s',      'in/hr',     'linear', 43200,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'yd/s',      'ft/s',      'linear', 3.0,                   0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'M/s',       'cM/s',      'linear', 100.0,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'cM/s',      'mM/s',      'linear', 10.0,                  0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mM/s',      'mm/hr',     'linear', 3600,                  0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mm/hr',     'mm/day',    'linear', 24,                    0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'in/hr',     'in/day',    'linear', 24,                    0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mph',       'kM/hr',     'linear', 1.609344,              0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mph',       'knots',     'linear', 0.8689762419006,       0.0,  NULL, NULL, NULL, NULL);

-- Voltage
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'V',         'volt',      'none',   NULL,                  NULL, NULL, NULL, NULL, NULL);

-- Volume (legacy to standard aliases)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'mL',        'cc',        'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ft^3',      'ft3',       'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'acre*ft',   'ac-ft',     'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'M^3',       'm3',        'none',   NULL,                  NULL, NULL, NULL, NULL, NULL);

-- Volume (conversions)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'gal',       'in^3',      'linear', 231.0,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'gal',       'qt',        'linear', 4.0,                   0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'gal',       'L',         'linear', 3.785412,              0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'gal',       'kgal',      'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'gal',       'mgal',      'linear', 0.000001,              0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'qt',        'pt',        'linear', 2.0,                   0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'pt',        'floz',      'linear', 16.0,                  0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'L',         'mL',        'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mL',        'uL',        'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kL',        'L',         'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'm^3',       'L',         'linear', 1000.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ft^3',      'in^3',      'linear', 1728.0,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'acre*ft',   'ft^3',      'linear', 43560.0,               0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ac-ft',     'm3',        'linear', 1233.4818,             0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ac-ft',     'kaf',       'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'm3',        '1000 m3',   'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'm3',        'km3',       'linear', 1.0E-9,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'km3',       'mile3',     'linear', 0.2399128,             0.0,  NULL, NULL, NULL, NULL);

-- Area (legacy to standard aliases)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'ft^2',      'ft2',       'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kM^2',      'km2',       'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'M^2',       'm2',        'none',   NULL,                  NULL, NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mi^2',      'mile2',     'none',   NULL,                  NULL, NULL, NULL, NULL, NULL);

-- Area (conversions)
INSERT INTO UNITCONVERTER (ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F) VALUES
(nextval('UnitConverterIdSeq'), 'cM^2',      'M^2',       'linear', 1.0E-4,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mM^2',      'M^2',       'linear', 1.0E-6,                0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'kM^2',      'M^2',       'linear', 1000000.0,             0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'yd^2',      'ft^2',      'linear', 9.0,                   0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ft^2',      'in^2',      'linear', 144.0,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'ft^2',      'M^2',       'linear', 0.09290304,            0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mi^2',      'ft^2',      'linear', 2.78784E7,             0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'mi^2',      'acre',      'linear', 640.0,                 0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'acre',      'ft^2',      'linear', 43560.0,               0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'acre',      'ha',        'linear', 0.4046856,             0.0,  NULL, NULL, NULL, NULL),
(nextval('UnitConverterIdSeq'), 'm2',        '1000 m2',   'linear', 0.001,                 0.0,  NULL, NULL, NULL, NULL);
