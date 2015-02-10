import sys,os
import shutil
import gzip
from time import sleep

separator="/"
folder=sys.argv[1]
print "Looking for files in " + folder
for dirpath, dirname, filenames in os.walk(folder):
        for filename in filenames:
                print "Found filename" + filename
                if filename[-2:] != "gz":
                    break
                items=filename.split('_')
                country=items[2][0:2].upper()
                sourcefile = dirpath + separator + filename
                countryfolder = folder + separator + country
                destfile = countryfolder + separator + filename
                if os.path.isdir(countryfolder):
                   shutil.rmtree(countryfolder) 
                print "creating folder " + countryfolder
                os.mkdir(countryfolder)
                print "move file " + sourcefile + " to " + destfile
                shutil.move(sourcefile, destfile)

                print "open compressed file " + destfile
                f=gzip.open(destfile,'rb')
                content=f.read()
                f.close()

                # from path/avia_par_at.tsv.gz to path/01_avia_par_at.tsv
                destfilename = destfile[0:-3]
                print "destfilename = " + destfilename
                base=os.path.basename(destfilename)
                print "base = " + base
                dirname=os.path.dirname(destfile)
                print "dirname = " + dirname
                destfilename=dirname+"/"+"01_" + base 

                print "writing file " + destfilename 
                f=open(destfilename,'w')
                f.write(content)
                f.close()
                
                shutil.copy(destfilename, countryfolder + separator + "02_importAirportPair.tsv")
                
                
                
                
                

