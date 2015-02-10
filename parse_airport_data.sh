#!/bin/sh

CWD=`pwd` 
MONTH="02"
BASE="/home/pdeschacht/2015/$MONTH"

ALBA="Albatross/2014" # Albatross
CLAC="CLAC/2014"
MX="MX/2014" # MX
KAC="KAC/2014/12" # KAC
ANAC="ANAC/2014/12"
BTRE="BTRE/2014/11"
# BTRE2="BTRE/2014/10"
BTSDB1B="BTS/DB1B/2014/Q3"
BTST100="BTS/T100/2014/07"
DHMI="DHMI/2014/12"
EUROSTATAIRPORTS="EUROSTAT/Airports/2015/$MONTH/"
#EUROSTATLEGS="EUROSTAT/Legs/2015/$MONTH"
EUROSTATLEGS="Eurostat/Legs/2015/$MONTH"
FAA="FAA/2013"
INDONESIA="INDONESIA/2014/12"
ICAODATAPLUS="ICAO_DATAPLUS/2015/$MONTH"
JAC="JAC/2014/01"

scrape() {
  cd ~/dev/ta/pax/paxscrape
  lein run --source all --output /home/pdeschacht/2015/$MONTH
}

paxparser() {
  cd ~/dev/paxparser
  #
  # POR 
  #
  rm -rf $BASE/POR/2015/$MONTH/airports.csv
  rm -rf $BASE/POR/2015/$MONTH/airlines.csv
  rm -rf $BASE/POR/2015/$MONTH/countries.csv
  rm -rf $BASE/POR/2015/$MONTH/cities.csv

  lein run --input $BASE/POR/2015/$MONTH/ori_por_public.csv --output $BASE/POR/2015/$MONTH/airports.csv --spec ref-airport-spec
  lein run --input $BASE/POR/2015/$MONTH/ori_airlines.csv   --output $BASE/POR/2015/$MONTH/airlines.csv --spec ref-airline-spec
  lein run --input $BASE/POR/2015/$MONTH/ori_countries.csv  --output $BASE/POR/2015/$MONTH/countries.csv --spec ref-country-spec
  lein run --input $BASE/POR/2015/$MONTH/ori_por_public.csv --output $BASE/POR/2015/$MONTH/cities.csv --spec ref-city-spec
  #
  # Albatross (use old paxparser to convert to old Albatross format - not the pax model)
  #
  rm -rf $BASE/$ALBA/albatross_*.csv

  lein run --input $BASE/$ALBA/raw_albatross.csv --output $BASE/$ALBA/albatross_2008.csv --spec albatross-2008-spec
  lein run --input $BASE/$ALBA/raw_albatross.csv --output $BASE/$ALBA/albatross_2009.csv --spec albatross-2009-spec
  lein run --input $BASE/$ALBA/raw_albatross.csv --output $BASE/$ALBA/albatross_2010.csv --spec albatross-2010-spec
  lein run --input $BASE/$ALBA/raw_albatross.csv --output $BASE/$ALBA/albatross_2011.csv --spec albatross-2011-spec
  lein run --input $BASE/$ALBA/raw_albatross.csv --output $BASE/$ALBA/albatross_2012.csv --spec albatross-2012-spec
  lein run --input $BASE/$ALBA/raw_albatross.csv --output $BASE/$ALBA/albatross_2013.csv --spec albatross-2013-spec
  #
  # ANAC
  #
  rm -rf $BASE/$ANAC/anac.csv

  lein run --input $BASE/$ANAC/raw_anac.xlsx --output $BASE/$ANAC/anac.csv --spec anac-2015-spec --sheet "(?i)passa"
  #
  # DHMI
  #
  rm -rf $BASE/$DHMI/dhmi.csv

  lein run --input $BASE/$DHMI/raw_dhmi.xlsx --output $BASE/$DHMI/dhmi.csv --spec dhmi-spec --sheet "(?i)yolcu"
  # 
  # INDONESIA
  #
  rm -rf $BASE/$INDONESIA/01_importAirport.csv

  lein run --input $BASE/$INDONESIA/raw_indonesia.csv --output $BASE/$INDONESIA/01_importAirport.csv --spec indonesia-spec 
}

pax2csv() {
  cd ~/dev/ta/pax/pax2csv
  #
  # ICAO_DATAPLUS
  # 
  rm -rf $BASE/$ICAODATAPLUS/01_importAirport.csv

  lein run --input $BASE/$ICAODATAPLUS/ICAO_DATAPLUS.xls --output $BASE/$ICAODATAPLUS/01_importAirport.csv --spec icao-dataplus-spec --sheet "(?i)sheet1"
  #
  # MX
  # 
  rm -rf $BASE/$MX/mx_*.csv

  lein run --input $BASE/$MX/raw_mx_airlines.xlsx --output $BASE/$MX/mx_airlines.csv --spec mx-airport-spec --sheet "(?i)PAXREG"
  lein run --input $BASE/$MX/raw_mx_citypair.xlsx --output $BASE/$MX/mx_citypair_dom.csv --spec mx-spec-2014 --sheet "(?i)reg[ ]*nac"
  lein run --input $BASE/$MX/raw_mx_citypair.xlsx --output $BASE/$MX/mx_citypair_int.csv --spec mx-spec-2014 --sheet "(?i)reg[ ]*int"
  #
  # KAC
  #
  rm -rf $BASE/$KAC/kac.csv

  lein run --input $BASE/$KAC/raw_kac.csv --output $BASE/$KAC/kac.csv --spec kac-spec
}


enrich() {
  #
  # enrich the data
  #
  cd ~/dev/ta/pax/paxenrich

  echo $MONTH

  cp synonyms.csv $BASE/POR/2015/$MONTH

  lein run --ref $BASE/POR/2015/$MONTH --input $BASE/MX/2014/$MXMONTH/mx_citypair_dom.csv --output $BASE/MX/2014/$MXMONTH/01_importCityPair.csv
  lein run --ref $BASE/POR/2015/$MONTH --input $BASE/MX/2014/$MXMONTH/mx_citypair_int.csv --output $BASE/MX/2014/$MXMONTH/01_importCityPair.csv

  lein run --ref $BASE/POR/2015/$MONTH --input $BASE/$KAC/kac.csv --output $BASE/$KAC/01_importAirportPairAirline.csv
}


prepare_folder() {
  cd $BASE

  mv $BASE/ACI/international $BASE/ACI/monthly_international
  mv $BASE/ACI/worldwide     $BASE/ACI/monthly_worldwide

  mv $BASE/$ALBA/albatross_2008.csv $BASE/$ALBA/01_2008_importAirport.csv 
  mv $BASE/$ALBA/albatross_2009.csv $BASE/$ALBA/01_2009_importAirport.csv 
  mv $BASE/$ALBA/albatross_2010.csv $BASE/$ALBA/01_2010_importAirport.csv 
  mv $BASE/$ALBA/albatross_2011.csv $BASE/$ALBA/01_2011_importAirport.csv 
  mv $BASE/$ALBA/albatross_2012.csv $BASE/$ALBA/01_2012_importAirport.csv 
  mv $BASE/$ALBA/albatross_2013.csv $BASE/$ALBA/01_2013_importAirport.csv 

  mv $BASE/$ANAC/anac.csv $BASE/$ANAC/01_importAirport.csv
  mv $BASE/ANAC $BASE/ANAC_Brasil_2

  mv $BASE/$BTRE/raw_btre_int.xlsx $BASE/$BTRE/00_International_airline_activity.xlsx
  mv $BASE/$BTRE/raw_btre_dom.xlsx $BASE/$BTRE/00_Domestic_airline_activity.xlsx
#  mv $BASE/$BTRE2/raw_btre_int.xlsx $BASE/$BTRE2/00_International_airline_activity.xlsx
#  mv $BASE/$BTRE2/raw_btre_dom.xlsx $BASE/$BTRE2/00_Domestic_airline_activity.xlsx
  mv $BASE/BTRE $BASE/BTRE_Australia

  cd $BASE/$BTSDB1B
  unzip raw_bts_db1b.zip
  mv Origin_and_Destination*.csv 01_importAirportPair.csv

  cd $BASE/$BTST100
  unzip raw_bts_t100.zip
  mv *CARRIER*.csv 02_importAirportPairAirline.csv

  mv $BASE/$BTS/T100 $BASE/$BTS/T100Segment

  cd $BASE/$CLAC
  unrar -x raw_clac.rar
  mv *.xls CLAC-2014.xls
  mv $BASE/CLAC $BASE/CLAC_LatinAmerica

  mv $BASE/DESTATIS $BASE/Destatis
  mv $BASE/$DHMI/dhmi.csv $BASE/$DHMI/01_importAirport.csv


  cd $BASE/$EUROSTATAIRPORTS
  gunzip $BASE/$EUROSTATAIRPORTS/avia_paoa.tsv.gz
  mv $BASE/$EUROSTATAIRPORTS/*.tsv $BASE/$EUROSTATAIRPORTS/02_importAirport.tsv

  cd $BASE/$EUROSTATLEGS/
  python $CWD/eurostat_extract_legs.py ./
  
  cd $BASE
  mv $BASE/EUROSTAT $BASE/Eurostat

  cd $BASE

  mkdir -p $BASE/FAA/2015/$MONTH
  mv $BASE/$FAA $BASE/FAA/2015/$MONTH

  cp $BASE/$JAC/raw_jac_dom_citypairairline.xlsx $BASE/$JAC/01_citypairairline_dom.xlsx
  cp $BASE/$JAC/raw_jac_int_citypairairline.xlsx $BASE/$JAC/01_citypairairline_int.xlsx
  mv $BASE/JAC $BASE/JAC_Chile
 
}
# scrape
# paxparser
# pax2csv
# enrich
prepare_folder
