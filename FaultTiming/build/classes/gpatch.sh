benchmark=/home/guanpu/Desktop/DFix-Public/public/FFix/input
foo=$benchmark/foo
zk1653=$benchmark/zk-1653
hb2611=$benchmark/hb-2611
hb10090=$benchmark/hb-10090

walalib=../../lib/WALA
classpath=$walalib/com.ibm.wala.core/target/classes:$walalib/com.ibm.wala.util/target/classes:$walalib/com.ibm.wala.shrike/target/classes:$walalib/com.ibm.wala.core.testdata/target/classes:$walalib/com.ibm.wala.core.tests/target/classes:$walalib/../guava-19.0.jar:$walalib/../javaparser-core-3.2.4-SNAPSHOT.jar:$walalib/../javaparser-core-generators-3.2.4-SNAPSHOT.jar:$walalib/../javaparser-metamodel-generator-3.2.4-SNAPSHOT.jar:.

time java -Xmx10G -cp $classpath uchicago.ffix.gppatch $zk1653 > result/p-zk1653
time java -Xmx10G -cp $classpath uchicago.ffix.gppatch $hb2611 > result/p-hb2611 
time java -Xmx10G -cp $classpath uchicago.ffix.gppatch $hb10090 > result/p-hb10090

