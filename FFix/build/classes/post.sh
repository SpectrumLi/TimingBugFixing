benchmark=/home/haopliu/DFFix/FFix/code/FFix/input
foo=$benchmark/foo
hb10090=$benchmark/hb-10090
hbnew=$benchmark/hb-new
walalib=/mnt/storage/packages/wala/WALA-R_1.3.5
classpath=$walalib/com.ibm.wala.core/bin:$walalib/com.ibm.wala.util/bin:$walalib/com.ibm.wala.shrike/bin:$walalib/com.ibm.wala.core.testdata/bin:$walalib/com.ibm.wala.core.tests/bin:.
#echo $toy

time java -Xmx10G -cp $classpath uchicago.ffix.post $hbnew
#time java -Xmx10G -cp $classpath uchicago.ffix.post $hb10090

