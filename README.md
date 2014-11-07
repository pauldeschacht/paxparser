paxparser
=========
#
# referential data
#
lein run --input ~/2014/11/POR/2014/11/ori_por_public.csv --output ~/2014/11/POR/2014/11/airports.csv --hint airport
lein run --input ~/2014/11/POR/2014/11/ori_airlines.csv --output ~/2014/11/POR/2014/11/airlines.csv
 lein run --input ~/2014/11/POR/2014/11/ori_countries.csv --output ~/2014/11/POR/2014/11/countries.csv
lein run --input ~/2014/11/POR/2014/11/ori_por_public.csv --output ~/2014/11/POR/2014/11/cities.csv --hint city 
#
# some examples
#
lein run --input /home/pdeschacht/dev/airtraffic2/pax/download/2014/08/MX/2014/05/mx.xlsx --output /home/pdeschacht/dev/airtraffic2/pax/download/2014/08/MX/2014/05/mx_int.csv --sheet REGINT
lein run --input /home/pdeschacht/pax/2014/09/ICAO_DATAPLUS/2014/09/ICAO.xls --output /home/pdeschacht/pax/2014/09/ICAO_DATAPLUS/2014/09/ICAO.csv --sheet "Sheet1" 
#
# Mind the exclamation mark in the hint (to differentiate with yyyy used in the input and output folder
#
lein run --input ~/2014/11/Albatross/2013/Albatross.csv --output ~/2014/11/Albatross/2013/01_2013_importAirport.csv --hint 2013! 
lein run --input ~/2014/11/Albatross/2013/Albatross.csv --output ~/2014/11/Albatross/2013/01_2012_importAirport.csv --hint 2012!

lein run --input ~/2014/11/DHMI/2014/10/2014_10_dhmi.xlsx --output ~/2014/11/DHMI/2014/10/dhmi.csv --sheet "(?i)yolcu" 
lein run --input ~/2014/11/MX/2014/09/SASE_Septiembre_2014.xlsx --output ~/2014/11/MX/2014/09/mx_dom.csv --sheet "(?i)REG[ ]*NAC" --hint regnac
lein run --input ~/2014/11/MX/2014/09/SASE_Septiembre_2014.xlsx --output ~/2014/11/MX/2014/09/mx_int.csv --sheet "(?i)REG[ ]*INT" --hint regint
