benchmark=/home/guanpu/Desktop/DFix-Public/public/FFix/input
foo=$benchmark/foo
zk1653=$benchmark/zk-1653
hb3596=$benchmark/hb-3596
hb2611=$benchmark/hb-2611
hb10090=$benchmark/hb-10090

walalib=../../lib/WALA 
classpath=$walalib/com.ibm.wala.core/target/classes:$walalib/com.ibm.wala.util/target/classes:$walalib/com.ibm.wala.shrike/target/classes:$walalib/com.ibm.wala.core.testdata/target/classes:$walalib/com.ibm.wala.core.tests/target/classes:.


#time java -Xmx10G -cp $classpath uchicago.ffix.transaction $zk1653
#time java -Xmx10G -cp $classpath uchicago.ffix.transaction $hb3596
time java -Xmx10G -cp $classpath uchicago.ffix.transaction $hb2611
#time java -Xmx10G -cp $classpath uchicago.ffix.transaction $hb10090

