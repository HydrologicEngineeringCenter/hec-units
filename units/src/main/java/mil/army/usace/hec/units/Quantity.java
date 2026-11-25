package mil.army.usace.hec.units;

//                                                                                    store      ------Display Units-----
//   CODE   ABSTRACT PARAMETER                  ID             NAME                  UNIT ID      SI       Non-SI         DESCRIPTION
//   ------ ----------------------------------- -----------    --------------------- ---------- ---------- -------------- -----------------------------------------------------------------------------
//    [ 1,    "None",                             "%",           "Percent",            "%",       "%",       "%",           "Ratio expressed as hundredths"                                               ],
public final record Quantity(long code, String dimension, String id, String name, String storeUnit, String siUnit, String usUnit, String Scription) {
    
}
