benchmark=/home/haopliu/DFFix/FFix/code/FFix/input
foo=$benchmark/foo
hb10090=$benchmark/hb-10090
ca5393=$benchmark/ca-5393
walalib=/mnt/storage/packages/wala/WALA-R_1.3.5
classpath=$walalib/com.ibm.wala.core/bin:$walalib/com.ibm.wala.util/bin:$walalib/com.ibm.wala.shrike/bin:$walalib/com.ibm.wala.core.testdata/bin:$walalib/com.ibm.wala.core.tests/bin:.
#echo $toy

#time java -Xmx10G -cp $classpath uchicago.ffix.nodedump $ca5393
time java -Xmx10G -cp $classpath uchicago.ffix.nodedump $foo
#time java -Xmx2G -cp $classpath uchicago.dfix.ssat $toy

