
for bcm in *; do
    #echo "ITem " $bcm ";" 
    if [[ -d $bcm ]]; then
	echo $bcm
	cd $bcm
	#cd patch
	
	for pat in *; do
	    #echo $pat
	    if [[ ( $pat == patch* ) && ( -d $pat ) ]]; then
		cd $pat
		echo $pat
		#echo "" > details.patch
		cp /dev/null details.patch
		pwd
		cd src
		for f in *; do
		    echo "diff $f vs ../patch-$f >> ../details.patch"
		    diff -Naur $f ../patch/$f >> ../details.patch
		done 

	        cd ../../
	    fi 
	done 

	cd ../
    fi
    #pwd
done
