benchmark=/home/guanpu/Desktop/DFix-Public/public/FFix/input 
foo=$benchmark/foo
hb10090=$benchmark/hb-10090
ca5393=$benchmark/ca-5393
walalib=../../lib/WALA 
classpath=$walalib/com.ibm.wala.core/target/classes:$walalib/com.ibm.wala.util/target/classes:$walalib/com.ibm.wala.shrike/target/classes:$walalib/com.ibm.wala.core.testdata/target/classes:$walalib/com.ibm.wala.core.tests/target/classes:.
#echo $toy

#time java -Xmx10G -cp $classpath uchicago.ffix.rollback $foo
#time java -Xmx10G -cp $classpath uchicago.ffix.rollback $hb10090
time java -Xmx10G -cp $classpath uchicago.ffix.rollback $ca5393

