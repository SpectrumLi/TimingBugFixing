benchmark=/home/haopliu/DFFix/FFix/code/FFix/input
zk1653=$benchmark/zk-1653
walalib=/mnt/storage/packages/wala/WALA-R_1.3.5
classpath=$walalib/com.ibm.wala.core/bin:$walalib/com.ibm.wala.util/bin:$walalib/com.ibm.wala.shrike/bin:$walalib/com.ibm.wala.core.testdata/bin:$walalib/com.ibm.wala.core.tests/bin:.
#echo $toy

time java -Xmx10G -cp $classpath uchicago.ffix.toy $zk1653
#time java -Xmx10G -cp $classpath uchicago.dfix.ssat $mr4637
#time java -Xmx10G -cp $classpath uchicago.dfix.ssat $mr3274
#time java -Xmx2G -cp $classpath uchicago.dfix.ssat $zk1144
#time java -Xmx2G -cp $classpath uchicago.dfix.ssat $zk1270
#time java -Xmx2G -cp $classpath uchicago.dfix.ssat $toy

