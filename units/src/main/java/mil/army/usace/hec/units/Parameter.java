package mil.army.usace.hec.units;

//                                                                                    store      ------Display Units-----
//   CODE   ABSTRACT PARAMETER                  ID             NAME                  UNIT ID      SI       Non-SI         DESCRIPTION
//   ------ ----------------------------------- -----------    --------------------- ---------- ---------- -------------- -----------------------------------------------------------------------------
//    [ 1,    "None",                             "%",           "Percent",            "%",       "%",       "%",           "Ratio expressed as hundredths"                                               ],
public final record Parameter(long code, String parameter, String id, String name, String storeUnit, String siUnit, String usUnit, String Scription) {
    
}
